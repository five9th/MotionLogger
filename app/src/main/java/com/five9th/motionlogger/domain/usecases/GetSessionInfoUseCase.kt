package com.five9th.motionlogger.domain.usecases

import com.five9th.motionlogger.domain.entities.SessionInfo
import com.five9th.motionlogger.domain.repos.SessionsRepo
import javax.inject.Inject

class GetSessionInfoUseCase @Inject constructor (
    private val repo: SessionsRepo
) {
    operator fun invoke(sessionId: Int): SessionInfo? {
        return repo.getSessionInfo(sessionId)
    }
}