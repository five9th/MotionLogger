package com.five9th.motionlogger.domain.entities

data class CollectingSession(
    val info: SessionInfo,
    val schema: SensorSchema,
    val samples: List<SensorSample>,
) {
    val id: Int
        get() = info.id

    fun getWindows(windowSize: Int): List<SampleWindow> =
        samples
        .chunked(windowSize)
        .mapNotNull { list ->
            if (list.size == windowSize)
                SampleWindow(
                    schema,
                    list.toList()
                )
            else
                null
        }
}

data class SessionInfo(
    val id: Int,
    val keyWord: String,

    /** Amount of seconds from 0 to 86_399, represents time in HH:mm:ss format */
    val startTimeInSeconds: Int,

    /** Amount of seconds from 0 to 86_399, represents time in HH:mm:ss format */
    val stopTimeInSeconds: Int
)