package com.five9th.motionlogger.domain.entities

data class SensorsInfo(
    val hasAccelerometer: Boolean,
    val hasGyroscope: Boolean,
    val hasMagnetometer: Boolean,
    val hasGameRotationVector: Boolean,
    val hasRotationVector: Boolean
)