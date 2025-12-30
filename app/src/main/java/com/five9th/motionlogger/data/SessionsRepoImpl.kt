package com.five9th.motionlogger.data

import com.five9th.motionlogger.domain.entities.SessionInfo
import com.five9th.motionlogger.domain.repos.SessionsRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class SessionsRepoImpl @Inject constructor() : SessionsRepo {

    private val _sessions = MutableStateFlow<List<SessionInfo>>(emptyList())
    override val sessions: StateFlow<List<SessionInfo>> = _sessions

    override fun addSession(session: SessionInfo) {
        _sessions.update { it + session }
    }

    override fun addAll(sessions: List<SessionInfo>) {
        _sessions.update { current ->
            (current + sessions)
                .distinctBy { it.id } // avoid duplicates (by id)
        }
    }

    override fun removeSession(sessionId: Int) {
        _sessions.update { list ->
            list.filterNot { it.id == sessionId }
        }
    }

    override fun clear() {
        _sessions.value = emptyList()
    }
}
