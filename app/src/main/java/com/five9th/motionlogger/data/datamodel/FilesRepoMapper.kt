package com.five9th.motionlogger.data.datamodel

import com.five9th.motionlogger.data.repos.FilesRepoImpl.Companion.FILENAME_PATTERN
import com.five9th.motionlogger.data.repos.FilesRepoImpl.Companion.FILENAME_REGEX
import com.five9th.motionlogger.domain.entities.CollectingSession
import com.five9th.motionlogger.domain.entities.SensorField
import com.five9th.motionlogger.domain.entities.SensorSchema
import com.five9th.motionlogger.domain.entities.SessionInfo
import com.five9th.motionlogger.domain.utils.TimeFormatHelper

class FilesRepoMapper {

    fun mapDomainToFileModel(session: CollectingSession): SessionCSVModel {
        return SessionCSVModel(
            makeSessionFilename(session.info),
            session.schema.toCsvHeader(),
            session.samples
        )
    }

    fun mapFileModelToDomain(csvModel: SessionCSVModel): CollectingSession? {
        val info = parseSessionFilename(csvModel.filename) ?: return null
        val schema = schemaFromCsvHeader(csvModel.header)

        return CollectingSession(info, schema, csvModel.samples)
    }

    private fun SensorSchema.toCsvHeader(
        timestampColumn: String = "timestamp"
    ): String {
        return buildString {
            append(timestampColumn)

            for (field in fields) {
                append(',')
                append(field.id)
            }
        }
    }

    private fun schemaFromCsvHeader(
        header: String,
        timestampColumn: String = "timestamp"
    ): SensorSchema {

        val columns = header
            .split(',')
            .map { it.trim() }

        require(columns.isNotEmpty()) {
            "CSV header is empty"
        }

        require(columns.first() == timestampColumn) {
            "First column must be '$timestampColumn'"
        }

        return SensorSchema(
            SensorSchema.VERSION_NOT_SET,
            columns
                .drop(1)
                .map { SensorField(it) }
        )
    }


    private fun makeSessionFilename(sessionInfo: SessionInfo): String {
        val id = sessionInfo.id
        val keyWord = sessionInfo.keyWord
        val start = TimeFormatHelper.timeOfDaySecondsToHhMmSs(sessionInfo.startTimeInSeconds)
        val stop = TimeFormatHelper.timeOfDaySecondsToHhMmSs(sessionInfo.stopTimeInSeconds)

        return FILENAME_PATTERN.format(id, keyWord, start, stop)
    }

    /**
     * Retrieves values stored in a filename.
     *
     * [name] - filename with extension.
     * */
    fun parseSessionFilename(name: String): SessionInfo? {
        val match = FILENAME_REGEX.matchEntire(name) ?: return null

        val (idStr, keyWord, startStr, stopStr) = match.destructured

        val id = idStr.toIntOrNull()
        val start = TimeFormatHelper.hhMmSsToSeconds(startStr)
        val stop = TimeFormatHelper.hhMmSsToSeconds(stopStr)

        if (id == null || start < 0 || stop < 0) return null

        return SessionInfo(id, keyWord, start, stop)
    }

    fun parseFilenameListToSessionInfoList(filenames: List<String>): List<SessionInfo> {
        return filenames.mapNotNull { filename -> parseSessionFilename(filename) }
    }
}