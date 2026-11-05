package com.example

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.ui.*
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Calendar

// App state models
data class LiveBatteryState(
    val level: Int = 80,
    val isCharging: Boolean = false,
    val temperature: Float? = 34.3f,
    val voltage: Int? = 4107,
    val plugged: Int = 0,
    val status: Int = BatteryManager.BATTERY_STATUS_UNKNOWN,
    val health: Int = BatteryManager.BATTERY_HEALTH_GOOD,
    val cycleCount: Int? = null
) {
    val healthString: String?
        get() = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheated"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> null
        }

    val sourceString: String
        get() = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Power"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Internal"
        }
}

private fun parseBatteryChangedIntent(intent: Intent, fallbackLevel: Int): LiveBatteryState {
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    val pct = if (level >= 0 && scale > 0) (level * 100) / scale else fallbackLevel

    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
    val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL ||
        plugged != 0

    val tempRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
    val temperature: Float? = if (tempRaw > 0) tempRaw / 10.0f else null

    val voltRaw = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
    val voltage: Int? = if (voltRaw > 0) voltRaw else null

    val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
    val cycleCount = intent.getIntExtra("android.os.extra.CYCLE_COUNT", -1).takeIf { it >= 0 }

    return LiveBatteryState(
        level = pct,
        isCharging = isCharging,
        temperature = temperature,
        voltage = voltage,
        plugged = plugged,
        status = status,
        health = health,
        cycleCount = cycleCount
    )
}

fun Context.readInitialBatteryState(): LiveBatteryState {
    try {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val stickyIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(null, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(null, filter)
        }
        if (stickyIntent != null) {
            return parseBatteryChangedIntent(stickyIntent, 80)
        }
    } catch (e: Exception) {
        Log.e("MainActivity", "Failed to read initial sticky battery intent", e)
    }

    val bm = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    if (bm != null) {
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
        val isCharging = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) bm.isCharging else false
        return LiveBatteryState(
            level = if (level > 0) level else 80,
            isCharging = isCharging
        )
    }
    return LiveBatteryState()
}

fun Context.batteryStatusFlow(): Flow<LiveBatteryState> = callbackFlow {
    var latestState = readInitialBatteryState()
    trySend(latestState).isSuccess

    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_DISCONNECTED -> {
                    latestState = latestState.copy(
                        isCharging = false,
                        plugged = 0,
                        status = BatteryManager.BATTERY_STATUS_DISCHARGING
                    )
                    trySend(latestState).isSuccess
                }
                Intent.ACTION_POWER_CONNECTED -> {
                    latestState = latestState.copy(
                        isCharging = true,
                        status = BatteryManager.BATTERY_STATUS_CHARGING
                    )
                    trySend(latestState).isSuccess
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    latestState = parseBatteryChangedIntent(intent, latestState.level)
                    trySend(latestState).isSuccess
                }
            }
        }
    }

    var stickyIntent: Intent? = null
    try {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        stickyIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
    } catch (e: Exception) {
        Log.e("MainActivity", "Failed to register battery receiver", e)
    }

    if (stickyIntent != null && stickyIntent.action == Intent.ACTION_BATTERY_CHANGED) {
        latestState = parseBatteryChangedIntent(stickyIntent, latestState.level)
        trySend(latestState).isSuccess
    }

    awaitClose {
        try {
            unregisterReceiver(receiver)
        } catch (e: Exception) {
            // Ignore
        }
    }
}

class MainActivity : ComponentActivity() {

    private lateinit var telemetryDataStore: TelemetryDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        telemetryDataStore = TelemetryDataStore(applicationContext)

        val initialBattery = readInitialBatteryState()
        val db = AppDatabase.getDatabase(applicationContext)

        // Pre-warm settings and recent logs with a strict 250ms safety timeout
        var initialSettings = StartupSettingsPayload()
        var initialLogs: List<BatteryLogEntry> = emptyList()

