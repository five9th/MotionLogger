package com.five9th.motionlogger.domain.utils

import java.text.SimpleDateFormat
import java.util.Calendar
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

        fun unixTimeMillisToTimeOfDaySeconds(millis: Long): Int {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = millis
            }

            val hours = calendar.get(Calendar.HOUR_OF_DAY)
            val minutes = calendar.get(Calendar.MINUTE)
            val seconds = calendar.get(Calendar.SECOND)

            return hours * 3600 + minutes * 60 + seconds
        }

        fun timeOfDaySecondsToHhMmSs(seconds: Int): String {
            val h = seconds / 3600
            val m = (seconds % 3600) / 60
            val s = seconds % 60
            return "%02d:%02d:%02d".format(h, m, s)
        }

    }
}