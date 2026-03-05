package com.manumnoha.healthbridge

import android.app.Application
import android.content.Intent
import androidx.work.Configuration
import com.manumnoha.healthbridge.service.BridgeService
import com.manumnoha.healthbridge.worker.CgmSyncWorker
import com.manumnoha.healthbridge.worker.WodifySyncWorker

class HealthBridgeApp : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setMinimumLoggingLevel(android.util.Log.INFO).build()

    override fun onCreate() {
        super.onCreate()
        startForegroundService(Intent(this, BridgeService::class.java))
        CgmSyncWorker.schedule(this)
        WodifySyncWorker.schedule(this)
    }
}
