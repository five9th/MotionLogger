package com.five9th.motionlogger.data.ml

import com.five9th.motionlogger.domain.entities.SensorSample
import com.five9th.motionlogger.domain.entities.SensorSchema

class DataPreprocessor(val schema: SensorSchema) {
    companion object {
        private const val G = 9.81f

        // z-score params evaluated on train dataset
        private val GYRO_MEAN_XYZ = floatArrayOf(
            0.0077761267F,
            0.010961921F,
            0.009427973F
        )
        private val GYRO_STD_XYZ = floatArrayOf(
            1.3516988F,
            1.3322682F,
            0.7647826F
        )
        private val ACC_MEAN_XYZ = floatArrayOf(
            0.0022688229F,
            0.046449035F,
            0.039350007F
        )
        private val ACC_STD_XYZ = floatArrayOf(
            0.3236451F,
            0.5328667F,
            0.40060645F
        )
    }

    // Converts accel values: m/s^2 -> g
    fun convertAccToG(s: SensorSample): SensorSample {
        val fields = schema.toString().split(',')
        val values = s.values

        if (fields.size != values.size) throw RuntimeException("Schemas mismatch")

        for (i in values.indices) {
            if (fields[i].contains("acc")) values[i] /= G
        }

        return SensorSample(s.timestampMs, values)
    }

    // Applies z-score normalisation to gyro and accel
    fun applyZScore(s: SensorSample): SensorSample {
        TODO()
//        return s.copy(
//            accX = zScore(s.accX, ACC_MEAN_XYZ[0], ACC_STD_XYZ[0]),
//            accY = zScore(s.accX, ACC_MEAN_XYZ[1], ACC_STD_XYZ[1]),
//            accZ = zScore(s.accX, ACC_MEAN_XYZ[2], ACC_STD_XYZ[2]),
//
//            gyroX = zScore(s.gyroX, GYRO_MEAN_XYZ[0], GYRO_STD_XYZ[0]),
//            gyroY = zScore(s.gyroX, GYRO_MEAN_XYZ[1], GYRO_STD_XYZ[1]),
//            gyroZ = zScore(s.gyroX, GYRO_MEAN_XYZ[2], GYRO_STD_XYZ[2]),
//        )
    }

    private fun zScore(v: Float, mean: Float, std: Float): Float = (v - mean) / std
}