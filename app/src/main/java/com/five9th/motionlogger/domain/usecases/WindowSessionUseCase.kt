package com.five9th.motionlogger.domain.usecases

import com.five9th.motionlogger.domain.entities.CollectingSession
import com.five9th.motionlogger.domain.entities.WINDOW_SIZE

class WindowSessionUseCase {
    operator fun invoke(session: CollectingSession) = session.getWindows(WINDOW_SIZE)
}