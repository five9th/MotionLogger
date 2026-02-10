package com.five9th.motionlogger.data.repos

import android.app.Application
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import android.util.Log
import com.five9th.motionlogger.domain.entities.SensorSample
import com.five9th.motionlogger.domain.entities.SensorsInfo
import com.five9th.motionlogger.domain.repos.SensorsRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.sqrt

// TODO: handle the situation when some sensors are missing
class SensorsRepoImpl @Inject constructor (
    app: Application
) : SensorsRepo, SensorEventListener {

    companion object {
        private const val SAMPLE_FREQ_HZ = 50
        private const val MILLIS_IN_SECOND = 1_000
        private const val MICROS_IN_SECOND = 1_000_000
    }

    private val sensors = SensorsBundle(app)

    override fun getSensorsInfo(): SensorsInfo {
        return sensors.getSensorsInfo()
    }

    private val _flow = MutableSharedFlow<SensorSample>(extraBufferCapacity = 64)

    private var lastAccel: FloatArray? = null
    private var lastGyro: FloatArray? = null
    private var lastEuler: FloatArray? = null

    private var startTimestamp = 0L

    private var samplingJob: Job? = null

    private val _isCollecting = MutableStateFlow(false)
    override val isCollecting: StateFlow<Boolean> = _isCollecting

    override fun start() {
        if (isCollecting.value) return

        _isCollecting.value = true

        startTimestamp = SystemClock.elapsedRealtime()

        registerListeners()
        startSampler()
    }

    private fun registerListeners() {
        val periodMicros = MICROS_IN_SECOND / SAMPLE_FREQ_HZ

        sensors.linearAcceleration?.let { sensors.sm.registerListener(this, it, periodMicros) }
        sensors.gyroscope?.let { sensors.sm.registerListener(this, it, periodMicros) }
        sensors.gameRotationVector?.let { sensors.sm.registerListener(this, it, periodMicros) }
    }

    private fun startSampler() {
        val delayMs = (MILLIS_IN_SECOND / SAMPLE_FREQ_HZ).toLong()

        samplingJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                delay(delayMs)
                getAndEmitSample()
            }
        }
    }

    private fun getAndEmitSample() {
        val a = lastAccel
        val g = lastGyro
        val e = lastEuler

        if (a != null && g != null && e != null) {
            val sample = SensorSample(
                timestampMs = getSampleTimestamp(),
                accX = a[0], accY = a[1], accZ = a[2],
                gyroX = g[0], gyroY = g[1], gyroZ = g[2],
                roll = e[0], pitch = e[1], yaw = e[2]
            )
            _flow.tryEmit(sample)
        }
    }

    private fun getSampleTimestamp(): Long = SystemClock.elapsedRealtime() - startTimestamp

    override fun stop() {
        _isCollecting.value = false

        samplingJob?.cancel()
        sensors.sm.unregisterListener(this)
    }

    override fun getFlow(): Flow<SensorSample> = _flow


    // temp log for testing
    private var accCounter = 0
    private var gyrCounter = 0
    private var rotCounter = 0

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                lastAccel = event.values.clone()
                accCounter++
                if (accCounter % 50 == 1) Log.d("SENSOR_ACCEL", event.values.contentToString())
            }
            Sensor.TYPE_GYROSCOPE -> {
                lastGyro = event.values.clone()
                gyrCounter++
                if (gyrCounter % 50 == 1) Log.d("SENSOR_GYRO", event.values.contentToString())
            }
            Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                lastEuler = processGameRotationVector(event.values)
                rotCounter++
                if (rotCounter % 50 == 1) Log.d("SENSOR_GAME", event.values.contentToString())
            }
        }
    }

    private fun processGameRotationVector(values: FloatArray): FloatArray {
        val qx = values[0]
        val qy = values[1]
        val qz = values[2]

        val qw = if (values.size >= 4) {
            values[3]
        } else {
            val sum = 1f - (qx*qx + qy*qy + qz*qz)
            if (sum > 0) sqrt(sum) else 0f
        }


        return quaternionToEuler(floatArrayOf(qx, qy, qz, qw))
    }

    private fun quaternionToEuler(q: FloatArray): FloatArray {
        // q = [x, y, z, w] from TYPE_GAME_ROTATION_VECTOR
        val rotMat = FloatArray(9)

        // Convert quaternion → rotation matrix
        SensorManager.getRotationMatrixFromVector(rotMat, q)

        // Convert rotation matrix → Euler angles (azimuth, pitch, roll)
        val orientation = FloatArray(3)
        SensorManager.getOrientation(rotMat, orientation)

        // orientation = [yaw (azimuth), pitch, roll] in radians
        // If you want roll/pitch/yaw in your dataset order, rearrange as needed.
        return floatArrayOf(
            orientation[2],  // roll
            orientation[1],  // pitch
            orientation[0]   // yaw
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}