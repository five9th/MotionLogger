package com.five9th.motionlogger.domain.usecases.ml

import com.five9th.motionlogger.domain.entities.ModelOutput
import com.five9th.motionlogger.domain.entities.SampleWindow
import com.five9th.motionlogger.domain.repos.ModelInference
import javax.inject.Inject

class AnalyzeWindowUseCase @Inject constructor (
    private val model: ModelInference
) {
    suspend operator fun invoke(
        window: SampleWindow
    ): ModelOutput {
        return model
            .run(window)
            .adjust()  // stair classes are dominating rn for some reason
    }
}