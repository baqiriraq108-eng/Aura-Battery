package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BatteryLogEntry
import com.example.ui.theme.AuraTactileColors
import com.example.ui.theme.LocalAuraClayPalette
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.UnifiedClayHeadingFontSize
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Unified 3D Clay Primary Section Heading used across all screens.
 * Enforces the exact same bold font family and 17.sp size without overflowing or overlapping.
 */
@Composable
fun UnifiedClaySectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalAuraClayPalette.current.inkColor,
    textAlign: TextAlign = TextAlign.Start
) {
    Text(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium.copy(
            fontFamily = PlusJakartaSansFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = UnifiedClayHeadingFontSize,
            lineHeight = 22.sp,
            color = color
        )
    )
}

/**
 * 4-Point Star (✦) separator icon used in the top header navigation bar:
 * "Home ✦ Diagnostics ✦ Settings"
 */
@Composable
fun FourPointStarIcon(
    color: Color = AuraTactileColors.StarGold,
    modifier: Modifier = Modifier.size(11.dp)
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outerR = size.minDimension / 2f
        val innerR = outerR * 0.28f

        val starPath = Path().apply {
            moveTo(cx, cy - outerR)
            quadraticTo(cx + innerR * 0.4f, cy - innerR * 0.4f, cx + outerR, cy)
            quadraticTo(cx + innerR * 0.4f, cy + innerR * 0.4f, cx, cy + outerR)
            quadraticTo(cx - innerR * 0.4f, cy + innerR * 0.4f, cx - outerR, cy)
            quadraticTo(cx - innerR * 0.4f, cy - innerR * 0.4f, cx, cy - outerR)
            close()
        }
        drawPath(path = starPath, color = color, style = Fill)
    }
}

/**
 * Level, single-line Top Star-Separated Navigation Bar ("Home ✦ Diagnostics ✦ Settings").
 */
data class NavTabSpec(
    val route: String,
    val label: String,
    val activeFace: Color,
    val activeLip: Color,
    val activeText: Color
)

/**
 * Level, single-line Top Star-Separated Navigation Bar ("Home ✦ Diagnostics ✦ Settings")
 * with a distinct, calibrated chromatic signature color for each tab:
 * - Home: Terracotta Clay (matched to peak & hero energy)
 * - Diagnostics: Slate Clay (matched to diagnostic instruments)
 * - Settings: Ochre Clay (matched to system control centers)
 */
@Composable
fun StarTopNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val clayPalette = LocalAuraClayPalette.current
    val navTabs = listOf(
        NavTabSpec(
            route = "dashboard",
            label = "Home",
            activeFace = clayPalette.primaryAccentFace,
            activeLip = clayPalette.primaryAccentLip,
            activeText = AuraTactileColors.CreamText
        ),
        NavTabSpec(
            route = "diagnostics",
            label = "Diagnostics",
            activeFace = clayPalette.primaryAccentFace,
            activeLip = clayPalette.primaryAccentLip,
            activeText = AuraTactileColors.CreamText
        ),
        NavTabSpec(
            route = "settings",
            label = "Settings",
            activeFace = clayPalette.primaryAccentFace,
            activeLip = clayPalette.primaryAccentLip,
            activeText = AuraTactileColors.CreamText
        )
    )

    val unselectedTabText = AuraTactileColors.CreamText

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        navTabs.forEachIndexed { index, tab ->
            val selected = currentRoute == tab.route
            NavBadgeItem(
                label = tab.label,
                selected = selected,
                activeFace = tab.activeFace,
                activeLip = tab.activeLip,
                activeText = tab.activeText,
                unselectedText = unselectedTabText,
                onClick = { onNavigate(tab.route) },
                modifier = Modifier.testTag("nav_${tab.route}_tab")
            )

            if (index < navTabs.lastIndex) {
                Spacer(modifier = Modifier.width(6.dp))
                FourPointStarIcon(
                    color = unselectedTabText.copy(alpha = 0.85f),
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
        }
    }
}

@Composable
private fun NavBadgeItem(
    label: String,
    selected: Boolean,
    activeFace: Color,
    activeLip: Color,
    activeText: Color,
    unselectedText: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val pressOffset by animateDpAsState(
        targetValue = if (isPressed) 3.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "NavPressOffset"
    )
    val lipHeight by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 5.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "NavLipHeight"
    )

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 42.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .padding(bottom = lipHeight + 2.dp)
                    .drawBehind {
                        val r = 14.dp.toPx()
                        val cr = CornerRadius(r, r)
                        val lipPx = lipHeight.toPx()
                        val yOff = pressOffset.toPx()

                        drawRoundRect(
                            color = Color.Black.copy(alpha = 0.24f),
                            topLeft = Offset(0f, lipPx + 2.dp.toPx()),
                            size = Size(size.width, size.height),
                            cornerRadius = cr
                        )
                        drawRoundRect(
                            color = activeLip,
                            topLeft = Offset(0f, lipPx),
                            size = Size(size.width, size.height),
                            cornerRadius = cr
                        )
                        drawRoundRect(
                            color = activeFace,
                            topLeft = Offset(0f, yOff),
                            size = Size(size.width, size.height),
                            cornerRadius = cr
                        )
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.24f),
                            topLeft = Offset(0f, yOff),
                            size = Size(size.width, size.height),
                            cornerRadius = cr,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                    .offset(y = pressOffset)
                    .padding(horizontal = 13.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = activeText
                    )
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .padding(horizontal = 9.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = unselectedText
                    )
                )
            }
        }
    }
}

/**
 * Sculpted Wavy Clay Background Header used on the Home screen.
 */
@Composable
fun SculptedForestWaveCanvas(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val clayPalette = LocalAuraClayPalette.current
    val headerColor = clayPalette.headerWaveFace
    val headerLipColor = clayPalette.headerWaveLip

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val lipPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w, h * 0.82f)
            cubicTo(
                w * 0.82f, h * 0.90f,
                w * 0.68f, h * 0.78f,
                w * 0.50f, h * 0.85f
            )
            cubicTo(
                w * 0.28f, h * 0.92f,
                w * 0.14f, h * 0.98f,
                0f, h * 0.88f
            )
            close()
        }
        drawPath(path = lipPath, color = headerLipColor)

        val wavePath = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w, h * 0.79f)
            cubicTo(
                w * 0.82f, h * 0.87f,
                w * 0.68f, h * 0.75f,
                w * 0.50f, h * 0.82f
            )
            cubicTo(
                w * 0.28f, h * 0.89f,
                w * 0.14f, h * 0.95f,
                0f, h * 0.85f
            )
            close()
        }
        drawPath(path = wavePath, color = headerColor)
    }
}

