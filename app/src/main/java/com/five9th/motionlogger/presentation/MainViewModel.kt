package com.five9th.motionlogger.presentation

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.five9th.motionlogger.domain.entities.SensorsInfo
import com.five9th.motionlogger.domain.usecases.GetSensorsInfoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


// TODO: load samples to current_session.csv every few minutes
// TODO: display list of recorded sessions
// TODO: add session id

@HiltViewModel
class MainViewModel @Inject constructor (
    private val getSensorsInfoUseCase: GetSensorsInfoUseCase,
    application: Application
) : AndroidViewModel(application) {

    private val tag = "MainViewModel"

    private var service: SensorCollectionService? = null // TODO: deal with this
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

    private fun bindToService() {
        if (!isBound) {
            val intent = SensorCollectionService.newIntent(application)
            application.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onCleared() {
        if (isBound) {
            application.unbindService(serviceConnection)
            isBound = false
        }
        super.onCleared()
    }


    fun getSensorsInfo() {
        _sensorsInfoLD.value = getSensorsInfoUseCase()
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