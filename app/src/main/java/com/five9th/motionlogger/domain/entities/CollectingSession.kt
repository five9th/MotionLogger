package com.five9th.motionlogger.domain.entities

data class CollectingSession(
    val info: SessionInfo,
    val samples: List<SensorSample>,
) {
    val id: Int
        get() = info.id
}

data class SessionInfo(
    val id: Int,

    /** Amount of seconds from 0 to 86_399, represents time in HH:mm:ss format */
    val startTimeInSeconds: Int,

    /** Amount of seconds from 0 to 86_399, represents time in HH:mm:ss format */
    val stopTimeInSeconds: Int
)