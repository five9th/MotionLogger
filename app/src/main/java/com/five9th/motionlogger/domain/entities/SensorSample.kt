package com.five9th.motionlogger.domain.entities

// TODO: use schema-base approach instead
data class SensorSample(
    val timestampMs: Long,
    val accX: Float, val accY: Float, val accZ: Float,
    val gyroX: Float, val gyroY: Float, val gyroZ: Float,
    val roll: Float, val pitch: Float, val yaw: Float,
)