package com.manumnoha.healthbridge.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.manumnoha.healthbridge.network.ApiClient
import com.manumnoha.healthbridge.network.WatchReadingJson
import com.manumnoha.healthbridge.network.WatchIngestRequest
import com.manumnoha.healthbridge.network.WorkoutIngestRequest
import com.manumnoha.healthbridge.network.WorkoutJson
import com.manumnoha.healthbridge.samsung.SamsungHealthReader
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

private const val TAG = "SamsungHealthSync"
private const val DEVICE_ID = "samsung-health-phone"
private val ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC)

class SamsungHealthSyncWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val reader = SamsungHealthReader(applicationContext)

        if (!reader.isAvailable()) {
            Log.i(TAG, "Health Connect not available on this device — skipping")
            return Result.success()
        }
        if (!reader.hasPermissions()) {
            Log.w(TAG, "Health Connect permissions not granted — skipping")
            return Result.success()
        }

        val since = Instant.now().minusSeconds(35 * 60) // last 35 min (5-min buffer)

        return try {
            // ── Sensor samples → /ingest/watch ──────────────────────────────
            val samples = reader.readSamples(since)
            if (samples.isNotEmpty()) {
                val readings = samples.map { s ->
                    WatchReadingJson(
                        recorded_at = ISO.format(s.recordedAt),
                        heart_rate_bpm = s.heartRateBpm?.toFloat(),
                        spo2_percent = s.spo2Percent?.toFloat(),
                        steps_total = s.steps?.toInt(),
                        calories_kcal = s.caloriesKcal?.toFloat(),
                    )
                }
                val resp = ApiClient.service.ingestWatch(WatchIngestRequest(DEVICE_ID, readings))
                Log.i(TAG, "Watch samples: accepted=${resp.accepted} dup=${resp.duplicate_skipped}")
            }

            // ── Exercise sessions → /ingest/workout ─────────────────────────
            val sessions = reader.readSessions(since)
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
                    )
                }
                val resp = ApiClient.service.ingestWorkout(WorkoutIngestRequest(workouts))
                Log.i(TAG, "Sessions: accepted=${resp.accepted} updated=${resp.updated}")
            }

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
            val request = PeriodicWorkRequestBuilder<SamsungHealthSyncWorker>(30, TimeUnit.MINUTES)
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
