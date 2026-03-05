package com.manumnoha.healthbridge.cgm

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class NightscoutEntry(
    @SerializedName("_id")       val id: String,
    @SerializedName("sgv")       val sgv: Int,
    @SerializedName("date")      val dateMs: Long,
    @SerializedName("dateString") val dateString: String,
    @SerializedName("direction") val direction: String?,
    @SerializedName("device")    val device: String?,
    @SerializedName("noise")     val noise: Int?,
)

interface NightscoutApi {
    @GET("api/v1/entries.json")
    suspend fun getEntries(
        @Query("count") count: Int = 20,
        @Query("find[dateString][\$gte]") since: String? = null,
        @Query("token") token: String,
    ): List<NightscoutEntry>
}

class NightscoutClient(baseUrl: String) {
    val api: NightscoutApi = Retrofit.Builder()
        .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(NightscoutApi::class.java)
}
