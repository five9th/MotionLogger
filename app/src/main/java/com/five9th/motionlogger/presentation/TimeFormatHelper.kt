package com.five9th.motionlogger.presentation

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimeFormatHelper {
    companion object {
        fun elapsedMillisToMmSs(millis: Long): String {
            val totalSeconds = millis / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%02d:%02d".format(minutes, seconds)
        }

        fun unixTimeMillisToHhMmSs(millis: Long): String {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            return sdf.format(Date(millis))
        }
    }
}