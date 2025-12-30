package com.five9th.motionlogger.presentation.vm

import android.app.Application
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.five9th.motionlogger.domain.entities.SensorsInfo
import com.five9th.motionlogger.domain.entities.SessionInfo
import com.five9th.motionlogger.domain.usecases.GetSensorsInfoUseCase
import com.five9th.motionlogger.domain.usecases.ObserveSessionListUseCase
import com.five9th.motionlogger.domain.usecases.ReloadSavedSessionsUseCase
import com.five9th.motionlogger.presentation.uimodel.CollectionStats
import com.five9th.motionlogger.presentation.service.SensorCollectionService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


// TODO: load samples to current_session.csv every few minutes
// TODO: display list of recorded sessions

@HiltViewModel
class MainViewModel @Inject constructor (
    private val getSensorsInfoUseCase: GetSensorsInfoUseCase,
    private val reloadSavedSessionsUseCase: ReloadSavedSessionsUseCase,
    private val observeSessionListUseCase: ObserveSessionListUseCase,
    application: Application
) : AndroidViewModel(application) {

    private val tag = "MainViewModel"

    // TODO: maintain sessions list: ArrayList<SessionInfo>

    // TODO: maybe ViewModel should not interact with a service directly
    private var service: SensorCollectionService? = null
    private var isBound = false

    // ---- UI State ----
    private val _sensorsInfoLD = MutableLiveData<SensorsInfo>()
    val sensorsInfoLD: LiveData<SensorsInfo> = _sensorsInfoLD

    private val _isCollectingSF = MutableStateFlow(false)
    val isCollectingSF = _isCollectingSF.asStateFlow()

    private val _collectionStatsSF = MutableStateFlow(
        CollectionStats(0L, 0)
    )
    val collectionStatsSF = _collectionStatsSF.asStateFlow()

    val sessionListSF: StateFlow<List<SessionInfo>> = observeSessionListUseCase()

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

    // after serviceConnection got initialized
    init {
        // check if the service is running (in case activity was finished and recreated)
        bindToService()
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


    // ---- Public methods for Activity ----

    fun getSensorsInfo() {
        _sensorsInfoLD.value = getSensorsInfoUseCase()
    }

    fun reloadSavedSessions() {
        viewModelScope.launch {
            reloadSavedSessionsUseCase()
        }
    }

    fun startCollect() {
        if (isCollectingSF.value) return

        // Start as foreground service
        val intent = SensorCollectionService.newIntentStart(application)
        application.startForegroundService(intent)

        // Also bind to get updates
        bindToService()
    }

    fun stopCollect() {
        if (!isCollectingSF.value || !isBound) return
        service?.stopCollectAndSave()
    }
}