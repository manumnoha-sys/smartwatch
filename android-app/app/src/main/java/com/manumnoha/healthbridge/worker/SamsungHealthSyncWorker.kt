package com.manumnoha.healthbridge.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.manumnoha.healthbridge.network.ApiClient
import com.manumnoha.healthbridge.network.WatchReadingJson
import com.manumnoha.healthbridge.network.WatchIngestRequest
import com.manumnoha.healthbridge.network.WorkoutIngestRequest
import com.manumnoha.healthbridge.network.WorkoutJson
import com.manumnoha.healthbridge.network.SleepIngestRequest
import com.manumnoha.healthbridge.network.SleepSessionJson
import com.manumnoha.healthbridge.network.WellnessIngestRequest
import com.manumnoha.healthbridge.network.WellnessSnapshotJson
import com.manumnoha.healthbridge.samsung.SamsungHealthReader
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

private const val TAG = "SamsungHealthSync"
private const val DEVICE_ID = "samsung-health-phone"
private const val PREFS = "samsung_health_sync"
private const val KEY_LAST_SYNC = "last_sync_epoch_sec"
// Overlap ensures we never miss a reading at the boundary between syncs
private const val OVERLAP_SEC = 5 * 60L
private val ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC)

class SamsungHealthSyncWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "doWork() started")
        val reader = SamsungHealthReader(applicationContext)

        if (!reader.isAvailable()) {
            Log.i(TAG, "Health Connect not available on this device — skipping")
            return Result.success()
        }
        val granted = reader.grantedPermissions()
        if (granted.isEmpty()) {
            Log.w(TAG, "No Health Connect permissions granted — skipping")
            return Result.success()
        }
        Log.i(TAG, "Granted permissions: ${granted.size}/${com.manumnoha.healthbridge.samsung.HEALTH_CONNECT_PERMISSIONS.size}")

        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastSyncSec = prefs.getLong(KEY_LAST_SYNC, 0L)
        val since: Instant = if (lastSyncSec == 0L) {
            // First run: catch up on last 24 hours
            Instant.now().minusSeconds(24 * 60 * 60)
        } else {
            // Subsequent runs: go back to last sync minus overlap to avoid gaps
            Instant.ofEpochSecond(lastSyncSec - OVERLAP_SEC)
        }
        Log.i(TAG, "Reading samples since $since (lastSync=$lastSyncSec)")

        return try {
            // ── Sensor samples → /ingest/watch ──────────────────────────────
            val samples = reader.readSamples(since)
            Log.i(TAG, "Got ${samples.size} samples from Health Connect")
            if (samples.isNotEmpty()) {
                val readings = samples.map { s ->
                    WatchReadingJson(
                        recorded_at = ISO.format(s.recordedAt),
                        heart_rate_bpm = s.heartRateBpm?.toFloat(),
                        spo2_percent = s.spo2Percent?.toFloat(),
                        steps_total = s.steps?.toInt(),
                        calories_kcal = s.caloriesKcal?.toFloat(),
                        active_calories_kcal = s.activeCaloriesKcal?.toFloat(),
                        distance_meters = s.distanceMeters?.toFloat(),
                        floors_climbed = s.floorsClimbed?.toFloat(),
                    )
                }
                val resp = ApiClient.service.ingestWatch(WatchIngestRequest(DEVICE_ID, readings))
                Log.i(TAG, "Watch samples: accepted=${resp.accepted} dup=${resp.duplicate_skipped}")
            }

            // ── Exercise sessions → /ingest/workout ─────────────────────────
            val sessions = reader.readSessions(since)
            Log.i(TAG, "Got ${sessions.size} sessions from Health Connect")
            if (sessions.isNotEmpty()) {
                val workouts = sessions.map { s ->
                    WorkoutJson(
                        external_id = s.externalId,
                        performed_at = ISO.format(s.startTime),
                        wod_name = s.title ?: exerciseTypeName(s.exerciseType),
                        result_type = "Duration",
                        result = "${s.durationMinutes} min",
                        duration_minutes = s.durationMinutes,
                        notes = s.notes,
                        avg_hr_bpm = s.hrZones?.avgHrBpm?.toFloat(),
                        max_hr_bpm = s.hrZones?.maxHrBpm?.toFloat(),
                        calories_active_kcal = s.activeCaloriesKcal?.toFloat(),
                        zone1_minutes = s.hrZones?.zone1Minutes,
                        zone2_minutes = s.hrZones?.zone2Minutes,
                        zone3_minutes = s.hrZones?.zone3Minutes,
                        zone4_minutes = s.hrZones?.zone4Minutes,
                        zone5_minutes = s.hrZones?.zone5Minutes,
                    )
                }
                val resp = ApiClient.service.ingestWorkout(WorkoutIngestRequest(workouts))
                Log.i(TAG, "Sessions: accepted=${resp.accepted} updated=${resp.updated}")
            }

            // ── Wellness snapshots → /ingest/wellness ────────────────────────
            val wellnessPoints = reader.readWellness(since)
            Log.i(TAG, "Got ${wellnessPoints.size} wellness points from Health Connect")
            if (wellnessPoints.isNotEmpty()) {
                val snapshots = wellnessPoints.map { w ->
                    WellnessSnapshotJson(
                        recorded_at = ISO.format(w.recordedAt),
                        resting_hr_bpm = w.restingHrBpm?.toFloat(),
                        hrv_rmssd_ms = w.hrvRmssdMs?.toFloat(),
                        vo2_max = w.vo2Max?.toFloat(),
                        respiratory_rate_brpm = w.respiratoryRateBrpm?.toFloat(),
                        skin_temp_celsius = w.skinTempCelsius?.toFloat(),
                        weight_kg = w.weightKg?.toFloat(),
                        body_fat_percent = w.bodyFatPercent?.toFloat(),
                        bmr_kcal = w.bmrKcal?.toFloat(),
                    )
                }
                val resp = ApiClient.service.ingestWellness(WellnessIngestRequest(DEVICE_ID, snapshots))
                Log.i(TAG, "Wellness: accepted=${resp.accepted} dup=${resp.duplicate_skipped}")
            }

            // ── Sleep sessions → /ingest/sleep ──────────────────────────────
            val sleepSessions = reader.readSleep(since)
            Log.i(TAG, "Got ${sleepSessions.size} sleep sessions from Health Connect")
            if (sleepSessions.isNotEmpty()) {
                val sleepList = sleepSessions.map { s ->
                    SleepSessionJson(
                        external_id = s.externalId,
                        start_time = ISO.format(s.startTime),
                        end_time = ISO.format(s.endTime),
                        duration_minutes = s.durationMinutes,
                        total_sleep_minutes = s.totalSleepMinutes,
                        deep_sleep_minutes = s.deepSleepMinutes,
                        light_sleep_minutes = s.lightSleepMinutes,
                        rem_sleep_minutes = s.remSleepMinutes,
                        awake_minutes = s.awakeMinutes,
                        notes = s.notes,
                    )
                }
                val resp = ApiClient.service.ingestSleep(SleepIngestRequest(sleepList))
                Log.i(TAG, "Sleep: accepted=${resp.accepted} updated=${resp.updated}")
            }

            // Save last sync time on success
            prefs.edit().putLong(KEY_LAST_SYNC, Instant.now().epochSecond).apply()
            Log.i(TAG, "Sync complete, saved last sync time")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed: ${e.message}")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun exerciseTypeName(type: Int): String = when (type) {
        56  -> "Running"
        74  -> "Walking"
        4   -> "Badminton"
        2   -> "Baseball"
        3   -> "Basketball"
        8   -> "Cycling"
        78  -> "Swimming"
        82  -> "Volleyball"
        84  -> "Weightlifting"
        else -> "Workout"
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SamsungHealthSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 2, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "samsung_health_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
