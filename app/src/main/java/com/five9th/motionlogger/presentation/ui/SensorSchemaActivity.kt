package com.five9th.motionlogger.presentation.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.five9th.motionlogger.R
import com.five9th.motionlogger.databinding.ActivityMainBinding
import com.five9th.motionlogger.databinding.ActivitySensorSchemaBinding
import com.five9th.motionlogger.domain.entities.SensorField
import com.five9th.motionlogger.domain.entities.SensorSchema
import com.five9th.motionlogger.presentation.vm.MainViewModel

class SensorSchemaActivity : AppCompatActivity() {

    private val availableFields = listOf(
        "acc_x", "acc_y", "acc_z",
        "gyro_x", "gyro_y", "gyro_z",
        "mag_x", "mag_y", "mag_z",
        "lin_acc_x", "lin_acc_y", "lin_acc_z",
        "gravity_x", "gravity_y", "gravity_z",
        "roll", "pitch", "yaw"
    )

    private lateinit var binding: ActivitySensorSchemaBinding

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySensorSchemaBinding.inflate(layoutInflater)
        setContentView(binding.root)



        populateAvailableFields()
        setListeners()
    }


    private fun populateAvailableFields() {
        binding.tvAvailableFields.text =
            "Available fields:\n\n" +
                    availableFields.joinToString(", ")
    }

    private fun setListeners() {
        binding.btnSave.setOnClickListener {
            val text = binding.etSchema.text.toString()
            val res = parseSchema(text)

            if (res.isFailure) {
                show("[error] ${res.exceptionOrNull()?.message}")
            }
            else if (res.isSuccess) {
                val s = res.getOrDefault(null)
                mainViewModel.userSchema = s
                show("Schema updated (${s?.fields?.size} fields)")
            }
        }
    }

    private fun parseSchema(text: String): Result<SensorSchema> {
        val ids = text.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

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

    private fun show(m: String) {
        Toast.makeText(this, m, Toast.LENGTH_LONG).show()
    }
}