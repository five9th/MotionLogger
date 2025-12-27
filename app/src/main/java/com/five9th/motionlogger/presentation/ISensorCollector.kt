package com.five9th.motionlogger.presentation

import com.five9th.motionlogger.domain.entities.SensorSample
import kotlinx.coroutines.flow.StateFlow

interface ISensorCollector {
    // (TODO) How it (maybe) should be redesigned:
    fun startCollect() // use `newIntentStart`
    fun stopCollectAndSave() // and `newIntentStop` instead
    val isCollectingSF: StateFlow<Boolean> // use a repo that the Service emits to and the ViewModel collects from
    val collectionStatsSF: StateFlow<CollectionStats> //  -//-
    val sessionIdSF: StateFlow<Int> //  -//-
}