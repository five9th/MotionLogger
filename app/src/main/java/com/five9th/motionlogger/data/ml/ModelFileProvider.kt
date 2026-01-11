package com.five9th.motionlogger.data.ml

import android.app.Application
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import javax.inject.Inject

class ModelFileProvider @Inject constructor (
    private val application: Application
) {

    companion object {
        private const val MODEL_FILE_NAME = "tiny_cnn-raw-no-gravity.tflite"
    }

    private val tag = "ModelFileProvider"

    fun getInterpreter(): Interpreter {
        val modelBuffer = FileUtil.loadMappedFile(application, MODEL_FILE_NAME)
        val interpreter = Interpreter(modelBuffer)

        val input = interpreter.getInputTensor(0).shape()
        val output = interpreter.getOutputTensor(0).shape()

        Log.d(tag, "Model loaded, shapes: input = (${input.joinToString()}); output = (${output.joinToString()});")

        return interpreter
    }
}