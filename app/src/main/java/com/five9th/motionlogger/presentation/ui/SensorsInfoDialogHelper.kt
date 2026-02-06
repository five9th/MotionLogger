package com.five9th.motionlogger.presentation.ui

import android.content.Context
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.five9th.motionlogger.R
import com.five9th.motionlogger.databinding.DialogSensorInfoBinding
import com.five9th.motionlogger.domain.entities.SensorsInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class SensorsInfoDialogHelper(
    private val context: Context,
    private val inflater: LayoutInflater,
    private val scope: CoroutineScope
) {

    fun showSensorsInfo(
        infoFlow: StateFlow<SensorsInfo?>,
        requestInfoCallback: () -> Unit
    ) {
        val info = infoFlow.value

        if (info != null) {
            showSensorsInfoDialog(info)
        }
        else {
            requestAndShowSensorsInfo(infoFlow, requestInfoCallback)
        }
    }

    private fun showSensorsInfoDialog(info: SensorsInfo) {
        val binding = DialogSensorInfoBinding.inflate(inflater)

        bindInfo(binding, info)

        AlertDialog.Builder(context)
            .setView(binding.root)
            .setPositiveButton(context.getString(android.R.string.ok)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun bindInfo(binding: DialogSensorInfoBinding, sensorsInfo: SensorsInfo) {
        val strYes = context.getString(R.string.yes)
        val strNo = context.getString(R.string.no)

        binding.tvHasAccelerometer.text = if (sensorsInfo.hasAccelerometer) strYes else strNo
        binding.tvHasGyroscope.text = if (sensorsInfo.hasGyroscope) strYes else strNo
        binding.tvHasMagnetometer.text = if (sensorsInfo.hasMagnetometer) strYes else strNo
        binding.tvHasGameRotationVector.text = if (sensorsInfo.hasGameRotationVector) strYes else strNo
        binding.tvHasRotationVector.text = if (sensorsInfo.hasRotationVector) strYes else strNo
        binding.tvHasLinearAcceleration.text = if (sensorsInfo.hasLinearAcceleration) strYes else strNo
        binding.tvHasGravity.text = if (sensorsInfo.hasGravity) strYes else strNo
    }

    private fun requestAndShowSensorsInfo(
        flow: StateFlow<SensorsInfo?>, requestCallback: () -> Unit
    ) {
        requestCallback()

        scope.launch {
            val info = withTimeoutOrNull(2000) {
                flow.first { it != null }
            }

            if (info != null) {
                showSensorsInfoDialog(info)
            }
            else {
                showError()
            }
        }
    }

    private fun showError() {
        Toast.makeText(context, context.getString(R.string.error), Toast.LENGTH_SHORT).show()
    }
}