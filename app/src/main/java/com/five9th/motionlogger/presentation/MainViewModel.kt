package com.five9th.motionlogger.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.five9th.motionlogger.data.SensorsRepoImpl
import com.five9th.motionlogger.domain.entities.SensorSample
import com.five9th.motionlogger.domain.entities.SensorsInfo
import com.five9th.motionlogger.domain.usecases.GetSensorsInfoUseCase
import com.five9th.motionlogger.domain.usecases.ObserveSensorsUseCase
import com.five9th.motionlogger.domain.usecases.StartCollectUseCase
import com.five9th.motionlogger.domain.usecases.StopCollectUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val sensorsRepo = SensorsRepoImpl(app) // use DI (TODO)

    private val getSensorsInfoUseCase = GetSensorsInfoUseCase(sensorsRepo)

    private val observeSensorsUseCase = ObserveSensorsUseCase(sensorsRepo)
    private val startCollectUseCase = StartCollectUseCase(sensorsRepo)
    private val stopCollectUseCase = StopCollectUseCase(sensorsRepo)

    private val _sensorsInfoLD = MutableLiveData<SensorsInfo>()
    val sensorsInfoLD: LiveData<SensorsInfo>
        get() = _sensorsInfoLD

    fun getSensorsInfo() {
        _sensorsInfoLD.value = getSensorsInfoUseCase()
    }

    val sensorDataSF: StateFlow<SensorSample?> = observeSensorsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun startCollect() {
        startCollectUseCase()
    }

    fun stopCollect() {
        stopCollectUseCase()
    }
}