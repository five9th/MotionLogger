package com.five9th.motionlogger.domain.usecases

import com.five9th.motionlogger.domain.entities.SensorSample

class SaveSamplesUseCase(private val repo: FilesRepo) {
    suspend operator fun invoke(samples: List<SensorSample>, filename: String) {
        repo.saveSamples(samples, filename)
    }
}