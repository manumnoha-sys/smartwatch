package com.manumnoha.healthbridge.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.manumnoha.healthbridge.BuildConfig
import com.manumnoha.healthbridge.googlefit.GoogleFitClient
import com.manumnoha.healthbridge.googlefit.GoogleFitTokenStore
import com.manumnoha.healthbridge.network.ApiClient
import com.manumnoha.healthbridge.network.WatchIngestRequest
import com.manumnoha.healthbridge.network.WatchReadingJson
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

private const val TAG       = "GoogleFitSync"
private const val DEVICE_ID = "google-fit"
private val ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC)

class GoogleFitSyncWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (BuildConfig.GOOGLE_FIT_CLIENT_ID.isBlank()) return Result.success()

        val tokenStore = GoogleFitTokenStore(applicationContext)
        if (!tokenStore.hasTokens()) {
            Log.i(TAG, "No Google Fit tokens — user needs to connect via the app")
            return Result.success()
        }

        val client = GoogleFitClient(
            BuildConfig.GOOGLE_FIT_CLIENT_ID,
            BuildConfig.GOOGLE_FIT_CLIENT_SECRET,
        )

        return try {
            if (!tokenStore.isAccessTokenValid()) {
                val resp = client.refreshToken(tokenStore.refreshToken!!)
                tokenStore.saveTokens(
                    resp.accessToken,
                    resp.refreshToken ?: tokenStore.refreshToken!!,
                    resp.expiresIn,
                )
                Log.i(TAG, "Token refreshed")
            }

            val endMs   = System.currentTimeMillis()
            val startMs = endMs - 35 * 60_000L   // last 35 min

            val response = client.aggregate(tokenStore.accessToken!!, startMs, endMs)

            val readings = mutableMapOf<String, WatchReadingJson>()

            for (bucket in response.bucket) {
                val bucketStartMs = bucket.startTimeMillis.toLong()
                val ts = ISO.format(Instant.ofEpochMilli(bucketStartMs))

                for (dataset in bucket.dataset) {
                    for (point in dataset.point) {
                        val value = point.value.firstOrNull() ?: continue
                        when {
                            dataset.dataSourceId.contains("heart_rate") -> {
                                val bpm = value.fpVal?.toFloat() ?: continue
                                if (bpm > 0) readings[ts] = (readings[ts] ?: WatchReadingJson(ts))
                                    .copy(heart_rate_bpm = bpm)
                            }
                            dataset.dataSourceId.contains("step_count") -> {
                                val steps = value.intVal ?: value.fpVal?.toInt() ?: continue
                                if (steps > 0) readings[ts] = (readings[ts] ?: WatchReadingJson(ts))
                                    .copy(steps_total = steps)
                            }
                            dataset.dataSourceId.contains("calories") -> {
                                val kcal = value.fpVal?.toFloat() ?: continue
                                if (kcal > 0) readings[ts] = (readings[ts] ?: WatchReadingJson(ts))
                                    .copy(calories_kcal = kcal)
                            }
                        }
                    }
                }
            }

            if (readings.isNotEmpty()) {
                val resp = ApiClient.service.ingestWatch(
                    WatchIngestRequest(DEVICE_ID, readings.values.toList())
                )
                Log.i(TAG, "Google Fit ingest: accepted=${resp.accepted} dup=${resp.duplicate_skipped}")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed: ${e.message}")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<GoogleFitSyncWorker>(30, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 2, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "google_fit_sync", ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }
}
