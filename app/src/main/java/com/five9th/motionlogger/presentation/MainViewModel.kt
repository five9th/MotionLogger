package com.five9th.motionlogger.presentation

import android.app.Application
import android.os.SystemClock
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CollectionStats(
    val elapsedMillis: Long,
    val samplesCount: Int
)

// (2) TODO: collect in foreground service
// TODO: load samples to current_session.csv every few minutes
// TODO: display list of recorded sessions
// TODO: add session id
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val tag = "MainViewModel"

    // ---- repos ----
    private val sensorsRepo = SensorsRepoImpl(app) // use DI (TODO)
    private val filesRepo = FilesRepoImpl(app) // use DI

    // ---- Use Cases ----
    private val getSensorsInfoUseCase = GetSensorsInfoUseCase(sensorsRepo)

    private val observeSensorsUseCase = ObserveSensorsUseCase(sensorsRepo)
    private val startCollectUseCase = StartCollectUseCase(sensorsRepo)
    private val stopCollectUseCase = StopCollectUseCase(sensorsRepo)

    private val saveSamplesUseCase = SaveSamplesUseCase(filesRepo)

    // ---- LiveData's and Flow's ----
    private val _sensorsInfoLD = MutableLiveData<SensorsInfo>()
    val sensorsInfoLD: LiveData<SensorsInfo> = _sensorsInfoLD

    private val sensorDataSF: StateFlow<SensorSample?> = observeSensorsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val isCollectingSF: StateFlow<Boolean> = sensorsRepo.isCollecting

    private val _collectionStatsFS = MutableStateFlow(
        CollectionStats(0L, 0)
    )
    val collectionStatsFS: StateFlow<CollectionStats> = _collectionStatsFS



    private val collectedSamples = mutableListOf<SensorSample>()

    private var startTimestamp: Long = 0L
    private var stopTimestamp: Long = 0L

    private var collectingJob: Job? = null

    private var timerJob: Job? = null
    private var startTimerTime: Long = 0L

    fun getSensorsInfo() {
        _sensorsInfoLD.value = getSensorsInfoUseCase()
    }

    fun startCollect() {
        if (isCollectingSF.value) return

        startCollectUseCase()  // tell repo to collect sensor data
        startCollectJob()  // saving samples from the flow to the list
        startTimerJob()
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

    private fun startTimerJob() {
        if (timerJob != null) return

        startTimerTime = SystemClock.elapsedRealtime()

        timerJob = viewModelScope.launch {
            while (isActive) {
                updateValuesByTimer()
                delay(200) // update UI 5x per second (smooth, cheap)
            }
        }
    }

    private fun updateValuesByTimer() {
        // elapsed time
        val elapsed = SystemClock.elapsedRealtime() - startTimerTime

        // samples count
        val count = collectedSamples.size

        _collectionStatsFS.value = CollectionStats(elapsed, count)
    }

    fun stopCollect() {
        if (!isCollectingSF.value) return

        stopCollectUseCase()
        stopCollectJob()
        stopTimerJob()

        stopTimestamp = System.currentTimeMillis()

        saveToCsv()
    }

    private fun stopCollectJob() {
        collectingJob?.cancel()
        collectingJob = null
    }

    private fun stopTimerJob() {
        timerJob?.cancel()
        timerJob = null

        // reset values
        _collectionStatsFS.value = CollectionStats(0L, 0)
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