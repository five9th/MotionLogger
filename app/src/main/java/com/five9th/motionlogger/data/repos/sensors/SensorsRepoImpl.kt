package com.five9th.motionlogger.data.repos.sensors

import android.app.Application
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import android.util.Log
import com.five9th.motionlogger.domain.entities.SensorSample
import com.five9th.motionlogger.domain.entities.SensorSchema
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
        private const val SAMPLE_FREQ_HZ = 50  // TODO: move to settings
        private const val MILLIS_IN_SECOND = 1_000
        private const val MICROS_IN_SECOND = 1_000_000
    }

    private val tag = "SensorsRepo"

    private val sensors = SensorsBundle(app)

    override fun getSensorsInfo(): SensorsInfo {
        return sensors.getSensorsInfo()
    }

    private val _flow = MutableSharedFlow<SensorSample>(extraBufferCapacity = 64)
    override fun getFlow(): Flow<SensorSample> = _flow

    private val mapper = SensorTypeMapper()

    private var currentSchema: SensorSchema? = null
    private var currentSources = setOf<SensorSource>()

    // example: lastValues[SensorSource.ACCELEROMETER] = [acc_x, acc_y, acc_z]
    private val lastValues = mutableMapOf<SensorSource, FloatArray>()

    // example: "acc_y" -> lastValues[SensorSource.ACCELEROMETER][1]
    private fun Map<SensorSource, FloatArray>.getFieldValue(fieldId: String): Float? {
        val mapping = FieldMapping.MAPPINGS[fieldId] ?: return null
        return this[mapping.source]?.getOrNull(mapping.index)
    }

    private var startTimestamp = 0L

    private var samplingJob: Job? = null

    private val _isCollecting = MutableStateFlow(false)
    override val isCollecting: StateFlow<Boolean> = _isCollecting

    override fun start(schema: SensorSchema) {
        if (isCollecting.value) return
        _isCollecting.value = true

        // init by new schema
        currentSchema = schema
        currentSources = SensorSource.requiredSources(schema)
        val sensors = mapper.getRequiredSensors(currentSources)

        registerListeners(sensors)

        startTimestamp = SystemClock.elapsedRealtime()

        startSampler()
    }

    private fun registerListeners(sensorTypes: Iterable<Int>) {
        val periodMicros = MICROS_IN_SECOND / SAMPLE_FREQ_HZ

        for (type in sensorTypes) {
            val sensor = sensors.sm.getDefaultSensor(type)  // todo: warn if sensor is null
            sensors.sm.registerListener(this, sensor, periodMicros)
        }
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
        val schema = currentSchema
        if (schema == null) {
            Log.w(tag, "Schema is null")
            return
        }

        val fields = schema.fields
        val values = FloatArray(fields.size)

        for (i in 0..fields.size) {
            val field = fields[i]
            val fieldValue = lastValues.getFieldValue(field.id)

            if (fieldValue == null) {
                Log.d(tag, "No value for ${field.id}; EmitSample aborted.")
                return
            }

            values[i] = fieldValue
        }

        val sample = SensorSample(
            timestampMs = getSampleTimestamp(),
            values = values
        )

        _flow.tryEmit(sample)
    }



    private fun getSampleTimestamp(): Long = SystemClock.elapsedRealtime() - startTimestamp

    override fun stop() {
        _isCollecting.value = false

        clearCollections()

        samplingJob?.cancel()
        sensors.sm.unregisterListener(this)
    }

    private fun clearCollections() {
        currentSchema = null
        currentSources = setOf()
        lastValues.clear()
    }


    // temp log for testing
    private var accCounter = 0
    private var gyrCounter = 0
    private var rotCounter = 0

    /** Describes the strategy on how the required SensorSource data is actually collected/evaluated.
     * Tied to [FieldMapping.MAPPINGS] */
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                lastValues[SensorSource.LINEAR_ACCELERATION] = event.values.clone()
                if (accCounter++ % 50 == 1) Log.d("SENSOR_ACCEL", event.values.contentToString())
            }
            Sensor.TYPE_GYROSCOPE -> {
                lastValues[SensorSource.GYROSCOPE] = event.values.clone()
                if (gyrCounter++ % 50 == 1) Log.d("SENSOR_GYRO", event.values.contentToString())
            }
            Sensor.TYPE_GAME_ROTATION_VECTOR -> { // this sensor type corresponds to two Sources
                val q = processGameRotationVector(event.values)

                if (SensorSource.GAME_ROTATION_VECTOR in currentSources) {
                    lastValues[SensorSource.GAME_ROTATION_VECTOR] = q
                }
                if (SensorSource.ATTITUDE in currentSources) {
                    lastValues[SensorSource.GAME_ROTATION_VECTOR] = quaternionToRollPitchYaw(q)
                }

                if (rotCounter++ % 50 == 1) Log.d("SENSOR_ROT", event.values.contentToString())
            }
            // TODO: other types
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

        return floatArrayOf(qx, qy, qz, qw)
    }

    private fun quaternionToRollPitchYaw(q: FloatArray): FloatArray {
        // q = [x, y, z, w] from TYPE_GAME_ROTATION_VECTOR
        val rotMat = FloatArray(9)

        // Convert quaternion → rotation matrix
        SensorManager.getRotationMatrixFromVector(rotMat, q)

        // Convert rotation matrix → Euler angles (azimuth, pitch, roll)
        val orientation = FloatArray(3)
        SensorManager.getOrientation(rotMat, orientation)

        // orientation = [yaw (azimuth), pitch, roll] in radians
        return floatArrayOf(
            orientation[2],  // roll
            orientation[1],  // pitch
            orientation[0]   // yaw
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}