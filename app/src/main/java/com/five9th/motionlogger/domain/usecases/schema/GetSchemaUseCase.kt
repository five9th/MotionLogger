package com.five9th.motionlogger.domain.usecases.schema

import com.five9th.motionlogger.domain.entities.SensorSchema
import com.five9th.motionlogger.domain.repos.SchemaRepo
import javax.inject.Inject

class GetSchemaUseCase @Inject constructor (
    private val repo: SchemaRepo
) {
    operator fun invoke(version: Int): SensorSchema? {
        return repo.getSchema(version)
    }
}