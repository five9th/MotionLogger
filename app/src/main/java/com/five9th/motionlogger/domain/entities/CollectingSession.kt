package com.five9th.motionlogger.domain.entities

class CollectingSession(
    val id: Int,

    val samples: List<SensorSample>,

    /** Amount of seconds from 0 to 86_399, represents time in HH:mm:ss format */
    val startTimeInSeconds: Int,

    /** Amount of seconds from 0 to 86_399, represents time in HH:mm:ss format */
    val stopTimeInSeconds: Int
)