package com.manumnoha.healthbridge.worker

import android.content.Context
import androidx.work.*
import com.manumnoha.healthbridge.BuildConfig
import com.manumnoha.healthbridge.network.ApiClient
import com.manumnoha.healthbridge.network.WorkoutIngestRequest
import com.manumnoha.healthbridge.network.WorkoutJson
import com.manumnoha.healthbridge.wodify.WodifyClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class WodifySyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (BuildConfig.WODIFY_API_KEY.isBlank()) return Result.success()

        val today = LocalDate.now()
        val weekAgo = today.minusDays(7)
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE

        return try {
            val client = WodifyClient(BuildConfig.WODIFY_API_KEY, BuildConfig.WODIFY_ATHLETE_ID)
            val records = client.getRecentWorkouts(fmt.format(weekAgo), fmt.format(today))

            if (records.isEmpty()) return Result.success()

            val workouts = records.map { r ->
                WorkoutJson(
                    external_id = r.id,
                    performed_at = "${r.classDate}T00:00:00Z",
                    class_name = r.className,
                    wod_name = r.wod?.name,
                    result = r.result,
                    result_type = r.resultType,
                    rx = r.rx,
                    coach = r.coachName,
                    location = r.locationName,
                    notes = r.notes,
                )
            }

            ApiClient.service.ingestWorkout(WorkoutIngestRequest(workouts))
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WodifySyncWorker>(30, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 2, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "wodify_sync", ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }
}
