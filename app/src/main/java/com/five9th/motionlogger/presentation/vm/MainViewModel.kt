package com.five9th.motionlogger.presentation.vm

import android.app.Application
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.five9th.motionlogger.domain.entities.SensorSchema
import com.five9th.motionlogger.domain.entities.SensorsInfo
import com.five9th.motionlogger.domain.entities.SessionInfo
import com.five9th.motionlogger.domain.usecases.GetSensorsInfoUseCase
import com.five9th.motionlogger.domain.usecases.ObserveSessionListUseCase
import com.five9th.motionlogger.domain.usecases.ReloadSavedSessionsUseCase
import com.five9th.motionlogger.domain.usecases.schema.GetDefaultSchemaUseCase
import com.five9th.motionlogger.domain.usecases.schema.PutSchemaUseCase
import com.five9th.motionlogger.domain.usecases.schema.SetCurrentSchemaUseCase
import com.five9th.motionlogger.presentation.uimodel.CollectionStats
import com.five9th.motionlogger.presentation.service.SensorCollectionService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// TODO: disable the start btn if keyword is invalid and disable the keyword input if collection is in progress
// TODO: load samples to current_session.csv every few minutes

@HiltViewModel
class MainViewModel @Inject constructor (
    private val getSensorsInfoUseCase: GetSensorsInfoUseCase,
    private val reloadSavedSessionsUseCase: ReloadSavedSessionsUseCase,
    private val observeSessionListUseCase: ObserveSessionListUseCase,

    private val putSchemaUseCase: PutSchemaUseCase,
    private val setCurrentSchemaUseCase: SetCurrentSchemaUseCase,
    private val getDefaultSchemaUseCase: GetDefaultSchemaUseCase,

    realTimeRunner: RealTimeRecognitionRunner,
    application: Application
) : AndroidViewModel(application) {

    private val tag = "MainViewModel"

    // TODO: maybe ViewModel should not interact with a service directly
    private var service: SensorCollectionService? = null
    private var isBound = false

    // ---- UI State ----
    private val _sensorsInfoSF = MutableStateFlow<SensorsInfo?>(null)
    val sensorsInfoSF = _sensorsInfoSF.asStateFlow()

    private val _isCollectingSF = MutableStateFlow(false)
    val isCollectingSF = _isCollectingSF.asStateFlow()

    private val _collectionStatsSF = MutableStateFlow(
        CollectionStats(0L, 0)
    )
    val collectionStatsSF = _collectionStatsSF.asStateFlow()

    private val _sessionListSF = MutableStateFlow<List<SessionInfo>>(listOf())
    val sessionListSF: StateFlow<List<SessionInfo>> = _sessionListSF.asStateFlow()

    /** Activity order: ["dws", "ups", "wlk", "jog", "std", "sit"] */
    val currentActivityScoresSF = realTimeRunner.currentScoresSF

    val realTimeRecognitionMsgSF = realTimeRunner.messageSF


    init {
        viewModelScope.launch {
            observeSessionListUseCase().collect { list ->
                // maintain order by session id
                _sessionListSF.value = list.sortedBy { session -> session.id }
            }
        }
    }

    // ---- Bind to Service ----
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as SensorCollectionService.LocalBinder
            service = localBinder.getService()
            isBound = true

            collectServiceFlows()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            service = null
        }
    }

    // after serviceConnection got initialized
    init {
        // check if the service is running (in case activity was finished and recreated)
        bindToService()
    }

    private fun collectServiceFlows() {
        if (!isBound) return

        viewModelScope.launch {
            service?.isCollectingSF?.collect {
                _isCollectingSF.value = it
            }
        }

        viewModelScope.launch {
            service?.collectionStatsSF?.collect {
                _collectionStatsSF.value = it
            }
        }
    }

    /** If Service is already bound or doesn't exist - nothing happens */
    private fun bindToService() {
        if (!isBound) {
            val intent = SensorCollectionService.newIntent(application)
            application.bindService(intent, serviceConnection, 0)
        }
    }

    override fun onCleared() {
        if (isBound) {
            application.unbindService(serviceConnection)
            isBound = false
        }
        super.onCleared()
    }


    // ---- Real-time HAR ----
    init {
        realTimeRunner.start()

        // debug
        viewModelScope.launch {
            realTimeRunner.currentScoresSF.collect {
                Log.d(tag, "scores: $it")
            }
        }
    }

    // ---- schema ----
    private fun getSchemaVer(): Int {
        val schema = tryGetSchemaFromUser()

        val schemaVer = if (schema != null) {
            putSchemaUseCase(schema)
            setCurrentSchemaUseCase(schema)

            schema.version
        }
        else {
            val defSchema = getDefaultSchemaUseCase()
            setCurrentSchemaUseCase(defSchema)

            0 // default schema
        }

        return schemaVer
    }

    private fun tryGetSchemaFromUser(): SensorSchema? {
        //todo
        return null
    }


    // ---- Public methods for Activity ----

    fun getSensorsInfo() {
        _sensorsInfoSF.value = getSensorsInfoUseCase()
    }

    fun reloadSavedSessions() {
        viewModelScope.launch {
            reloadSavedSessionsUseCase()
        }
    }

    fun startCollect() {
        if (isCollectingSF.value) return

        val schemaVer = getSchemaVer()
        Log.d(tag, "Got schema version: $schemaVer")

        // Start as foreground service
        val intent = SensorCollectionService.newIntentStart(application, schemaVer)
        application.startForegroundService(intent)

        // Also bind to get updates
        bindToService()
    }

    fun stopCollectAndSave(sessionKeyWord: String) {
        if (!isCollectingSF.value || !isBound) return
        service?.stopCollectAndSave(sessionKeyWord)
    }
}