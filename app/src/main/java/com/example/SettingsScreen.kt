package com.example

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import kotlin.math.roundToInt
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.*
import com.example.ui.theme.AuraTactileColors
import com.example.ui.theme.ExpressivePalettes
import com.example.ui.theme.LocalAuraClayPalette
import com.example.ui.theme.PlusJakartaSansFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class QuietTimeTarget {
    FROM,
    TO
}

@Composable
fun SettingsScreen(
    telemetryDataStore: TelemetryDataStore,
    currentRoute: String = "settings",
    onNavigate: (String) -> Unit = {},
    isDark: Boolean = isSystemInDarkTheme(),
    initialHighThresh: Int = 80,
    initialLowThresh: Int = 20,
    initialShowTemp: Boolean = true,
    initialShowVolt: Boolean = true,
    initialShowHealth: Boolean = true,
    initialShowChargeMethod: Boolean = true,
    initialShowCycles: Boolean = true,
    initialQuietHoursEnabled: Boolean = false,
    initialQuietStartHour: Int = 10,
    initialQuietStartAmPm: String = "PM",
    initialQuietEndHour: Int = 7,
    initialQuietEndAmPm: String = "AM",
    initialThemeMode: String = "System",
    initialThemePalette: String = "AuraEarthy",
    initialBackgroundServiceEnabled: Boolean = true,
    initialRingtoneUri: String = "",
    initialRingtoneName: String = "Default Tone",
    initialAlertDurationSeconds: Int = 60,
    initialSnoozeDurationMinutes: Int = 5
) {
    val context = LocalContext.current
    val clayPalette = LocalAuraClayPalette.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val highThresh by telemetryDataStore.highThresholdFlow.collectAsState(initial = initialHighThresh)
    val lowThresh by telemetryDataStore.lowThresholdFlow.collectAsState(initial = initialLowThresh)
    val themeMode by telemetryDataStore.themeModeFlow.collectAsState(initial = initialThemeMode)
    val themePalette by telemetryDataStore.themePaletteFlow.collectAsState(initial = initialThemePalette)

    val showTemp by telemetryDataStore.showTempFlow.collectAsState(initial = initialShowTemp)
    val showVolt by telemetryDataStore.showVoltFlow.collectAsState(initial = initialShowVolt)
    val showHealth by telemetryDataStore.showHealthFlow.collectAsState(initial = initialShowHealth)
    val showChargeMethod by telemetryDataStore.showChargeMethodFlow.collectAsState(initial = initialShowChargeMethod)
    val showCycles by telemetryDataStore.showCyclesFlow.collectAsState(initial = initialShowCycles)

    val backgroundServiceEnabled by telemetryDataStore.backgroundServiceEnabledFlow.collectAsState(initial = initialBackgroundServiceEnabled)

    val quietHoursEnabled by telemetryDataStore.quietHoursEnabledFlow.collectAsState(initial = initialQuietHoursEnabled)
    val quietHoursStartHour by telemetryDataStore.quietHoursStartHourFlow.collectAsState(initial = initialQuietStartHour)
    val quietHoursStartAmPm by telemetryDataStore.quietHoursStartAmPmFlow.collectAsState(initial = initialQuietStartAmPm)
    val quietHoursEndHour by telemetryDataStore.quietHoursEndHourFlow.collectAsState(initial = initialQuietEndHour)
    val quietHoursEndAmPm by telemetryDataStore.quietHoursEndAmPmFlow.collectAsState(initial = initialQuietEndAmPm)

    val ringtoneUri by telemetryDataStore.ringtoneUriFlow.collectAsState(initial = initialRingtoneUri)
    val ringtoneName by telemetryDataStore.ringtoneNameFlow.collectAsState(initial = initialRingtoneName)
    val alertDurationSeconds by telemetryDataStore.alertDurationSecondsFlow.collectAsState(initial = initialAlertDurationSeconds)
    val snoozeDurationMinutes by telemetryDataStore.snoozeDurationMinutesFlow.collectAsState(initial = initialSnoozeDurationMinutes)

    var localHighThresh by rememberSaveable(highThresh) { mutableIntStateOf(highThresh) }
    var localLowThresh by rememberSaveable(lowThresh) { mutableIntStateOf(lowThresh) }

    var localStartHour by remember(quietHoursStartHour) { mutableIntStateOf(quietHoursStartHour.coerceIn(1, 12)) }
    var localStartAmPm by remember(quietHoursStartAmPm) {
        mutableStateOf(if (quietHoursStartAmPm.equals("AM", ignoreCase = true)) "AM" else "PM")
    }
    var localEndHour by remember(quietHoursEndHour) { mutableIntStateOf(quietHoursEndHour.coerceIn(1, 12)) }
    var localEndAmPm by remember(quietHoursEndAmPm) {
        mutableStateOf(if (quietHoursEndAmPm.equals("PM", ignoreCase = true)) "PM" else "AM")
    }

    // Quiet Hours Time Options animation state:
    // Symmetrical 350ms Fade + Expand/Collapse transition for both opening and closing
    var activeTimeTarget by remember { mutableStateOf<QuietTimeTarget?>(null) }
    var lastTimeTarget by remember { mutableStateOf(QuietTimeTarget.FROM) }
    if (activeTimeTarget != null) {
        lastTimeTarget = activeTimeTarget!!
    }

    // Ringtone Picker Launcher (supports Xiaomi HyperOS/MIUI Theme Ringtone Store & System Picker)
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val pickedUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            } ?: data?.data
            if (pickedUri != null) {
                val toneTitle = try {
                    RingtoneManager.getRingtone(context, pickedUri)?.getTitle(context) ?: "Custom Tone"
                } catch (e: Exception) {
                    "Custom Tone"
                }
                coroutineScope.launch {
                    telemetryDataStore.saveRingtone(pickedUri.toString(), toneTitle)
                }
            } else {
                coroutineScope.launch {
                    telemetryDataStore.saveRingtone("", "Default Tone")
                }
            }
        }
    }

    val launchRingtonePicker: () -> Unit = {
        val existingUri = if (ringtoneUri.isNotBlank()) {
            Uri.parse(ringtoneUri)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
        val ringtoneTypeFlags = RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_RINGTONE or RingtoneManager.TYPE_NOTIFICATION

        // Prioritize Xiaomi HyperOS / MIUI Themes Ringtone Store (`com.android.thememanager`)
        val hyperOsRingtoneStoreIntents = listOf(
            Intent("miui.intent.action.RINGTONE_PICKER").apply {
                setPackage("com.android.thememanager")
                putExtra("miui.intent.extra.ringtone.TYPE_FLAGS", ringtoneTypeFlags)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, ringtoneTypeFlags)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Tone")
            },
            Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                setPackage("com.android.thememanager")
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, ringtoneTypeFlags)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Tone")
            },
            Intent(Intent.ACTION_VIEW, Uri.parse("theme://zhuti.xiaomi.com/ringtone?miback=true&miref=${context.packageName}")).apply {
                setPackage("com.android.thememanager")
            },
            Intent("miui.intent.action.RINGTONE_PICKER").apply {
                putExtra("miui.intent.extra.ringtone.TYPE_FLAGS", ringtoneTypeFlags)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, ringtoneTypeFlags)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Tone")
            }
        )

        var launchedHyperOsStore = false
        for (storeIntent in hyperOsRingtoneStoreIntents) {
            try {
                ringtonePickerLauncher.launch(storeIntent)
                launchedHyperOsStore = true
                break
            } catch (ignored: Exception) {
                // Continue to next Xiaomi HyperOS / MIUI intent candidate
            }
        }

        if (!launchedHyperOsStore) {
            try {
                val fallbackPickerIntent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, ringtoneTypeFlags)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Tone")
                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
                }
                ringtonePickerLauncher.launch(fallbackPickerIntent)
            } catch (e: Exception) {
                Log.e("SettingsScreen", "Failed to launch ringtone picker", e)
            }
        }
    }

    val cardFace = clayPalette.cardFace
    val cardLip = clayPalette.cardLip
    val inkColor = clayPalette.inkColor

    val setBackgroundTelemetry: (Boolean) -> Unit = { isEnabled ->
        coroutineScope.launch {
            telemetryDataStore.saveBackgroundServiceEnabled(isEnabled)
            try {
                val serviceIntent = Intent(context, BatteryTelemetryService::class.java)
                if (isEnabled) {
                    context.startService(serviceIntent)
                } else {
                    context.stopService(serviceIntent)
                }
            } catch (t: Throwable) {
                Log.e("SettingsScreen", "Failed to toggle background telemetry service", t)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(clayPalette.screenBackground)
            .verticalScroll(scrollState)
            .padding(bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Clean Sculpted Clay Wave Header
        CompactWaveScreenHeader(
            title = "Settings",
            currentRoute = currentRoute,
            onNavigate = onNavigate,
            isDark = isDark
        )

        // 2. Background Telemetry Card (Only the 2 Clay Options Below the Heading — No Top Switch)
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            Tactile3DPebbleCard(
                faceColor = cardFace,
                lipColor = cardLip,
                lipDepth = 6.dp,
                cornerRadius = 22.dp,
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    UnifiedClaySectionTitle(
                        text = "Background Telemetry",
                        color = inkColor
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Tactile3DPebbleCard(
                            faceColor = if (backgroundServiceEnabled) clayPalette.primaryAccentFace else clayPalette.recessedTrackBg,
                            lipColor = if (backgroundServiceEnabled) clayPalette.primaryAccentLip else clayPalette.recessedTrackBorder,
                            lipDepth = if (backgroundServiceEnabled) 5.dp else 3.5.dp,
                            cornerRadius = 16.dp,
                            contentPadding = PaddingValues(vertical = 11.dp, horizontal = 10.dp),
                            onClick = { setBackgroundTelemetry(true) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("telemetry_run_button")
                        ) {
                            Text(
                                text = "RUN TELEMETRY",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.align(Alignment.Center),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = if (backgroundServiceEnabled) {
                                        AuraTactileColors.textOn(clayPalette.primaryAccentFace)
                                    } else {
                                        inkColor
                                    }
                                )
                            )
                        }

                        Tactile3DPebbleCard(
                            faceColor = if (!backgroundServiceEnabled) AuraTactileColors.TileRustFace else clayPalette.recessedTrackBg,
                            lipColor = if (!backgroundServiceEnabled) AuraTactileColors.TileRustLip else clayPalette.recessedTrackBorder,
                            lipDepth = if (!backgroundServiceEnabled) 5.dp else 3.5.dp,
                            cornerRadius = 16.dp,
                            contentPadding = PaddingValues(vertical = 11.dp, horizontal = 10.dp),
                            onClick = { setBackgroundTelemetry(false) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("telemetry_stop_button")
                        ) {
                            Text(
                                text = "STOP TELEMETRY",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.align(Alignment.Center),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = if (!backgroundServiceEnabled) AuraTactileColors.CreamText else inkColor
                                )
                            )
                        }
                    }
                }
            }
        }

        // 3. Critical Thresholds, Alert Active Duration, Snooze Duration & Alarm Tone Card
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            Tactile3DPebbleCard(
                faceColor = cardFace,
                lipColor = cardLip,
                lipDepth = 6.dp,
                cornerRadius = 22.dp,
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    UnifiedClaySectionTitle(
                        text = "Critical Thresholds",
                        color = inkColor
                    )

                    Tactile3DThresholdBar(
                        label = "High Critical Point",
                        value = localHighThresh,
                        valueRange = 50..100,
                        barColor = clayPalette.primaryAccentFace,
                        lipColor = clayPalette.primaryAccentLip,
                        badgeTextColor = AuraTactileColors.CreamText,
                        isDark = isDark,
                        onValueChange = { localHighThresh = it },
                        onValueChangeFinished = {
                            coroutineScope.launch {
                                telemetryDataStore.saveHighThreshold(localHighThresh)
                            }
                        }
                    )

                    Tactile3DThresholdBar(
                        label = "Low Critical Point",
                        value = localLowThresh,
                        valueRange = 5..49,
                        barColor = clayPalette.secondaryAccentFace,
                        lipColor = clayPalette.secondaryAccentLip,
                        badgeTextColor = AuraTactileColors.CreamText,
                        isDark = isDark,
                        onValueChange = { localLowThresh = it },
                        onValueChangeFinished = {
                            coroutineScope.launch {
                                telemetryDataStore.saveLowThreshold(localLowThresh)
                            }
                        }
                    )

                    // Proportionate 3D Clay Selector for Alert Active Duration & Auto-Cycle (30s, 60s, 120s)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Alert Active Duration (Auto-Cycle)",
                            maxLines = 1,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = PlusJakartaSansFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = inkColor
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(30, 60, 120).forEach { seconds ->
                                val isSelected = alertDurationSeconds == seconds
                                FastClayOptionChip(
                                    text = "${seconds}s",
                                    faceColor = if (isSelected) clayPalette.primaryAccentFace else clayPalette.recessedTrackBg,
                                    lipColor = if (isSelected) clayPalette.primaryAccentLip else clayPalette.recessedTrackBorder,
                                    textColor = if (isSelected) {
                                        AuraTactileColors.textOn(clayPalette.primaryAccentFace)
                                    } else {
                                        inkColor
                                    },
                                    lipDepth = if (isSelected) 4.dp else 2.5.dp,
                                    cornerRadius = 13.dp,
                                    onClick = {
                                        coroutineScope.launch {
                                            telemetryDataStore.saveAlertDurationSeconds(seconds)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("alert_duration_${seconds}s")
                                )
                            }
                        }
                    }

                    // Proportionate 3D Clay Selector for Snooze Duration (5m, 10m, 15m)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Snooze Duration",
                            maxLines = 1,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = PlusJakartaSansFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = inkColor
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(5, 10, 15).forEach { minutes ->
                                val isSelected = snoozeDurationMinutes == minutes
                                FastClayOptionChip(
                                    text = "${minutes}m",
                                    faceColor = if (isSelected) clayPalette.tertiaryAccentFace else clayPalette.recessedTrackBg,
                                    lipColor = if (isSelected) clayPalette.tertiaryAccentLip else clayPalette.recessedTrackBorder,
                                    textColor = if (isSelected) AuraTactileColors.CreamText else inkColor,
                                    lipDepth = if (isSelected) 4.dp else 2.5.dp,
                                    cornerRadius = 13.dp,
                                    onClick = {
                                        coroutineScope.launch {
                                            telemetryDataStore.saveSnoozeDurationMinutes(minutes)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("snooze_duration_${minutes}m")
                                )
                            }
                        }
                    }

                    // Unified Single-Card 3D Clay Alarm Tone Selector (No redundant "CHANGE" badge)
                    Tactile3DPebbleCard(
                        faceColor = AuraTactileColors.TileTerracottaFace,
                        lipColor = AuraTactileColors.TileTerracottaLip,
                        lipDepth = 5.dp,
                        cornerRadius = 18.dp,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        onClick = launchRingtonePicker,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ringtone_current_pod")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsActive,
                                contentDescription = "Alarm Tone",
                                tint = AuraTactileColors.CreamText,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "ALARM TONE",
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp,
                                        letterSpacing = 0.6.sp,
                                        color = AuraTactileColors.CreamText
                                    )
                                )
                                Text(
                                    text = ringtoneName,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = AuraTactileColors.CreamText
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Quiet Hours Card (Progressive Cascading Slide-Down on Open, Single Unified Motion on Close)
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            Tactile3DPebbleCard(
                faceColor = cardFace,
                lipColor = cardLip,
                lipDepth = 6.dp,
                cornerRadius = 22.dp,
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Header Row: "Quiet Hours" + Single Clay3DSwitch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UnifiedClaySectionTitle(
                            text = "Quiet Hours",
                            color = inkColor,
                            modifier = Modifier.weight(1f)
                        )

                        Clay3DSwitch(
                            checked = quietHoursEnabled,
                            onCheckedChange = { isChecked ->
                                coroutineScope.launch {
                                    telemetryDataStore.saveQuietHoursEnabled(isChecked)
                                }
                            },
                            modifier = Modifier.testTag("quiet_hours_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Symmetrical 2-Column 3D Clay "FROM" & "TO" Time Selector Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ClayQuietTimePod(
                            label = "FROM",
                            hour = localStartHour,
                            amPm = localStartAmPm,
                            isExpanded = activeTimeTarget == QuietTimeTarget.FROM,
                            accentFace = clayPalette.primaryAccentFace,
                            accentLip = clayPalette.primaryAccentLip,
                            accentText = AuraTactileColors.textOn(clayPalette.primaryAccentFace),
                            onPodClick = {
                                activeTimeTarget = if (activeTimeTarget == QuietTimeTarget.FROM) null else QuietTimeTarget.FROM
                            },
                            onAmPmSelect = { selectedPeriod ->
                                localStartAmPm = selectedPeriod
                                activeTimeTarget = QuietTimeTarget.FROM
                                coroutineScope.launch {
                                    telemetryDataStore.saveQuietHoursStartAmPm(selectedPeriod)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quiet_from_pod")
                        )

                        ClayQuietTimePod(
                            label = "TO",
                            hour = localEndHour,
                            amPm = localEndAmPm,
                            isExpanded = activeTimeTarget == QuietTimeTarget.TO,
                            accentFace = clayPalette.tertiaryAccentFace,
                            accentLip = clayPalette.tertiaryAccentLip,
                            accentText = AuraTactileColors.textOn(clayPalette.tertiaryAccentFace),
                            onPodClick = {
                                activeTimeTarget = if (activeTimeTarget == QuietTimeTarget.TO) null else QuietTimeTarget.TO
                            },
                            onAmPmSelect = { selectedPeriod ->
                                localEndAmPm = selectedPeriod
                                activeTimeTarget = QuietTimeTarget.TO
                                coroutineScope.launch {
                                    telemetryDataStore.saveQuietHoursEndAmPm(selectedPeriod)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quiet_to_pod")
                        )
                    }

                    // Smooth, zero-jitter bottom-to-top Curtain Close & top-to-bottom Open for Time Options Panel.
                    // Including the 12.dp top spacing inside the measured height ensures the panel wipes
                    // continuously from the bottom edge through the "FROM/TO TIME OPTIONS" header and
                    // all the way to 0px without stalling on the title row or snapping at completion.
                    val isTrayOpen = activeTimeTarget != null
                    val openEasing = remember { CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f) }
                    val closeBottomToTopEasing = remember { CubicBezierEasing(0.35f, 0.0f, 0.90f, 1.0f) }
                    val trayExpandFraction by animateFloatAsState(
                        targetValue = if (isTrayOpen) 1f else 0f,
                        animationSpec = tween(
                            durationMillis = if (isTrayOpen) 260 else 240,
                            easing = if (isTrayOpen) openEasing else closeBottomToTopEasing
                        ),
                        label = "QuietTimeOptionsTrayFraction"
                    )

                    if (trayExpandFraction > 0f) {
                        val target = activeTimeTarget ?: lastTimeTarget
                        val isFrom = target == QuietTimeTarget.FROM
                        val selectedHour = if (isFrom) localStartHour else localEndHour
                        val selectedAmPm = if (isFrom) localStartAmPm else localEndAmPm
                        val activeAccentFace = if (isFrom) clayPalette.primaryAccentFace else clayPalette.tertiaryAccentFace
                        val activeAccentLip = if (isFrom) clayPalette.primaryAccentLip else clayPalette.tertiaryAccentLip
                        val activeAccentText = AuraTactileColors.textOn(activeAccentFace)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clipToBounds()
                                .layout { measurable, constraints ->
                                    val placeable = measurable.measure(constraints)
                                    val currentHeight = (placeable.height * trayExpandFraction)
                                        .roundToInt()
                                        .coerceIn(0, placeable.height)
                                    layout(placeable.width, currentHeight) {
                                        placeable.placeRelative(0, 0)
                                    }
                                }
                                .padding(top = 12.dp)
                        ) {
                            ClayTimeSelectionOptionsTray(
                                title = if (isFrom) "FROM TIME OPTIONS" else "TO TIME OPTIONS",
                                selectedHour = selectedHour,
                                selectedAmPm = selectedAmPm,
                                accentFace = activeAccentFace,
                                accentLip = activeAccentLip,
                                accentText = activeAccentText,
                                onSelectHour = { pickedHour ->
                                    if (isFrom) {
                                        localStartHour = pickedHour
                                        coroutineScope.launch {
                                            telemetryDataStore.saveQuietHoursStartHour(pickedHour)
                                        }
                                    } else {
                                        localEndHour = pickedHour
                                        coroutineScope.launch {
                                            telemetryDataStore.saveQuietHoursEndHour(pickedHour)
                                        }
                                    }
                                },
                                onSelectAmPm = { pickedAmPm ->
                                    if (isFrom) {
                                        localStartAmPm = pickedAmPm
                                        coroutineScope.launch {
                                            telemetryDataStore.saveQuietHoursStartAmPm(pickedAmPm)
                                        }
                                    } else {
                                        localEndAmPm = pickedAmPm
                                        coroutineScope.launch {
                                            telemetryDataStore.saveQuietHoursEndAmPm(pickedAmPm)
                                        }
                                    }
                                },
                                onClose = { activeTimeTarget = null }
                            )
                        }
                    }
                }
            }
        }

        // 5. Display Indicators Card (100% Color-Matched to Main Dashboard Tiles!)
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            Tactile3DPebbleCard(
                faceColor = cardFace,
                lipColor = cardLip,
                lipDepth = 6.dp,
                cornerRadius = 22.dp,
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    UnifiedClaySectionTitle(
                        text = "Display Indicators",
                        color = inkColor
                    )

                    // Row 1: Voltage (Slate-Indigo Clay) + Health (Calibrated Sage-Emerald Green Clay)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ClayIndicatorToggleTile(
                            title = "Voltage",
                            active = showVolt,
                            activeFaceColor = AuraTactileColors.TileSlateFace,
                            activeLipColor = AuraTactileColors.TileSlateLip,
                            activeTextColor = AuraTactileColors.CreamText,
                            onClick = {
                                coroutineScope.launch { telemetryDataStore.saveShowVolt(!showVolt) }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("toggle_indicator_voltage")
                        )

                        ClayIndicatorToggleTile(
                            title = "Health",
                            active = showHealth,
                            activeFaceColor = AuraTactileColors.TileHealthGreenFace,
                            activeLipColor = AuraTactileColors.TileHealthGreenLip,
                            activeTextColor = AuraTactileColors.CreamText,
                            onClick = {
                                coroutineScope.launch { telemetryDataStore.saveShowHealth(!showHealth) }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("toggle_indicator_health")
                        )
                    }

                    // Row 2: Temp. (Berry Rust Clay) + Source (Mustard Ochre Clay)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ClayIndicatorToggleTile(
                            title = "Temp.",
                            active = showTemp,
                            activeFaceColor = AuraTactileColors.TileRustFace,
                            activeLipColor = AuraTactileColors.TileRustLip,
                            activeTextColor = AuraTactileColors.CreamText,
                            onClick = {
                                coroutineScope.launch { telemetryDataStore.saveShowTemp(!showTemp) }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("toggle_indicator_temp")
                        )

                        ClayIndicatorToggleTile(
                            title = "Source",
                            active = showChargeMethod,
                            activeFaceColor = AuraTactileColors.TileMustardFace,
                            activeLipColor = AuraTactileColors.TileMustardLip,
                            activeTextColor = AuraTactileColors.CreamText,
                            onClick = {
                                coroutineScope.launch { telemetryDataStore.saveShowChargeMethod(!showChargeMethod) }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("toggle_indicator_source")
                        )
                    }

                    // Row 3: Cycles (Warm Plum Clay)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        ClayIndicatorToggleTile(
                            title = "Cycles",
                            active = showCycles,
                            activeFaceColor = AuraTactileColors.TilePlumFace,
                            activeLipColor = AuraTactileColors.TilePlumLip,
                            activeTextColor = AuraTactileColors.CreamText,
                            onClick = {
                                coroutineScope.launch { telemetryDataStore.saveShowCycles(!showCycles) }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("toggle_indicator_cycles")
                        )
                    }
                }
            }
        }

        // 6. Color Palette Studio Card (High-Craft 3D Clay Orb Medallions in a 3x2 Showcase)
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            Tactile3DPebbleCard(
                faceColor = cardFace,
                lipColor = cardLip,
                lipDepth = 6.dp,
                cornerRadius = 22.dp,
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    UnifiedClaySectionTitle(
                        text = "Color Palette",
                        color = inkColor
                    )

                    // Recessed 3D Clay Cradle for System / Light / Dark Mode Pills
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(clayPalette.recessedTrackBg)
                            .border(1.5.dp, clayPalette.recessedTrackBorder, RoundedCornerShape(18.dp))
                            .padding(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("System", "Light", "Dark").forEach { option ->
                                val isSelected = themeMode == option
                                val btnFace = if (isSelected) clayPalette.tertiaryAccentFace else clayPalette.cardFace
                                val btnLip = if (isSelected) clayPalette.tertiaryAccentLip else clayPalette.cardLip
                                val txtColor = if (isSelected) AuraTactileColors.CreamText else inkColor

                                Tactile3DPebbleCard(
                                    faceColor = btnFace,
                                    lipColor = btnLip,
                                    lipDepth = if (isSelected) 4.5.dp else 3.dp,
                                    cornerRadius = 14.dp,
                                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 6.dp),
                                    onClick = {
                                        coroutineScope.launch {
                                            telemetryDataStore.saveThemeMode(option)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.align(Alignment.Center),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        val icon = when (option) {
                                            "Light" -> Icons.Outlined.WbSunny
                                            "Dark" -> Icons.Outlined.DarkMode
                                            else -> Icons.Outlined.BrightnessAuto
                                        }
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = txtColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = option,
                                            maxLines = 1,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp,
                                                color = txtColor
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3x2 Grid of Sculpted 3D Clay Color Orb Medallions
                    ExpressivePalettes.allPalettes.chunked(3).forEach { rowPalettes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowPalettes.forEach { pal ->
                                val isSelected = themePalette.equals(pal.id, ignoreCase = true)
                                val shortLabel = pal.nameEn.substringBefore(" &").trim()

                                Clay3DColorMedallion(
                                    name = shortLabel,
                                    primaryOrbColor = pal.accentColor,
                                    secondaryRingColor = pal.secondarySwatchColor,
                                    selected = isSelected,
                                    onClick = {
                                        coroutineScope.launch {
                                            telemetryDataStore.saveThemePalette(pal.id)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 7. Permissions Card: 2 Rows (4 Items) of Pure Label-Free 3D Clay Sculptures
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            Tactile3DPebbleCard(
                faceColor = cardFace,
                lipColor = cardLip,
                lipDepth = 6.dp,
                cornerRadius = 22.dp,
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    UnifiedClaySectionTitle(
                        text = "Permissions",
                        color = inkColor
                    )

                    // Balanced 3-Column 3D Clay System Permissions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Clay3DSculptedPermissionTile(
                            sculptureType = ClayPermissionSculptureType.ALERT_BELL_CHIME,
                            pedestalFace = AuraTactileColors.TileMustardFace,
                            pedestalLip = AuraTactileColors.TileMustardLip,
                            sculptureMainFace = AuraTactileColors.TileTerracottaFace,
                            sculptureMainLip = AuraTactileColors.TileTerracottaLip,
                            sculptureAccentFace = AuraTactileColors.CreamText,
                            contentDescription = "Alert Channel",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                try {
                                    val intent = Intent().apply {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            action = Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS
                                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                            putExtra(Settings.EXTRA_CHANNEL_ID, "battery_alerts_channel")
                                        } else {
                                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    try {
                                        val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                        context.startActivity(fallback)
                                    } catch (ex: Exception) {
                                        Log.e("SettingsScreen", "Failed to open notification settings", ex)
                                    }
                                }
                            }
                        )

                        Clay3DSculptedPermissionTile(
                            sculptureType = ClayPermissionSculptureType.BATTERY_POWER_CORE,
                            pedestalFace = AuraTactileColors.TileTerracottaFace,
                            pedestalLip = AuraTactileColors.TileTerracottaLip,
                            sculptureMainFace = AuraTactileColors.TileSlateFace,
                            sculptureMainLip = AuraTactileColors.TileSlateLip,
                            sculptureAccentFace = AuraTactileColors.TileMustardFace,
                            contentDescription = "Unrestricted Battery",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                try {
                                    val intent = Intent().apply {
                                        action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                                    } catch (ex: Exception) {
                                        try {
                                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.fromParts("package", context.packageName, null)
                                            }
                                            context.startActivity(intent)
                                        } catch (inner: Exception) {
                                            Log.e("SettingsScreen", "Failed to open battery settings", inner)
                                        }
                                    }
                                }
                            }
                        )

                        Clay3DSculptedPermissionTile(
                            sculptureType = ClayPermissionSculptureType.SHIELD_AUTOSTART_GUARD,
                            pedestalFace = AuraTactileColors.TilePlumFace,
                            pedestalLip = AuraTactileColors.TilePlumLip,
                            sculptureMainFace = AuraTactileColors.TileMustardFace,
                            sculptureMainLip = AuraTactileColors.TileMustardLip,
                            sculptureAccentFace = AuraTactileColors.TileRustFace,
                            contentDescription = "Autostart",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                var launched = false
                                val miuiIntents = listOf(
                                    Intent().setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
                                    Intent().setClassName("com.miui.securitycenter", "com.miui.powercenter.PowerSettings"),
                                    Intent().setClassName("com.miui.securitycenter", "com.miui.securityscan.MainActivity")
                                )
                                for (miuiIntent in miuiIntents) {
                                    try {
                                        miuiIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(miuiIntent)
                                        launched = true
                                        break
                                    } catch (e: Exception) {
                                        // continue
                                    }
                                }
                                if (!launched) {
                                    try {
                                        val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                        context.startActivity(fallbackIntent)
                                    } catch (e: Exception) {
                                        Log.e("SettingsScreen", "Failed to open autostart fallback settings", e)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Ultra-lightweight Single-Node 3D Clay Button used inside the Time Selection Options Tray
 * and Duration selectors so opening and closing has zero layout/allocation stutter.
 */
@Composable
private fun FastClayOptionChip(
    text: String,
    faceColor: Color,
    lipColor: Color,
    textColor: Color,
    lipDepth: Dp = 3.5.dp,
    cornerRadius: Dp = 13.dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .padding(bottom = lipDepth + 1.dp)
            .drawBehind {
                val r = cornerRadius.toPx()
                val cr = CornerRadius(r, r)
                val lipPx = lipDepth.toPx()
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.14f),
                    topLeft = Offset(0f, lipPx + 1.dp.toPx()),
                    size = Size(size.width, size.height),
                    cornerRadius = cr
                )
                drawRoundRect(
                    color = lipColor,
                    topLeft = Offset(0f, lipPx),
                    size = Size(size.width, size.height),
                    cornerRadius = cr
                )
                drawRoundRect(
                    color = faceColor,
                    topLeft = Offset.Zero,
                    size = Size(size.width, size.height),
                    cornerRadius = cr
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.22f),
                    topLeft = Offset.Zero,
                    size = Size(size.width, size.height),
                    cornerRadius = cr,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            maxLines = 1,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = textColor
            )
        )
    }
}

/**
 * Interactive 3D Clay Pod for Quiet Hours ("FROM" and "TO"):
 * - Tapping the pod or the time badge opens the full 3D Clay Time Selection Options Tray below.
 * - Top line: Thick, bold, well-spaced "FROM" / "TO" heading centered on its own line (15.sp ExtraBold).
 * - Middle: Tactile 3D Clay Time Badge ("10:00" / "7:00") that opens/closes the hour options when tapped.
 * - Bottom: Dedicated 2-column 3D clay "AM" | "PM" selector with generous separation.
 */
@Composable
private fun ClayQuietTimePod(
    label: String,
    hour: Int,
    amPm: String,
    isExpanded: Boolean,
    accentFace: Color,
    accentLip: Color,
    accentText: Color,
    onPodClick: () -> Unit,
    onAmPmSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val clayPalette = LocalAuraClayPalette.current

    Tactile3DPebbleCard(
        faceColor = if (isExpanded) accentFace.copy(alpha = 0.16f) else clayPalette.recessedTrackBg,
        lipColor = if (isExpanded) accentFace else clayPalette.recessedTrackBorder,
        lipDepth = 4.dp,
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
        onClick = onPodClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Thick, Bold "FROM" / "TO" Title Centered on Its Own Line (Well-Separated from AM/PM)
            Text(
                text = label.uppercase(),
                maxLines = 1,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    letterSpacing = 0.9.sp,
                    color = clayPalette.inkColor
                )
            )

            // 2. Interactive 3D Clay Time Display Pill (Tapping Opens/Closes the Time Selection Options)
            FastClayOptionChip(
                text = "$hour:00",
                faceColor = if (isExpanded) accentFace else clayPalette.cardFace,
                lipColor = if (isExpanded) accentLip else clayPalette.cardLip,
                textColor = if (isExpanded) accentText else clayPalette.inkColor,
                lipDepth = 4.dp,
                cornerRadius = 14.dp,
                onClick = onPodClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quiet_${label.lowercase()}_time_button")
            )

            // 3. Dedicated 2-Column 3D Clay AM / PM Selector (Spacious & Unmistakable)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("AM", "PM").forEach { period ->
                    val isSelected = amPm.equals(period, ignoreCase = true)
                    FastClayOptionChip(
                        text = period,
                        faceColor = if (isSelected) accentFace else clayPalette.cardFace,
                        lipColor = if (isSelected) accentLip else clayPalette.cardLip,
                        textColor = if (isSelected) accentText else clayPalette.inkColor,
                        lipDepth = 3.dp,
                        cornerRadius = 12.dp,
                        onClick = { onAmPmSelect(period) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quiet_${label.lowercase()}_${period.lowercase()}")
                    )
                }
            }
        }
    }
}

/**
 * 3D Clay Time Selection Options Tray with fixed internal geometry so bottom-to-top
 * curtain closing and top-to-bottom opening never re-measure or shift any child rows.
 */
@Composable
private fun ClayTimeSelectionOptionsTray(
    title: String,
    selectedHour: Int,
    selectedAmPm: String,
    accentFace: Color,
    accentLip: Color,
    accentText: Color,
    onSelectHour: (Int) -> Unit,
    onSelectAmPm: (String) -> Unit,
    onClose: () -> Unit
) {
    val clayPalette = LocalAuraClayPalette.current
    val hours = remember { (1..12).toList().chunked(4) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(clayPalette.recessedTrackBg)
            .border(1.5.dp, clayPalette.recessedTrackBorder, RoundedCornerShape(20.dp))
            .padding(12.dp)
            .testTag("quiet_time_options_tray")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 0: Header Row (Title + Done Button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        letterSpacing = 0.6.sp,
                        color = clayPalette.inkColor
                    )
                )

                FastClayOptionChip(
                    text = "DONE",
                    faceColor = accentFace,
                    lipColor = accentLip,
                    textColor = accentText,
                    lipDepth = 3.dp,
                    cornerRadius = 11.dp,
                    onClick = onClose,
                    modifier = Modifier.testTag("quiet_time_options_done")
                )
            }

            // Rows 1..3: 4x3 Grid of Lightweight 3D Clay Hour Buttons (1:00 to 12:00)
            hours.forEach { rowHours ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowHours.forEach { hr ->
                        val isSelected = selectedHour == hr
                        FastClayOptionChip(
                            text = "$hr:00",
                            faceColor = if (isSelected) accentFace else clayPalette.cardFace,
                            lipColor = if (isSelected) accentLip else clayPalette.cardLip,
                            textColor = if (isSelected) accentText else clayPalette.inkColor,
                            lipDepth = 3.dp,
                            cornerRadius = 13.dp,
                            onClick = { onSelectHour(hr) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quiet_hour_option_$hr")
                        )
                    }
                }
            }

            // Row 4: Symmetrical 2-Column AM / PM Selector inside the Time Options Tray
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("AM", "PM").forEach { period ->
                    val isSelected = selectedAmPm.equals(period, ignoreCase = true)
                    FastClayOptionChip(
                        text = period,
                        faceColor = if (isSelected) accentFace else clayPalette.cardFace,
                        lipColor = if (isSelected) accentLip else clayPalette.cardLip,
                        textColor = if (isSelected) accentText else clayPalette.inkColor,
                        lipDepth = 3.dp,
                        cornerRadius = 13.dp,
                        onClick = { onSelectAmPm(period) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quiet_tray_period_${period.lowercase()}")
                    )
                }
            }
        }
    }
}

/**
 * Color-Harmonized 3D Clay Indicator Toggle Tile in Settings:
 * Uses the exact same 3D clay face, lip, and text colors as its counterpart on the Main Dashboard.
 */
@Composable
private fun ClayIndicatorToggleTile(
    title: String,
    active: Boolean,
    activeFaceColor: Color,
    activeLipColor: Color,
    activeTextColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clayPalette = LocalAuraClayPalette.current
    val faceColor = if (active) activeFaceColor else clayPalette.recessedTrackBg
    val lipColor = if (active) activeLipColor else clayPalette.recessedTrackBorder
    val txtColor = if (active) activeTextColor else clayPalette.inkColor

    Tactile3DPebbleCard(
        faceColor = faceColor,
        lipColor = lipColor,
        lipDepth = if (active) 5.dp else 3.5.dp,
        cornerRadius = 16.dp,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
        onClick = onClick,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) activeTextColor.copy(alpha = 0.22f)
                        else clayPalette.cardFace
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (active) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = txtColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = txtColor
                )
            )
        }
    }
}
