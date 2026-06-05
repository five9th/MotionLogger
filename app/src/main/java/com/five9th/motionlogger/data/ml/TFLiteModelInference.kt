package com.five9th.motionlogger.data.ml

import android.util.Log
import com.five9th.motionlogger.domain.entities.ModelOutput
import com.five9th.motionlogger.domain.entities.N_CLASSES
import com.five9th.motionlogger.domain.entities.SampleWindow
import com.five9th.motionlogger.domain.entities.SensorSample
import com.five9th.motionlogger.domain.entities.WINDOW_SIZE
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

    private val tag = "ML"

    private val preprocessor = DataPreprocessor()

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

    // model expects shape (1, 128, 6) -- 128 samples, 6 sensors
    // model expects sensor order: gyro.x/y/z, accel.x/y/z
    private fun mapDomainToModelInput(window: SampleWindow): Array<Array<FloatArray>> {
        fun sampleToFloatArray(s: SensorSample): FloatArray {
            return floatArrayOf(
//                s.roll, s.pitch, s.yaw,
                s.gyroX, s.gyroY, s.gyroZ,
                s.accX, s.accY, s.accZ
            )
        }

        return Array(1) {
            Array(WINDOW_SIZE) { i ->
                // maybe optimize this later
                val s = window.samples[i]
                val sConv = preprocessor.convertUnits(s)
                val sNorm = preprocessor.applyZScore(sConv)

                sampleToFloatArray(sNorm)
            }
        }
    }

    // model's output shape is (1, 6)
    private fun createOutputBuffer() = Array(1) { FloatArray(N_CLASSES) }

    override fun close() {
        Log.d(tag, "Interpreter closing requested; interpreter is null: ${_interpreter == null};")

        _interpreter?.close()
        _interpreter = null
        isInterpreterLoaded = false
    }
}