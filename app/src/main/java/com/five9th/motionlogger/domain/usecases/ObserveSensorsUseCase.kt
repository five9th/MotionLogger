package com.five9th.motionlogger.domain.usecases

import com.five9th.motionlogger.domain.entities.SensorSample
import com.five9th.motionlogger.domain.repos.SensorsRepo
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSensorsUseCase @Inject constructor (
    private val repo: SensorsRepo
) {
    operator fun invoke(): Flow<SensorSample> = repo.getFlow()
}