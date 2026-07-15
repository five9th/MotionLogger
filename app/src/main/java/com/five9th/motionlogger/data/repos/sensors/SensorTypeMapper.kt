package com.five9th.motionlogger.data.repos.sensors

import android.hardware.Sensor

class SensorTypeMapper {
    fun SensorSource.androidSensorType(): Int? =
        when (this) {
            SensorSource.ACCELEROMETER ->
                Sensor.TYPE_ACCELEROMETER

            SensorSource.GYROSCOPE ->
                Sensor.TYPE_GYROSCOPE

            SensorSource.MAGNETOMETER ->
                Sensor.TYPE_MAGNETIC_FIELD

            SensorSource.GAME_ROTATION_VECTOR ->
                Sensor.TYPE_GAME_ROTATION_VECTOR

            SensorSource.ROTATION_VECTOR ->
                Sensor.TYPE_ROTATION_VECTOR

            SensorSource.LINEAR_ACCELERATION ->
                Sensor.TYPE_LINEAR_ACCELERATION

            SensorSource.GRAVITY ->
                Sensor.TYPE_GRAVITY

            SensorSource.ATTITUDE ->   // computing roll,pitch,yaw from TYPE_ROTATION_VECTOR
                Sensor.TYPE_ROTATION_VECTOR
        }

    fun getRequiredSensors(sources: Iterable<SensorSource>): Set<Int> =
        sources.mapNotNull { it.androidSensorType() }
        .toSet()
}