        runBlocking(Dispatchers.IO) {
            try {
                withTimeoutOrNull(250L) {
                    initialSettings = telemetryDataStore.loadStartupSettings()
                    initialLogs = db.batteryLogDao().getAllLogsSnapshot()
                }
            } catch (t: Throwable) {
                Log.e("MainActivity", "Cold-start pre-warm timeout or error", t)
            }
        }

        // Apply matching window background immediately so Android never flashes white or wrong color
        val isInitialDark = when (initialSettings.themeMode) {
            "Light" -> false
            "Dark" -> true
            else -> {
                val nightModeFlags = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }
        val startupClayPalette = ExpressivePalettes.getClayPalette(initialSettings.themePalette, isInitialDark)
        val startupBgArgb = startupClayPalette.screenBackground.toArgb()
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(startupBgArgb))
        window.decorView.setBackgroundColor(startupBgArgb)

        // Start or stop BatteryTelemetryService silently
        lifecycleScope.launch {
            telemetryDataStore.backgroundServiceEnabledFlow
                .distinctUntilChanged()
                .collect { isEnabled ->
                    try {
                        val serviceIntent = Intent(this@MainActivity, BatteryTelemetryService::class.java)
                        if (isEnabled) {
                            startService(serviceIntent)
                        } else {
                            stopService(serviceIntent)
                        }
                    } catch (t: Throwable) {
                        Log.e("MainActivity", "Error starting/stopping telemetry service", t)
                    }
                }
        }

        val requestPermissionLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { _ -> }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val context = LocalContext.current
            val liveState by remember(context) { context.batteryStatusFlow() }
                .collectAsStateWithLifecycle(initialValue = initialBattery)

            val themeMode by telemetryDataStore.themeModeFlow.collectAsState(initial = initialSettings.themeMode)
            val themePalette by telemetryDataStore.themePaletteFlow.collectAsState(initial = initialSettings.themePalette)

            val isDarkTheme = when (themeMode) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }

            val palette = ExpressivePalettes.getPalette(themePalette)
            val clayPalette = ExpressivePalettes.getClayPalette(themePalette, isDarkTheme)
            val colorScheme = if (isDarkTheme) palette.darkScheme else palette.lightScheme

            SideEffect {
                val bgArgb = clayPalette.screenBackground.toArgb()
                window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(bgArgb))
                window.decorView.setBackgroundColor(bgArgb)
            }

            CompositionLocalProvider(LocalAuraClayPalette provides clayPalette) {
                MaterialTheme(
                    typography = Typography,
                    colorScheme = colorScheme
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = clayPalette.screenBackground
                    ) {
                        MainNavigationRouter(
                            telemetryDataStore = telemetryDataStore,
                            liveState = liveState,
                            isDark = isDarkTheme,
                            initialSettings = initialSettings,
                            initialLogs = initialLogs
                        )
                    }
                }
            }
        }
    }
}

private fun isCurrentTimeInQuietHours(
    enabled: Boolean,
    startHour: Int,
    startAmPm: String,
    endHour: Int,
    endAmPm: String
): Boolean {
    if (!enabled) return false
    val calendar = Calendar.getInstance()
    val currentHour24 = calendar.get(Calendar.HOUR_OF_DAY)
    val currentMinute = calendar.get(Calendar.MINUTE)

    fun get24Hour(hour12: Int, amPm: String): Int {
        val isPm = amPm.equals("PM", ignoreCase = true)
        return when {
            hour12 == 12 -> if (isPm) 12 else 0
            isPm -> hour12 + 12
            else -> hour12
        }
    }

    val start24 = get24Hour(startHour, startAmPm)
    val end24 = get24Hour(endHour, endAmPm)
    val currentVal = currentHour24 * 60 + currentMinute
    val startVal = start24 * 60
    val endVal = end24 * 60

    return if (startVal < endVal) {
        currentVal in startVal until endVal
    } else if (startVal > endVal) {
        currentVal >= startVal || currentVal < endVal
    } else {
        true
    }
}

