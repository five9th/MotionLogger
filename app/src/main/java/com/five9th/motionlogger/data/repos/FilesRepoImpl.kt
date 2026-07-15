package com.five9th.motionlogger.data.repos

import android.app.Application
import com.five9th.motionlogger.data.datamodel.FilesRepoMapper
import com.five9th.motionlogger.data.datamodel.SessionCSVModel
import com.five9th.motionlogger.domain.entities.CollectingSession
import com.five9th.motionlogger.domain.entities.SensorSample
import com.five9th.motionlogger.domain.entities.SessionInfo
import com.five9th.motionlogger.domain.repos.FilesRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.File
import javax.inject.Inject

class FilesRepoImpl @Inject constructor (
    private val app: Application
) : FilesRepo {

    companion object {
        private const val SESSIONS_DIR = "sessions"
        private const val LAST_ID_FILE = "last_session_id.txt"

        // Example: "session-001-route_1-12:35:42-12:40:21.csv"
        const val FILENAME_PATTERN = "session-%03d-%s-%s-%s.csv"
        val FILENAME_REGEX =
            Regex("""session-(\d+)-(.*)-(.+)-(.+)\.csv""")
    }

    private val mapper = FilesRepoMapper()

    override suspend fun saveSession(session: CollectingSession) {
        saveSamples(mapper.mapDomainToFileModel(session))
    }

    private suspend fun saveSamples(fileModel: SessionCSVModel) {
        withContext(Dispatchers.IO) {
            val sessionsDir = getSessionsDir()
            val file = File(sessionsDir, fileModel.filename)

            file.bufferedWriter().use { writer ->
                writeSamples(writer, fileModel)
            }
        }
    }

    private fun writeSamples(writer: BufferedWriter, fileModel: SessionCSVModel) {
        writer.appendLine(fileModel.header)

        for (s in fileModel.samples) {
            writer.appendLine(
                "${s.timestampMs}," + s.values.joinToString(",")
            )
        }
    }

    override suspend fun getSavedSessions(): List<SessionInfo> {
        // get list of filenames
        val files: List<String> = getSessionsDir().listFiles { file ->
            file.isFile
        }?.map { file -> file.name } ?: listOf()

        // retrieve SessionInfo from filenames
        val sessions = mapper.parseFilenameListToSessionInfoList(files)

        return sessions
    }

    override suspend fun getSession(sessionId: Int): CollectingSession? {
        val file = getFileBySessionId(sessionId) ?: return null
        val csvModel = readSessionFromCsv(file)

        return mapper.mapFileModelToDomain(csvModel)
    }

    private fun getFileBySessionId(sessionId: Int): File? {
        // get list of files
        val files: Array<File> = getSessionsDir().listFiles { file ->
            file.isFile
        } ?: return null

        for (file in files) { // session-001-12:35:42-12:40:21.csv
            val parts = file.name.split('-')
            if (parts.size > 1) {
                val id = parts[1].toIntOrNull() ?: return null

                if (id == sessionId) return file
            }
        }

        return null
    }

    private fun readSessionFromCsv(file: File): SessionCSVModel {
        val lines = file.readLines()

        require(lines.isNotEmpty()) {
            "CSV file is empty"
        }

        val header = lines.first().trim()
        val expectedColumns = header.split(',').size

        val samples = lines
            .drop(1)
            .filter { it.isNotBlank() }
            .mapIndexed { lineIndex, line ->
                val cols = line.split(',')

                require(cols.size == expectedColumns) {
                    "Line ${lineIndex + 2}: expected $expectedColumns columns, got ${cols.size}"
                }

                val timestampMs = cols[0].trim().toLong()

                val values = FloatArray(cols.size - 1) { i ->
                    cols[i + 1].trim().toFloat()
                }

                SensorSample(
                    timestampMs = timestampMs,
                    values = values
                )
            }

        return SessionCSVModel(
            filename = file.name,
            header = header,
            samples = samples
        )
    }


    override suspend fun removeSession(sessionId: Int) {
        TODO("Not yet implemented")
    }

    override suspend fun saveLastId(id: Int) {
        withContext(Dispatchers.IO) {
            lastIdFile.writeText(id.toString())
        }
    }

    override suspend fun getLastId(): Int {
        return withContext(Dispatchers.IO) {
            if (!lastIdFile.exists()) {
                0 // start from 0 if no sessions yet
            } else {
                lastIdFile.readText().trim().toIntOrNull() ?: 0
            }
        }
    }

    private fun getSessionsDir(): File {
        val baseDir = app.getExternalFilesDir(null)
        val sessionsDir = File(baseDir, SESSIONS_DIR)

        if (!sessionsDir.exists()) {
            sessionsDir.mkdirs()   // create dir (and parents)
        }

        return sessionsDir
    }

    private val lastIdFile: File
        get() = File(app.getExternalFilesDir(null), LAST_ID_FILE)
}