package com.five9th.motionlogger.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import com.five9th.motionlogger.domain.entities.SensorsInfo

class SensorsBundle(context: Context) {
    val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    val accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val gyroscope = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    val magnetometer = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    val gameRotationVector = sm.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
    val rotationVector = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    fun getSensorsInfo(): SensorsInfo {
        return SensorsInfo(
            accelerometer != null,
            gyroscope != null,
            magnetometer != null,
            gameRotationVector != null,
            rotationVector != null
        )
    }
}