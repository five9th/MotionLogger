package com.five9th.motionlogger.data.ml

import com.five9th.motionlogger.domain.entities.ModelOutput
import com.five9th.motionlogger.domain.entities.SampleWindow
import com.five9th.motionlogger.domain.entities.SensorSample
import com.five9th.motionlogger.domain.repos.ModelInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import javax.inject.Inject

class TFLiteModelInference @Inject constructor (
    private val provider: ModelFileProvider
) : ModelInference {

    private var isInterpreterLoaded = false
    private var _interpreter: Interpreter? = null

    private fun getInterpreter(): Interpreter
        = if (!isInterpreterLoaded || _interpreter == null) loadInterpreter() else _interpreter!!

    private fun loadInterpreter(): Interpreter { // must be called off main thread
        val interpreter = provider.getInterpreter()

        _interpreter = interpreter
        isInterpreterLoaded = true

        return interpreter
    }

    private val mutex = Mutex()

    override suspend fun run(window: SampleWindow): ModelOutput {

        val outputBuffer: Array<FloatArray>

        withContext(Dispatchers.Default) {
            mutex.withLock {
                val inputBuffer = mapDomainToModelInput(window)  // shape (1, 128, 9)
                outputBuffer = createOutputBuffer()    // shape (1, 6)

                getInterpreter().run(inputBuffer, outputBuffer)
            }
        }

        return ModelOutput(scores = outputBuffer[0].toList())
    }

    // model expects shape (1, 128, 9) -- 128 samples, 9 sensors
    // model expects sensor order: roll, pitch, yaw, gyro.x/y/z, accel.x/y/z
    private fun mapDomainToModelInput(window: SampleWindow): Array<Array<FloatArray>> {
        fun sampleToFloatArray(s: SensorSample): FloatArray {
            return floatArrayOf(
                s.roll, s.pitch, s.yaw,
                s.gyroX, s.gyroY, s.gyroZ,
                s.accX, s.accY, s.accZ
            )
        }

        return Array(1) {
            Array(128) { i ->
                sampleToFloatArray(window.samples[i])
            }
        }
    }

    // model's output shape is (1, 6)
    private fun createOutputBuffer() = Array(1) { FloatArray(6) }

    override fun close() {
        _interpreter?.close()
        _interpreter = null
        isInterpreterLoaded = false
    }
}