package com.five9th.motionlogger.presentation.vm

import android.util.Log
import androidx.lifecycle.ViewModel
import com.five9th.motionlogger.domain.entities.SensorField
import com.five9th.motionlogger.domain.entities.SensorSchema
import com.five9th.motionlogger.domain.usecases.schema.SetCurrentSchemaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

@HiltViewModel
class SensorSchemaViewModel @Inject constructor (
    private val setCurrentSchemaUseCase: SetCurrentSchemaUseCase
) : ViewModel() {

    private val tag = "SensorSchema"

    private val availableFields = listOf(  // TODO: store this centralized, somewhere in domain
        "acc_x", "acc_y", "acc_z",
        "gyro_x", "gyro_y", "gyro_z",
        "mag_x", "mag_y", "mag_z",
        "lin_acc_x", "lin_acc_y", "lin_acc_z",
        "gravity_x", "gravity_y", "gravity_z",
        "roll", "pitch", "yaw"
    )

    private val _messagesSF = MutableSharedFlow<String>(extraBufferCapacity = 6)
    val messagesSF = _messagesSF.asSharedFlow()

    // TODO: should be a UseCase
    fun getAvailableFields() = availableFields

    fun setSchemaFromText(schemaStr: String): Boolean { // todo: validate not empty
        val res = parseSchema(schemaStr)

        return if (res.isSuccess) {
            val schema = res.getOrNull()
            sendMessage("Schema updated (${schema?.fields?.size} fields)")
            setCurrentSchemaUseCase(schema)
            true
        } else {
            sendMessage("[error] ${res.exceptionOrNull()?.message}")
            false
        }
    }

    // TODO: should be a UseCase
    private fun parseSchema(text: String): Result<SensorSchema> {
        val ids = text.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (ids.isEmpty()) {
            return Result.failure(
                IllegalArgumentException(
                    "Input is empty"
                )
            )
        }

        val invalid = ids.filter { it !in availableFields }

        if (invalid.isNotEmpty()) {
            return Result.failure(
                IllegalArgumentException(
                    "Unknown fields: ${invalid.joinToString(", ")}"
                )
            )
        }

        return Result.success(
            SensorSchema(SensorSchema.VERSION_NOT_SET, ids.map(::SensorField))
        )
    }

    private fun sendMessage(msg: String) {
        val res = _messagesSF.tryEmit(msg)
        Log.d(tag, "[sent: $res] $msg")
    }
}