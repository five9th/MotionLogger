package com.five9th.motionlogger.domain.usecases

import com.five9th.motionlogger.domain.repos.FilesRepo
import com.five9th.motionlogger.domain.repos.SessionsRepo
import javax.inject.Inject

class RemoveSessionUseCase @Inject constructor (
    private val filesRepo: FilesRepo,
    private val sessionsRepo: SessionsRepo
) {
    suspend operator fun invoke(sessionId: Int) {
        sessionsRepo.removeSession(sessionId)
        filesRepo.removeSession(sessionId)
    }
}