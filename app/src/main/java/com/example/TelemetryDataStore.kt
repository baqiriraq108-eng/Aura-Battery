package com.example

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class StartupSettingsPayload(
    val highThresh: Int = 80,
    val lowThresh: Int = 20,
    val showTemp: Boolean = true,
    val showVolt: Boolean = true,
    val showHealth: Boolean = true,
    val showChargeMethod: Boolean = true,
    val showCycles: Boolean = true,
    val themeMode: String = "System",
    val themePalette: String = "AuraEarthy",
    val backgroundServiceEnabled: Boolean = true,
    val autoStartOnBootEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietStartHour: Int = 10,
    val quietStartAmPm: String = "PM",
    val quietEndHour: Int = 7,
    val quietEndAmPm: String = "AM",
    val ringtoneUri: String = "",
    val ringtoneName: String = "Default Tone",
    val alertDurationSeconds: Int = 60,
    val snoozeDurationMinutes: Int = 5,
    val snoozeUntilTimestamp: Long = 0L,
    val bootAutoStartEnabled: Boolean = true
)

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "telemetry_settings")

class TelemetryDataStore(private val context: Context) {
    companion object {
        val HIGH_THRESHOLD = intPreferencesKey("high_threshold")
        val LOW_THRESHOLD = intPreferencesKey("low_threshold")
        val SHOW_TEMP = booleanPreferencesKey("show_temp")
        val SHOW_VOLT = booleanPreferencesKey("show_volt")
        val SHOW_HEALTH = booleanPreferencesKey("show_health")
        val SHOW_CHARGE_METHOD = booleanPreferencesKey("show_charge_method")
        val SHOW_CYCLES = booleanPreferencesKey("show_cycles")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val THEME_PALETTE = stringPreferencesKey("theme_palette")
        val BACKGROUND_SERVICE_ENABLED = booleanPreferencesKey("background_service_enabled")
        val AUTO_START_ON_BOOT_ENABLED = booleanPreferencesKey("auto_start_on_boot_enabled")
        val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        val QUIET_HOURS_START_HOUR = intPreferencesKey("quiet_hours_start_hour")
        val QUIET_HOURS_START_AM_PM = stringPreferencesKey("quiet_hours_start_am_pm")
        val QUIET_HOURS_END_HOUR = intPreferencesKey("quiet_hours_end_hour")
        val QUIET_HOURS_END_AM_PM = stringPreferencesKey("quiet_hours_end_am_pm")
        val RINGTONE_URI = stringPreferencesKey("ringtone_uri")
        val RINGTONE_NAME = stringPreferencesKey("ringtone_name")
        val ALERT_DURATION_SECONDS = intPreferencesKey("alert_duration_seconds")
        val SNOOZE_DURATION_MINUTES = intPreferencesKey("snooze_duration_minutes")
        val SNOOZE_UNTIL_TIMESTAMP = longPreferencesKey("snooze_until_timestamp")
        val BOOT_AUTO_START_ENABLED = booleanPreferencesKey("boot_auto_start_enabled")
    }

    private val safePreferencesFlow: Flow<Preferences> = context.dataStore.data
        .catch { emit(emptyPreferences()) }

    private fun <T> Preferences.safeGet(key: Preferences.Key<T>, defaultValue: T): T {
        return try {
            this[key] ?: defaultValue
        } catch (t: Throwable) {
            defaultValue
        }
    }

    val highThresholdFlow: Flow<Int> = safePreferencesFlow.map { preferences ->
        preferences.safeGet(HIGH_THRESHOLD, 80).coerceIn(50, 100)
    }

    val lowThresholdFlow: Flow<Int> = safePreferencesFlow.map { preferences ->
        preferences.safeGet(LOW_THRESHOLD, 20).coerceIn(5, 49)
    }

    val showTempFlow: Flow<Boolean> = safePreferencesFlow.map { preferences ->
        preferences.safeGet(SHOW_TEMP, true)
    }

    val showVoltFlow: Flow<Boolean> = safePreferencesFlow.map { preferences ->
        preferences.safeGet(SHOW_VOLT, true)
    }

    val showHealthFlow: Flow<Boolean> = safePreferencesFlow.map { preferences ->
        preferences.safeGet(SHOW_HEALTH, true)
    }

