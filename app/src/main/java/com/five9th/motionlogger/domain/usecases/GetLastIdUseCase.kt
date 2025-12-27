package com.five9th.motionlogger.domain.usecases

import javax.inject.Inject

class GetLastIdUseCase @Inject constructor (
    private val repo: FilesRepo
) {
    suspend operator fun invoke(): Int {
        return repo.getLastId()
    }
}