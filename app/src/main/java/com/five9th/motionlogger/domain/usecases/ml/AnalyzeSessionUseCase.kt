package com.five9th.motionlogger.domain.usecases.ml

import com.five9th.motionlogger.domain.entities.ModelOutput
import com.five9th.motionlogger.domain.entities.SampleWindow
import com.five9th.motionlogger.domain.entities.SessionAnalysisResult
import com.five9th.motionlogger.domain.entities.WindowPrediction
import javax.inject.Inject

class AnalyzeSessionUseCase @Inject constructor (
    private val analyzeWindowUseCase: AnalyzeWindowUseCase
) {
    private val tag = "AnalyzeSessionUseCase"

    suspend operator fun invoke(
        windows: List<SampleWindow>
    ): SessionAnalysisResult {
        val results = ArrayList<WindowPrediction>()

        for (i in windows.indices) {
            val window = windows[i]
            val output = analyzeWindowUseCase(window)
            val prediction = mapModelOutputToWindowPrediction(output, i)

            results += prediction
        }

        return SessionAnalysisResult(results)
    }

    private fun mapModelOutputToWindowPrediction(output: ModelOutput, index: Int): WindowPrediction {
        val predictedClass = output.getPredictedClass()
        return WindowPrediction(index, predictedClass)
    }
}
