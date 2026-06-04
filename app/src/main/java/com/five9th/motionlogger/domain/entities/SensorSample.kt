package com.five9th.motionlogger.domain.entities

// TODO: use schema-base approach instead
data class SensorSample(
    val timestampMs: Long,                                // ms
    val accX: Float, val accY: Float, val accZ: Float,    // m/s^2
    val gyroX: Float, val gyroY: Float, val gyroZ: Float, // rads/s
    val roll: Float, val pitch: Float, val yaw: Float,    // rads
)