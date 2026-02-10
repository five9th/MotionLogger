package com.five9th.motionlogger.presentation.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.five9th.motionlogger.R

class NotificationHelper(
    private val context: Context,
    private val launcher: ActivityResultLauncher<String>
) {
    private val tag = "NotificationHelper"

    /** Upon execution of this method only one of three callbacks will be called:
     * - onGranted;
     * - onDenied;
     * - ActivityResultCallback from the given launcher */
    fun ensurePermission(onGranted: () -> Unit, onDenied: () -> Unit) {
        if (hasPermission()) {
            Log.d(tag, "onGranted")
            onGranted()
        } else if (shouldRequest()) {
            Log.d(tag, "launch request")
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            Log.d(tag, "onDenied")
            onDenied()
        }
    }

    private fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun shouldRequest() =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU


    private fun openAppNotificationSettings() {
        val intent =
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }

        context.startActivity(intent)
    }

    fun showNotificationSettingsDialog() {
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.notifications_required))
            .setMessage(context.getString(R.string.notifications_required_message))
            .setNegativeButton(context.getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(context.getString(R.string.settings)) { _, _ ->
                openAppNotificationSettings()
            }
            .setCancelable(true)
            .show()
    }
}
