package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val telemetryDataStore = TelemetryDataStore(context.applicationContext)
                    val isEnabled = telemetryDataStore.backgroundServiceEnabledFlow.first()
                    if (isEnabled) {
                        val serviceIntent = Intent(context, BatteryTelemetryService::class.java)
                        try {
                            context.startService(serviceIntent)
                        } catch (e: Exception) {
                            Log.w("BootReceiver", "Background startService deferred until app launch", e)
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
