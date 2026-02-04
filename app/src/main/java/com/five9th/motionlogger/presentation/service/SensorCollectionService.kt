package com.five9th.motionlogger.presentation.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.five9th.motionlogger.R
import com.five9th.motionlogger.domain.utils.TimeFormatHelper
import com.five9th.motionlogger.presentation.ui.MainActivity
import com.five9th.motionlogger.presentation.uimodel.CollectionStats
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class SensorCollectionService : Service(), ISensorCollector {

    private val tag = "SensorService"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val lock = WakeLockHelper(this, logsOn = true)

    @Inject lateinit var runner: SensorServiceRunner

    private lateinit var notificationManager: NotificationManager

    private lateinit var builder: NotificationCompat.Builder

    private var updateNotificationJob: Job? = null

    private var isServiceStarted = false

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): SensorCollectionService = this@SensorCollectionService
    }

    override fun onCreate() {  // Hilt injects before onCreate()
        super.onCreate()

        initFlows()
        initNotification()
    }

    private fun initFlows() {  // After onCreate() was called
        isCollectingSF = runner.isCollectingSF
        collectionStatsSF = runner.collectionStatsSF
        sessionIdSF = runner.sessionIdSF
        savingCompletedSF = runner.savingCompletedSF
    }

    private fun initNotification() {
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        builder = createNotificationBuilder()
        createNotificationChannel()
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
            waitForSaving()
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

    private fun waitForSaving() {
        scope.launch {
            savingCompletedSF.collect {
                stopSelf()  // stop service after saving completes
            }
        }
    }

    private fun stopCollectionService() {
        // stop self after saving
        stopCollectAndSave()
    }

    override fun onDestroy() {
        scope.cancel()
        lock.cleanup()
        runner.cancelScope()
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


    // ------ ISensorCollector impl by runner ------
    override fun startCollect() {
        runner.startCollect()
        lock.acquireWakeLock()
    }

    override fun stopCollectAndSave() {
        lock.releaseWakeLock()
        runner.stopCollectAndSave()
    }

    override lateinit var isCollectingSF: StateFlow<Boolean>
    override lateinit var collectionStatsSF: StateFlow<CollectionStats>
    override lateinit var sessionIdSF: StateFlow<Int>
    override lateinit var savingCompletedSF: SharedFlow<Unit>
    // ------


    companion object {
        private const val ACTION_START = "START_COLLECTION"
        private const val ACTION_STOP = "STOP_COLLECTION"

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