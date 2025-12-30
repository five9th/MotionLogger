package com.five9th.motionlogger.domain.repos

import com.five9th.motionlogger.domain.entities.SessionInfo
import kotlinx.coroutines.flow.StateFlow

interface SessionsRepo {
    val sessions: StateFlow<List<SessionInfo>>
    fun addSession(session: SessionInfo)
    fun removeSession(sessionId: Int)
}