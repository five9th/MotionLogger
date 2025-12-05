package com.five9th.motionlogger.domain.usecases


class StopCollectUseCase(private val repo: SensorsRepo) {
    operator fun invoke() {
        repo.stop()
    }
}