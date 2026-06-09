package com.five9th.motionlogger.presentation.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.five9th.motionlogger.databinding.ActivitySensorSchemaBinding
import com.five9th.motionlogger.presentation.vm.SensorSchemaViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SensorSchemaActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySensorSchemaBinding

    private val viewModel: SensorSchemaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySensorSchemaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        populateAvailableFields()
        setListeners()
        collectFlows()
    }


    private fun populateAvailableFields() {
        binding.tvAvailableFields.text =  // <-- temp solution (todo)
            "Available fields:\n\n" +
                    viewModel.getAvailableFields().joinToString(", ")
    }

    private fun setListeners() {
        binding.btnSave.setOnClickListener {
            onSaveBtnClick()
        }
    }

    private fun collectFlows() {
        lifecycleScope.launch {
            viewModel.messagesSF.collect(::showMsg)
        }
    }

    private fun onSaveBtnClick() {
        val schemaText = binding.etSchema.text.toString()

        val success = viewModel.setSchemaFromText(schemaText)

        if (success) {
//            finish()
        }
    }

    private fun showMsg(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}