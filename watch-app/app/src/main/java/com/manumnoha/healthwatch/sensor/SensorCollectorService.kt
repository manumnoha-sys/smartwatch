package com.manumnoha.healthwatch.sensor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseType
import com.manumnoha.healthwatch.data.LocalDatabase
import com.manumnoha.healthwatch.data.SensorReading
import kotlinx.coroutines.*
import java.time.Instant

class SensorCollectorService : Service(), SensorEventListener {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var sensorManager: SensorManager
    private var lastHr: Float? = null
    private var lastHrAccuracy: Int? = null
    private var lastSpo2: Float? = null
    private var lastSteps: Int? = null
    private var lastAccel: Triple<Float, Float, Float>? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        registerSensors()
        startAccelSampling()
        startHealthServices()
    }

    private fun registerSensors() {
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun startAccelSampling() {
        // Persist a snapshot every 10 seconds
        scope.launch {
            val dao = LocalDatabase.get(applicationContext).readingDao()
            while (isActive) {
                delay(10_000)
                dao.insert(
                    SensorReading(
                        recordedAt = Instant.now().toEpochMilli(),
                        heartRateBpm = lastHr,
                        heartRateAccuracy = lastHrAccuracy,
                        spo2Percent = lastSpo2,
                        stepsTotal = lastSteps,
                        accelX = lastAccel?.first,
                        accelY = lastAccel?.second,
                        accelZ = lastAccel?.third,
                    )
                )
            }
        }
    }

    private fun startHealthServices() {
        // Use Health Services for heart rate and SpO2 (Samsung routes these through HS)
        scope.launch {
            try {
                val client = HealthServices.getClient(applicationContext).exerciseClient
                val config = ExerciseConfig.builder(ExerciseType.OTHER_WORKOUT)
                    .setDataTypes(
                        setOf(
                            DataType.HEART_RATE_BPM,
                            DataType.HEART_RATE_BPM_STATS,
                            DataType.VO2_MAX,
                        )
                    )
                    .build()
                client.setUpdateCallback(scope) { update ->
                    update.latestMetrics.getData(DataType.HEART_RATE_BPM).lastOrNull()?.let {
                        lastHr = it.value.toFloat()
                    }
                }
                client.startExercise(config)
            } catch (e: Exception) {
                // Health Services not available — fall back to raw SensorManager
                sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)?.let {
                    sensorManager.registerListener(this@SensorCollectorService, it, SensorManager.SENSOR_DELAY_NORMAL)
                }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_HEART_RATE -> {
                lastHr = event.values[0]
                lastHrAccuracy = event.accuracy
            }
            Sensor.TYPE_STEP_COUNTER -> lastSteps = event.values[0].toInt()
            Sensor.TYPE_ACCELEROMETER -> lastAccel = Triple(event.values[0], event.values[1], event.values[2])
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Health Monitoring", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Health Dashboard")
            .setContentText("Collecting sensor data…")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()

    companion object {
        private const val NOTIF_ID = 1
        private const val CHANNEL_ID = "health_collector"
    }
}
