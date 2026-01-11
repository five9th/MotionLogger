package com.five9th.motionlogger.domain.repos

import com.five9th.motionlogger.domain.entities.ModelOutput
import com.five9th.motionlogger.domain.entities.SampleWindow

interface ModelInference {
    fun run(input: SampleWindow): ModelOutput
}