package com.manumnoha.healthwatch.network

data class SensorReadingJson(
    val recorded_at: String,        // ISO-8601 UTC
    val heart_rate_bpm: Float?,
    val heart_rate_accuracy: Int?,
    val spo2_percent: Float?,
    val spo2_accuracy: Int?,
    val steps_total: Int?,
    val calories_kcal: Float?,
    val accel_x: Float?,
    val accel_y: Float?,
    val accel_z: Float?,
)

data class WatchIngestRequest(
    val device_id: String,
    val readings: List<SensorReadingJson>,
)

data class WatchIngestResponse(
    val accepted: Int,
    val duplicate_skipped: Int,
)
