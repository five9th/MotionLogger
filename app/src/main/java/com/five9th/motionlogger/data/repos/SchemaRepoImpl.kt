package com.five9th.motionlogger.data.repos

import android.util.Log
import com.five9th.motionlogger.domain.entities.SensorField
import com.five9th.motionlogger.domain.entities.SensorSchema
import com.five9th.motionlogger.domain.repos.SchemaRepo
import javax.inject.Inject

// TODO: this repo needs an overhaul
class SchemaRepoImpl @Inject constructor() : SchemaRepo {

    private val tag = "SchemaRepo"

    private val defaultSchema = SensorSchema(0, listOf(
        SensorField("roll"), SensorField("pitch"), SensorField("yaw"),
        SensorField("gyro_x"), SensorField("gyro_y"), SensorField("gyro_z"),
        SensorField("lin_acc_x"), SensorField("lin_acc_y"), SensorField("lin_acc_z"),
    ))

    private var currentSchemaVer = 0

    private val schemas = mutableMapOf<Int, SensorSchema>(
        0 to defaultSchema
    )


    override fun getDefaultSchema() = defaultSchema

    override fun getCurrentSchema() = schemas[currentSchemaVer]

    override fun setCurrentSchema(schema: SensorSchema?) {
        val version = if (schema == null) {
            SensorSchema.VERSION_NOT_SET
        } else {
            putSchema(schema)
        }

        currentSchemaVer = version

//        schema?.version = version
        Log.d(tag, "set current schema: ${schema?.toDescriptionStr()}")
    }

    override fun putSchema(schema: SensorSchema): Int {
        if (schema.isVersionNotSet)
            schema.version = getNewVersion()
        else if (schema.version > highestVersion)
            highestVersion = schema.version  // fighting the collisions (sort of)

        schemas[schema.version] = schema

        Log.d(tag, "put schema: ${schema.toDescriptionStr()}")
        return schema.version
    }

    override fun getSchema(version: Int): SensorSchema? {
        return schemas.getOrDefault(version, null)
    }


    private var highestVersion = 0

    private fun getNewVersion(): Int {
        return ++highestVersion
    }
}