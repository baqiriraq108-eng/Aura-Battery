package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.*
import com.example.ui.theme.AuraTactileColors
import com.example.ui.theme.LocalAuraClayPalette
import com.example.ui.theme.PlusJakartaSansFontFamily
import kotlinx.coroutines.Dispatchers
import java.util.Calendar

@Composable
fun AdvancedDiagnosticsScreen(
    logs: List<BatteryLogEntry>? = null,
    liveState: LiveBatteryState? = null,
    currentRoute: String = "diagnostics",
    onNavigate: (String) -> Unit = {},
    isDark: Boolean = isSystemInDarkTheme()
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context.applicationContext) }
    val dbLogs by remember { db.batteryLogDao().getAllLogsFlow() }
        .collectAsState(initial = emptyList(), context = Dispatchers.IO)

    val effectiveLogs = logs ?: dbLogs
    val effectiveLiveState = liveState ?: LiveBatteryState()

    var stateOfHealth by remember { mutableStateOf<Double?>(null) }

    LaunchedEffect(Unit) {
        val stats = getBatteryDiagnostics(context)
        stateOfHealth = stats.first
    }

    AdvancedDiagnosticsScreenContent(
        logs = effectiveLogs,
        liveState = effectiveLiveState,
        stateOfHealth = stateOfHealth,
        currentRoute = currentRoute,
        onNavigate = onNavigate,
        isDark = isDark
    )
}

@Composable
fun AdvancedDiagnosticsScreenContent(
    logs: List<BatteryLogEntry>,
    liveState: LiveBatteryState = LiveBatteryState(),
    stateOfHealth: Double? = null,
    currentRoute: String = "diagnostics",
    onNavigate: (String) -> Unit = {},
    isDark: Boolean = isSystemInDarkTheme()
) {
    val clayPalette = LocalAuraClayPalette.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(clayPalette.screenBackground)
            .verticalScroll(scrollState)
            .padding(bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Clean Sculpted Clay Wave Header
        CompactWaveScreenHeader(
            title = "Diagnostics",
            currentRoute = currentRoute,
            onNavigate = onNavigate,
            isDark = isDark
        )

        // 2. Cohesive, Balanced 3D Clay Diagnostics Layout (Zero Awkward Empty Spaces & Zero Dummy Data)
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            ClayChargeCurveShowcaseCard(
                logs = logs,
                liveState = liveState,
                stateOfHealth = stateOfHealth,
                isDark = isDark
            )
        }
    }
}

private data class DiagnosticStatBadgeSpec(
    val key: String,
    val label: String,
    val value: String,
    val faceColor: Color,
    val lipColor: Color,
    val textColor: Color
)

/**
 * Restructured & Balanced 3D Clay Diagnostics Showcase:
 * - Queries ONLY genuine hardware metrics; never fabricates SOH percentages, cycle counts, or historical logs.
 * - If SOH (`stateOfHealth`) or any hardware sensor is restricted/null, that tile is completely removed from
 *   composition and the remaining tiles dynamically reflow to fill the card with zero visual voids.
 */
