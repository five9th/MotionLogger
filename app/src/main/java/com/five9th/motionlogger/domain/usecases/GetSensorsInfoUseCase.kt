package com.five9th.motionlogger.domain.usecases

import com.five9th.motionlogger.domain.entities.SensorsInfo

class GetSensorsInfoUseCase(private val repo: SensorsRepo) {
    fun getSensorsInfo(): SensorsInfo {
        return repo.getSensorsInfo()
    }
}