package com.five9th.motionlogger.domain.usecases

import com.five9th.motionlogger.domain.entities.SensorSample
import com.five9th.motionlogger.domain.entities.SensorsInfo
import kotlinx.coroutines.flow.StateFlow

interface SensorsRepo {
    fun getSensorsInfo(): SensorsInfo
    fun start()
    fun stop()
    fun getFlow(): kotlinx.coroutines.flow.Flow<SensorSample>
    val isCollecting: StateFlow<Boolean>
}