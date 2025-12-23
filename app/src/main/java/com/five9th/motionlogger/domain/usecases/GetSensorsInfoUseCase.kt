package com.five9th.motionlogger.domain.usecases

import com.five9th.motionlogger.domain.entities.SensorsInfo
import javax.inject.Inject

class GetSensorsInfoUseCase @Inject constructor (
    private val repo: SensorsRepo
) {
    operator fun invoke(): SensorsInfo {
        return repo.getSensorsInfo()
    }
}