package com.five9th.motionlogger.domain.usecases

import com.five9th.motionlogger.domain.repos.FilesRepo
import com.five9th.motionlogger.domain.repos.SessionsRepo
import javax.inject.Inject

class ReloadSavedSessionsUseCase @Inject constructor (
    private val filesRepo: FilesRepo,
    private val sessionsRepo: SessionsRepo
) {
    suspend operator fun invoke() {
        sessionsRepo.clear()
        val sessions = filesRepo.getSavedSessions()
        sessionsRepo.addAll(sessions)
    }
}