@Composable
fun MainNavigationRouter(
    telemetryDataStore: TelemetryDataStore,
    liveState: LiveBatteryState,
    isDark: Boolean = isSystemInDarkTheme(),
    initialSettings: StartupSettingsPayload = StartupSettingsPayload(),
    initialLogs: List<BatteryLogEntry> = emptyList()
) {
    val context = LocalContext.current
    val clayPalette = LocalAuraClayPalette.current
    val scope = rememberCoroutineScope()
    var currentRoute by rememberSaveable { mutableStateOf("dashboard") }

    BackHandler(enabled = currentRoute != "dashboard") {
        currentRoute = "dashboard"
    }

    val db = remember { AppDatabase.getDatabase(context.applicationContext) }
    val logs by remember { db.batteryLogDao().getAllLogsFlow() }
        .collectAsState(initial = initialLogs, context = Dispatchers.IO)

    val themeMode by telemetryDataStore.themeModeFlow.collectAsState(initial = initialSettings.themeMode)
    val themePalette by telemetryDataStore.themePaletteFlow.collectAsState(initial = initialSettings.themePalette)
    val backgroundServiceEnabled by telemetryDataStore.backgroundServiceEnabledFlow.collectAsState(initial = initialSettings.backgroundServiceEnabled)
    val ringtoneUri by telemetryDataStore.ringtoneUriFlow.collectAsState(initial = initialSettings.ringtoneUri)
    val ringtoneName by telemetryDataStore.ringtoneNameFlow.collectAsState(initial = initialSettings.ringtoneName)
    val alertDurationSeconds by telemetryDataStore.alertDurationSecondsFlow.collectAsState(initial = initialSettings.alertDurationSeconds)

    val highThresh by telemetryDataStore.highThresholdFlow.collectAsState(initial = initialSettings.highThresh)
    val lowThresh by telemetryDataStore.lowThresholdFlow.collectAsState(initial = initialSettings.lowThresh)
    val showTemp by telemetryDataStore.showTempFlow.collectAsState(initial = initialSettings.showTemp)
    val showVolt by telemetryDataStore.showVoltFlow.collectAsState(initial = initialSettings.showVolt)
    val showHealth by telemetryDataStore.showHealthFlow.collectAsState(initial = initialSettings.showHealth)
    val showChargeMethod by telemetryDataStore.showChargeMethodFlow.collectAsState(initial = initialSettings.showChargeMethod)
    val showCycles by telemetryDataStore.showCyclesFlow.collectAsState(initial = initialSettings.showCycles)

    val snoozeDurationMinutes by telemetryDataStore.snoozeDurationMinutesFlow.collectAsState(initial = initialSettings.snoozeDurationMinutes)
    val snoozeUntilTimestamp by telemetryDataStore.snoozeUntilTimestampFlow.collectAsState(initial = initialSettings.snoozeUntilTimestamp)

    val quietHoursEnabled by telemetryDataStore.quietHoursEnabledFlow.collectAsState(initial = initialSettings.quietHoursEnabled)
    val quietStartHour by telemetryDataStore.quietHoursStartHourFlow.collectAsState(initial = initialSettings.quietStartHour)
    val quietStartAmPm by telemetryDataStore.quietHoursStartAmPmFlow.collectAsState(initial = initialSettings.quietStartAmPm)
    val quietEndHour by telemetryDataStore.quietHoursEndHourFlow.collectAsState(initial = initialSettings.quietEndHour)
    val quietEndAmPm by telemetryDataStore.quietHoursEndAmPmFlow.collectAsState(initial = initialSettings.quietEndAmPm)

    val quietActive = remember(quietHoursEnabled, quietStartHour, quietStartAmPm, quietEndHour, quietEndAmPm) {
        isCurrentTimeInQuietHours(quietHoursEnabled, quietStartHour, quietStartAmPm, quietEndHour, quietEndAmPm)
    }

    val navigateToRoute: (String) -> Unit = { route ->
        if (route != currentRoute) {
            currentRoute = route
        }
    }

    Scaffold(
        containerColor = clayPalette.screenBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(clayPalette.screenBackground)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
        ) {
            when (currentRoute) {
                "diagnostics" -> {
                    AdvancedDiagnosticsScreen(
                        logs = logs,
                        liveState = liveState,
                        currentRoute = currentRoute,
                        onNavigate = navigateToRoute,
                        isDark = isDark
                    )
                }
                "settings" -> {
                    SettingsScreen(
                        telemetryDataStore = telemetryDataStore,
                        currentRoute = currentRoute,
                        onNavigate = navigateToRoute,
                        isDark = isDark,
                        initialHighThresh = highThresh,
                        initialLowThresh = lowThresh,
                        initialShowTemp = showTemp,
                        initialShowVolt = showVolt,
                        initialShowHealth = showHealth,
                        initialShowChargeMethod = showChargeMethod,
                        initialShowCycles = showCycles,
                        initialQuietHoursEnabled = quietHoursEnabled,
                        initialQuietStartHour = quietStartHour,
                        initialQuietStartAmPm = quietStartAmPm,
                        initialQuietEndHour = quietEndHour,
                        initialQuietEndAmPm = quietEndAmPm,
                        initialThemeMode = themeMode,
                        initialThemePalette = themePalette,
                        initialBackgroundServiceEnabled = backgroundServiceEnabled,
                        initialRingtoneUri = ringtoneUri,
                        initialRingtoneName = ringtoneName,
                        initialAlertDurationSeconds = alertDurationSeconds,
                        initialSnoozeDurationMinutes = snoozeDurationMinutes
                    )
                }
                else -> {
                    DashboardScreen(
                        liveState = liveState,
                        logs = logs,
                        highThreshold = highThresh,
                        lowThreshold = lowThresh,
                        showTemp = showTemp,
                        showVolt = showVolt,
                        showHealth = showHealth,
                        showChargeMethod = showChargeMethod,
                        showCycles = showCycles,
                        snoozeDurationMinutes = snoozeDurationMinutes,
                        snoozeUntilTimestamp = snoozeUntilTimestamp,
                        onSnoozeAlert = {
                            val snoozeUntil = System.currentTimeMillis() + snoozeDurationMinutes * 60_000L
                            scope.launch {
                                telemetryDataStore.saveSnoozeUntilTimestamp(snoozeUntil)
                            }
                            try {
                                val snoozeIntent = Intent(context, BatteryTelemetryService::class.java).apply {
                                    action = BatteryTelemetryService.ACTION_SNOOZE_ALERT
                                }
                                context.startService(snoozeIntent)
                            } catch (e: Exception) {
                                Log.e("MainActivity", "Failed to send snooze intent", e)
                            }
                        },
                        quietHoursActive = quietActive,
                        isDark = isDark,
                        currentRoute = currentRoute,
                        onNavigate = navigateToRoute
                    )
                }
            }
        }
    }
}

