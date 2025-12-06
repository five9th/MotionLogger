package com.five9th.motionlogger.presentation

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.five9th.motionlogger.data.FilesRepoImpl
import com.five9th.motionlogger.data.SensorsRepoImpl
import com.five9th.motionlogger.domain.entities.SensorSample
import com.five9th.motionlogger.domain.entities.SensorsInfo
import com.five9th.motionlogger.domain.usecases.GetSensorsInfoUseCase
import com.five9th.motionlogger.domain.usecases.ObserveSensorsUseCase
import com.five9th.motionlogger.domain.usecases.SaveSamplesUseCase
import com.five9th.motionlogger.domain.usecases.StartCollectUseCase
import com.five9th.motionlogger.domain.usecases.StopCollectUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// TODO: collect in foreground service
// TODO: load samples to current_session.csv every few minutes
// TODO: display timer (elapsed time and collected samples
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val tag = "MainViewModel"

    private val sensorsRepo = SensorsRepoImpl(app) // use DI (TODO)
    private val filesRepo = FilesRepoImpl(app) // use DI

    private val getSensorsInfoUseCase = GetSensorsInfoUseCase(sensorsRepo)

    private val observeSensorsUseCase = ObserveSensorsUseCase(sensorsRepo)
    private val startCollectUseCase = StartCollectUseCase(sensorsRepo)
    private val stopCollectUseCase = StopCollectUseCase(sensorsRepo)

    private val saveSamplesUseCase = SaveSamplesUseCase(filesRepo)

    private val _sensorsInfoLD = MutableLiveData<SensorsInfo>()
    val sensorsInfoLD: LiveData<SensorsInfo>
        get() = _sensorsInfoLD

    private val collectedSamples = mutableListOf<SensorSample>()

    private var startTimestamp: Long = 0L
    private var stopTimestamp: Long = 0L

    private var collectingJob: Job? = null

    fun getSensorsInfo() {
        _sensorsInfoLD.value = getSensorsInfoUseCase()
    }

    val sensorDataSF: StateFlow<SensorSample?> = observeSensorsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val isCollectingSF: StateFlow<Boolean> = sensorsRepo.isCollecting

    fun startCollect() {
        if (isCollectingSF.value) return

        startCollectUseCase()  // tell repo to collect sensor data
        startCollectJob()  // saving samples from the flow to the list
    }

    private fun startCollectJob() {
        if (collectingJob != null) return

        collectedSamples.clear()
        startTimestamp = System.currentTimeMillis()

        collectingJob = viewModelScope.launch {
            sensorDataSF.collect { sample ->
                if (sample != null) {
                    collectedSamples.add(sample)
                }
            }
        }
    }

    fun stopCollect() {
        if (!isCollectingSF.value) return

        stopCollectUseCase()
        stopCollectJob()

        stopTimestamp = System.currentTimeMillis()

        saveToCsv()
    }

    private fun stopCollectJob() {
        collectingJob?.cancel()
        collectingJob = null
    }

    private fun saveToCsv() {
        if (collectedSamples.isEmpty()) {
            Log.w(tag, "Nothing to save")
            return
        }

        val filename = makeFileName()
        Log.d(tag, "Saving ${collectedSamples.size} samples into \"$filename\".")

        viewModelScope.launch {
            saveSamplesUseCase(collectedSamples, filename)
        }
    }

    private fun makeFileName(): String {
        return "session-${formatTimestamp(startTimestamp)}-${formatTimestamp(stopTimestamp)}.csv"
    }

    private fun formatTimestamp(millis: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(millis))
    }
}