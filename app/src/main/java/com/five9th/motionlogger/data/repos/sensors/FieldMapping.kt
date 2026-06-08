package com.five9th.motionlogger.data.repos.sensors

data class FieldMapping(
    val source: SensorSource,
    val index: Int
) {
    companion object {
        /** corresponds to [SensorSource.fromSensorField] */
        val MAPPINGS = mapOf(
            "acc_x" to FieldMapping(SensorSource.ACCELEROMETER, 0),
            "acc_y" to FieldMapping(SensorSource.ACCELEROMETER, 1),
            "acc_z" to FieldMapping(SensorSource.ACCELEROMETER, 2),

            "gyro_x" to FieldMapping(SensorSource.GYROSCOPE, 0),
            "gyro_y" to FieldMapping(SensorSource.GYROSCOPE, 1),
            "gyro_z" to FieldMapping(SensorSource.GYROSCOPE, 2),

            "mag_x" to FieldMapping(SensorSource.MAGNETOMETER, 0),
            "mag_y" to FieldMapping(SensorSource.MAGNETOMETER, 1),
            "mag_z" to FieldMapping(SensorSource.MAGNETOMETER, 2),

            "game_rot_x" to FieldMapping(SensorSource.GAME_ROTATION_VECTOR, 0),
            "game_rot_y" to FieldMapping(SensorSource.GAME_ROTATION_VECTOR, 1),
            "game_rot_z" to FieldMapping(SensorSource.GAME_ROTATION_VECTOR, 2),
            "game_rot_w" to FieldMapping(SensorSource.GAME_ROTATION_VECTOR, 3),

            "rot_x" to FieldMapping(SensorSource.ROTATION_VECTOR, 0),
            "rot_y" to FieldMapping(SensorSource.ROTATION_VECTOR, 1),
            "rot_z" to FieldMapping(SensorSource.ROTATION_VECTOR, 2),
            "rot_w" to FieldMapping(SensorSource.ROTATION_VECTOR, 3),

            "mag_x" to FieldMapping(SensorSource.MAGNETOMETER, 0),
            "mag_y" to FieldMapping(SensorSource.MAGNETOMETER, 1),
            "mag_z" to FieldMapping(SensorSource.MAGNETOMETER, 2),

            "lin_acc_x" to FieldMapping(SensorSource.LINEAR_ACCELERATION, 0),
            "lin_acc_y" to FieldMapping(SensorSource.LINEAR_ACCELERATION, 1),
            "lin_acc_z" to FieldMapping(SensorSource.LINEAR_ACCELERATION, 2),

            "gravity_x" to FieldMapping(SensorSource.GRAVITY, 0),
            "gravity_y" to FieldMapping(SensorSource.GRAVITY, 1),
            "gravity_z" to FieldMapping(SensorSource.GRAVITY, 2),

            "roll" to FieldMapping(SensorSource.ATTITUDE, 0),
            "pitch" to FieldMapping(SensorSource.ATTITUDE, 1),
            "yaw" to FieldMapping(SensorSource.ATTITUDE, 2),
        )
    }
}