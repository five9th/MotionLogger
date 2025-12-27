package com.five9th.motionlogger.data

import android.app.Application
import com.five9th.motionlogger.domain.entities.SensorSample
import com.five9th.motionlogger.domain.usecases.FilesRepo
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
    }

    override suspend fun saveSamples(samples: List<SensorSample>, filename: String) {
        withContext(Dispatchers.IO) {
            val sessionsDir = getSessionsDir()
            val file = File(sessionsDir, filename)

            file.bufferedWriter().use { writer ->
                writeSamples(writer, samples)
            }
        }
    }

    private fun writeSamples(writer: BufferedWriter, samples: List<SensorSample>) {
        writer.appendLine("timestamp,ax,ay,az,gx,gy,gz,roll,pitch,yaw")  // <-- this hardcoded stuff is kinda bad :/

        for (s in samples) {
            writer.appendLine(
                "${s.timestampMs},${s.accX},${s.accY},${s.accZ}," +
                        "${s.gyroX},${s.gyroY},${s.gyroZ}," +
                        "${s.roll},${s.pitch},${s.yaw}"
            )
        }
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