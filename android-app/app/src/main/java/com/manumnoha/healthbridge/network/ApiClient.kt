package com.manumnoha.healthbridge.network

import com.google.gson.annotations.SerializedName
import com.manumnoha.healthbridge.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// ── Request / response bodies ────────────────────────────────────────────────

data class WatchReadingJson(
    val recorded_at: String,
    val heart_rate_bpm: Float? = null,
    val heart_rate_accuracy: Int? = null,
    val spo2_percent: Float? = null,
    val steps_total: Int? = null,
    val calories_kcal: Float? = null,
    val active_calories_kcal: Float? = null,
    val distance_meters: Float? = null,
    val floors_climbed: Float? = null,
    val accel_x: Float? = null,
    val accel_y: Float? = null,
    val accel_z: Float? = null,
)

data class WellnessSnapshotJson(
    val recorded_at: String,
    val resting_hr_bpm: Float? = null,
    val hrv_rmssd_ms: Float? = null,
    val vo2_max: Float? = null,
    val respiratory_rate_brpm: Float? = null,
    val skin_temp_celsius: Float? = null,
    val weight_kg: Float? = null,
    val body_fat_percent: Float? = null,
    val bmr_kcal: Float? = null,
)

data class WatchIngestRequest(val device_id: String, val readings: List<WatchReadingJson>)
data class WatchIngestResponse(val accepted: Int, val duplicate_skipped: Int)

data class GlucoseReadingJson(
    val external_id: String,
    val source: String,
    val recorded_at: String,
    val glucose_mgdl: Float,
    val trend: String? = null,
    val trend_rate_mgdl_per_min: Float? = null,
    val device: String? = null,
    val noise: Int? = null,
    val raw_sgv: Int? = null,
)

data class GlucoseIngestRequest(val readings: List<GlucoseReadingJson>)
data class GlucoseIngestResponse(val accepted: Int, val duplicate_skipped: Int)

data class WorkoutJson(
    val external_id: String,
    val performed_at: String,
    val class_name: String? = null,
    val wod_name: String? = null,
    val result: String? = null,
    val result_type: String? = null,
    val score_by_type: Float? = null,
    val rx: Boolean = false,
    val coach: String? = null,
    val location: String? = null,
    val duration_minutes: Int? = null,
    val notes: String? = null,
)

data class WorkoutIngestRequest(val workouts: List<WorkoutJson>)
data class WorkoutIngestResponse(val accepted: Int, val updated: Int, val duplicate_skipped: Int)

data class SleepSessionJson(
    val external_id: String,
    val start_time: String,
    val end_time: String,
    val duration_minutes: Int,
    val total_sleep_minutes: Int? = null,
    val deep_sleep_minutes: Int? = null,
    val light_sleep_minutes: Int? = null,
    val rem_sleep_minutes: Int? = null,
    val awake_minutes: Int? = null,
    val notes: String? = null,
)

data class SleepIngestRequest(val sessions: List<SleepSessionJson>)
data class SleepIngestResponse(val accepted: Int, val updated: Int)

data class WellnessIngestRequest(val device_id: String, val snapshots: List<WellnessSnapshotJson>)
data class WellnessIngestResponse(val accepted: Int, val duplicate_skipped: Int)

// ── Retrofit interface ────────────────────────────────────────────────────────

interface HealthApiService {
    @POST("ingest/watch")
    suspend fun ingestWatch(@Body body: WatchIngestRequest): WatchIngestResponse

    @POST("ingest/glucose")
    suspend fun ingestGlucose(@Body body: GlucoseIngestRequest): GlucoseIngestResponse

    @POST("ingest/workout")
    suspend fun ingestWorkout(@Body body: WorkoutIngestRequest): WorkoutIngestResponse

    @POST("ingest/sleep")
    suspend fun ingestSleep(@Body body: SleepIngestRequest): SleepIngestResponse

    @POST("ingest/wellness")
    suspend fun ingestWellness(@Body body: WellnessIngestRequest): WellnessIngestResponse
}

// ── Singleton client ──────────────────────────────────────────────────────────

object ApiClient {
    val service: HealthApiService by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("X-Api-Key", BuildConfig.API_KEY)
                        .build()
                )
            })
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("${BuildConfig.SERVER_URL}/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HealthApiService::class.java)
    }
}
