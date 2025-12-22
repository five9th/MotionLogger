package com.five9th.motionlogger.presentation

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.five9th.motionlogger.R
import com.five9th.motionlogger.data.FilesRepoImpl
import com.five9th.motionlogger.data.SensorsRepoImpl
import com.five9th.motionlogger.domain.entities.SensorSample
import com.five9th.motionlogger.domain.usecases.ObserveSensorsUseCase
import com.five9th.motionlogger.domain.usecases.SaveSamplesUseCase
import com.five9th.motionlogger.domain.usecases.StartCollectUseCase
import com.five9th.motionlogger.domain.usecases.StopCollectUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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

class SensorCollectionService(app: Application) : Service(), ISensorCollector {

    private val tag = "SensorCollectionService"

    // ==== Sensor Data Collection ====

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ---- repos ----
    private val sensorsRepo = SensorsRepoImpl(app) // use DI (TODO)
    private val filesRepo = FilesRepoImpl(app) // use DI

    // ---- Use Cases ----
    private val observeSensorsUseCase = ObserveSensorsUseCase(sensorsRepo)
    private val startCollectUseCase = StartCollectUseCase(sensorsRepo)
    private val stopCollectUseCase = StopCollectUseCase(sensorsRepo)

    private val saveSamplesUseCase = SaveSamplesUseCase(filesRepo)

    // ---- Flows ----
    private val sensorDataSF: StateFlow<SensorSample?> = observeSensorsUseCase()
        .stateIn(scope, SharingStarted.WhileSubscribed(), null)

    private val _collectionStatsSF = MutableStateFlow(
        CollectionStats(0L, 0)
    )
    override val collectionStatsSF: StateFlow<CollectionStats> = _collectionStatsSF

    override val isCollectingSF: StateFlow<Boolean> = sensorsRepo.isCollecting

    // ---- Variables ----
    private val collectedSamples = mutableListOf<SensorSample>()

    private var startTimestamp: Long = 0L
    private var stopTimestamp: Long = 0L

    private var collectingJob: Job? = null

    private var timerJob: Job? = null
    private var startTimerTime: Long = 0L

    // ---- Methods ----

    override fun startCollect() {
        if (isCollectingSF.value) return

        startCollectUseCase()  // tell repo to collect sensor data
        startCollectJob()  // saving samples from the flow to the list
        startTimerJob()
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

        val filename = makeFileName()
        Log.d(tag, "Saving ${collectedSamples.size} samples into \"$filename\".")

        return scope.launch {
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

    override fun getCollectedData(): List<SensorSample> {
        TODO("Not yet implemented")
    }


    // ====== Service-specific stuff ======

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
            startForeground(NOTIFICATION_ID, createChannelAndNotification())
            isServiceStarted = true
        }

        startCollect()
    }

    private fun stopCollectionService() {
        // stop self after saving
        stopCollectAndSave()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun createChannelAndNotification(): Notification {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel(notificationManager)

        return createNotification()
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )

        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Title")
        .setContentText("Text")
        .setSubText("subtext")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .build()

    companion object {
        private const val ACTION_START = "START_COLLECTION"
        private const val ACTION_STOP = "STOP_COLLECTION"

        private const val NOTIFICATION_ID = 100

        private const val CHANNEL_ID = "collecting_sensor_data"
        private const val CHANNEL_NAME = "Collecting sensor data"

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