package com.manumnoha.healthbridge.worker

import android.content.Context
import androidx.work.*
import com.manumnoha.healthbridge.BuildConfig
import com.manumnoha.healthbridge.cgm.DexcomShareClient
import com.manumnoha.healthbridge.cgm.NightscoutClient
import com.manumnoha.healthbridge.network.ApiClient
import com.manumnoha.healthbridge.network.GlucoseIngestRequest
import com.manumnoha.healthbridge.network.GlucoseReadingJson
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class CgmSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val fmt = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC)

    override suspend fun doWork(): Result {
        val readings = mutableListOf<GlucoseReadingJson>()

        // Primary: Nightscout
        if (BuildConfig.NIGHTSCOUT_URL.isNotBlank()) {
            try {
                val client = NightscoutClient(BuildConfig.NIGHTSCOUT_URL)
                val entries = client.api.getEntries(count = 20, token = BuildConfig.NIGHTSCOUT_TOKEN)
                entries.forEach { e ->
                    readings += GlucoseReadingJson(
                        external_id = e.id,
                        source = "nightscout",
                        recorded_at = fmt.format(Instant.ofEpochMilli(e.dateMs)),
                        glucose_mgdl = e.sgv.toFloat(),
                        trend = e.direction,
                        device = e.device,
                        noise = e.noise,
                        raw_sgv = e.sgv,
                    )
                }
            } catch (_: Exception) { /* fall through to Dexcom */ }
        }

        // Fallback: Dexcom Share
        if (readings.isEmpty() && BuildConfig.DEXCOM_USERNAME.isNotBlank()) {
            try {
                val client = DexcomShareClient(BuildConfig.DEXCOM_USERNAME, BuildConfig.DEXCOM_PASSWORD)
                val values = client.getLatestReadings(20)
                values.forEach { v ->
                    val epochMs = client.parseEpochMs(v.wt)
                    readings += GlucoseReadingJson(
                        external_id = "dexcom_$epochMs",
                        source = "dexcom_share",
                        recorded_at = fmt.format(Instant.ofEpochMilli(epochMs)),
                        glucose_mgdl = v.value.toFloat(),
                        trend = v.trend,
                    )
                }
            } catch (_: Exception) { }
        }

        if (readings.isEmpty()) return Result.success()

        return try {
            ApiClient.service.ingestGlucose(GlucoseIngestRequest(readings))
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<CgmSyncWorker>(5, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "cgm_sync", ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }
}
