package com.manumnoha.healthbridge.samsung

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant

private const val TAG = "SamsungHealthReader"

data class HealthConnectSample(
    val recordedAt: Instant,
    val heartRateBpm: Double? = null,
    val spo2Percent: Double? = null,
    val steps: Long? = null,
    val caloriesKcal: Double? = null,
)

data class HealthConnectSession(
    val externalId: String,
    val startTime: Instant,
    val endTime: Instant,
    val exerciseType: Int,
    val title: String?,
    val notes: String?,
    val durationMinutes: Int,
)

val HEALTH_CONNECT_PERMISSIONS = setOf(
    HealthPermission.getReadPermission(HeartRateRecord::class),
    HealthPermission.getReadPermission(StepsRecord::class),
    HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    HealthPermission.getReadPermission(OxygenSaturationRecord::class),
    HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
)

class SamsungHealthReader(context: Context) {

    private val client: HealthConnectClient? = runCatching {
        HealthConnectClient.getOrCreate(context)
    }.getOrElse {
        Log.w(TAG, "Health Connect not available: ${it.message}")
        null
    }

    fun isAvailable(): Boolean = client != null

    suspend fun hasPermissions(): Boolean {
        val c = client ?: return false
        return c.permissionController.getGrantedPermissions()
            .containsAll(HEALTH_CONNECT_PERMISSIONS)
    }

    /** Read HR + SpO2 + steps + calories in 10-min buckets from [since] to now. */
    suspend fun readSamples(since: Instant): List<HealthConnectSample> {
        val c = client ?: return emptyList()
        val filter = TimeRangeFilter.between(since, Instant.now())
        val samples = mutableMapOf<Instant, HealthConnectSample>()

        // Heart rate
        runCatching {
            c.readRecords(ReadRecordsRequest(HeartRateRecord::class, filter)).records
                .flatMap { it.samples }
                .forEach { s ->
                    val bucket = bucket10min(s.time)
                    samples[bucket] = (samples[bucket] ?: HealthConnectSample(bucket))
                        .copy(heartRateBpm = s.beatsPerMinute.toDouble())
                }
        }.onFailure { Log.w(TAG, "HR read failed: ${it.message}") }

        // SpO2
        runCatching {
            c.readRecords(ReadRecordsRequest(OxygenSaturationRecord::class, filter)).records
                .forEach { r ->
                    val bucket = bucket10min(r.time)
                    samples[bucket] = (samples[bucket] ?: HealthConnectSample(bucket))
                        .copy(spo2Percent = r.percentage.value)
                }
        }.onFailure { Log.w(TAG, "SpO2 read failed: ${it.message}") }

        // Steps
        runCatching {
            c.readRecords(ReadRecordsRequest(StepsRecord::class, filter)).records
                .forEach { r ->
                    val bucket = bucket10min(r.startTime)
                    samples[bucket] = (samples[bucket] ?: HealthConnectSample(bucket))
                        .copy(steps = r.count)
                }
        }.onFailure { Log.w(TAG, "Steps read failed: ${it.message}") }

        // Calories
        runCatching {
            c.readRecords(ReadRecordsRequest(TotalCaloriesBurnedRecord::class, filter)).records
                .forEach { r ->
                    val bucket = bucket10min(r.startTime)
                    samples[bucket] = (samples[bucket] ?: HealthConnectSample(bucket))
                        .copy(caloriesKcal = r.energy.inKilocalories)
                }
        }.onFailure { Log.w(TAG, "Calories read failed: ${it.message}") }

        return samples.values.toList().sortedBy { it.recordedAt }
    }

    /** Read exercise sessions from [since] to now. */
    suspend fun readSessions(since: Instant): List<HealthConnectSession> {
        val c = client ?: return emptyList()
        val filter = TimeRangeFilter.between(since, Instant.now())
        return runCatching {
            c.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, filter)).records
                .map { r ->
                    val durationMin = ((r.endTime.epochSecond - r.startTime.epochSecond) / 60).toInt()
                    HealthConnectSession(
                        externalId = "hc_${r.metadata.id}",
                        startTime = r.startTime,
                        endTime = r.endTime,
                        exerciseType = r.exerciseType,
                        title = r.title,
                        notes = r.notes,
                        durationMinutes = durationMin,
                    )
                }
        }.getOrElse {
            Log.w(TAG, "Exercise read failed: ${it.message}")
            emptyList()
        }
    }

    private fun bucket10min(t: Instant): Instant {
        val epochSec = t.epochSecond
        return Instant.ofEpochSecond(epochSec - (epochSec % 600))
    }
}
