package com.five9th.motionlogger.presentation.service

import android.content.Context
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.five9th.motionlogger.domain.utils.TimeFormatHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class WakeLockHelper(private val context: Context, private val logsOn: Boolean = false) {

    private var wakeLock: PowerManager.WakeLock? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _logsSF = MutableSharedFlow<String>(replay = 1024)
    val logsSF = _logsSF.asSharedFlow()

    init {
        initWakeLock()
    }

    private fun initWakeLock() {
        if (wakeLock != null) {
            log("wakeLock was initialized already.")
            return
        }

        val pm = ContextCompat.getSystemService(context, PowerManager::class.java) as PowerManager

        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKE_LOCK_TAG
        ).apply {
            setReferenceCounted(false)
        }
    }

    fun acquireWakeLock() {
        val lock = wakeLock
        if (lock != null) {
            lock.acquire(ACQUIRE_TIME_MS)
            log("wakeLock acquired for 10 min")

            // TODO: deffer a task to re-acquire after the timeout passes
        } else {
            log("wakeLock was not initialized")
        }
    }

    fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
            log("wakeLock was released (isHeld: ${wakeLock?.isHeld}).")
        } else {
            log("wakeLock was not acquired, no need to release.")
        }
    }

    /** Releases the WakeLock and resources. The object becomes unusable after the call of this method. */
    fun cleanup() {
        releaseWakeLock()
        wakeLock = null
        scope.cancel()
    }

    private fun log(msg: String) {
        if (!logsOn) return

        Log.d(TAG, msg)

        val fullMsg = "[${getTimestamp()}] $msg"

        scope.launch {
            _logsSF.emit(fullMsg)
        }
    }

    private fun getTimestamp(): String {
        val currentTimeMs = System.currentTimeMillis()
        val secondsOfDay = TimeFormatHelper.unixTimeMillisToTimeOfDaySeconds(currentTimeMs)

        return TimeFormatHelper.timeOfDaySecondsToHhMmSs(secondsOfDay)
    }

    companion object {
        private const val TAG = "WakeLockHelper"

        private const val WAKE_LOCK_TAG = "MotionLogger:WakeLockHelper"
        private const val ACQUIRE_TIME_MS = 10 * 60 * 1000L  // 10 min
    }
}