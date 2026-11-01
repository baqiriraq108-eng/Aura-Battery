package com.example

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "telemetry_settings")

class TelemetryDataStore(private val context: Context) {
    companion object {
        val HIGH_THRESHOLD = intPreferencesKey("high_threshold")
        val LOW_THRESHOLD = intPreferencesKey("low_threshold")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val FONT_FAMILY_PATH = stringPreferencesKey("font_family_path")
        val SHOW_TEMP = booleanPreferencesKey("show_temp")
        val SHOW_VOLT = booleanPreferencesKey("show_volt")
        val SHOW_CHARGE_METHOD = booleanPreferencesKey("show_charge_method")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val THEME_PALETTE = stringPreferencesKey("theme_palette")
        val BACKGROUND_SERVICE_ENABLED = booleanPreferencesKey("background_service_enabled")
        val ALERT_SOUND_URI = stringPreferencesKey("alert_sound_uri")
        val ALERT_SOUND_TITLE = stringPreferencesKey("alert_sound_title")
        val CONSUMED_CYCLES = doublePreferencesKey("consumed_cycles")
        val HIDE_ON_SCROLL = booleanPreferencesKey("hide_on_scroll")
        val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        val QUIET_HOURS_START_HOUR = intPreferencesKey("quiet_hours_start_hour")
        val QUIET_HOURS_START_AM_PM = stringPreferencesKey("quiet_hours_start_am_pm")
        val QUIET_HOURS_END_HOUR = intPreferencesKey("quiet_hours_end_hour")
        val QUIET_HOURS_END_AM_PM = stringPreferencesKey("quiet_hours_end_am_pm")
    }

    val highThresholdFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[HIGH_THRESHOLD] ?: 80
    }

    val lowThresholdFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[LOW_THRESHOLD] ?: 20
    }

    val fontScaleFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[FONT_SCALE] ?: 1.0f
    }

    val fontFamilyPathFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[FONT_FAMILY_PATH] ?: ""
    }

    val showTempFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_TEMP] ?: true
    }

    val showVoltFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_VOLT] ?: true
    }

    val showChargeMethodFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_CHARGE_METHOD] ?: true
    }

    val themeModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_MODE] ?: "System"
    }

    val themePaletteFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_PALETTE] ?: "HyperLime"
    }

    val backgroundServiceEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[BACKGROUND_SERVICE_ENABLED] ?: true
    }

    val alertSoundUriFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[ALERT_SOUND_URI] ?: ""
    }

    val alertSoundTitleFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[ALERT_SOUND_TITLE] ?: "Default System Sound"
    }

    val consumedCyclesFlow: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[CONSUMED_CYCLES] ?: 0.0
    }

    val hideOnScrollFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HIDE_ON_SCROLL] ?: true
    }

    val quietHoursEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[QUIET_HOURS_ENABLED] ?: false
    }

    val quietHoursStartHourFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[QUIET_HOURS_START_HOUR] ?: 10
    }

    val quietHoursStartAmPmFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[QUIET_HOURS_START_AM_PM] ?: "PM"
    }

    val quietHoursEndHourFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[QUIET_HOURS_END_HOUR] ?: 7
    }

    val quietHoursEndAmPmFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[QUIET_HOURS_END_AM_PM] ?: "AM"
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

    suspend fun saveFontScale(value: Float) {
        context.dataStore.edit { preferences ->
            preferences[FONT_SCALE] = value
        }
    }

    suspend fun saveFontFamilyPath(value: String) {
        context.dataStore.edit { preferences ->
            preferences[FONT_FAMILY_PATH] = value
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

    suspend fun saveShowChargeMethod(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_CHARGE_METHOD] = value
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

    suspend fun saveAlertSoundUri(value: String) {
        context.dataStore.edit { preferences ->
            preferences[ALERT_SOUND_URI] = value
        }
    }

    suspend fun saveAlertSoundTitle(value: String) {
        context.dataStore.edit { preferences ->
            preferences[ALERT_SOUND_TITLE] = value
        }
    }

    suspend fun saveConsumedCycles(value: Double) {
        context.dataStore.edit { preferences ->
            preferences[CONSUMED_CYCLES] = value
        }
    }

    suspend fun saveHideOnScroll(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HIDE_ON_SCROLL] = value
        }
    }
}
