package com.example

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: BatteryLogEntry)

    @Query("SELECT * FROM battery_logs WHERE timestamp >= :cutoffTime ORDER BY timestamp ASC")
    fun getLogsInPast24HoursFlow(cutoffTime: Long): Flow<List<BatteryLogEntry>>

    @Query("SELECT * FROM battery_logs ORDER BY timestamp ASC")
    fun getAllLogsFlow(): Flow<List<BatteryLogEntry>>

    @Query("SELECT * FROM battery_logs ORDER BY timestamp ASC")
    suspend fun getAllLogsSnapshot(): List<BatteryLogEntry>

    @Query("DELETE FROM battery_logs WHERE timestamp < :cutoffTime")
    suspend fun pruneLogs(cutoffTime: Long)

    @Query("SELECT * FROM battery_logs WHERE batteryLevel = 100 ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastFullChargeLog(): BatteryLogEntry?
}
