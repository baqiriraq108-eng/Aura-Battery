package com.example

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "battery_logs")
data class BatteryLogEntry(
    @PrimaryKey val timestamp: Long, // Epoch millisecond
    val batteryLevel: Int,           // 0 to 100
    val isCharging: Boolean,         // True if plugged in, False otherwise
    val topPackageName: String? = null // Top executing background package during this interval
)
