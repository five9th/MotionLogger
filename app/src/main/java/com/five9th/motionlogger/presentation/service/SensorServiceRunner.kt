package com.five9th.motionlogger.presentation.service

import android.os.SystemClock
import android.util.Log
import com.five9th.motionlogger.domain.entities.CollectingSession
import com.five9th.motionlogger.domain.entities.SensorSample
import com.five9th.motionlogger.domain.entities.SessionInfo
import com.five9th.motionlogger.domain.usecases.GetLastIdUseCase
import com.five9th.motionlogger.domain.usecases.ObserveCollectingStateUseCase
import com.five9th.motionlogger.domain.usecases.ObserveSensorsUseCase
import com.five9th.motionlogger.domain.usecases.SaveLastIdUseCase
import com.five9th.motionlogger.domain.usecases.SaveSessionUseCase
import com.five9th.motionlogger.domain.usecases.StartCollectUseCase
import com.five9th.motionlogger.domain.usecases.StopCollectUseCase
import com.five9th.motionlogger.domain.utils.TimeFormatHelper
import com.five9th.motionlogger.presentation.uimodel.CollectionStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

class SensorServiceRunner @Inject constructor (
    observeCollectingStateUseCase: ObserveCollectingStateUseCase,
    observeSensorsUseCase: ObserveSensorsUseCase,

    private val startCollectUseCase: StartCollectUseCase,
    private val stopCollectUseCase: StopCollectUseCase,

    private val saveSessionUseCase: SaveSessionUseCase,

    private val saveLastIdUseCase: SaveLastIdUseCase,
    private val getLastIdUseCase: GetLastIdUseCase
) : ISensorCollector {

    private val tag = "SensorService"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ---- Flows ----
    private val sensorDataSF: StateFlow<SensorSample?> = observeSensorsUseCase()
        .stateIn(scope, SharingStarted.WhileSubscribed(), null)  // <-- maybe use smth different

    override val isCollectingSF: StateFlow<Boolean> = observeCollectingStateUseCase()

    private val _collectionStatsSF = MutableStateFlow(
        CollectionStats(0L, 0)
    )
    override val collectionStatsSF: StateFlow<CollectionStats> = _collectionStatsSF.asStateFlow()

    private val _sessionIdSF = MutableStateFlow(ID_UNDEFINED)
    override val sessionIdSF: StateFlow<Int> = _sessionIdSF.asStateFlow()

    private val _savingCompletedSF = MutableSharedFlow<Unit>()
    override val savingCompletedSF: SharedFlow<Unit> = _savingCompletedSF.asSharedFlow()

    // ---- Variables ----
    // TODO: add some self-counting timer for debugging
    private val collectedSamples = mutableListOf<SensorSample>()

    private var startTimestamp: Long = 0L
    private var stopTimestamp: Long = 0L

    private var collectingJob: Job? = null

    private var timerJob: Job? = null
    private var startTimerTime: Long = 0L

    private var sessionId = ID_UNDEFINED
    private var readLastIdJob: Job? = null

    // ---- Methods ----
    init {
        initLastId()
    }

    private fun initLastId() {
        readLastIdJob = scope.launch {
            sessionId = getLastIdUseCase()
        }
    }

    override fun startCollect() {
        if (isCollectingSF.value) return

        setIdForNewSession()

        startCollectUseCase()  // tell repo to collect sensor data
        startCollectJob()  // saving samples from the flow to the list
        startTimerJob()
    }

    private fun setIdForNewSession() { // TODO: test this sequence
        if (sessionId == ID_UNDEFINED) { // if last id hasn't been read (yet)
            defferIdIncrementation()
        }
        else {
            incrementSessionId()
        }
    }

    private fun defferIdIncrementation() {
        scope.launch {
            // wait for data
            withTimeoutOrNull(2000) {
                readLastIdJob?.join()
            }
            readLastIdJob?.cancel() // cansel after the wait (in case it's still running)

            // if id is still undefined after the wait
            if (sessionId == ID_UNDEFINED) {
                handleMissingLastSessionId() // set some value into sessionId
            }

            incrementSessionId()
        }
    }

    private fun incrementSessionId() {
        sessionId++
        _sessionIdSF.value = sessionId

        scope.launch {
            saveLastIdUseCase(sessionId)
        }
    }

    /** Defines what the app does if it failed to retrieve last session ID */
    private fun handleMissingLastSessionId() {
        sessionId = 0 // just start over
    }

    private fun startCollectJob() {
        if (collectingJob != null) return

        collectedSamples.clear()
        startTimestamp = System.currentTimeMillis()

        collectingJob = scope.launch {
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

        timerJob = scope.launch {
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

        _collectionStatsSF.value = CollectionStats(elapsed, count)
    }

    override fun stopCollectAndSave() {
        stopCollect()
        saveAndFinish()
    }

    private fun stopCollect() {
        if (!isCollectingSF.value) return

        stopCollectUseCase()
        stopCollectJob()
        stopTimerJob()

        stopTimestamp = System.currentTimeMillis()
    }

    private fun saveAndFinish() {
        val savingJob = saveToCsv()

        scope.launch {
            savingJob?.join()
            _savingCompletedSF.emit(Unit)
        }
    }

    private fun stopCollectJob() {
        collectingJob?.cancel()
        collectingJob = null
    }

    private fun stopTimerJob() {
        timerJob?.cancel()
        timerJob = null

        // reset values
        _collectionStatsSF.value = CollectionStats(0L, 0)
    }

    private fun saveToCsv(): Job? {
        if (collectedSamples.isEmpty()) {
            Log.w(tag, "Nothing to save")
            return null
        }

        val session = makeSession()

        Log.d(tag, "Saving session #${session.id} (${session.samples.size} samples).")

        return scope.launch {
            saveSessionUseCase(session)
        }
    }

    private fun makeSession() = CollectingSession(
        SessionInfo(
            id = sessionId,
            startTimeInSeconds = TimeFormatHelper.unixTimeMillisToTimeOfDaySeconds(startTimestamp),
            stopTimeInSeconds = TimeFormatHelper.unixTimeMillisToTimeOfDaySeconds(stopTimestamp)
        ),
        collectedSamples
    )

    fun cancelScope() {
        scope.cancel()
    }

    companion object {
        private const val ID_UNDEFINED = -1
    }
}