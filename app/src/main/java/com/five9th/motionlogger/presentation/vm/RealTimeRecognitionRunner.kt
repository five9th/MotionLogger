package com.five9th.motionlogger.presentation.vm

import com.five9th.motionlogger.domain.entities.SampleWindow
import com.five9th.motionlogger.domain.entities.SensorSample
import com.five9th.motionlogger.domain.usecases.ObserveSensorsUseCase
import com.five9th.motionlogger.domain.usecases.ml.AnalyzeWindowUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class RealTimeRecognitionRunner @Inject constructor (
    private val observeSensorsUseCase: ObserveSensorsUseCase,
    private val analyzeWindowUseCase: AnalyzeWindowUseCase
) {
    private val windowSize = 128
    private val stepSize = 64

    private val zeroList = List(6) {0f}

    private val buffer = ArrayDeque<SensorSample>(windowSize)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _currentScoresSF = MutableStateFlow(zeroList)
    val currentScoresSF = _currentScoresSF.asStateFlow()


    private var isStarted = false

    fun start() {
        if (isStarted) return

        isStarted = true

        scope.launch {
            observeSensorsUseCase().collect(::addNewSample)
        }
    }

    private suspend fun addNewSample(sample: SensorSample) {
        buffer.addLast(sample)

        if (buffer.size == windowSize) {
            analyze(buffer.toList())

            repeat(stepSize) {
                if (buffer.isNotEmpty()) {
                    buffer.removeFirst()
                }
            }
        }
    }

    private suspend fun analyze(samples: List<SensorSample>) {
        val output = analyzeWindowUseCase(SampleWindow(samples))
        _currentScoresSF.value = output.scores
    }
}