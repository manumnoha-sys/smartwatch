package com.manumnoha.healthwatch.worker

import android.content.Context
import androidx.work.*
import com.manumnoha.healthwatch.BuildConfig
import com.manumnoha.healthwatch.data.LocalDatabase
import com.manumnoha.healthwatch.network.ApiClient
import com.manumnoha.healthwatch.network.SensorReadingJson
import com.manumnoha.healthwatch.network.WatchIngestRequest
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val dao = LocalDatabase.get(applicationContext).readingDao()
        val pending = dao.getPending()
        if (pending.isEmpty()) return Result.success()

        val fmt = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC)

        val body = WatchIngestRequest(
            device_id = BuildConfig.DEVICE_ID,
            readings = pending.map { r ->
                SensorReadingJson(
                    recorded_at = fmt.format(Instant.ofEpochMilli(r.recordedAt)),
                    heart_rate_bpm = r.heartRateBpm,
                    heart_rate_accuracy = r.heartRateAccuracy,
                    spo2_percent = r.spo2Percent,
                    spo2_accuracy = r.spo2Accuracy,
                    steps_total = r.stepsTotal,
                    calories_kcal = r.caloriesKcal,
                    accel_x = r.accelX,
                    accel_y = r.accelY,
                    accel_z = r.accelZ,
                )
            }
        )

        return try {
            ApiClient.service.ingestWatch(body)
            dao.markSynced(pending.map { it.id })
            // clean up rows older than 24h
            dao.deleteSynced(System.currentTimeMillis() - 86_400_000L)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "health_sync"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(5, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
