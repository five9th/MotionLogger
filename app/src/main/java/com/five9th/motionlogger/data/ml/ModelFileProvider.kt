package com.five9th.motionlogger.data.ml

import android.app.Application
import android.util.Log
import com.five9th.motionlogger.domain.entities.SensorSchema
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import javax.inject.Inject

class ModelFileProvider @Inject constructor (
    private val application: Application
) {

    companion object {
        // models:
        // tiny_cnn-raw-att-zscore-gyro-user_acc.tflite
        // tiny_cnn-raw-no-gravity.tflite
        // tiny_cnn-2-zscore-gyro-user_acc.tflite
        private const val MODEL_FILE_NAME = "tiny_cnn-2-zscore-gyro-user_acc.tflite"
        private const val MODEL_INPUT_SCHEMA =      // should load from file along with the model
            "gyro_x,gyro_y,gyro_z," +
                    "lin_acc_x,lin_acc_y,lin_acc_z"
    }

    private val tag = "ML"

    fun getModel(): MLModel {
        val modelBuffer = FileUtil.loadMappedFile(application, MODEL_FILE_NAME)
        val interpreter = Interpreter(modelBuffer)

        val schema = SensorSchema.fromString(MODEL_INPUT_SCHEMA)

        val input = interpreter.getInputTensor(0).shape()
        val output = interpreter.getOutputTensor(0).shape()

        Log.d(tag, "Model loaded, shapes: " +
                "input = (${input.joinToString()}); output = (${output.joinToString()}); " +
                "schema: '$schema'")

        return MLModel(interpreter, schema)
    }
}