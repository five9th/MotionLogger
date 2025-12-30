package com.five9th.motionlogger.domain.usecases

import com.five9th.motionlogger.domain.repos.SensorsRepo
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObserveCollectingStateUseCase @Inject constructor (
    private val repo: SensorsRepo
) {
    operator fun invoke(): StateFlow<Boolean> = repo.isCollecting
}