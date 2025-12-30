package com.five9th.motionlogger.domain.usecases

import com.five9th.motionlogger.domain.entities.SessionInfo
import com.five9th.motionlogger.domain.repos.SessionsRepo
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObserveSessionListUseCase @Inject constructor (
    private val repo: SessionsRepo
) {
    operator fun invoke(): StateFlow<List<SessionInfo>> = repo.sessions
}