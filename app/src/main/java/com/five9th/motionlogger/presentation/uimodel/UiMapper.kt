package com.five9th.motionlogger.presentation.uimodel

import android.content.Context
import com.five9th.motionlogger.R
import com.five9th.motionlogger.domain.entities.SessionInfo
import com.five9th.motionlogger.domain.utils.TimeFormatHelper

class UiMapper(private val context: Context) {
    fun mapDomainToUiModel(session: SessionInfo): SessionItem {
        return SessionItem(
            number = session.id.toString(),
            keyWord = "", // add this later
            startTime = TimeFormatHelper.timeOfDaySecondsToHhMmSs(session.startTimeInSeconds),
            stopTime = TimeFormatHelper.timeOfDaySecondsToHhMmSs(session.stopTimeInSeconds),
            duration = secondsToDurationString(
                session.stopTimeInSeconds - session.startTimeInSeconds
            )
        )
    }

    private fun secondsToDurationString(seconds: Int): String {
        val t = TimeFormatHelper.timeBundleFromSeconds(seconds)

        return if (t.hours > 0)
            context.getString(R.string.duration_h_min_sec, t.hours, t.minutes, t.seconds)
        else if (t.minutes > 0)
            context.getString(R.string.duration_min_sec, t.minutes, t.seconds)
        else
            context.getString(R.string.duration_sec, t.seconds)

    }
}