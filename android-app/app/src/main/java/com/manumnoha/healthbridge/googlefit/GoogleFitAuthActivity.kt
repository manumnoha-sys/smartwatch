package com.manumnoha.healthbridge.googlefit

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.manumnoha.healthbridge.BuildConfig
import kotlinx.coroutines.*

private const val TAG = "GoogleFitAuth"

class GoogleFitAuthActivity : Activity() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val data: Uri? = intent.data
        if (data?.scheme == "com.manumnoha.healthbridge" && data.host == "google-fit-callback") {
            val code  = data.getQueryParameter("code")
            val error = data.getQueryParameter("error")
            when {
                error != null -> {
                    Log.e(TAG, "OAuth error: $error")
                    Toast.makeText(this, "Google Fit auth failed: $error", Toast.LENGTH_LONG).show()
                    finish()
                }
                code != null -> exchangeCode(code)
                else -> finish()
            }
        } else {
            startOAuth()
        }
    }

    private fun startOAuth() {
        if (BuildConfig.GOOGLE_FIT_CLIENT_ID.isBlank()) {
            Toast.makeText(this, "Google Fit client ID not configured", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val authUri = GoogleFitClient(
            BuildConfig.GOOGLE_FIT_CLIENT_ID,
            BuildConfig.GOOGLE_FIT_CLIENT_SECRET,
        ).buildAuthUri()
        startActivity(Intent(Intent.ACTION_VIEW, authUri))
    }

    private fun exchangeCode(code: String) {
        scope.launch {
            try {
                val resp = withContext(Dispatchers.IO) {
                    GoogleFitClient(
                        BuildConfig.GOOGLE_FIT_CLIENT_ID,
                        BuildConfig.GOOGLE_FIT_CLIENT_SECRET,
                    ).exchangeCode(code)
                }
                val tokenStore = GoogleFitTokenStore(this@GoogleFitAuthActivity)
                // Google only returns refresh_token on first auth — keep existing if absent
                val refreshToken = resp.refreshToken ?: tokenStore.refreshToken ?: ""
                tokenStore.saveTokens(resp.accessToken, refreshToken, resp.expiresIn)
                Log.i(TAG, "Google Fit connected successfully")
                Toast.makeText(this@GoogleFitAuthActivity, "Google Fit connected!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Token exchange failed: ${e.message}")
                Toast.makeText(this@GoogleFitAuthActivity, "Failed to connect Google Fit: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
