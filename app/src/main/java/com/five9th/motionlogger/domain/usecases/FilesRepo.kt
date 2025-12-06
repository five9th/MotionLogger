package com.five9th.motionlogger.domain.usecases

import com.five9th.motionlogger.domain.entities.SensorSample

interface FilesRepo {
    suspend fun saveSamples(samples: List<SensorSample>, filename: String)
}