@Composable
fun ClayChargeCurveShowcaseCard(
    logs: List<BatteryLogEntry>,
    liveState: LiveBatteryState,
    stateOfHealth: Double? = null,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val clayPalette = LocalAuraClayPalette.current
    val currentLevel = liveState.level.coerceIn(1, 100)
    val isCharging = liveState.isCharging

    val durationString = remember(logs, currentLevel) {
        calculateActiveDuration(logs, currentLevel)
    }

    // Genuine hardware SOH percentage only: null when OS/OEM restricts access
    val genuineHealthPct: Int? = remember(stateOfHealth) {
        stateOfHealth?.takeIf { it > 0.0 }?.toInt()?.coerceIn(1, 100)
    }

    // Genuine recorded logs + live hardware reading (no fabricated historical offsets)
    val displayLogs = remember(logs, currentLevel, isCharging) {
        val now = System.currentTimeMillis()
        val windowMs = 24 * 60 * 60 * 1000L
        val filtered = logs.filter { it.timestamp >= now - windowMs }

        if (filtered.isEmpty()) {
            listOf(
                BatteryLogEntry(
                    timestamp = now - 60_000L,
                    batteryLevel = currentLevel,
                    isCharging = isCharging
                ),
                BatteryLogEntry(
                    timestamp = now,
                    batteryLevel = currentLevel,
                    isCharging = isCharging
                )
            )
        } else {
            val last = filtered.last()
            val withLive = if (now - last.timestamp > 5000L || last.batteryLevel != currentLevel || last.isCharging != isCharging) {
                filtered + BatteryLogEntry(
                    timestamp = now,
                    batteryLevel = currentLevel,
                    isCharging = isCharging
                )
            } else {
                filtered
            }
            if (withLive.size == 1) {
                listOf(
                    withLive.first().copy(timestamp = withLive.first().timestamp - 60_000L),
                    withLive.first()
                )
            } else {
                withLive
            }
        }
    }

    val minRecorded = remember(displayLogs, currentLevel) {
        if (displayLogs.isNotEmpty()) displayLogs.minOf { it.batteryLevel } else currentLevel
    }
    val maxRecorded = remember(displayLogs, currentLevel) {
        if (displayLogs.isNotEmpty()) displayLogs.maxOf { it.batteryLevel } else currentLevel
    }
    val avgRecorded = remember(displayLogs, currentLevel) {
        if (displayLogs.isNotEmpty()) displayLogs.map { it.batteryLevel }.average().toInt() else currentLevel
    }

    val curveSummaryBadges = remember(maxRecorded, minRecorded, liveState.voltage) {
        buildList {
            add(
                DiagnosticStatBadgeSpec(
                    key = "peak",
                    label = "Peak Level",
                    value = "$maxRecorded%",
                    faceColor = AuraTactileColors.TileTerracottaFace,
                    lipColor = AuraTactileColors.TileTerracottaLip,
                    textColor = AuraTactileColors.CreamText
                )
            )
            add(
                DiagnosticStatBadgeSpec(
                    key = "lowest",
                    label = "Lowest Point",
                    value = "$minRecorded%",
                    faceColor = AuraTactileColors.TileMustardFace,
                    lipColor = AuraTactileColors.TileMustardLip,
                    textColor = AuraTactileColors.CreamText
                )
            )
            if (liveState.voltage != null && liveState.voltage > 0) {
                add(
                    DiagnosticStatBadgeSpec(
                        key = "voltage",
                        label = "Voltage",
                        value = "${liveState.voltage} mV",
                        faceColor = AuraTactileColors.TileSlateFace,
                        lipColor = AuraTactileColors.TileSlateLip,
                        textColor = AuraTactileColors.CreamText
                    )
                )
            }
        }
    }

    val healthReliefBadges = remember(liveState, durationString) {
        buildList {
            val genuineHealthCondition = liveState.healthString
            if (genuineHealthCondition != null) {
                add(
                    DiagnosticStatBadgeSpec(
                        key = "condition",
                        label = "Condition",
                        value = genuineHealthCondition,
                        faceColor = AuraTactileColors.TileHealthGreenFace,
                        lipColor = AuraTactileColors.TileHealthGreenLip,
                        textColor = AuraTactileColors.CreamText
                    )
                )
            }
            add(
                DiagnosticStatBadgeSpec(
                    key = "active_time",
                    label = "Active Time",
                    value = durationString,
                    faceColor = AuraTactileColors.TileMustardFace,
                    lipColor = AuraTactileColors.TileMustardLip,
                    textColor = AuraTactileColors.CreamText
                )
            )
            if (liveState.temperature != null && liveState.temperature > 0f) {
                add(
                    DiagnosticStatBadgeSpec(
                        key = "temp",
                        label = "Temp.",
                        value = String.format("%.1f °C", liveState.temperature),
                        faceColor = AuraTactileColors.TileRustFace,
                        lipColor = AuraTactileColors.TileRustLip,
                        textColor = AuraTactileColors.CreamText
                    )
                )
            }
            add(
                DiagnosticStatBadgeSpec(
                    key = "source",
                    label = "Source",
                    value = liveState.sourceString,
                    faceColor = AuraTactileColors.TileSlateFace,
                    lipColor = AuraTactileColors.TileSlateLip,
                    textColor = AuraTactileColors.CreamText
                )
            )
            if (liveState.cycleCount != null && liveState.cycleCount >= 0) {
                add(
                    DiagnosticStatBadgeSpec(
                        key = "cycles",
                        label = "Cycles",
                        value = "${liveState.cycleCount}",
                        faceColor = AuraTactileColors.TilePlumFace,
                        lipColor = AuraTactileColors.TilePlumLip,
                        textColor = AuraTactileColors.CreamText
                    )
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("battery_usage_graph_card"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // --- CARD 1: SCULPTED 3D CLAY CHARGE VARIATION CURVE CARD ---
        Tactile3DPebbleCard(
            faceColor = clayPalette.cardFace,
            lipColor = clayPalette.cardLip,
            lipDepth = 6.dp,
            cornerRadius = 22.dp,
            contentPadding = PaddingValues(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Balanced Header Row: Section Title + Compact Live Status Pill
                UnifiedClaySectionTitle(
                    text = "Charge Variation Curve",
                    color = clayPalette.inkColor,
                    modifier = Modifier.fillMaxWidth()
                )

                // Sculpted Recessed 3D Clay Chart Well (Compact 184.dp height with tight proportions)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(184.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(clayPalette.recessedTrackBg)
                        .border(
                            width = 1.5.dp,
                            color = clayPalette.recessedTrackBorder,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    val timeLabels = remember {
                        List(5) { idx ->
                            val hoursAgo = (4 - idx) * 6
                            if (hoursAgo == 0) "Now" else {
                                val cal = Calendar.getInstance()
                                cal.add(Calendar.HOUR_OF_DAY, -hoursAgo)
                                String.format("%02d:00", cal.get(Calendar.HOUR_OF_DAY))
                            }
                        }
                    }

                    val labelColorArgb = clayPalette.mutedInkColor.toArgb()
                    val textPaintRight = remember(labelColorArgb) {
                        android.graphics.Paint().apply {
                            color = labelColorArgb
                            textSize = 22f
                            textAlign = android.graphics.Paint.Align.LEFT
                            isAntiAlias = true
                            isFakeBoldText = true
                        }
                    }
                    val textPaintBottom = remember(labelColorArgb) {
                        android.graphics.Paint().apply {
                            color = labelColorArgb
                            textSize = 21f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                            isFakeBoldText = true
                        }
                    }

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val leftPad = 12.dp.toPx()
                        val rightPad = 40.dp.toPx()
                        val topPad = 10.dp.toPx()
                        val bottomPad = 22.dp.toPx()

                        val plotW = (size.width - leftPad - rightPad).coerceAtLeast(10f)
                        val plotH = (size.height - topPad - bottomPad).coerceAtLeast(10f)
                        val endX = leftPad + plotW
                        val endY = topPad + plotH

                        listOf(25, 50, 75, 100).forEach { pct ->
                            val y = endY - (pct / 100f) * plotH
                            drawLine(
                                color = clayPalette.recessedTrackBorder.copy(alpha = 0.65f),
                                start = Offset(leftPad, y),
                                end = Offset(endX, y),
                                strokeWidth = 1.4.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                            )
                            drawContext.canvas.nativeCanvas.drawText(
                                "$pct%",
                                endX + 5.dp.toPx(),
                                y + 4.dp.toPx(),
                                textPaintRight
                            )
                        }

                        timeLabels.forEachIndexed { idx, label ->
                            val x = leftPad + (idx / 4f) * plotW
                            drawContext.canvas.nativeCanvas.drawText(
                                label,
                                x,
                                endY + 17.dp.toPx(),
                                textPaintBottom
                            )
                        }

                        val sampleCount = 7
                        val sampledEntries = if (displayLogs.size >= sampleCount) {
                            val step = (displayLogs.size - 1).toFloat() / (sampleCount - 1)
                            List(sampleCount) { i ->
                                displayLogs[(i * step).toInt().coerceIn(0, displayLogs.lastIndex)]
                            }
                        } else {
                            displayLogs
                        }

                        if (sampledEntries.size >= 2) {
                            val pts = sampledEntries.mapIndexed { idx, entry ->
                                val x = leftPad + (idx.toFloat() / (sampledEntries.size - 1).coerceAtLeast(1)) * plotW
                                val y = endY - (entry.batteryLevel.coerceIn(5, 100) / 100f) * plotH
                                Offset(x, y)
                            }

                            val clayFillPath = Path().apply {
                                moveTo(pts.first().x, endY)
                                lineTo(pts.first().x, pts.first().y)
                                for (i in 0 until pts.lastIndex) {
                                    val p0 = pts[i]
                                    val p1 = pts[i + 1]
                                    val cx = p0.x + (p1.x - p0.x) * 0.5f
                                    cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                                }
                                lineTo(pts.last().x, endY)
                                close()
                            }

                            drawPath(
                                path = clayFillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        clayPalette.primaryAccentFace.copy(alpha = 0.34f),
                                        clayPalette.secondaryAccentFace.copy(alpha = 0.14f),
                                        Color.Transparent
                                    ),
                                    startY = topPad,
                                    endY = endY
                                )
                            )

                            fun buildCurvePath(yOffset: Float): Path {
                                return Path().apply {
                                    moveTo(pts.first().x, pts.first().y + yOffset)
                                    for (i in 0 until pts.lastIndex) {
                                        val p0 = pts[i]
                                        val p1 = pts[i + 1]
                                        val cx = p0.x + (p1.x - p0.x) * 0.5f
                                        cubicTo(
                                            cx, p0.y + yOffset,
                                            cx, p1.y + yOffset,
                                            p1.x, p1.y + yOffset
                                        )
                                    }
                                }
                            }

                            val ribbonStroke = 8.dp.toPx()

                            drawPath(
                                path = buildCurvePath(5.5.dp.toPx()),
                                color = Color.Black.copy(alpha = 0.18f),
                                style = Stroke(width = ribbonStroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )

                            for (step in 4 downTo 1) {
                                drawPath(
                                    path = buildCurvePath(step * 1.05.dp.toPx()),
                                    color = clayPalette.primaryAccentLip,
                                    style = Stroke(width = ribbonStroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            }

                            drawPath(
                                path = buildCurvePath(0f),
                                color = clayPalette.primaryAccentFace,
                                style = Stroke(width = ribbonStroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )

                            drawPath(
                                path = buildCurvePath(-1.6.dp.toPx()),
                                color = Color.White.copy(alpha = 0.42f),
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )

                            pts.forEachIndexed { idx, pt ->
                                val isEndpoint = idx == 0 || idx == pts.lastIndex
                                val nodeR = if (isEndpoint) 6.5.dp.toPx() else 5.dp.toPx()
                                val nodeFace = if (idx == pts.lastIndex && isCharging) {
                                    clayPalette.secondaryAccentFace
                                } else {
                                    clayPalette.primaryAccentFace
                                }
                                val nodeLip = if (idx == pts.lastIndex && isCharging) {
                                    clayPalette.secondaryAccentLip
                                } else {
                                    clayPalette.primaryAccentLip
                                }

                                drawCircle(
                                    color = Color.Black.copy(alpha = 0.20f),
                                    radius = nodeR,
                                    center = Offset(pt.x, pt.y + 3.5.dp.toPx())
                                )
                                drawCircle(
                                    color = nodeLip,
                                    radius = nodeR,
                                    center = Offset(pt.x, pt.y + 2.2.dp.toPx())
                                )
                                drawCircle(
                                    color = nodeFace,
                                    radius = nodeR,
                                    center = pt
                                )
                                drawCircle(
                                    color = AuraTactileColors.CreamText,
                                    radius = nodeR * 0.44f,
                                    center = pt
                                )
                            }
                        }
                    }
                }

                // Adaptive Diversified Clay Summary Footer (Reflows dynamically if Voltage is restricted/null)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    curveSummaryBadges.forEach { badge ->
                        ClayMiniStatBadge(
                            label = badge.label,
                            value = badge.value,
                            faceColor = badge.faceColor,
                            lipColor = badge.lipColor,
                            textColor = badge.textColor,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("diagnostics_summary_${badge.key}")
                        )
                    }
                }
            }
        }

        // --- CARD 2: 3D CLAY BATTERY HEALTH RELIEF & ADAPTIVE TELEMETRY GRID ---
        Tactile3DPebbleCard(
            faceColor = clayPalette.cardFace,
            lipColor = clayPalette.cardLip,
            lipDepth = 6.dp,
            cornerRadius = 22.dp,
            contentPadding = PaddingValues(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                UnifiedClaySectionTitle(
                    text = "Battery Health Relief",
                    color = clayPalette.inkColor
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Sculpted 3D Clay Health Arch Plinth ONLY when genuine hardware SOH is available.
                    // When restricted by OS (genuineHealthPct == null), completely remove from composition!
                    if (genuineHealthPct != null) {
                        Box(
                            modifier = Modifier
                                .width(114.dp)
                                .height(128.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(clayPalette.recessedTrackBg)
                                .border(1.5.dp, clayPalette.recessedTrackBorder, RoundedCornerShape(18.dp))
                                .padding(8.dp)
                                .testTag("diagnostics_soh_arch"),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(92.dp)) {
                                val w = size.width
                                val h = size.height
                                val strokeW = 9.5.dp.toPx()
                                val arcInset = strokeW / 2f + 4.dp.toPx()
                                val arcSize = Size(w - arcInset * 2, h - arcInset * 2)
                                val topLeft = Offset(arcInset, arcInset)

                                drawArc(
                                    color = clayPalette.recessedTrackBorder,
                                    startAngle = 140f,
                                    sweepAngle = 260f,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeW + 2.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = clayPalette.cardFace,
                                    startAngle = 140f,
                                    sweepAngle = 260f,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                )

                                val sweep = 260f * (genuineHealthPct / 100f)
                                drawArc(
                                    color = AuraTactileColors.TileHealthGreenLip,
                                    startAngle = 140f,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    topLeft = Offset(topLeft.x, topLeft.y + 2.5.dp.toPx()),
                                    size = arcSize,
                                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = AuraTactileColors.TileHealthGreenFace,
                                    startAngle = 140f,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$genuineHealthPct%",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 19.sp,
                                        color = clayPalette.inkColor
                                    )
                                )
                                Text(
                                    text = "SOH",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp,
                                        letterSpacing = 0.6.sp,
                                        color = clayPalette.mutedInkColor
                                    )
                                )
                            }
                        }
                    }

                    // Right (or Full-Width when SOH is restricted): Adaptive 2-Column Reflow Grid of Genuine Telemetry Pods
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("diagnostics_health_grid"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        healthReliefBadges.chunked(2).forEach { rowBadges ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowBadges.forEach { badge ->
                                    ClayMiniStatBadge(
                                        label = badge.label,
                                        value = badge.value,
                                        faceColor = badge.faceColor,
                                        lipColor = badge.lipColor,
                                        textColor = badge.textColor,
                                        verticalPadding = 8.dp,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("diagnostics_badge_${badge.key}")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- CARD 3: COMPACT 3-COLUMN TELEMETRY DYNAMICS STRIP ---
        Tactile3DPebbleCard(
            faceColor = clayPalette.cardFace,
            lipColor = clayPalette.cardLip,
            lipDepth = 6.dp,
            cornerRadius = 22.dp,
            contentPadding = PaddingValues(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                UnifiedClaySectionTitle(
                    text = "Telemetry Overview",
                    color = clayPalette.inkColor
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ClayMiniStatBadge(
                        label = "24h Average",
                        value = "$avgRecorded%",
                        faceColor = clayPalette.primaryAccentFace,
                        lipColor = clayPalette.primaryAccentLip,
                        textColor = AuraTactileColors.textOn(clayPalette.primaryAccentFace),
                        modifier = Modifier.weight(1f)
                    )
                    ClayMiniStatBadge(
                        label = "Range Delta",
                        value = "${(maxRecorded - minRecorded).coerceAtLeast(0)}%",
                        faceColor = AuraTactileColors.TilePlumFace,
                        lipColor = AuraTactileColors.TilePlumLip,
                        textColor = AuraTactileColors.CreamText,
                        modifier = Modifier.weight(1f)
                    )
                    ClayMiniStatBadge(
                        label = "State",
                        value = if (isCharging) "Charging" else "Discharging",
                        faceColor = clayPalette.tertiaryAccentFace,
                        lipColor = clayPalette.tertiaryAccentLip,
                        textColor = AuraTactileColors.CreamText,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ClayMiniStatBadge(
    label: String,
    value: String,
    faceColor: Color,
    lipColor: Color,
    textColor: Color,
    verticalPadding: Dp = 9.dp,
    modifier: Modifier = Modifier
) {
    Tactile3DPebbleCard(
        faceColor = faceColor,
        lipColor = lipColor,
        lipDepth = 4.dp,
        cornerRadius = 16.dp,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = verticalPadding),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp,
                    color = textColor
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = textColor
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AdvancedDiagnosticsScreenPreview() {
    val mockLogs = listOf(
        BatteryLogEntry(System.currentTimeMillis() - 4 * 3600 * 1000, 95, false),
        BatteryLogEntry(System.currentTimeMillis() - 3 * 3600 * 1000, 88, false),
        BatteryLogEntry(System.currentTimeMillis() - 2 * 3600 * 1000, 80, true),
        BatteryLogEntry(System.currentTimeMillis() - 1 * 3600 * 1000, 85, true),
        BatteryLogEntry(System.currentTimeMillis(), 78, false)
    )
    AdvancedDiagnosticsScreenContent(
        logs = mockLogs,
        liveState = LiveBatteryState(level = 78, isCharging = false, temperature = 33.8f, voltage = 4092),
        stateOfHealth = 98.5
    )
}
