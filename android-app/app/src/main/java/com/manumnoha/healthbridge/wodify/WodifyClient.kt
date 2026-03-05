package com.manumnoha.healthbridge.wodify

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

data class WodifyWod(
    @SerializedName("name") val name: String?,
)

data class WodifyPerformanceRecord(
    @SerializedName("id")            val id: String,
    @SerializedName("class_date")    val classDate: String,
    @SerializedName("class_name")    val className: String?,
    @SerializedName("wod")           val wod: WodifyWod?,
    @SerializedName("result")        val result: String?,
    @SerializedName("result_type")   val resultType: String?,
    @SerializedName("rx")            val rx: Boolean = false,
    @SerializedName("coach_name")    val coachName: String?,
    @SerializedName("location_name") val locationName: String?,
    @SerializedName("notes")         val notes: String?,
)

data class WodifyResponse(
    @SerializedName("data") val data: List<WodifyPerformanceRecord>,
)

interface WodifyApi {
    @GET("v2/performance_records")
    suspend fun getPerformanceRecords(
        @Header("Authorization") bearer: String,
        @Query("athlete_id") athleteId: String,
        @Query("date_from") dateFrom: String,   // YYYY-MM-DD
        @Query("date_to") dateTo: String,
    ): WodifyResponse
}

class WodifyClient(private val apiKey: String, private val athleteId: String) {

    private val bearer = "Bearer $apiKey"

    private val api: WodifyApi = Retrofit.Builder()
        .baseUrl("https://api.wodify.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(WodifyApi::class.java)

    suspend fun getRecentWorkouts(dateFrom: String, dateTo: String): List<WodifyPerformanceRecord> =
        api.getPerformanceRecords(bearer, athleteId, dateFrom, dateTo).data
}
