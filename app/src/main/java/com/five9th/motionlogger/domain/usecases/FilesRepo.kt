package com.five9th.motionlogger.domain.usecases

import com.five9th.motionlogger.domain.entities.CollectingSession

interface FilesRepo {
    suspend fun saveSession(session: CollectingSession)
    suspend fun saveLastId(id: Int)
    suspend fun getLastId(): Int
}