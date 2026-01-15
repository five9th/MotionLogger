package com.five9th.motionlogger.data.datamodel

import com.five9th.motionlogger.domain.entities.SensorSample

data class SessionCSVModel(
    val filename: String,
    val columns: List<String>,  // e.g. ['timestamp','ax','ay','az']
    val samples: List<SensorSample>
)