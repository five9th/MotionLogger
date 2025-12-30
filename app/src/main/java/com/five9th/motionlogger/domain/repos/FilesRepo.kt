package com.five9th.motionlogger.domain.repos

import com.five9th.motionlogger.domain.entities.CollectingSession
import com.five9th.motionlogger.domain.entities.SessionInfo

interface FilesRepo {
    suspend fun saveSession(session: CollectingSession)
    suspend fun getSavedSessions(): List<SessionInfo>
    suspend fun removeSession(sessionId: Int)
    suspend fun saveLastId(id: Int)
    suspend fun getLastId(): Int
}