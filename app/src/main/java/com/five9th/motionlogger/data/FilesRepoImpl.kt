package com.five9th.motionlogger.data

import android.app.Application
import com.five9th.motionlogger.domain.entities.CollectingSession
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

        const val FILENAME_PATTERN = "session-%03d-%s-%s.csv" // session-1-12:35:42-12:40:21.csv
    }

    private val mapper = RepoMapper()

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
        val columnNames = fileModel.columns.joinToString(separator = ",")
        writer.appendLine(columnNames)

        for (s in fileModel.samples) {
            writer.appendLine(
                "${s.timestampMs},${s.accX},${s.accY},${s.accZ}," +
                        "${s.gyroX},${s.gyroY},${s.gyroZ}," +
                        "${s.roll},${s.pitch},${s.yaw}"
            )
        }
    }

    override suspend fun getSavedSessions(): List<SessionInfo> {
        // TODO: read actual files
        return listOf(
            SessionInfo(  // test dump
                123, 65, 3700
            )
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