package com.five9th.motionlogger.domain.entities


data class SessionAnalysisResult(
    val windowResults: List<WindowPrediction>
) {
    fun getPercentages(): Map<ActivityClass, Float> {
        if (windowResults.isEmpty()) return emptyMap()

        val total = windowResults.size.toFloat()

        return windowResults
            .groupingBy { it.predictedClass }
            .eachCount()
            .mapValues { (_, count) ->
                count / total
            }
    }
}

data class WindowPrediction(
    val windowIndex: Int,
    val predictedClass: ActivityClass
)
