package com.five9th.motionlogger.domain.usecases

import com.five9th.motionlogger.domain.entities.CollectingSession
import com.five9th.motionlogger.domain.repos.FilesRepo
import javax.inject.Inject

class GetSessionUseCase @Inject constructor (
    private val repo: FilesRepo
) {
    suspend operator fun invoke(sessionId: Int): CollectingSession? {
        return repo.getSession(sessionId)
    }
}