package com.five9th.motionlogger.domain.usecases

import com.five9th.motionlogger.domain.entities.ModelOutput
import com.five9th.motionlogger.domain.entities.SampleWindow
import com.five9th.motionlogger.domain.repos.ModelInference
import javax.inject.Inject

class PredictActivityUseCase @Inject constructor (
    private val model: ModelInference
) {
    suspend operator fun invoke(window: SampleWindow): ModelOutput {
        return model.run(window)
    }
}