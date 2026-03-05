package com.manumnoha.healthbridge.cgm

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

private const val DEXCOM_APP_ID = "d89443d2-327c-4a6f-89e5-496bbb0317db"
private const val BASE_URL = "https://share2.dexcom.com/ShareWebServices/Services/"

data class DexcomLoginRequest(
    val accountName: String,
    val password: String,
    val applicationId: String = DEXCOM_APP_ID,
)

data class DexcomGlucoseValue(
    @SerializedName("WT")    val wt: String,    // "Date(epochMs+0000)"
    @SerializedName("Value") val value: Int,
    @SerializedName("Trend") val trend: String?,
)

interface DexcomApi {
    @POST("General/LoginPublisherAccountByName")
    suspend fun login(@Body body: DexcomLoginRequest): String   // returns session ID

    @GET("Publisher/ReadPublisherLatestGlucoseValues")
    suspend fun getReadings(
        @Query("sessionId") sessionId: String,
        @Query("minutes") minutes: Int = 1440,
        @Query("maxCount") maxCount: Int = 20,
    ): List<DexcomGlucoseValue>
}

class DexcomShareClient(private val username: String, private val password: String) {

    private var sessionId: String? = null

    private val api: DexcomApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(DexcomApi::class.java)

    /** Returns epoch millis from Dexcom's "Date(1234567890000+0000)" format. */
    fun parseEpochMs(wt: String): Long =
        Regex("""\d+""").find(wt)?.value?.toLong() ?: 0L

    suspend fun getLatestReadings(count: Int = 20): List<DexcomGlucoseValue> {
        if (sessionId == null) authenticate()
        return try {
            api.getReadings(sessionId!!, maxCount = count)
        } catch (e: Exception) {
            // Re-authenticate on failure and retry once
            authenticate()
            api.getReadings(sessionId!!, maxCount = count)
        }
    }

    private suspend fun authenticate() {
        sessionId = api.login(DexcomLoginRequest(username, password))
            .trim('"')   // response is a quoted string
    }
}
