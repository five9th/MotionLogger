package com.five9th.motionlogger.data.ml

import android.util.Log
import com.five9th.motionlogger.domain.entities.ModelOutput
import com.five9th.motionlogger.domain.entities.N_CLASSES
import com.five9th.motionlogger.domain.entities.SampleWindow
import com.five9th.motionlogger.domain.entities.WINDOW_SIZE
import com.five9th.motionlogger.domain.repos.ModelInference
import com.five9th.motionlogger.domain.usecases.schema.GetSchemaUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import javax.inject.Inject

class TFLiteModelInference @Inject constructor (
    private val provider: ModelFileProvider,
    getSchemaUseCase: GetSchemaUseCase  // <-- temp solution (todo)
) : ModelInference {

    private val tag = "ML"

    private val inputSchema = getSchemaUseCase(0)!! // just get default

    private val preprocessor = DataPreprocessor(inputSchema)

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

        if (inputSchema != window.schema)
            throw RuntimeException("Schemas mismatch: model: '$inputSchema'; window: '${window.schema}'")

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
        return Array(1) {
            Array(WINDOW_SIZE) { i ->
                // maybe optimize this later
                var s = window.samples[i]
                s = preprocessor.convertAccToG(s)
//                s = preprocessor.applyZScore(s) // not impl

                s.values
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