package com.manumnoha.healthbridge

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.manumnoha.healthbridge.fitbit.FitbitAuthActivity
import com.manumnoha.healthbridge.fitbit.FitbitTokenStore
import com.manumnoha.healthbridge.googlefit.GoogleFitAuthActivity
import com.manumnoha.healthbridge.googlefit.GoogleFitTokenStore

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 80, 48, 48)
        }

        val statusText = TextView(this).apply {
            text = buildStatusText()
            textSize = 16f
        }

        val fitbitBtn = Button(this).apply {
            text = fitbitButtonLabel()
            setOnClickListener {
                startActivity(Intent(this@MainActivity, FitbitAuthActivity::class.java))
            }
        }

        val googleFitBtn = Button(this).apply {
            text = googleFitButtonLabel()
            setOnClickListener {
                startActivity(Intent(this@MainActivity, GoogleFitAuthActivity::class.java))
            }
        }

        layout.addView(statusText)
        layout.addView(fitbitBtn)
        layout.addView(googleFitBtn)
        setContentView(layout)
    }

    override fun onResume() {
        super.onResume()
        // Refresh status after returning from Fitbit auth
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

    private fun fitbitButtonLabel(): String =
        if (FitbitTokenStore(this).hasTokens()) "Reconnect Fitbit" else "Connect Fitbit"

    private fun googleFitButtonLabel(): String =
        if (GoogleFitTokenStore(this).hasTokens()) "Reconnect Google Fit" else "Connect Google Fit"
}
