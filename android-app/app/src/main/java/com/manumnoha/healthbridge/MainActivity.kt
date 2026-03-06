package com.manumnoha.healthbridge

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.manumnoha.healthbridge.fitbit.FitbitAuthActivity
import com.manumnoha.healthbridge.fitbit.FitbitTokenStore
import com.manumnoha.healthbridge.googlefit.GoogleFitAuthActivity
import com.manumnoha.healthbridge.googlefit.GoogleFitTokenStore
import com.manumnoha.healthbridge.samsung.HEALTH_CONNECT_PERMISSIONS
import com.manumnoha.healthbridge.worker.SamsungHealthSyncWorker
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val requestPermissions =
        registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
            if (granted.containsAll(HEALTH_CONNECT_PERMISSIONS)) {
                Toast.makeText(this, "Health Connect connected!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Some Health Connect permissions denied", Toast.LENGTH_LONG).show()
            }
            setContentView(createLayout())
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createLayout())
    }

    override fun onResume() {
        super.onResume()
        setContentView(createLayout())
    }

    private fun createLayout(): LinearLayout {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 80, 48, 48)
        }
        layout.addView(TextView(this).apply {
            text = buildStatusText()
            textSize = 16f
        })
        layout.addView(Button(this).apply {
            text = healthConnectButtonLabel()
            setOnClickListener { requestHealthConnectPermissions() }
        })
        layout.addView(Button(this).apply {
            text = "Sync Now (Samsung Health)"
            setOnClickListener {
                val req = OneTimeWorkRequestBuilder<SamsungHealthSyncWorker>().build()
                WorkManager.getInstance(this@MainActivity).enqueue(req)
                Toast.makeText(this@MainActivity, "Sync started — check logcat", Toast.LENGTH_SHORT).show()
            }
        })
        layout.addView(Button(this).apply {
            text = fitbitButtonLabel()
            setOnClickListener {
                startActivity(Intent(this@MainActivity, FitbitAuthActivity::class.java))
            }
        })
        layout.addView(Button(this).apply {
            text = googleFitButtonLabel()
            setOnClickListener {
                startActivity(Intent(this@MainActivity, GoogleFitAuthActivity::class.java))
            }
        })
        return layout
    }

    private fun requestHealthConnectPermissions() {
        lifecycleScope.launch {
            val client = runCatching { HealthConnectClient.getOrCreate(this@MainActivity) }.getOrNull()
            if (client == null) {
                Toast.makeText(this@MainActivity, "Health Connect not available", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val granted = client.permissionController.getGrantedPermissions()
            if (granted.containsAll(HEALTH_CONNECT_PERMISSIONS)) {
                Toast.makeText(this@MainActivity, "Already connected to Health Connect", Toast.LENGTH_SHORT).show()
            } else {
                requestPermissions.launch(HEALTH_CONNECT_PERMISSIONS)
            }
        }
    }

    private fun buildStatusText(): String {
        val fitbit = if (FitbitTokenStore(this).hasTokens()) "connected" else "not connected"
        return """
            Health Bridge is running.

            Syncing:
            · Samsung Health / Galaxy Watch (every 30 min)
            · Fitbit: $fitbit (every 30 min)
            · Google Fit: ${if (GoogleFitTokenStore(this).hasTokens()) "connected" else "not connected"} (every 30 min)
            · CGM glucose (every 5 min)
            · Wodify workouts (every 30 min)

            Server: ${BuildConfig.SERVER_URL}
        """.trimIndent()
    }

    private fun healthConnectButtonLabel(): String = "Connect Health Connect (Galaxy Watch)"
    private fun fitbitButtonLabel(): String =
        if (FitbitTokenStore(this).hasTokens()) "Reconnect Fitbit" else "Connect Fitbit"
    private fun googleFitButtonLabel(): String =
        if (GoogleFitTokenStore(this).hasTokens()) "Reconnect Google Fit" else "Connect Google Fit"
}
