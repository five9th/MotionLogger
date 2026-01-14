package com.five9th.motionlogger.presentation.uimodel

data class CollectionStats(
    val elapsedMillis: Long,
    val samplesCount: Int
)

data class SessionItem(
    val number: String,
    val keyWord: String,
    val startTime: String,
    val stopTime: String,
    val duration: String
)