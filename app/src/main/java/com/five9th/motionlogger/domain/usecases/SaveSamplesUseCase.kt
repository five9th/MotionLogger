package com.five9th.motionlogger.domain.usecases

import com.five9th.motionlogger.domain.entities.CollectingSession
import javax.inject.Inject

class SaveSamplesUseCase @Inject constructor (
    private val repo: FilesRepo
) {
    suspend operator fun invoke(session: CollectingSession) {
        repo.saveSession(session)
    }
}