    val showChargeMethodFlow: Flow<Boolean> = safePreferencesFlow.map { preferences ->
        preferences.safeGet(SHOW_CHARGE_METHOD, true)
    }

    val showCyclesFlow: Flow<Boolean> = safePreferencesFlow.map { preferences ->
        preferences.safeGet(SHOW_CYCLES, true)
    }

    val themeModeFlow: Flow<String> = safePreferencesFlow.map { preferences ->
        preferences.safeGet(THEME_MODE, "System")
    }

    val themePaletteFlow: Flow<String> = safePreferencesFlow.map { preferences ->
        preferences.safeGet(THEME_PALETTE, "AuraEarthy")
    }

    val backgroundServiceEnabledFlow: Flow<Boolean> = safePreferencesFlow.map { preferences ->
        preferences.safeGet(BACKGROUND_SERVICE_ENABLED, true)
    }

    val autoStartOnBootEnabledFlow: Flow<Boolean> = safePreferencesFlow.map { preferences ->
        preferences.safeGet(AUTO_START_ON_BOOT_ENABLED, true)
    }

    val quietHoursEnabledFlow: Flow<Boolean> = safePreferencesFlow.map { preferences ->
        preferences.safeGet(QUIET_HOURS_ENABLED, false)
    }

    val quietHoursStartHourFlow: Flow<Int> = safePreferencesFlow.map { preferences ->
        preferences.safeGet(QUIET_HOURS_START_HOUR, 10).coerceIn(1, 12)
    }

    val quietHoursStartAmPmFlow: Flow<String> = safePreferencesFlow.map { preferences ->
        preferences.safeGet(QUIET_HOURS_START_AM_PM, "PM")
    }

    val quietHoursEndHourFlow: Flow<Int> = safePreferencesFlow.map { preferences ->
        preferences.safeGet(QUIET_HOURS_END_HOUR, 7).coerceIn(1, 12)
    }

    val quietHoursEndAmPmFlow: Flow<String> = safePreferencesFlow.map { preferences ->
        preferences.safeGet(QUIET_HOURS_END_AM_PM, "AM")
    }

    val ringtoneUriFlow: Flow<String> = safePreferencesFlow.map { preferences ->
        preferences.safeGet(RINGTONE_URI, "")
    }

    val ringtoneNameFlow: Flow<String> = safePreferencesFlow.map { preferences ->
        preferences.safeGet(RINGTONE_NAME, "Default Tone")
    }

    val alertDurationSecondsFlow: Flow<Int> = safePreferencesFlow.map { preferences ->
        preferences.safeGet(ALERT_DURATION_SECONDS, 60).coerceIn(15, 300)
    }

    val snoozeDurationMinutesFlow: Flow<Int> = safePreferencesFlow.map { preferences ->
        preferences.safeGet(SNOOZE_DURATION_MINUTES, 5).coerceIn(1, 60)
    }

    val snoozeUntilTimestampFlow: Flow<Long> = safePreferencesFlow.map { preferences ->
        preferences.safeGet(SNOOZE_UNTIL_TIMESTAMP, 0L)
    }

    val bootAutoStartEnabledFlow: Flow<Boolean> = safePreferencesFlow.map { preferences ->
        preferences.safeGet(BOOT_AUTO_START_ENABLED, true)
    }

