package com.five9th.motionlogger.presentation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.five9th.motionlogger.R
import com.five9th.motionlogger.domain.entities.CollectingSession
import com.five9th.motionlogger.domain.entities.SensorSample
import com.five9th.motionlogger.domain.usecases.GetLastIdUseCase
import com.five9th.motionlogger.domain.usecases.ObserveSensorsUseCase
import com.five9th.motionlogger.domain.usecases.SaveLastIdUseCase
import com.five9th.motionlogger.domain.usecases.SaveSamplesUseCase
import com.five9th.motionlogger.domain.usecases.SensorsRepo
import com.five9th.motionlogger.domain.usecases.StartCollectUseCase
import com.five9th.motionlogger.domain.usecases.StopCollectUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject


data class CollectionStats(
    val elapsedMillis: Long,
    val samplesCount: Int
)

// TODO: move non-service logic into some worker/runner class
@AndroidEntryPoint
class SensorCollectionService : Service(), ISensorCollector {

    private val tag = "SensorCollectionService"

    // ==== Sensor Data Collection ====

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ---- Repo and Use Cases ----
    @Inject lateinit var sensorsRepo: SensorsRepo

    @Inject lateinit var observeSensorsUseCase: ObserveSensorsUseCase
    @Inject lateinit var startCollectUseCase: StartCollectUseCase
    @Inject lateinit var stopCollectUseCase: StopCollectUseCase

    @Inject lateinit var saveSamplesUseCase: SaveSamplesUseCase

    @Inject lateinit var saveLastIdUseCase: SaveLastIdUseCase
    @Inject lateinit var getLastIdUseCase: GetLastIdUseCase

    // ---- Flows ----
    private lateinit var sensorDataSF: StateFlow<SensorSample?>

    override lateinit var isCollectingSF: StateFlow<Boolean>

    private val _collectionStatsSF = MutableStateFlow(
        CollectionStats(0L, 0)
    )
    override val collectionStatsSF: StateFlow<CollectionStats> = _collectionStatsSF.asStateFlow()

    private val _sessionIdSF = MutableStateFlow(ID_UNDEFINED)
    override val sessionIdSF: StateFlow<Int> = _sessionIdSF.asStateFlow()

    // ---- Variables ----
    private val collectedSamples = mutableListOf<SensorSample>()

    private var startTimestamp: Long = 0L
    private var stopTimestamp: Long = 0L

    private var collectingJob: Job? = null

    private var timerJob: Job? = null
    private var startTimerTime: Long = 0L

    private var updateNotificationJob: Job? = null

    private var sessionId = ID_UNDEFINED
    private var readLastIdJob: Job? = null

    // ---- Methods ----

    override fun onCreate() {  // Hilt injects before onCreate()
        super.onCreate()

        initFlows()
        initNotification()
        initLastId()
    }

    private fun initFlows() {  // After onCreate() was called
        sensorDataSF = observeSensorsUseCase()
            .stateIn(scope, SharingStarted.WhileSubscribed(), null)

        isCollectingSF = sensorsRepo.isCollecting
    }

    private fun initNotification() {
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        builder = createNotificationBuilder()
        createNotificationChannel()
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
            stopSelf()
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
            saveSamplesUseCase(session)
        }
    }

    private fun makeSession() = CollectingSession(
        sessionId,
        collectedSamples,
        TimeFormatHelper.unixTimeMillisToTimeOfDaySeconds(startTimestamp),
        TimeFormatHelper.unixTimeMillisToTimeOfDaySeconds(stopTimestamp)
    )


    // ====== Service-specific stuff ======

    private lateinit var notificationManager: NotificationManager

    private lateinit var builder: NotificationCompat.Builder

    private var isServiceStarted = false

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): SensorCollectionService = this@SensorCollectionService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startCollectionService()
            ACTION_STOP -> stopCollectionService()
            null -> stopSelf()  // if service got restarted (somehow)
        }
        return START_NOT_STICKY
    }

    private fun startCollectionService() {
        if (!isServiceStarted) {
            startForeground(NOTIFICATION_ID, rebuildNotification())
            isServiceStarted = true

            startUpdNotificationJob()
        }

        startCollect()
    }

    private fun startUpdNotificationJob() {
        if (updateNotificationJob != null) return

        updateNotificationJob = scope.launch {
            while (isActive) {
                updateNotification()
                delay(1000) // slower update rate than ViewModel
            }
        }
    }

    private fun stopCollectionService() {
        // stop self after saving
        stopCollectAndSave()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun createNotificationBuilder(): NotificationCompat.Builder {
        // Intent to open app when notification is tapped
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Stop action button
        val stopIntent = Intent(this, SensorCollectionService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.collecting_sensor_data))
            .setContentText("collecting...")
            .setSmallIcon(R.drawable.ic_sensors)
            .setOnlyAlertOnce(true)
            .setOngoing(true) // Cannot be dismissed by user
            .setContentIntent(contentIntent)
            .addAction(
                R.drawable.ic_stop,
                "Stop",
                stopPendingIntent
            )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW // LOW = no sound/vibration
        ).apply {
            description = getString(R.string.notif_channel_description)
            setShowBadge(false)
        }

        notificationManager.createNotificationChannel(channel)
    }

    private fun rebuildNotification(
        elapsedTime: String = "00:00",
        sampleCount: Int = 0
    ): Notification = builder
        .setContentText(getString(R.string.notif_content_text, elapsedTime, sampleCount))
        .build()

    private fun updateNotification() {
        val stats = collectionStatsSF.value
        val elapsed = TimeFormatHelper.elapsedMillisToMmSs(stats.elapsedMillis)
        val count = stats.samplesCount

        val notification = rebuildNotification(elapsed, count)

        notificationManager.notify(NOTIFICATION_ID, notification)
    }


    companion object {
        private const val ACTION_START = "START_COLLECTION"
        private const val ACTION_STOP = "STOP_COLLECTION"

        private const val ID_UNDEFINED = -1

        private const val NOTIFICATION_ID = 100

        private const val CHANNEL_ID = "collecting_sensor_data"

        fun newIntent(context: Context) =
            Intent(context, SensorCollectionService::class.java)

        fun newIntentStart(context: Context) =
            Intent(context, SensorCollectionService::class.java)
                .setAction(ACTION_START)

        fun newIntentStop(context: Context) =
            Intent(context, SensorCollectionService::class.java)
                .setAction(ACTION_STOP)
    }
}