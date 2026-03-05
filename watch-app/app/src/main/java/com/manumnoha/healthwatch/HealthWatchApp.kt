package com.manumnoha.healthwatch

import android.app.Application
import android.content.Intent
import androidx.work.Configuration
import com.manumnoha.healthwatch.sensor.SensorCollectorService
import com.manumnoha.healthwatch.worker.SyncWorker

class HealthWatchApp : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setMinimumLoggingLevel(android.util.Log.INFO).build()

    override fun onCreate() {
        super.onCreate()
        startForegroundService(Intent(this, SensorCollectorService::class.java))
        SyncWorker.schedule(this)
    }
}
