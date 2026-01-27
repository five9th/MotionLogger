package com.five9th.motionlogger.presentation.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

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
}
