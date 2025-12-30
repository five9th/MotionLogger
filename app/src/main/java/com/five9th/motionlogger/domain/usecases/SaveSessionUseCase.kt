package com.five9th.motionlogger.domain.usecases

import com.five9th.motionlogger.domain.entities.CollectingSession
import com.five9th.motionlogger.domain.repos.FilesRepo
import com.five9th.motionlogger.domain.repos.SessionsRepo
import javax.inject.Inject

class SaveSessionUseCase @Inject constructor (
    private val filesRepo: FilesRepo,
    private val sessionsRepo: SessionsRepo
) {
    suspend operator fun invoke(session: CollectingSession) {
        sessionsRepo.addSession(session.info)
        filesRepo.saveSession(session)
    }
}