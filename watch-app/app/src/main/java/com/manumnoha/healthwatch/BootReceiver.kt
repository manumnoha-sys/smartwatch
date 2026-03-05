package com.manumnoha.healthwatch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.manumnoha.healthwatch.sensor.SensorCollectorService
import com.manumnoha.healthwatch.worker.SyncWorker

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            context.startForegroundService(Intent(context, SensorCollectorService::class.java))
            SyncWorker.schedule(context)
        }
    }
}
