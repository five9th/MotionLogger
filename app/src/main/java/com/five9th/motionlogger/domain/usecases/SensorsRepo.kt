package com.five9th.motionlogger.domain.usecases

import com.five9th.motionlogger.domain.entities.SensorsInfo

interface SensorsRepo {
    fun getSensorsInfo(): SensorsInfo
}