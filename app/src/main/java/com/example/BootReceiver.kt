package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (
            action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val telemetryDataStore = TelemetryDataStore(context.applicationContext)
                    val bootAutoStart = telemetryDataStore.bootAutoStartEnabledFlow.first()
                    val isEnabled = telemetryDataStore.backgroundServiceEnabledFlow.first()
                    if (bootAutoStart && isEnabled) {
                        val serviceIntent = Intent(context, BatteryTelemetryService::class.java)
                        try {
                            context.startService(serviceIntent)
                        } catch (inner: Throwable) {
                            Log.w("BootReceiver", "Background startService deferred until app launch", inner)
                        }
                    }
                } catch (t: Throwable) {
                    Log.e("BootReceiver", "Failed to start BatteryTelemetryService on boot", t)
                } finally {
                    try {
                        pendingResult.finish()
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            }
        }
    }
}