private data class DashboardIndicatorTileSpec(
    val key: String,
    val title: String,
    val value: String,
    val faceColor: Color,
    val lipColor: Color,
    val textColor: Color,
    val iconType: TactileMetricIconType
)

/**
 * MODULE 1: HOME / DASHBOARD SCREEN
 * - "25h Activity" centered directly below the hero charging circle.
 * - Repeating visual alert banner triggers ONLY when High Limit (charging) or Low Limit (discharging) is active
 *   and not snoozed, and automatically dismisses the instant the user unplugs or connects the charger.
 * - Indicator tiles dynamically reflow and auto-center when any indicator is toggled off in Settings
 *   or suppressed due to OS hardware restriction, with 100% synchronized colors including calibrated Health Green.
 */
@Composable
fun DashboardScreen(
    liveState: LiveBatteryState,
    logs: List<BatteryLogEntry> = emptyList(),
    highThreshold: Int = 80,
    lowThreshold: Int = 20,
    showTemp: Boolean = true,
    showVolt: Boolean = true,
    showHealth: Boolean = true,
    showChargeMethod: Boolean = true,
    showCycles: Boolean = true,
    snoozeDurationMinutes: Int = 5,
    snoozeUntilTimestamp: Long = 0L,
    onSnoozeAlert: () -> Unit = {},
    quietHoursActive: Boolean = false,
    isDark: Boolean = isSystemInDarkTheme(),
    currentRoute: String = "dashboard",
    onNavigate: (String) -> Unit = {}
) {
    val clayPalette = LocalAuraClayPalette.current
    val scrollState = rememberScrollState()

    var localSnoozedUntil by remember(snoozeUntilTimestamp) { mutableLongStateOf(snoozeUntilTimestamp) }

    // Clear local snooze immediately when charger state changes away from the alert condition
    LaunchedEffect(liveState.isCharging) {
        localSnoozedUntil = 0L
    }

    val isCurrentlySnoozed = localSnoozedUntil > System.currentTimeMillis()

    val isHighAlertTriggered = !quietHoursActive && !isCurrentlySnoozed &&
        liveState.isCharging && liveState.level >= highThreshold
    val isLowAlertTriggered = !quietHoursActive && !isCurrentlySnoozed &&
        !liveState.isCharging && liveState.level <= lowThreshold

    // Build list of active & hardware-available indicator tiles for dynamic reflow
    val visibleIndicatorTiles = remember(
        liveState,
        showVolt,
        showHealth,
        showTemp,
        showChargeMethod,
        showCycles
    ) {
        buildList {
            if (showVolt && liveState.voltage != null && liveState.voltage > 0) {
                add(
                    DashboardIndicatorTileSpec(
                        key = "voltage",
                        title = "Voltage",
                        value = String.format("%.2f V", liveState.voltage / 1000.0),
                        faceColor = AuraTactileColors.TileSlateFace,
                        lipColor = AuraTactileColors.TileSlateLip,
                        textColor = AuraTactileColors.CreamText,
                        iconType = TactileMetricIconType.NONE
                    )
                )
            }
            val genuineHealth = liveState.healthString
            if (showHealth && genuineHealth != null) {
                add(
                    DashboardIndicatorTileSpec(
                        key = "health",
                        title = "Health",
                        value = genuineHealth,
                        faceColor = AuraTactileColors.TileHealthGreenFace,
                        lipColor = AuraTactileColors.TileHealthGreenLip,
                        textColor = AuraTactileColors.CreamText,
                        iconType = TactileMetricIconType.CLOCK_HISTORY
                    )
                )
            }
            if (showTemp && liveState.temperature != null && liveState.temperature > 0f) {
                add(
                    DashboardIndicatorTileSpec(
                        key = "temp",
                        title = "Temp.",
                        value = String.format("%.1f °C", liveState.temperature),
                        faceColor = AuraTactileColors.TileRustFace,
                        lipColor = AuraTactileColors.TileRustLip,
                        textColor = AuraTactileColors.CreamText,
                        iconType = TactileMetricIconType.THERMOMETER
                    )
                )
            }
            if (showChargeMethod) {
                add(
                    DashboardIndicatorTileSpec(
                        key = "source",
                        title = "Source",
                        value = liveState.sourceString,
                        faceColor = AuraTactileColors.TileMustardFace,
                        lipColor = AuraTactileColors.TileMustardLip,
                        textColor = AuraTactileColors.CreamText,
                        iconType = TactileMetricIconType.LIGHTNING_BOLT
                    )
                )
            }
            if (showCycles && liveState.cycleCount != null && liveState.cycleCount >= 0) {
                add(
                    DashboardIndicatorTileSpec(
                        key = "cycles",
                        title = "Cycles",
                        value = "${liveState.cycleCount}",
                        faceColor = AuraTactileColors.TilePlumFace,
                        lipColor = AuraTactileColors.TilePlumLip,
                        textColor = AuraTactileColors.CreamText,
                        iconType = TactileMetricIconType.CLOCK_HISTORY
                    )
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(clayPalette.screenBackground)
            .verticalScroll(scrollState)
            .padding(bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- TOP HERO SECTION ---
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            SculptedForestWaveCanvas(
                isDark = isDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StarTopNavigationBar(
                    currentRoute = currentRoute,
                    onNavigate = onNavigate
                )

                Spacer(modifier = Modifier.height(6.dp))

                SandHeroMedallion(
                    level = liveState.level,
                    isCharging = liveState.isCharging,
                    temperature = liveState.temperature,
                    voltage = liveState.voltage,
                    showTemp = showTemp,
                    showVolt = showVolt,
                    isDark = isDark
                )
            }
        }

        // Repeating Visual Alert Banner (Locked while threshold is reached; supports inline Snooze & auto-dismisses on charger action)
        if (isHighAlertTriggered || isLowAlertTriggered) {
            Spacer(modifier = Modifier.height(10.dp))
            RepeatingClayVisualAlertBanner(
                title = if (isHighAlertTriggered) {
                    BatteryTelemetryService.HIGH_ALERT_TITLE
                } else {
                    BatteryTelemetryService.LOW_ALERT_TITLE
                },
                body = if (isHighAlertTriggered) {
                    BatteryTelemetryService.HIGH_ALERT_BODY
                } else {
                    BatteryTelemetryService.LOW_ALERT_BODY
                },
                isHighAlert = isHighAlertTriggered,
                snoozeDurationMinutes = snoozeDurationMinutes,
                onSnoozeClick = {
                    localSnoozedUntil = System.currentTimeMillis() + snoozeDurationMinutes * 60_000L
                    onSnoozeAlert()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .testTag("active_threshold_visual_alert")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- MIDDLE SECTION: Centered "25h Activity" + 3D Clay Ribbon + Diversified Clay Hills ---
        Activity25hSection(
            logs = logs,
            currentLevel = liveState.level,
            isCharging = liveState.isCharging,
            isDark = isDark
        )

        // --- BOTTOM SECTION: Adaptive Reflowing Grid of 3D Clay Metric Pebbles ---
        if (visibleIndicatorTiles.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .testTag("dashboard_indicators_grid"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                visibleIndicatorTiles.chunked(2).forEach { rowTiles ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowTiles.forEach { tile ->
                            TactileMetricTile(
                                title = tile.title,
                                value = tile.value,
                                faceColor = tile.faceColor,
                                lipColor = tile.lipColor,
                                textColor = tile.textColor,
                                iconType = tile.iconType,
                                tiltDegrees = 0f,
                                onClick = null,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dashboard_tile_${tile.key}")
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Repeating Visual Alert Banner rendered in 3D clay style when High or Low threshold is reached.
 * Includes an inline 3D Clay "Snooze" button and automatically disappears as soon as charger is unplugged/connected.
 */
@Composable
private fun RepeatingClayVisualAlertBanner(
    title: String,
    body: String,
    isHighAlert: Boolean,
    snoozeDurationMinutes: Int,
    onSnoozeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "VisualAlertPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AlertPulseScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.80f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AlertGlowAlpha"
    )

    val faceColor = if (isHighAlert) AuraTactileColors.TileTerracottaFace else AuraTactileColors.TileRustFace
    val lipColor = if (isHighAlert) AuraTactileColors.TileTerracottaLip else AuraTactileColors.TileRustLip

    Tactile3DPebbleCard(
        faceColor = faceColor,
        lipColor = lipColor,
        lipDepth = 6.dp,
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        onClick = null,
        modifier = modifier.graphicsLayer {
            scaleX = pulseScale
            scaleY = pulseScale
            alpha = glowAlpha
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                TactileMetricCanvasIcon(
                    type = TactileMetricIconType.BELL_ALARM,
                    tint = AuraTactileColors.CreamText,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = AuraTactileColors.CreamText
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = AuraTactileColors.CreamText
                ),
                textAlign = TextAlign.Center
            )

            // Inline 3D Clay Snooze Action Button
            Tactile3DPebbleCard(
                faceColor = AuraTactileColors.TileMustardFace,
                lipColor = AuraTactileColors.TileMustardLip,
                lipDepth = 4.dp,
                cornerRadius = 14.dp,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                onClick = onSnoozeClick,
                modifier = Modifier
                    .defaultMinSize(minHeight = 40.dp)
                    .testTag("visual_alert_snooze_button")
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Snooze (${snoozeDurationMinutes}m)",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = PlusJakartaSansFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = AuraTactileColors.CreamText
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Queries ONLY real hardware battery capacity/health metrics from the Android OS.
 * Returns null if the host OEM or Android version restricts access to genuine capacity files,
 * ensuring zero fabricated or hardcoded SOH percentages.
 */
fun getBatteryDiagnostics(context: Context): Pair<Double?, String> {
    var healthPercentage: Double? = null
    val sysPathLogs = StringBuilder()

    try {
        val designFile = File("/sys/class/power_supply/battery/charge_full_design")
        val currentFile = File("/sys/class/power_supply/battery/charge_full")
        if (designFile.exists() && currentFile.exists() && designFile.canRead() && currentFile.canRead()) {
            val designVal = designFile.readText().trim().toDoubleOrNull()
            val currentVal = currentFile.readText().trim().toDoubleOrNull()
            if (designVal != null && currentVal != null && designVal > 0.0 && currentVal > 0.0) {
                val computed = (currentVal / designVal) * 100.0
                if (computed in 1.0..150.0) {
                    healthPercentage = computed.coerceAtMost(100.0)
                    sysPathLogs.append("SOH computed from hardware sysfs: ${String.format("%.1f", healthPercentage)}%\n")
                }
            }
        }
    } catch (e: Exception) {
        // OS/SELinux restricted access: return null so unsupported SOH tile collapses cleanly
    }

    val diagnosticLog = if (sysPathLogs.isEmpty()) "Restricted by OS" else sysPathLogs.toString()
    return Pair(healthPercentage, diagnosticLog)
}

fun calculateActiveDuration(logs: List<BatteryLogEntry>, currentLevel: Int): String {
    val now = System.currentTimeMillis()
    var lastFullChargeTime: Long? = null

    val last100 = logs.lastOrNull { it.batteryLevel == 100 }
    if (last100 != null) {
        lastFullChargeTime = last100.timestamp
    } else if (logs.size >= 2) {
        val maxLevel = logs.maxOfOrNull { it.batteryLevel } ?: 0
        if (maxLevel >= 80) {
            for (i in logs.size - 1 downTo 1) {
                val current = logs[i]
                val previous = logs[i - 1]
                if (!current.isCharging && previous.isCharging && current.batteryLevel >= maxLevel - 2) {
                    lastFullChargeTime = current.timestamp
                    break
                }
            }
        }
    } else if (logs.isNotEmpty()) {
        lastFullChargeTime = logs.first().timestamp
    }

    val durationMs = if (lastFullChargeTime != null) {
        (now - lastFullChargeTime).coerceAtLeast(0L)
    } else {
        0L
    }

    val hours = durationMs / (1000 * 60 * 60)
    val minutes = (durationMs / (1000 * 60)) % 60
    return "${hours}h ${minutes}m"
}
