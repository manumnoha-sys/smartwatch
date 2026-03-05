package com.manumnoha.healthbridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.manumnoha.healthbridge.service.BridgeService
import com.manumnoha.healthbridge.worker.CgmSyncWorker
import com.manumnoha.healthbridge.worker.SamsungHealthSyncWorker
import com.manumnoha.healthbridge.worker.WodifySyncWorker

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            context.startForegroundService(Intent(context, BridgeService::class.java))
            CgmSyncWorker.schedule(context)
            WodifySyncWorker.schedule(context)
            SamsungHealthSyncWorker.schedule(context)
        }
    }
}
