package com.manumnoha.healthbridge.googlefit

import android.content.Context
import android.content.SharedPreferences

private const val PREFS_NAME = "google_fit_tokens"
private const val KEY_ACCESS_TOKEN  = "access_token"
private const val KEY_REFRESH_TOKEN = "refresh_token"
private const val KEY_EXPIRES_AT    = "expires_at"

class GoogleFitTokenStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS_TOKEN, null)
        set(v) = prefs.edit().putString(KEY_ACCESS_TOKEN, v).apply()

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH_TOKEN, null)
        set(v) = prefs.edit().putString(KEY_REFRESH_TOKEN, v).apply()

    var expiresAt: Long
        get() = prefs.getLong(KEY_EXPIRES_AT, 0L)
        set(v) = prefs.edit().putLong(KEY_EXPIRES_AT, v).apply()

    fun isAccessTokenValid(): Boolean =
        accessToken != null && System.currentTimeMillis() < expiresAt - 60_000

    fun hasTokens(): Boolean = refreshToken != null

    fun saveTokens(accessToken: String, refreshToken: String, expiresInSeconds: Int) {
        this.accessToken  = accessToken
        this.refreshToken = refreshToken
        this.expiresAt    = System.currentTimeMillis() + expiresInSeconds * 1000L
    }

    fun clear() = prefs.edit().clear().apply()
}
