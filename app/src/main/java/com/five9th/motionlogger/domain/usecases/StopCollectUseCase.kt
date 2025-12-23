package com.five9th.motionlogger.domain.usecases

import javax.inject.Inject


class StopCollectUseCase @Inject constructor (
    private val repo: SensorsRepo
) {
    operator fun invoke() {
        repo.stop()
    }
}