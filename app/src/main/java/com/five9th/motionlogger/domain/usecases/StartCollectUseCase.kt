package com.five9th.motionlogger.domain.usecases

import javax.inject.Inject


class StartCollectUseCase @Inject constructor (
    private val repo: SensorsRepo
) {
    operator fun invoke() {
        repo.start()
    }
}