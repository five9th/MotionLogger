package com.five9th.motionlogger.data

import com.five9th.motionlogger.data.FilesRepoImpl.Companion.FILENAME_PATTERN
import com.five9th.motionlogger.domain.entities.CollectingSession
import com.five9th.motionlogger.domain.entities.SessionInfo
import com.five9th.motionlogger.domain.utils.TimeFormatHelper

class RepoMapper {
    fun mapDomainToFileModel(session: CollectingSession): SessionCSVModel {
        return SessionCSVModel(
            makeFileName(session.info),
            listOf("timestamp","ax","ay","az","gx","gy","gz","roll","pitch","yaw"), // hardcoded for now
            session.samples
        )
    }

    private fun makeFileName(sessionInfo: SessionInfo): String {
        val id = sessionInfo.id
        val start = TimeFormatHelper.timeOfDaySecondsToHhMmSs(sessionInfo.startTimeInSeconds)
        val stop = TimeFormatHelper.timeOfDaySecondsToHhMmSs(sessionInfo.stopTimeInSeconds)

        return FILENAME_PATTERN.format(id, start, stop)
    }
}