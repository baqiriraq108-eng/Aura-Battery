package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val telemetryDataStore = TelemetryDataStore(context.applicationContext)
            val isEnabled = runBlocking { telemetryDataStore.backgroundServiceEnabledFlow.first() }
            if (!isEnabled) {
                return
            }

            val serviceIntent = Intent(context, BatteryTelemetryService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
