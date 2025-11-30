package com.five9th.motionlogger.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.five9th.motionlogger.data.SensorsRepoImpl
import com.five9th.motionlogger.domain.entities.SensorsInfo
import com.five9th.motionlogger.domain.usecases.GetSensorsInfoUseCase

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val sensorsRepo = SensorsRepoImpl(app) // use DI (TODO)

    private val getSensorsInfoUseCase = GetSensorsInfoUseCase(sensorsRepo)

    private val _sensorsInfoLD = MutableLiveData<SensorsInfo>()
    val sensorsInfoLD: LiveData<SensorsInfo>
        get() = _sensorsInfoLD

    fun getSensorsInfo() {
        _sensorsInfoLD.value = getSensorsInfoUseCase.getSensorsInfo()
    }
}