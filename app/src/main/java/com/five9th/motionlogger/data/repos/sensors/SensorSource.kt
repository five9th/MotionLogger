package com.five9th.motionlogger.data.repos.sensors

import com.five9th.motionlogger.domain.entities.SensorSchema

enum class SensorSource {
    ACCELEROMETER,
    GYROSCOPE,
    MAGNETOMETER,

    GAME_ROTATION_VECTOR,
    ROTATION_VECTOR,

    LINEAR_ACCELERATION,
    GRAVITY,

    ATTITUDE;    // custom roll/pitch/yaw

    companion object {
        /** corresponds to [FieldMapping.MAPPINGS] */
        fun fromSensorField(fieldId: String): SensorSource? =
            when (fieldId) {
                "acc_x", "acc_y", "acc_z" ->
                    ACCELEROMETER

                "gyro_x", "gyro_y", "gyro_z" ->
                    GYROSCOPE

                "mag_x", "mag_y", "mag_z" ->
                    MAGNETOMETER

                "game_rot_x", "game_rot_y", "game_rot_z", "game_rot_w" ->
                    GAME_ROTATION_VECTOR

                "rot_x", "rot_y", "rot_z", "rot_w" ->
                    ROTATION_VECTOR

                "lin_acc_x", "lin_acc_y", "lin_acc_z" ->
                    LINEAR_ACCELERATION

                "gravity_x", "gravity_y", "gravity_z" ->
                    GRAVITY

                "roll", "pitch", "yaw" ->
                    ATTITUDE

                else -> null
            }

        fun requiredSources(schema: SensorSchema): Set<SensorSource> =
            schema.fields.mapNotNull { fromSensorField(it.id) }.toSet()
    }
}