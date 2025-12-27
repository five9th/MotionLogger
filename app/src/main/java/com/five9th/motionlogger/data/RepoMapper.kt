package com.five9th.motionlogger.data

import com.five9th.motionlogger.data.FilesRepoImpl.Companion.FILENAME_PATTERN
import com.five9th.motionlogger.domain.entities.CollectingSession
import com.five9th.motionlogger.presentation.TimeFormatHelper

class RepoMapper {
    fun mapDomainToFileModel(session: CollectingSession): SessionCSVModel {
        return SessionCSVModel(
            makeFileName(session),
            listOf("timestamp","ax","ay","az","gx","gy","gz","roll","pitch","yaw"), // hardcoded for now
            session.samples
        )
    }

    private fun makeFileName(session: CollectingSession): String {
        val id = session.id
        val start = TimeFormatHelper.timeOfDaySecondsToHhMmSs(session.startTimeInSeconds)
        val stop = TimeFormatHelper.timeOfDaySecondsToHhMmSs(session.stopTimeInSeconds)

        return FILENAME_PATTERN.format(id, start, stop)
    }
}