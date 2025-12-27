package com.five9th.motionlogger.domain.usecases

import javax.inject.Inject

class SaveLastIdUseCase @Inject constructor (
    private val repo: FilesRepo
) {
    suspend operator fun invoke(id: Int) {
        repo.saveLastId(id)
    }
}