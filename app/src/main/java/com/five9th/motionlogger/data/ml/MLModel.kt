package com.five9th.motionlogger.data.ml

import com.five9th.motionlogger.domain.entities.SensorSchema
import org.tensorflow.lite.Interpreter

class MLModel(
    val interpreter: Interpreter,
    val schema: SensorSchema
)