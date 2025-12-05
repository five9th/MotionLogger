package com.five9th.motionlogger.domain.usecases


class StartCollectUseCase(private val repo: SensorsRepo) {
    operator fun invoke() {
        repo.start()
    }
}