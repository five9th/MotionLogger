package com.five9th.motionlogger.domain.entities

const val WINDOW_SIZE = 128
const val N_FEATURES = 6
const val N_CLASSES = 6

/** Represents a window of 128 samples. Shape: (128, 6) */
data class SampleWindow(val samples: List<SensorSample>)

/** Output shape: (6,) - probability of a window belonging to each of 6 activity classes
 * ACT_LABELS = ["dws", "ups", "wlk", "jog", "std", "sit"] */
data class ModelOutput(val scores: List<Float>) {

    // TODO: add smth like CONFIDENCE_THRESHOLD (min prob & min diff from other classes)
    //  and return ActivityClass.UNKNOWN if it isn't met
    fun getPredictedClass(): ActivityClass {
        var classIndex = 0
        var maxProb = 0f

        for (i in scores.indices) {
            val prob = scores[i]
            if (prob > maxProb) {
                maxProb = prob
                classIndex = i
            }
        }

        return ActivityClass.fromInt(classIndex)
    }

    // ["dws", "ups", "wlk", "jog", "std", "sit"]
    private val coefs = arrayOf(
        0.9f, // dws
        0.9f, // ups
        1f,   // wlk
        1f,   // jog
        1f,   // std
        1f    // sit
    )

    fun adjust(): ModelOutput {
        return ModelOutput(List(6) { i -> scores[i] * coefs[i]})
    }
}