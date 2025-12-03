package com.five9th.motionlogger.domain.usecases

import com.five9th.motionlogger.domain.entities.SensorSample
import kotlinx.coroutines.flow.Flow

class ObserveSensorsUseCase(private val repo: SensorsRepo) {
    operator fun invoke(): Flow<SensorSample> = repo.getFlow()
}