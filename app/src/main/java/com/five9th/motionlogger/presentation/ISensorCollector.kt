package com.five9th.motionlogger.presentation

import com.five9th.motionlogger.domain.entities.SensorSample
import kotlinx.coroutines.flow.StateFlow

interface ISensorCollector {
    fun startCollect()
    fun stopCollectAndSave()
    val isCollectingSF: StateFlow<Boolean>
    val collectionStatsSF: StateFlow<CollectionStats>
    fun getCollectedData(): List<SensorSample>
}