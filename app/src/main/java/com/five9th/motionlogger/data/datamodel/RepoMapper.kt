package com.five9th.motionlogger.data.datamodel

import com.five9th.motionlogger.data.repos.FilesRepoImpl.Companion.FILENAME_PATTERN
import com.five9th.motionlogger.data.repos.FilesRepoImpl.Companion.FILENAME_REGEX
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

    fun mapFileModelToDomain(csvModel: SessionCSVModel): CollectingSession? {
        val info = parseSessionFilename(csvModel.filename) ?: return null

        return CollectingSession(info, csvModel.samples)
    }

    private fun makeFileName(sessionInfo: SessionInfo): String {
        val id = sessionInfo.id
        val start = TimeFormatHelper.timeOfDaySecondsToHhMmSs(sessionInfo.startTimeInSeconds)
        val stop = TimeFormatHelper.timeOfDaySecondsToHhMmSs(sessionInfo.stopTimeInSeconds)

        return FILENAME_PATTERN.format(id, start, stop)
    }

    /**
     * Retrieves values stored in a filename.
     *
     * [name] - filename with extension.
     * */
    fun parseSessionFilename(name: String): SessionInfo? {
        val match = FILENAME_REGEX.matchEntire(name) ?: return null

        val (idStr, startStr, stopStr) = match.destructured

        val id = idStr.toIntOrNull()
        val start = TimeFormatHelper.hhMmSsToSeconds(startStr)
        val stop = TimeFormatHelper.hhMmSsToSeconds(stopStr)

        if (id == null || start < 0 || stop < 0) return null

        return SessionInfo(id, start, stop)
    }

    fun parseFilenameListToSessionInfoList(filenames: List<String>): List<SessionInfo> {
        return filenames.mapNotNull { filename -> parseSessionFilename(filename) }
    }
}