package com.five9th.motionlogger.data.ml

import android.util.Log
import com.five9th.motionlogger.domain.entities.ModelOutput
import com.five9th.motionlogger.domain.entities.N_CLASSES
import com.five9th.motionlogger.domain.entities.SampleWindow
import com.five9th.motionlogger.domain.entities.SensorSchema
import com.five9th.motionlogger.domain.entities.WINDOW_SIZE
import com.five9th.motionlogger.domain.repos.ModelInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TFLiteModelInference @Inject constructor (
    private val provider: ModelFileProvider,
) : ModelInference {

    private val tag = "ML"

    private var isModelLoaded = false
    private var _model: MLModel? = null

    private fun getModel(): MLModel
        = if (!isModelLoaded || _model == null) loadModel() else _model!!

    private fun loadModel(): MLModel { // must be called off main thread
        val mlModel = provider.getModel()

        _model = mlModel
        isModelLoaded = true

        return mlModel
    }

    private val mutex = Mutex()

    override suspend fun run(window: SampleWindow): ModelOutput {
        val outputBuffer: Array<FloatArray>

        withContext(Dispatchers.Default) {
            mutex.withLock {
                val schema = getModel().schema

                checkSchema(modelSchema =  schema, dataSchema =  window.schema)

                val preprocessor = DataPreprocessor(schema)

                val inputBuffer = mapDomainToModelInput(window, preprocessor)  // shape (1, 128, 6)
                outputBuffer = createOutputBuffer()    // shape (1, 6)

                getModel().interpreter.run(inputBuffer, outputBuffer)
            }
        }

        return ModelOutput(scores = outputBuffer[0].toList())
    }

    private fun checkSchema(modelSchema: SensorSchema, dataSchema: SensorSchema) {
        if (modelSchema != dataSchema)
            throw RuntimeException(
                "Schemas mismatch: model: [${modelSchema.version}] '$modelSchema';\n" +
                        "window: [${dataSchema.version}] '${dataSchema}'")
    }

    // model expects shape (1, 128, 6) -- 128 samples, 6 sensors
    // model expects sensor order: gyro.x/y/z, accel.x/y/z
    private fun mapDomainToModelInput(
        window: SampleWindow,
        preprocessor: DataPreprocessor
    ): Array<Array<FloatArray>> {

        return Array(1) {
            Array(WINDOW_SIZE) { i ->
                // maybe optimize this later
                var s = window.samples[i]
                s = preprocessor.convertAccToG(s)
//                s = preprocessor.applyZScore(s) // not impl yet

                s.values
            }
        }
    }

    // model's output shape is (1, 6)
    private fun createOutputBuffer() = Array(1) { FloatArray(N_CLASSES) }

    override fun close() {
        Log.d(tag, "Interpreter closing requested; interpreter is null: ${_model == null};")

        _model?.interpreter?.close()
        _model = null
        isModelLoaded = false
    }
}