    suspend fun saveAlertDurationSeconds(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[ALERT_DURATION_SECONDS] = seconds.coerceIn(15, 300)
        }
    }

    suspend fun saveSnoozeDurationMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[SNOOZE_DURATION_MINUTES] = minutes.coerceIn(1, 60)
        }
    }

    suspend fun saveSnoozeUntilTimestamp(timestampMillis: Long) {
        context.dataStore.edit { preferences ->
            preferences[SNOOZE_UNTIL_TIMESTAMP] = timestampMillis
        }
    }

    suspend fun saveRingtone(uri: String, name: String) {
        context.dataStore.edit { preferences ->
            preferences[RINGTONE_URI] = uri
            preferences[RINGTONE_NAME] = name
        }
    }

    suspend fun saveQuietHoursEnabled(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[QUIET_HOURS_ENABLED] = value
        }
    }

    suspend fun saveQuietHoursStartHour(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[QUIET_HOURS_START_HOUR] = value
        }
    }

    suspend fun saveQuietHoursStartAmPm(value: String) {
        context.dataStore.edit { preferences ->
            preferences[QUIET_HOURS_START_AM_PM] = value
        }
    }

    suspend fun saveQuietHoursEndHour(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[QUIET_HOURS_END_HOUR] = value
        }
    }

    suspend fun saveQuietHoursEndAmPm(value: String) {
        context.dataStore.edit { preferences ->
            preferences[QUIET_HOURS_END_AM_PM] = value
        }
    }

    suspend fun saveHighThreshold(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[HIGH_THRESHOLD] = value
        }
    }

    suspend fun saveLowThreshold(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[LOW_THRESHOLD] = value
        }
    }

    suspend fun saveShowTemp(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_TEMP] = value
        }
    }

    suspend fun saveShowVolt(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_VOLT] = value
        }
    }

    suspend fun saveShowHealth(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_HEALTH] = value
        }
    }

    suspend fun saveShowChargeMethod(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_CHARGE_METHOD] = value
        }
    }

    suspend fun saveShowCycles(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_CYCLES] = value
        }
    }

    suspend fun saveThemeMode(value: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = value
        }
    }

    suspend fun saveThemePalette(value: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_PALETTE] = value
        }
    }

    suspend fun saveBackgroundServiceEnabled(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BACKGROUND_SERVICE_ENABLED] = value
        }
    }

    suspend fun saveAutoStartOnBootEnabled(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_START_ON_BOOT_ENABLED] = value
            preferences[BOOT_AUTO_START_ENABLED] = value
        }
    }

    suspend fun saveBootAutoStartEnabled(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BOOT_AUTO_START_ENABLED] = value
            preferences[AUTO_START_ON_BOOT_ENABLED] = value
        }
    }

    suspend fun loadStartupSettings(): StartupSettingsPayload {
        return try {
            val prefs = safePreferencesFlow.first()
            val bootEnabled = prefs.safeGet(BOOT_AUTO_START_ENABLED, prefs.safeGet(AUTO_START_ON_BOOT_ENABLED, true))
            StartupSettingsPayload(
                highThresh = prefs.safeGet(HIGH_THRESHOLD, 80).coerceIn(50, 100),
                lowThresh = prefs.safeGet(LOW_THRESHOLD, 20).coerceIn(5, 49),
                showTemp = prefs.safeGet(SHOW_TEMP, true),
                showVolt = prefs.safeGet(SHOW_VOLT, true),
                showHealth = prefs.safeGet(SHOW_HEALTH, true),
                showChargeMethod = prefs.safeGet(SHOW_CHARGE_METHOD, true),
                showCycles = prefs.safeGet(SHOW_CYCLES, true),
                themeMode = prefs.safeGet(THEME_MODE, "System"),
                themePalette = prefs.safeGet(THEME_PALETTE, "AuraEarthy"),
                backgroundServiceEnabled = prefs.safeGet(BACKGROUND_SERVICE_ENABLED, true),
                autoStartOnBootEnabled = bootEnabled,
                quietHoursEnabled = prefs.safeGet(QUIET_HOURS_ENABLED, false),
                quietStartHour = prefs.safeGet(QUIET_HOURS_START_HOUR, 10).coerceIn(1, 12),
                quietStartAmPm = prefs.safeGet(QUIET_HOURS_START_AM_PM, "PM"),
                quietEndHour = prefs.safeGet(QUIET_HOURS_END_HOUR, 7).coerceIn(1, 12),
                quietEndAmPm = prefs.safeGet(QUIET_HOURS_END_AM_PM, "AM"),
                ringtoneUri = prefs.safeGet(RINGTONE_URI, ""),
                ringtoneName = prefs.safeGet(RINGTONE_NAME, "Default Tone"),
                alertDurationSeconds = prefs.safeGet(ALERT_DURATION_SECONDS, 60),
                snoozeDurationMinutes = prefs.safeGet(SNOOZE_DURATION_MINUTES, 5),
                snoozeUntilTimestamp = prefs.safeGet(SNOOZE_UNTIL_TIMESTAMP, 0L),
                bootAutoStartEnabled = bootEnabled
            )
        } catch (t: Throwable) {
            StartupSettingsPayload()
        }
    }
}
