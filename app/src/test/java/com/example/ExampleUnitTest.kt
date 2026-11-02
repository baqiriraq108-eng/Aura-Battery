package com.example

import android.os.BatteryManager
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testLiveBatteryStateMappings() {
        val goodState = LiveBatteryState(
            level = 85,
            isCharging = true,
            health = BatteryManager.BATTERY_HEALTH_GOOD,
            plugged = BatteryManager.BATTERY_PLUGGED_AC
        )
        assertEquals("Good", goodState.healthString)
        assertEquals("AC Power", goodState.sourceString)

        val overheatState = LiveBatteryState(
            health = BatteryManager.BATTERY_HEALTH_OVERHEAT,
            plugged = BatteryManager.BATTERY_PLUGGED_USB
        )
        assertEquals("Overheated", overheatState.healthString)
        assertEquals("USB Port", overheatState.sourceString)

        val internalState = LiveBatteryState(
            health = BatteryManager.BATTERY_HEALTH_DEAD,
            plugged = 0
        )
        assertEquals("Dead", internalState.healthString)
        assertEquals("Internal", internalState.sourceString)
    }

    @Test
    fun testCalculateActiveDurationWithEmptyLogs() {
        val duration = calculateActiveDuration(emptyList(), 80)
        assertTrue(duration.contains("h") && duration.contains("m"))
    }

    @Test
    fun testCalculateActiveDurationWithFullCharge() {
        val now = System.currentTimeMillis()
        val logs = listOf(
            BatteryLogEntry(timestamp = now - 3600000L, batteryLevel = 100, isCharging = true),
            BatteryLogEntry(timestamp = now - 1800000L, batteryLevel = 90, isCharging = false),
            BatteryLogEntry(timestamp = now, batteryLevel = 80, isCharging = false)
        )
        val duration = calculateActiveDuration(logs, 80)
        assertEquals("1h 0m", duration)
    }

    @Test
    fun testBatteryLogEntryCreation() {
        val timestamp = System.currentTimeMillis()
        val entry = BatteryLogEntry(
            timestamp = timestamp,
            batteryLevel = 75,
            isCharging = false
        )
        assertEquals(timestamp, entry.timestamp)
        assertEquals(75, entry.batteryLevel)
        assertFalse(entry.isCharging)
    }
}
