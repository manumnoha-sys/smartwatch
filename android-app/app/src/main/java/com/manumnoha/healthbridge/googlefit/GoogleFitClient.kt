package com.manumnoha.healthbridge.googlefit

import android.net.Uri
import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

const val GOOGLE_FIT_REDIRECT_URI = "com.manumnoha.healthbridge://google-fit-callback"

private const val AUTH_HOST  = "https://accounts.google.com"
private const val TOKEN_HOST = "https://oauth2.googleapis.com"
private const val API_BASE   = "https://www.googleapis.com"

private val FITNESS_SCOPES = listOf(
    "https://www.googleapis.com/auth/fitness.heart_rate.read",
    "https://www.googleapis.com/auth/fitness.activity.read",
    "https://www.googleapis.com/auth/fitness.body.read",
)

// ── Token response ────────────────────────────────────────────────────────────

data class GoogleTokenResponse(
    @SerializedName("access_token")  val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    @SerializedName("expires_in")    val expiresIn: Int,
)

// ── Fitness aggregate request / response ──────────────────────────────────────

data class AggregateBy(@SerializedName("dataTypeName") val dataTypeName: String)
data class BucketByTime(@SerializedName("durationMillis") val durationMillis: Long)

data class AggregateRequest(
    @SerializedName("aggregateBy")    val aggregateBy: List<AggregateBy>,
    @SerializedName("bucketByTime")   val bucketByTime: BucketByTime,
    @SerializedName("startTimeMillis") val startTimeMillis: Long,
    @SerializedName("endTimeMillis")   val endTimeMillis: Long,
)

data class FitValue(
    @SerializedName("fpVal")      val fpVal: Double?  = null,
    @SerializedName("intVal")     val intVal: Int?    = null,
    @SerializedName("mapVal")     val mapVal: List<Any>? = null,
)

data class FitDataPoint(
    @SerializedName("startTimeNanos") val startTimeNanos: String,
    @SerializedName("endTimeNanos")   val endTimeNanos: String,
    @SerializedName("value")          val value: List<FitValue>,
)

data class FitDataset(
    @SerializedName("dataSourceId") val dataSourceId: String,
    @SerializedName("point")        val point: List<FitDataPoint> = emptyList(),
)

data class FitBucket(
    @SerializedName("startTimeMillis") val startTimeMillis: String,
    @SerializedName("endTimeMillis")   val endTimeMillis: String,
    @SerializedName("dataset")         val dataset: List<FitDataset>,
)

data class AggregateResponse(
    @SerializedName("bucket") val bucket: List<FitBucket> = emptyList(),
)

// ── Retrofit interfaces ───────────────────────────────────────────────────────

interface GoogleAuthApi {
    @FormUrlEncoded
    @POST("/token")
    suspend fun exchangeCode(
        @Field("code")          code: String,
        @Field("client_id")     clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("redirect_uri")  redirectUri: String = GOOGLE_FIT_REDIRECT_URI,
        @Field("grant_type")    grantType: String   = "authorization_code",
    ): GoogleTokenResponse

    @FormUrlEncoded
    @POST("/token")
    suspend fun refreshToken(
        @Field("refresh_token") refreshToken: String,
        @Field("client_id")     clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("grant_type")    grantType: String = "refresh_token",
    ): GoogleTokenResponse
}

interface GoogleFitApi {
    @POST("/fitness/v1/users/me/dataset:aggregate")
    suspend fun aggregate(
        @Header("Authorization") bearer: String,
        @Body body: AggregateRequest,
    ): AggregateResponse
}

// ── Client ────────────────────────────────────────────────────────────────────

class GoogleFitClient(
    private val clientId: String,
    private val clientSecret: String,
) {
    private val authApi: GoogleAuthApi = Retrofit.Builder()
        .baseUrl(TOKEN_HOST)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GoogleAuthApi::class.java)

    private val fitApi: GoogleFitApi = Retrofit.Builder()
        .baseUrl(API_BASE)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GoogleFitApi::class.java)

    fun buildAuthUri(): Uri =
        Uri.parse("$AUTH_HOST/o/oauth2/v2/auth").buildUpon()
            .appendQueryParameter("client_id",     clientId)
            .appendQueryParameter("redirect_uri",  GOOGLE_FIT_REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope",         FITNESS_SCOPES.joinToString(" "))
            .appendQueryParameter("access_type",   "offline")
            .appendQueryParameter("prompt",        "consent")
            .build()

    suspend fun exchangeCode(code: String): GoogleTokenResponse =
        authApi.exchangeCode(code, clientId, clientSecret)

    suspend fun refreshToken(refreshToken: String): GoogleTokenResponse =
        authApi.refreshToken(refreshToken, clientId, clientSecret)

    /** Fetch HR + steps + calories in [bucketMinutes]-minute buckets for the given time range. */
    suspend fun aggregate(
        accessToken: String,
        startMs: Long,
        endMs: Long,
        bucketMinutes: Int = 10,
    ): AggregateResponse = fitApi.aggregate(
        bearer = "Bearer $accessToken",
        body = AggregateRequest(
            aggregateBy = listOf(
                AggregateBy("com.google.heart_rate.bpm"),
                AggregateBy("com.google.step_count.delta"),
                AggregateBy("com.google.calories.expended"),
            ),
            bucketByTime   = BucketByTime(bucketMinutes * 60_000L),
            startTimeMillis = startMs,
            endTimeMillis   = endMs,
        )
    )
}
