package com.five9th.motionlogger.data.repos

import com.five9th.motionlogger.domain.entities.SensorField
import com.five9th.motionlogger.domain.entities.SensorSchema
import com.five9th.motionlogger.domain.repos.SchemaRepo
import javax.inject.Inject

class SchemaRepoImpl @Inject constructor() : SchemaRepo {

    private val defaultSchema = SensorSchema(0, listOf(
        SensorField("roll"), SensorField("pitch"), SensorField("yaw"),
        SensorField("gyro_x"), SensorField("gyro_y"), SensorField("gyro_z"),
        SensorField("lin_acc_x"), SensorField("lin_acc_y"), SensorField("lin_acc_z"),
    ))

    private var currentSchema: SensorSchema? = null

    private val schemas = mutableMapOf<Int, SensorSchema>(
        0 to defaultSchema
    )


    override fun getDefaultSchema() = defaultSchema

    override fun getCurrentSchema() = currentSchema

    override fun setCurrentSchema(schema: SensorSchema) {
        currentSchema = schema
    }

    override fun putSchema(schema: SensorSchema) {
        if (schema.isVersionNotSet)
            schema.version = getNewVersion()
        else if (schema.version > highestVersion)
            highestVersion = schema.version  // fighting the collisions (sort of)

        schemas[schema.version] = schema
    }

    override fun getSchema(version: Int): SensorSchema? {
        return schemas.getOrDefault(version, null)
    }


    private var highestVersion = 0

    private fun getNewVersion(): Int {
        return ++highestVersion
    }
}