/**
 * Clean, sculpted Clay Wave Header for Diagnostics & Settings screens.
 */
@Composable
fun CompactWaveScreenHeader(
    title: String = "",
    badgeText: String = "",
    currentRoute: String,
    onNavigate: (String) -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val clayPalette = LocalAuraClayPalette.current
    val headerColor = clayPalette.headerWaveFace
    val headerLipColor = clayPalette.headerWaveLip

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val w = size.width
                val h = size.height

                val lipPath = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(w, 0f)
                    lineTo(w, h - 6.dp.toPx())
                    quadraticTo(w * 0.5f, h + 6.dp.toPx(), 0f, h - 6.dp.toPx())
                    close()
                }
                drawPath(lipPath, headerLipColor)

                val wavePath = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(w, 0f)
                    lineTo(w, h - 10.dp.toPx())
                    quadraticTo(w * 0.5f, h + 2.dp.toPx(), 0f, h - 10.dp.toPx())
                    close()
                }
                drawPath(wavePath, headerColor)
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StarTopNavigationBar(
                currentRoute = currentRoute,
                onNavigate = onNavigate
            )
        }
    }
}

/**
 * Large Circular Sand Hero Medallion ("80% DISCHARGING 34.3°C • 4107 mV").
 */
@Composable
fun SandHeroMedallion(
    level: Int,
    isCharging: Boolean,
    temperature: Float?,
    voltage: Int?,
    showTemp: Boolean = true,
    showVolt: Boolean = true,
    isDark: Boolean = isSystemInDarkTheme(),
    modifier: Modifier = Modifier
) {
    val clayPalette = LocalAuraClayPalette.current
    val chargingScale = if (isCharging) {
        val infiniteTransition = rememberInfiniteTransition(label = "MedallionPulse")
        val animatedScale by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.02f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ChargingPulseScale"
        )
        animatedScale
    } else {
        1.0f
    }

    val faceColor = clayPalette.cardFace
    val lipColor = clayPalette.cardLip
    val inkColor = clayPalette.inkColor
    val subInkColor = clayPalette.mutedInkColor

    Box(
        modifier = modifier
            .size(224.dp)
            .graphicsLayer {
                scaleX = chargingScale
                scaleY = chargingScale
            }
            .testTag("hero_battery_medallion"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .offset(y = 9.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = if (isDark) 0.35f else 0.14f))
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .offset(y = 6.dp)
                .clip(CircleShape)
                .background(lipColor)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .clip(CircleShape)
                .background(faceColor)
                .border(
                    width = 1.5.dp,
                    color = Color.White.copy(alpha = if (isDark) 0.12f else 0.48f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCharging) {
                Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                    drawCircle(
                        color = clayPalette.secondaryAccentFace.copy(alpha = 0.55f),
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                        )
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 14.dp)
            ) {
                Text(
                    text = "$level%",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 56.sp,
                        lineHeight = 58.sp,
                        letterSpacing = (-1.5).sp,
                        color = inkColor
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (isCharging) "CHARGING" else "DISCHARGING",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = UnifiedClayHeadingFontSize,
                        letterSpacing = 1.1.sp,
                        color = inkColor
                    ),
                    textAlign = TextAlign.Center
                )

                val validTemp = temperature != null && temperature > 0f && showTemp
                val validVolt = voltage != null && voltage > 0 && showVolt
                val metricsLine = buildString {
                    if (validTemp) append(String.format("%.1f°C", temperature))
                    if (validTemp && validVolt) append(" • ")
                    if (validVolt) append("$voltage mV")
                }

                if (metricsLine.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = metricsLine,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = PlusJakartaSansFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = subInkColor
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * "25h Activity" Section:
 * - "Live Charging Wave" has been completely deleted.
 * - "25h Activity" title is centered directly below the hero charging circle using the
 *   unified 17.sp ExtraBold heading style.
 */
@Composable
fun Activity25hSection(
    logs: List<BatteryLogEntry>,
    currentLevel: Int,
    isCharging: Boolean,
    isDark: Boolean = isSystemInDarkTheme(),
    modifier: Modifier = Modifier
) {
    val clayPalette = LocalAuraClayPalette.current
    val inkColor = clayPalette.inkColor

    val wavePhase = if (isCharging) {
        val infiniteTransition = rememberInfiniteTransition(label = "ChargingWaveMotion")
        val animatedPhase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "WavePhaseAngle"
        )
        animatedPhase
    } else {
        0f
    }

    val baseWavePoints = remember(logs, currentLevel) {
        if (logs.size >= 7) {
            val step = (logs.size - 1).coerceAtLeast(1) / 6f
            List(7) { idx ->
                val itemIndex = (idx * step).toInt().coerceIn(0, logs.lastIndex)
                val levelNormalized = (logs[itemIndex].batteryLevel.coerceIn(10, 100) - 10) / 90f
                val undulation = if (idx % 2 == 1) 0.22f else -0.18f
                (levelNormalized * 0.60f + 0.38f + undulation).coerceIn(0.16f, 0.92f)
            }
        } else {
            val anchor = (currentLevel.coerceIn(15, 100) / 100f)
            listOf(
                0.34f,
                0.68f,
                0.18f,
                0.90f,
                0.38f,
                0.82f,
                (anchor * 0.50f + 0.42f).coerceIn(0.45f, 0.88f)
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .testTag("activity_25h_section"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Centered "25h Activity" heading directly beneath the charging circle
        UnifiedClaySectionTitle(
            text = "25h Activity",
            color = inkColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                fun animatedHeight(index: Int, baseVal: Float): Float {
                    if (!isCharging) return baseVal
                    val waveDelta = sin(wavePhase + index * 0.95f) * 0.11f
                    return (baseVal + waveDelta).coerceIn(0.14f, 0.94f)
                }

                // --- LAYER 1: 3D EXTRUDED CLAY RIBBON WAVE WITH SCULPTED END-CAPS ---
                val ribbonTopY = h * 0.10f
                val ribbonBottomY = h * 0.54f
                val ribbonSpan = ribbonBottomY - ribbonTopY

                val startX = w * 0.045f
                val endX = w * 0.955f
                val spanW = endX - startX

                val pts = listOf(
                    Offset(startX, ribbonBottomY - animatedHeight(0, baseWavePoints[0]) * ribbonSpan * 0.60f),
                    Offset(startX + spanW * 0.13f, ribbonBottomY - animatedHeight(1, baseWavePoints[1]) * ribbonSpan * 0.82f),
                    Offset(startX + spanW * 0.27f, ribbonBottomY - animatedHeight(2, baseWavePoints[2]) * ribbonSpan * 0.22f),
                    Offset(startX + spanW * 0.42f, ribbonBottomY - animatedHeight(3, baseWavePoints[3]) * ribbonSpan * 0.98f),
                    Offset(startX + spanW * 0.57f, ribbonBottomY - animatedHeight(4, baseWavePoints[4]) * ribbonSpan * 0.40f),
                    Offset(startX + spanW * 0.70f, ribbonBottomY - animatedHeight(5, baseWavePoints[5]) * ribbonSpan * 0.88f),
                    Offset(startX + spanW * 0.84f, ribbonBottomY - animatedHeight(6, baseWavePoints[4]) * ribbonSpan * 0.44f),
                    Offset(endX, ribbonBottomY - animatedHeight(7, baseWavePoints[6]) * ribbonSpan * 0.78f)
                )

                fun buildSmoothRibbonPath(yOffset: Float): Path {
                    return Path().apply {
                        moveTo(pts[0].x, pts[0].y + yOffset)
                        for (i in 0 until pts.lastIndex) {
                            val p0 = pts[i]
                            val p1 = pts[i + 1]
                            val cx1 = p0.x + (p1.x - p0.x) * 0.5f
                            val cy1 = p0.y + yOffset
                            val cx2 = p0.x + (p1.x - p0.x) * 0.5f
                            val cy2 = p1.y + yOffset
                            cubicTo(cx1, cy1, cx2, cy2, p1.x, p1.y + yOffset)
                        }
                    }
                }

                val mainStrokePx = 10.dp.toPx()
                val extrusionStepPx = 1.3.dp.toPx()

                drawPath(
                    path = buildSmoothRibbonPath(8.dp.toPx()),
                    color = Color.Black.copy(alpha = if (isDark) 0.30f else 0.12f),
                    style = Stroke(
                        width = mainStrokePx,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                for (step in 5 downTo 1) {
                    drawPath(
                        path = buildSmoothRibbonPath(step * extrusionStepPx),
                        color = clayPalette.primaryAccentLip,
                        style = Stroke(
                            width = mainStrokePx,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                drawPath(
                    path = buildSmoothRibbonPath(0f),
                    color = clayPalette.primaryAccentFace,
                    style = Stroke(
                        width = mainStrokePx,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                drawPath(
                    path = buildSmoothRibbonPath(-2.dp.toPx()),
                    color = Color.White.copy(alpha = 0.38f),
                    style = Stroke(
                        width = 2.2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                listOf(pts.first(), pts.last()).forEach { endPt ->
                    val capRadius = 7.5.dp.toPx()
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.18f),
                        radius = capRadius,
                        center = Offset(endPt.x, endPt.y + 6.5.dp.toPx())
                    )
                    drawCircle(
                        color = clayPalette.primaryAccentLip,
                        radius = capRadius,
                        center = Offset(endPt.x, endPt.y + 4.dp.toPx())
                    )
                    drawCircle(
                        color = clayPalette.primaryAccentFace,
                        radius = capRadius,
                        center = endPt
                    )
                    drawCircle(
                        color = AuraTactileColors.CreamText,
                        radius = capRadius * 0.42f,
                        center = endPt
                    )
                }

                // --- LAYER 2: DIVERSIFIED MULTI-COLORED CLAY HILLS ---
                val baselineY = h * 0.92f
                val hillLeftLimit = w * 0.02f
                val hillRightLimit = w * 0.98f
                val hillSpanW = hillRightLimit - hillLeftLimit

                fun drawOrganicClayHill(
                    startXFrac: Float,
                    peakXFrac: Float,
                    endXFrac: Float,
                    peakHeightFrac: Float,
                    phaseOffset: Float,
                    faceColor: Color,
                    rimColor: Color
                ) {
                    val dynamicPeakFrac = if (isCharging) {
                        peakHeightFrac + sin(wavePhase + phaseOffset) * 0.025f
                    } else {
                        peakHeightFrac
                    }
                    val sx = hillLeftLimit + hillSpanW * startXFrac
                    val px = hillLeftLimit + hillSpanW * peakXFrac
                    val py = baselineY - h * dynamicPeakFrac
                    val ex = hillLeftLimit + hillSpanW * endXFrac

                    val hillPath = Path().apply {
                        moveTo(sx, baselineY)
                        cubicTo(
                            sx + (px - sx) * 0.42f, baselineY,
                            px - (px - sx) * 0.36f, py,
                            px, py
                        )
                        cubicTo(
                            px + (ex - px) * 0.36f, py,
                            ex - (ex - px) * 0.42f, baselineY,
                            ex, baselineY
                        )
                        close()
                    }
                    drawPath(path = hillPath, color = faceColor, style = Fill)
                    drawPath(
                        path = hillPath,
                        color = rimColor,
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }

                drawOrganicClayHill(
                    0.43f, 0.56f, 0.70f, 0.22f, 1.8f,
                    AuraTactileColors.HillCopper, Color.White.copy(alpha = 0.22f)
                )
                drawOrganicClayHill(
                    0.13f, 0.25f, 0.39f, 0.20f, 0.9f,
                    AuraTactileColors.HillPlum, Color.White.copy(alpha = 0.22f)
                )
                drawOrganicClayHill(
                    0.00f, 0.10f, 0.22f, 0.24f, 0.0f,
                    AuraTactileColors.HillSlate, Color.White.copy(alpha = 0.22f)
                )
                drawOrganicClayHill(
                    0.26f, 0.40f, 0.55f, 0.33f, 1.4f,
                    AuraTactileColors.HillCoral, Color.White.copy(alpha = 0.24f)
                )
                drawOrganicClayHill(
                    0.55f, 0.69f, 0.83f, 0.27f, 2.3f,
                    AuraTactileColors.HillGold, Color.White.copy(alpha = 0.26f)
                )
                drawOrganicClayHill(
                    0.74f, 0.88f, 1.00f, 0.30f, 3.1f,
                    AuraTactileColors.HillMint, Color.White.copy(alpha = 0.24f)
                )

                drawRoundRect(
                    color = clayPalette.cardLip,
                    topLeft = Offset(hillLeftLimit - 2.dp.toPx(), baselineY - 1.dp.toPx()),
                    size = Size(hillSpanW + 4.dp.toPx(), 7.dp.toPx()),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                drawRoundRect(
                    color = clayPalette.cardFace,
                    topLeft = Offset(hillLeftLimit - 2.dp.toPx(), baselineY - 3.dp.toPx()),
                    size = Size(hillSpanW + 4.dp.toPx(), 5.dp.toPx()),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }
    }
}

/**
 * Reusable Level 3D Beveled Clay Pebble Card (0° tilt) with an extruded bottom lip,
 * subtle ground shadow, and interactive press physics.
 */
@Composable
fun Tactile3DPebbleCard(
    faceColor: Color,
    lipColor: Color,
    modifier: Modifier = Modifier,
    lipDepth: Dp = 7.dp,
    cornerRadius: Dp = 24.dp,
    tiltDegrees: Float = 0f,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    customBorderColor: Color? = null,
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val clayPalette = LocalAuraClayPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val currentPressOffset by animateDpAsState(
        targetValue = if (isPressed && onClick != null) (lipDepth * 0.65f) else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "PebblePressOffset"
    )

    val pressPx = currentPressOffset
    val borderColor = customBorderColor ?: when (faceColor) {
        clayPalette.cardFace -> clayPalette.cardBorder
        clayPalette.recessedTrackBg -> clayPalette.recessedTrackBorder
        else -> Color.White.copy(alpha = 0.22f)
    }

    Box(
        modifier = modifier
            .then(
                if (tiltDegrees != 0f) {
                    Modifier.graphicsLayer { rotationZ = tiltDegrees }
                } else {
                    Modifier
                }
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    }
                } else {
                    Modifier
                }
            )
            .padding(bottom = lipDepth + 2.dp)
            .drawBehind {
                val r = cornerRadius.toPx()
                val cr = CornerRadius(r, r)
                val lipPx = lipDepth.toPx()
                val yOff = pressPx.toPx()

                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.15f),
                    topLeft = Offset(0f, lipPx + 2.dp.toPx()),
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
                    topLeft = Offset(0f, yOff),
                    size = Size(size.width, size.height),
                    cornerRadius = cr
                )
                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset(0f, yOff),
                    size = Size(size.width, size.height),
                    cornerRadius = cr,
                    style = Stroke(width = borderWidth.toPx())
                )
            }
            .offset(y = pressPx)
            .padding(contentPadding),
        content = content
    )
}

/**
 * Custom 3D Clay Toggle Switch.
 */
@Composable
fun Clay3DSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val clayPalette = LocalAuraClayPalette.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    val thumbFraction = if (checked) 1f else 0f
    val trackColor = if (checked) clayPalette.primaryAccentFace.copy(alpha = 0.35f) else clayPalette.recessedTrackBg
    val knobFaceColor = if (checked) clayPalette.primaryAccentFace else clayPalette.sliderTrackBg
    val knobLipColor = if (checked) clayPalette.primaryAccentLip else clayPalette.recessedTrackBorder

    Box(
        modifier = modifier
            .size(width = 54.dp, height = 30.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(!checked)
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 2.dp.toPx() || h <= 2.dp.toPx()) return@Canvas
            val trackH = 20.dp.toPx()
            val trackTop = (h - trackH) / 2f
            val cornerR = CornerRadius(trackH / 2f, trackH / 2f)

            drawRoundRect(
                color = clayPalette.recessedTrackBorder,
                topLeft = Offset(0f, trackTop),
                size = Size(w, trackH + 2.dp.toPx()),
                cornerRadius = cornerR
            )
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(1.dp.toPx(), trackTop + 1.dp.toPx()),
                size = Size((w - 2.dp.toPx()).coerceAtLeast(1f), (trackH - 1.dp.toPx()).coerceAtLeast(1f)),
                cornerRadius = cornerR
            )

            val knobR = 10.5.dp.toPx()
            val minX = knobR + 3.dp.toPx()
            val maxX = (w - knobR - 3.dp.toPx()).coerceAtLeast(minX)
            val cx = minX + thumbFraction * (maxX - minX)
            val cy = h / 2f - 1.5.dp.toPx()

            drawCircle(
                color = Color.Black.copy(alpha = 0.22f),
                radius = knobR,
                center = Offset(cx, cy + 4.dp.toPx())
            )
            drawCircle(
                color = knobLipColor,
                radius = knobR,
                center = Offset(cx, cy + 2.6.dp.toPx())
            )
            drawCircle(
                color = knobFaceColor,
                radius = knobR,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = AuraTactileColors.CreamText,
                radius = knobR * 0.38f,
                center = Offset(cx, cy)
            )
        }
    }
}

enum class TactileMetricIconType {
    NONE,
    CLOCK_HISTORY,
    THERMOMETER,
    LIGHTNING_BOLT,
    CYCLE_RING,
    BELL_ALARM
}

@Composable
fun TactileMetricCanvasIcon(
    type: TactileMetricIconType,
    tint: Color,
    modifier: Modifier = Modifier.size(20.dp)
) {
    if (type == TactileMetricIconType.NONE) return

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()

        when (type) {
            TactileMetricIconType.CLOCK_HISTORY -> {
                val radius = size.minDimension * 0.40f
                val center = Offset(w / 2f, h / 2f)
                drawArc(
                    color = tint,
                    startAngle = -60f,
                    sweepAngle = 310f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                drawLine(
                    color = tint,
                    start = center,
                    end = Offset(center.x, center.y - radius * 0.55f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = center,
                    end = Offset(center.x + radius * 0.45f, center.y),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
            TactileMetricIconType.THERMOMETER -> {
                val cx = w * 0.5f
                val bulbRadius = w * 0.18f
                val bulbCenterY = h * 0.73f
                val stemTopY = h * 0.16f
                val stemHalfW = w * 0.10f

                drawRoundRect(
                    color = tint,
                    topLeft = Offset(cx - stemHalfW, stemTopY),
                    size = Size(stemHalfW * 2f, bulbCenterY - stemTopY),
                    cornerRadius = CornerRadius(stemHalfW, stemHalfW),
                    style = Stroke(width = stroke * 0.9f)
                )
                drawCircle(
                    color = tint,
                    radius = bulbRadius,
                    center = Offset(cx, bulbCenterY),
                    style = Stroke(width = stroke * 0.9f)
                )
                drawCircle(
                    color = tint,
                    radius = bulbRadius * 0.5f,
                    center = Offset(cx, bulbCenterY),
                    style = Fill
                )
            }
            TactileMetricIconType.LIGHTNING_BOLT -> {
                val boltPath = Path().apply {
                    moveTo(w * 0.56f, h * 0.10f)
                    lineTo(w * 0.24f, h * 0.54f)
                    lineTo(w * 0.50f, h * 0.54f)
                    lineTo(w * 0.42f, h * 0.90f)
                    lineTo(w * 0.78f, h * 0.44f)
                    lineTo(w * 0.52f, h * 0.44f)
                    close()
                }
                drawPath(
                    path = boltPath,
                    color = tint,
                    style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
            TactileMetricIconType.CYCLE_RING -> {
                val radius = size.minDimension * 0.38f
                val center = Offset(w / 2f, h / 2f)
                drawCircle(
                    color = tint,
                    radius = radius,
                    center = center,
                    style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f), 0f))
                )
            }
            TactileMetricIconType.BELL_ALARM -> {
                val radius = size.minDimension * 0.36f
                val center = Offset(w / 2f, h / 2f)
                drawCircle(
                    color = tint,
                    radius = radius,
                    center = center,
                    style = Stroke(width = stroke)
                )
                drawCircle(
                    color = tint,
                    radius = radius * 0.35f,
                    center = center,
                    style = Fill
                )
            }
            TactileMetricIconType.NONE -> Unit
        }
    }
}

/**
 * Level 3D Tactile Metric Pebble Tile (0° tilt) with unified 17.sp ExtraBold heading.
 */
@Composable
fun TactileMetricTile(
    title: String,
    value: String,
    faceColor: Color,
    lipColor: Color,
    textColor: Color,
    iconType: TactileMetricIconType = TactileMetricIconType.NONE,
    tiltDegrees: Float = 0f,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Tactile3DPebbleCard(
        faceColor = faceColor,
        lipColor = lipColor,
        lipDepth = 7.dp,
        cornerRadius = 24.dp,
        tiltDegrees = tiltDegrees,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 86.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = UnifiedClayHeadingFontSize,
                        color = textColor
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (iconType != TactileMetricIconType.NONE) {
                    Spacer(modifier = Modifier.width(6.dp))
                    TactileMetricCanvasIcon(
                        type = iconType,
                        tint = textColor,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = textColor
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Custom 3D Critical-Threshold Bar with its percentage readout placed directly next to the bar.
 */
@Composable
fun Tactile3DThresholdBar(
    label: String,
    value: Int,
    valueRange: IntRange,
    barColor: Color,
    lipColor: Color,
    badgeTextColor: Color,
    isDark: Boolean,
    onValueChange: (Int) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clayPalette = LocalAuraClayPalette.current
    val haptic = LocalHapticFeedback.current
    val inkColor = clayPalette.inkColor
    val trackBgColor = clayPalette.sliderTrackBg
    val trackBorderColor = clayPalette.recessedTrackBorder

    var trackWidthPx by remember { mutableFloatStateOf(1f) }
    val minVal = valueRange.first
    val maxVal = valueRange.last
    val rangeSpan = (maxVal - minVal).coerceAtLeast(1)

    val currentValState by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)

    var dragFloatValue by remember { mutableFloatStateOf(value.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(value, isDragging) {
        if (!isDragging) {
            dragFloatValue = value.toFloat()
        }
    }

    val fraction = ((value - minVal).toFloat() / rangeSpan).coerceIn(0f, 1f)

    fun xToValueFloat(xPx: Float, thumbRadiusPx: Float): Float {
        val minX = thumbRadiusPx
        val maxX = (trackWidthPx - thumbRadiusPx).coerceAtLeast(minX + 1f)
        val rawFraction = ((xPx - minX) / (maxX - minX)).coerceIn(0f, 1f)
        return minVal + rawFraction * rangeSpan
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = inkColor
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .onSizeChanged { trackWidthPx = it.width.toFloat() }
                    .pointerInput(minVal, maxVal) {
                        val thumbR = 11.dp.toPx()
                        detectTapGestures { offset ->
                            val tappedFloat = xToValueFloat(offset.x, thumbR)
                            val newInt = tappedFloat.roundToInt().coerceIn(minVal, maxVal)
                            dragFloatValue = newInt.toFloat()
                            if (newInt != currentValState) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentOnValueChange(newInt)
                            }
                            currentOnValueChangeFinished()
                        }
                    }
                    .pointerInput(minVal, maxVal) {
                        val thumbR = 11.dp.toPx()
                        detectHorizontalDragGestures(
                            onDragStart = { startOffset ->
                                isDragging = true
                                val minX = thumbR
                                val maxX = (trackWidthPx - thumbR).coerceAtLeast(minX + 1f)
                                val currentThumbX = minX + ((currentValState - minVal).toFloat() / rangeSpan) * (maxX - minX)
                                if (kotlin.math.abs(startOffset.x - currentThumbX) <= 38.dp.toPx()) {
                                    // Grabbed near the thumb: start smoothly from exact current degree without any jump
                                    dragFloatValue = currentValState.toFloat()
                                } else {
                                    // Grabbed elsewhere on the track: jump to touched degree and drag smoothly from there
                                    val touchedFloat = xToValueFloat(startOffset.x, thumbR)
                                    dragFloatValue = touchedFloat
                                    val newInt = touchedFloat.roundToInt().coerceIn(minVal, maxVal)
                                    if (newInt != currentValState) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        currentOnValueChange(newInt)
                                    }
                                }
                            },
                            onDragEnd = {
                                isDragging = false
                                currentOnValueChangeFinished()
                            },
                            onDragCancel = {
                                isDragging = false
                                currentOnValueChangeFinished()
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            val minX = thumbR
                            val maxX = (trackWidthPx - thumbR).coerceAtLeast(minX + 1f)
                            val usableSpan = (maxX - minX).coerceAtLeast(1f)
                            val deltaDegrees = (dragAmount / usableSpan) * rangeSpan
                            dragFloatValue = (dragFloatValue + deltaDegrees).coerceIn(minVal.toFloat(), maxVal.toFloat())
                            val nextDegree = dragFloatValue.roundToInt().coerceIn(minVal, maxVal)
                            if (nextDegree != currentValState) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentOnValueChange(nextDegree)
                            }
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    if (w <= 1f || h <= 1f) return@Canvas
                    val trackH = 16.dp.toPx().coerceAtMost(h)
                    val trackTop = (h - trackH) / 2f
                    val cornerR = CornerRadius(trackH / 2f, trackH / 2f)
                    val thumbR = 11.dp.toPx().coerceAtMost(w / 2f)
                    val minX = thumbR
                    val maxX = (w - thumbR).coerceAtLeast(minX)
                    val thumbCenterX = minX + fraction * (maxX - minX)

                    drawRoundRect(
                        color = trackBorderColor,
                        topLeft = Offset(0f, trackTop),
                        size = Size(w, trackH + 2.dp.toPx()),
                        cornerRadius = cornerR
                    )
                    drawRoundRect(
                        color = trackBgColor,
                        topLeft = Offset(0f, trackTop),
                        size = Size(w, trackH),
                        cornerRadius = cornerR
                    )

                    val minFillW = trackH.coerceAtMost(w)
                    val fillWidth = (thumbCenterX + thumbR * 0.5f).coerceIn(minFillW, w)
                    drawRoundRect(
                        color = lipColor,
                        topLeft = Offset(0f, trackTop + 3.dp.toPx()),
                        size = Size(fillWidth, trackH),
                        cornerRadius = cornerR
                    )
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(0f, trackTop),
                        size = Size(fillWidth, trackH),
                        cornerRadius = cornerR
                    )

                    val centerY = h / 2f - 1.dp.toPx()
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.22f),
                        radius = thumbR,
                        center = Offset(thumbCenterX, centerY + 4.dp.toPx())
                    )
                    drawCircle(
                        color = lipColor,
                        radius = thumbR,
                        center = Offset(thumbCenterX, centerY + 2.5.dp.toPx())
                    )
                    drawCircle(
                        color = barColor,
                        radius = thumbR,
                        center = Offset(thumbCenterX, centerY)
                    )
                    drawCircle(
                        color = AuraTactileColors.CreamText,
                        radius = thumbR * 0.42f,
                        center = Offset(thumbCenterX, centerY)
                    )
                }
            }

            // Explicit fixed width 78.dp so Tactile3DPebbleCard never consumes full Row width,
            // while giving ample room for "100%" and the "%" symbol with zero truncation.
            Tactile3DPebbleCard(
                faceColor = barColor,
                lipColor = lipColor,
                lipDepth = 4.dp,
                cornerRadius = 14.dp,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 7.dp),
                modifier = Modifier.width(78.dp)
            ) {
                Text(
                    text = "$value%",
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = badgeTextColor
                    )
                )
            }
        }
    }
}

/**
 * 4 Bespoke 3D Clay Sculpture Types representing the 4 bottom permissions/system actions
 * without any text labels.
 */
enum class ClayPermissionSculptureType {
    ALERT_BELL_CHIME,
    BATTERY_POWER_CORE,
    SHIELD_AUTOSTART_GUARD,
    SYSTEM_BOOT_STARTUP,
    SYSTEM_BOOT_IGNITION,
    EXPORT_DATA_TRAY
}

/**
 * Label-Free 3D Clay Sculpted Figurine Tile (arranged in a balanced 2x2 grid in Settings Permissions).
 * Each tile houses a custom multi-layered 3D clay figurine with extruded bevels, clay shadows,
 * and specular highlights representing its specific permission/action, preserving full uncrowded size.
 */
@Composable
fun Clay3DSculptedPermissionTile(
    sculptureType: ClayPermissionSculptureType,
    pedestalFace: Color,
    pedestalLip: Color,
    sculptureMainFace: Color,
    sculptureMainLip: Color,
    sculptureAccentFace: Color,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Tactile3DPebbleCard(
        faceColor = pedestalFace,
        lipColor = pedestalLip,
        lipDepth = 5.5.dp,
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(8.dp),
        onClick = onClick,
        modifier = modifier
            .height(84.dp)
            .testTag("clay_sculpture_${sculptureType.name.lowercase()}")
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(52.dp)) {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val cy = h / 2f

                // 1. Recessed circular clay well on the pedestal
                drawCircle(
                    color = Color.Black.copy(alpha = 0.16f),
                    radius = w * 0.45f,
                    center = Offset(cx, cy + 2.dp.toPx())
                )
                drawCircle(
                    color = pedestalLip.copy(alpha = 0.65f),
                    radius = w * 0.44f,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.14f),
                    radius = w * 0.44f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                when (sculptureType) {
                    ClayPermissionSculptureType.ALERT_BELL_CHIME -> {
                        // Acoustic 3D clay wave arcs on left and right
                        val waveArcSize = Size(w * 0.72f, h * 0.72f)
                        val waveTopLeft = Offset(cx - waveArcSize.width / 2f, cy - waveArcSize.height / 2f)
                        drawArc(
                            color = sculptureAccentFace,
                            startAngle = -35f,
                            sweepAngle = 70f,
                            useCenter = false,
                            topLeft = waveTopLeft,
                            size = waveArcSize,
                            style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = sculptureAccentFace,
                            startAngle = 145f,
                            sweepAngle = 70f,
                            useCenter = false,
                            topLeft = waveTopLeft,
                            size = waveArcSize,
                            style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // 3D Clapper sphere at bottom of bell
                        val clapperCenter = Offset(cx, h * 0.72f)
                        drawCircle(
                            color = sculptureMainLip,
                            radius = 5.5.dp.toPx(),
                            center = Offset(clapperCenter.x, clapperCenter.y + 2.dp.toPx())
                        )
                        drawCircle(
                            color = sculptureAccentFace,
                            radius = 5.5.dp.toPx(),
                            center = clapperCenter
                        )

                        // Top bell loop bead
                        drawCircle(
                            color = sculptureMainLip,
                            radius = 4.5.dp.toPx(),
                            center = Offset(cx, h * 0.22f + 2.dp.toPx())
                        )
                        drawCircle(
                            color = sculptureMainFace,
                            radius = 4.5.dp.toPx(),
                            center = Offset(cx, h * 0.22f)
                        )

                        // Sculpted 3D Bell Dome (Bottom Lip + Main Clay Dome + Specular Highlight)
                        fun buildBellPath(yOffset: Float): Path = Path().apply {
                            moveTo(cx, h * 0.24f + yOffset)
                            cubicTo(
                                w * 0.68f, h * 0.24f + yOffset,
                                w * 0.70f, h * 0.54f + yOffset,
                                w * 0.76f, h * 0.65f + yOffset
                            )
                            lineTo(w * 0.24f, h * 0.65f + yOffset)
                            cubicTo(
                                w * 0.30f, h * 0.54f + yOffset,
                                w * 0.32f, h * 0.24f + yOffset,
                                cx, h * 0.24f + yOffset
                            )
                            close()
                        }

                        drawPath(buildBellPath(4.dp.toPx()), Color.Black.copy(alpha = 0.22f))
                        drawPath(buildBellPath(2.8.dp.toPx()), sculptureMainLip)
                        drawPath(buildBellPath(0f), sculptureMainFace)

                        // Bell rim flange
                        drawRoundRect(
                            color = sculptureMainLip,
                            topLeft = Offset(w * 0.22f, h * 0.61f + 2.5.dp.toPx()),
                            size = Size(w * 0.56f, 6.5.dp.toPx()),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                        drawRoundRect(
                            color = sculptureMainFace,
                            topLeft = Offset(w * 0.22f, h * 0.61f),
                            size = Size(w * 0.56f, 6.5.dp.toPx()),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )

                        // Specular clay shine on dome
                        drawCircle(
                            color = Color.White.copy(alpha = 0.45f),
                            radius = 3.2.dp.toPx(),
                            center = Offset(cx - 5.dp.toPx(), h * 0.38f)
                        )
                    }

                    ClayPermissionSculptureType.BATTERY_POWER_CORE -> {
                        // Top 3D Battery Terminal Cap
                        drawRoundRect(
                            color = sculptureMainLip,
                            topLeft = Offset(w * 0.38f, h * 0.16f + 2.dp.toPx()),
                            size = Size(w * 0.24f, 6.dp.toPx()),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )
                        drawRoundRect(
                            color = sculptureAccentFace,
                            topLeft = Offset(w * 0.38f, h * 0.16f),
                            size = Size(w * 0.24f, 6.dp.toPx()),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )

                        // Main 3D Clay Battery Cylinder Body
                        val bodyTop = h * 0.24f
                        val bodyW = w * 0.48f
                        val bodyH = h * 0.54f
                        val bodyLeft = (w - bodyW) / 2f
                        val bodyCorner = CornerRadius(10.dp.toPx(), 10.dp.toPx())

                        drawRoundRect(
                            color = Color.Black.copy(alpha = 0.24f),
                            topLeft = Offset(bodyLeft, bodyTop + 4.5.dp.toPx()),
                            size = Size(bodyW, bodyH),
                            cornerRadius = bodyCorner
                        )
                        drawRoundRect(
                            color = sculptureMainLip,
                            topLeft = Offset(bodyLeft, bodyTop + 3.dp.toPx()),
                            size = Size(bodyW, bodyH),
                            cornerRadius = bodyCorner
                        )
                        drawRoundRect(
                            color = sculptureMainFace,
                            topLeft = Offset(bodyLeft, bodyTop),
                            size = Size(bodyW, bodyH),
                            cornerRadius = bodyCorner
                        )

                        // 3D Sculpted Lightning Bolt Relief on the Battery Core
                        fun buildBolt(yOffset: Float): Path = Path().apply {
                            moveTo(cx + 3.dp.toPx(), h * 0.31f + yOffset)
                            lineTo(cx - 7.dp.toPx(), h * 0.52f + yOffset)
                            lineTo(cx - 0.5.dp.toPx(), h * 0.52f + yOffset)
                            lineTo(cx - 3.5.dp.toPx(), h * 0.70f + yOffset)
                            lineTo(cx + 7.dp.toPx(), h * 0.48f + yOffset)
                            lineTo(cx + 0.5.dp.toPx(), h * 0.48f + yOffset)
                            close()
                        }

                        drawPath(buildBolt(2.dp.toPx()), sculptureMainLip)
                        drawPath(buildBolt(0f), sculptureAccentFace)
                    }

                    ClayPermissionSculptureType.SHIELD_AUTOSTART_GUARD -> {
                        // 3D Sculpted Clay Crest Shield
                        fun buildShield(yOffset: Float, scale: Float = 1f): Path = Path().apply {
                            val sw = w * 0.52f * scale
                            val sh = h * 0.58f * scale
                            val topY = cy - sh * 0.48f + yOffset
                            val botY = cy + sh * 0.52f + yOffset
                            moveTo(cx, topY)
                            lineTo(cx + sw / 2f, topY + sh * 0.18f)
                            cubicTo(
                                cx + sw / 2f, topY + sh * 0.62f,
                                cx + sw * 0.22f, botY - sh * 0.08f,
                                cx, botY
                            )
                            cubicTo(
                                cx - sw * 0.22f, botY - sh * 0.08f,
                                cx - sw / 2f, topY + sh * 0.62f,
                                cx - sw / 2f, topY + sh * 0.18f
                            )
                            close()
                        }

                        drawPath(buildShield(4.5.dp.toPx()), Color.Black.copy(alpha = 0.24f))
                        drawPath(buildShield(3.dp.toPx()), sculptureMainLip)
                        drawPath(buildShield(0f), sculptureMainFace)

                        // Inner 3D Clay Star/Play Emblem inside the Shield
                        val starR = w * 0.15f
                        val starPath = Path().apply {
                            moveTo(cx, cy - starR)
                            quadraticTo(cx + starR * 0.25f, cy - starR * 0.25f, cx + starR, cy)
                            quadraticTo(cx + starR * 0.25f, cy + starR * 0.25f, cx, cy + starR)
                            quadraticTo(cx - starR * 0.25f, cy + starR * 0.25f, cx - starR, cy)
                            quadraticTo(cx - starR * 0.25f, cy - starR * 0.25f, cx, cy - starR)
                            close()
                        }
                        drawCircle(
                            color = sculptureMainLip,
                            radius = starR * 1.05f,
                            center = Offset(cx, cy + 2.dp.toPx())
                        )
                        drawPath(starPath, sculptureAccentFace)
                    }

                    ClayPermissionSculptureType.SYSTEM_BOOT_STARTUP,
                    ClayPermissionSculptureType.SYSTEM_BOOT_IGNITION -> {
                        // 1. Symmetrical 3D Circular Power Button Medallion Base
                        val plinthR = w * 0.30f
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.25f),
                            radius = plinthR,
                            center = Offset(cx, cy + 4.2.dp.toPx())
                        )
                        drawCircle(
                            color = sculptureMainLip,
                            radius = plinthR,
                            center = Offset(cx, cy + 2.8.dp.toPx())
                        )
                        drawCircle(
                            color = sculptureMainFace,
                            radius = plinthR,
                            center = Offset(cx, cy)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.18f),
                            radius = plinthR - 0.8.dp.toPx(),
                            center = Offset(cx, cy),
                            style = Stroke(width = 1.2.dp.toPx())
                        )

                        // 2. Layered 3D Open Circular Power Ring (Centered with 12 o'clock Ignition Gap)
                        val ringR = w * 0.155f
                        val ringCy = cy + 1.5.dp.toPx()
                        val ringStroke = 3.8.dp.toPx()

                        drawArc(
                            color = sculptureMainLip,
                            startAngle = -52f,
                            sweepAngle = 284f,
                            useCenter = false,
                            topLeft = Offset(cx - ringR, ringCy - ringR + 2.dp.toPx()),
                            size = Size(ringR * 2f, ringR * 2f),
                            style = Stroke(width = ringStroke, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = sculptureAccentFace,
                            startAngle = -52f,
                            sweepAngle = 284f,
                            useCenter = false,
                            topLeft = Offset(cx - ringR, ringCy - ringR),
                            size = Size(ringR * 2f, ringR * 2f),
                            style = Stroke(width = ringStroke, cap = StrokeCap.Round)
                        )

                        // 3. Raised 3D Vertical Ignition Bar at 12 o'clock
                        val barW = 4.dp.toPx()
                        val barH = w * 0.19f
                        val barLeft = cx - barW / 2f
                        val barTop = cy - w * 0.205f
                        val barCorner = CornerRadius(barW / 2f, barW / 2f)

                        drawRoundRect(
                            color = sculptureMainLip,
                            topLeft = Offset(barLeft, barTop + 2.dp.toPx()),
                            size = Size(barW, barH),
                            cornerRadius = barCorner
                        )
                        drawRoundRect(
                            color = sculptureAccentFace,
                            topLeft = Offset(barLeft, barTop),
                            size = Size(barW, barH),
                            cornerRadius = barCorner
                        )
                    }

                    ClayPermissionSculptureType.EXPORT_DATA_TRAY -> {
                        // 3D Sculpted Clay Export Tray Base
                        val trayW = w * 0.56f
                        val trayH = 12.dp.toPx()
                        val trayLeft = (w - trayW) / 2f
                        val trayTop = h * 0.58f

                        drawRoundRect(
                            color = Color.Black.copy(alpha = 0.22f),
                            topLeft = Offset(trayLeft, trayTop + 4.dp.toPx()),
                            size = Size(trayW, trayH),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                        drawRoundRect(
                            color = sculptureMainLip,
                            topLeft = Offset(trayLeft, trayTop + 2.5.dp.toPx()),
                            size = Size(trayW, trayH),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                        drawRoundRect(
                            color = sculptureMainFace,
                            topLeft = Offset(trayLeft, trayTop),
                            size = Size(trayW, trayH),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )

                        // Two indicator clay beads on tray front
                        drawCircle(
                            color = sculptureAccentFace,
                            radius = 2.4.dp.toPx(),
                            center = Offset(cx - 6.dp.toPx(), trayTop + trayH / 2f)
                        )
                        drawCircle(
                            color = AuraTactileColors.CreamText,
                            radius = 2.4.dp.toPx(),
                            center = Offset(cx + 6.dp.toPx(), trayTop + trayH / 2f)
                        )

                        // 3D Extruded Upward Clay Arrow above the tray
                        fun buildArrow(yOffset: Float): Path = Path().apply {
                            moveTo(cx, h * 0.18f + yOffset)
                            lineTo(cx + w * 0.21f, h * 0.39f + yOffset)
                            lineTo(cx + w * 0.09f, h * 0.39f + yOffset)
                            lineTo(cx + w * 0.09f, h * 0.54f + yOffset)
                            lineTo(cx - w * 0.09f, h * 0.54f + yOffset)
                            lineTo(cx - w * 0.09f, h * 0.39f + yOffset)
                            lineTo(cx - w * 0.21f, h * 0.39f + yOffset)
                            close()
                        }

                        drawPath(buildArrow(3.5.dp.toPx()), Color.Black.copy(alpha = 0.20f))
                        drawPath(buildArrow(2.2.dp.toPx()), sculptureMainLip)
                        drawPath(buildArrow(0f), sculptureAccentFace)
                    }
                }
            }
        }
    }
}

/**
 * Tactile Claymorphic Color Palette Selector Card.
 * Renders an extruded 3D clay card with consistent bottom depth, beveled edges, and an embedded
 * dual-tone tactile clay preview capsule showcasing the primary accent and secondary container hues.
 */
@Composable
fun Clay3DColorMedallion(
    name: String,
    primaryOrbColor: Color,
    secondaryRingColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clayPalette = LocalAuraClayPalette.current

    // Selected: Card takes on the respective palette's accent color with extruded bottom depth
    // Unselected: Card matches the dark container styling of inactive theme mode toggles
    val selectedLipColor = Color(
        red = (primaryOrbColor.red * 0.68f).coerceIn(0f, 1f),
        green = (primaryOrbColor.green * 0.68f).coerceIn(0f, 1f),
        blue = (primaryOrbColor.blue * 0.68f).coerceIn(0f, 1f),
        alpha = 1f
    )
    val cardFace = if (selected) primaryOrbColor else clayPalette.recessedTrackBg
    val cardLip = if (selected) selectedLipColor else clayPalette.recessedTrackBorder
    val lipDepth = if (selected) 5.dp else 3.5.dp
    val cardBorder = if (selected) Color.White.copy(alpha = 0.35f) else clayPalette.recessedTrackBorder
    val labelColor = if (selected) AuraTactileColors.CreamText else clayPalette.unselectedTextColor

    Tactile3DPebbleCard(
        faceColor = cardFace,
        lipColor = cardLip,
        lipDepth = lipDepth,
        cornerRadius = 16.dp,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
        customBorderColor = cardBorder,
        borderWidth = 1.2.dp,
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tactile Dual-Tone Clay Capsule Preview (Primary Accent + Secondary Swatch)
            Canvas(
                modifier = Modifier.size(width = 38.dp, height = 15.dp)
            ) {
                val w = size.width
                val h = size.height
                val r = h / 2f
                val cr = CornerRadius(r, r)

                // 1. Bottom 3D clay shadow for the capsule
                drawRoundRect(
                    color = Color.Black.copy(alpha = if (selected) 0.30f else 0.18f),
                    topLeft = Offset(0f, 2.dp.toPx()),
                    size = Size(w, h),
                    cornerRadius = cr
                )

                // 2. Base dual-tone capsule with left & right color fields
                val halfW = w / 2f
                val leftPath = Path().apply {
                    moveTo(r, 0f)
                    lineTo(halfW, 0f)
                    lineTo(halfW, h)
                    lineTo(r, h)
                    arcTo(androidx.compose.ui.geometry.Rect(0f, 0f, h, h), 90f, 180f, false)
                    close()
                }
                val rightPath = Path().apply {
                    moveTo(halfW, 0f)
                    lineTo(w - r, 0f)
                    arcTo(androidx.compose.ui.geometry.Rect(w - h, 0f, w, h), -90f, 180f, false)
                    lineTo(halfW, h)
                    close()
                }

                drawPath(leftPath, primaryOrbColor)
                drawPath(rightPath, secondaryRingColor)

                // Center dividing groove
                drawLine(
                    color = Color.Black.copy(alpha = 0.25f),
                    start = Offset(halfW, 0f),
                    end = Offset(halfW, h),
                    strokeWidth = 1.2.dp.toPx()
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.25f),
                    start = Offset(halfW + 0.8.dp.toPx(), 0f),
                    end = Offset(halfW + 0.8.dp.toPx(), h),
                    strokeWidth = 0.8.dp.toPx()
                )

                // 3. Tactile top specular bevel highlight
                drawRoundRect(
                    color = Color.White.copy(alpha = if (selected) 0.42f else 0.22f),
                    topLeft = Offset.Zero,
                    size = Size(w, h),
                    cornerRadius = cr,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            Text(
                text = name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    color = labelColor
                )
            )
        }
    }
}
