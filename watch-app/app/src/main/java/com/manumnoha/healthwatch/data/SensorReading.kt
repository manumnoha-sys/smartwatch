package com.manumnoha.healthwatch.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sensor_readings",
    indices = [Index("recordedAt"), Index("synced")]
)
data class SensorReading(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordedAt: Long,           // epoch millis UTC
    val heartRateBpm: Float? = null,
    val heartRateAccuracy: Int? = null,
    val spo2Percent: Float? = null,
    val spo2Accuracy: Int? = null,
    val stepsTotal: Int? = null,
    val caloriesKcal: Float? = null,
    val accelX: Float? = null,
    val accelY: Float? = null,
    val accelZ: Float? = null,
    val synced: Boolean = false,
)
