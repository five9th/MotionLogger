package com.five9th.motionlogger.data

import android.app.Application
import com.five9th.motionlogger.domain.entities.SensorsInfo
import com.five9th.motionlogger.domain.usecases.SensorsRepo

class SensorsRepoImpl(private val app: Application) : SensorsRepo {

    private val sensors = SensorsBundle(app)

    override fun getSensorsInfo(): SensorsInfo {
        return sensors.getSensorsInfo()
    }
}