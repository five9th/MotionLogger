package com.five9th.motionlogger.domain.usecases

import android.util.Log
import com.five9th.motionlogger.domain.entities.ActivityClass
import com.five9th.motionlogger.domain.entities.CollectingSession
import com.five9th.motionlogger.domain.entities.ModelOutput
import com.five9th.motionlogger.domain.entities.SampleWindow
import com.five9th.motionlogger.domain.entities.SessionAnalysisResult
import com.five9th.motionlogger.domain.entities.WindowPrediction
import com.five9th.motionlogger.domain.repos.ModelInference
import kotlinx.coroutines.delay
import javax.inject.Inject

class AnalyzeSessionUseCase @Inject constructor (
    private val model: ModelInference
) {
    companion object {
        private const val WINDOW_SIZE = 128
    }
    private val tag = "AnalyzeSessionUseCase"

    suspend operator fun invoke(
        session: CollectingSession
    ): SessionAnalysisResult {
        // windowing (128)
        val windows = session.samples
            .chunked(WINDOW_SIZE)
            .mapNotNull { list ->
                if (list.size == WINDOW_SIZE)
                    SampleWindow(list.toList())
                else
                    null
            }

        Log.d(tag, "session ${session.id}; " +
                "samples: ${session.samples.size}; " +
                "expected windows: ${session.samples.size / 128f}; " +
                "got windows: ${windows.size}; " +
                "last window size: ${windows[windows.size - 1].samples.size};")
        delay(100)

        // model inference per window
        val results = ArrayList<WindowPrediction>()

        for (i in windows.indices) {
            val window = windows[i]
            val output = model.run(window)
            val prediction = mapModelOutputToWindowPrediction(output, i)

            results += prediction
        }

        // aggregation of results
        return SessionAnalysisResult(results)
    }

    private fun mapModelOutputToWindowPrediction(output: ModelOutput, index: Int): WindowPrediction {
        val predictedClass = getPredictedClass(output.scores)
        return WindowPrediction(index, predictedClass)
    }

    private fun getPredictedClass(scores: List<Float>): ActivityClass {
        var classIndex = 0
        var maxProb = 0f

        for (i in scores.indices) {
            val prob = scores[i]
            if (prob > maxProb) {
                maxProb = prob
                classIndex = i
            }
        }

        return classIndex
    }
}
