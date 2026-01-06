package com.five9th.motionlogger.domain.utils

import java.text.SimpleDateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
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
            val t = timeBundleFromSeconds(seconds)
            return "%02d:%02d:%02d".format(t.hours, t.minutes, t.seconds)
        }

        fun timeBundleFromSeconds(seconds: Int): TimeBundle {
            return TimeBundle(
                hours = seconds / 3600,
                minutes = (seconds % 3600) / 60,
                seconds = seconds % 60
            )
        }

        /** returns seconds of day or -1 if failed */
        fun hhMmSsToSeconds(timeStr: String): Int {
            val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")

            try {
                val time = LocalTime.parse(timeStr, formatter)
                return time.toSecondOfDay()
            }
            catch (e: Exception) {
                return -1
            }
        }

        data class TimeBundle(
            val hours: Int,
            val minutes: Int,
            val seconds: Int
        )
    }
}