package com.five9th.motionlogger.domain.repos

import com.five9th.motionlogger.domain.entities.SensorSchema

interface SchemaRepo {
    fun getDefaultSchema(): SensorSchema
    fun getCurrentSchema(): SensorSchema?
    fun setCurrentSchema(schema: SensorSchema)

    fun putSchema(schema: SensorSchema)
    fun getSchema(version: Int): SensorSchema?
}