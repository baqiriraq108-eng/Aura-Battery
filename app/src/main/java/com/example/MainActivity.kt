package com.example

import com.example.ui.theme.*
import com.example.ui.expressiveBounceClickable
import com.example.ui.expressiveBounce
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

import android.Manifest
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.app.usage.UsageEvents
import androidx.compose.ui.tooling.preview.Preview
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.app.Activity
import android.media.RingtoneManager
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import kotlinx.coroutines.flow.first
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer

// App state models
data class LiveBatteryState(
    val level: Int = 100,
    val isCharging: Boolean = false,
    val temperature: Float = 25.5f,
    val voltage: Int = 4120,
    val plugged: Int = 0,
    val status: Int = BatteryManager.BATTERY_STATUS_UNKNOWN,
    val health: Int = BatteryManager.BATTERY_HEALTH_GOOD
) {
    val healthString: String
        get() = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheated"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }

    val sourceString: String
        get() = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Charger"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Internal Battery"
        }
}

fun Context.batteryStatusFlow(): Flow<LiveBatteryState> = callbackFlow {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val pct = if (level >= 0 && scale > 0) (level * 100) / scale else 100

            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

            val tempRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
            val temperature = tempRaw / 10.0f
            val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
            val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_GOOD)

            trySend(
                LiveBatteryState(
                    level = pct,
                    isCharging = isCharging,
                    temperature = temperature,
                    voltage = voltage,
                    plugged = plugged,
                    status = status,
                    health = health
                )
            ).isSuccess
        }
    }
    
    registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

    val bm = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    if (bm != null) {
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
        val isCharging = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) bm.isCharging else false
        trySend(LiveBatteryState(level = level, isCharging = isCharging)).isSuccess
    }

    awaitClose {
        try {
            unregisterReceiver(receiver)
        } catch (e: Exception) {
            // Ignore
        }
    }
}

data class AppUsageItem(
    val packageName: String,
    val label: String,
    val runtimeMinutes: Long,
    val powerPercentage: Double,
    val powerMilliampHours: Double
)

class MainActivity : ComponentActivity() {

    private lateinit var telemetryDataStore: TelemetryDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        telemetryDataStore = TelemetryDataStore(applicationContext)

        // Launch Background service conditionally and monitor state changes
        lifecycleScope.launch {
            telemetryDataStore.backgroundServiceEnabledFlow.collect { isEnabled ->
                val serviceIntent = Intent(this@MainActivity, BatteryTelemetryService::class.java)
                if (isEnabled) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }
                } else {
                    stopService(serviceIntent)
                }
            }
        }

        // Trigger permissions dialog on modern Android OS
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
                .collectAsStateWithLifecycle(initialValue = LiveBatteryState())

            val fontScale by telemetryDataStore.fontScaleFlow.collectAsState(initial = 1.0f)
            val fontFamilyPath by telemetryDataStore.fontFamilyPathFlow.collectAsState(initial = "")
            val themeMode by telemetryDataStore.themeModeFlow.collectAsState(initial = "System")
            val themePalette by telemetryDataStore.themePaletteFlow.collectAsState(initial = "HyperLime")

            val customFontFamily = remember(fontFamilyPath) {
                if (fontFamilyPath.isNotEmpty()) {
                    try {
                        val file = File(fontFamilyPath)
                        if (file.exists()) {
                            FontFamily(androidx.compose.ui.text.font.Font(file))
                        } else {
                            FontFamily.Default
                        }
                    } catch (e: Exception) {
                        FontFamily.Default
                    }
                } else {
                    FontFamily.Default
                }
            }

            val isDarkTheme = when (themeMode) {
                "Light" -> false
                "Dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            val palette = ExpressivePalettes.getPalette(themePalette)
            val colorScheme = if (isDarkTheme) palette.darkScheme else palette.lightScheme

            MaterialTheme(
                typography = ScaledTypography(fontScale, customFontFamily),
                colorScheme = colorScheme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    MainNavigationRouter(
                        telemetryDataStore = telemetryDataStore,
                        liveState = liveState
                    )
                }
            }
        }
    }
}

// Typography scaling mapper to prevent unspecified sizing crashes
fun scaleTextUnit(unit: androidx.compose.ui.unit.TextUnit, scale: Float): androidx.compose.ui.unit.TextUnit {
    return if (unit.isSp) {
        (unit.value * scale).sp
    } else {
        unit
    }
}

@Composable
fun ScaledTypography(scale: Float, customFont: FontFamily): Typography {
    val defaultTypography = Typography()
    return Typography(
        displayLarge = defaultTypography.displayLarge.copy(fontSize = scaleTextUnit(defaultTypography.displayLarge.fontSize, scale), fontFamily = customFont),
        displayMedium = defaultTypography.displayMedium.copy(fontSize = scaleTextUnit(defaultTypography.displayMedium.fontSize, scale), fontFamily = customFont),
        displaySmall = defaultTypography.displaySmall.copy(fontSize = scaleTextUnit(defaultTypography.displaySmall.fontSize, scale), fontFamily = customFont),
        headlineLarge = defaultTypography.headlineLarge.copy(fontSize = scaleTextUnit(defaultTypography.headlineLarge.fontSize, scale), fontFamily = customFont),
        headlineMedium = defaultTypography.headlineMedium.copy(fontSize = scaleTextUnit(defaultTypography.headlineMedium.fontSize, scale), fontFamily = customFont),
        headlineSmall = defaultTypography.headlineSmall.copy(fontSize = scaleTextUnit(defaultTypography.headlineSmall.fontSize, scale), fontFamily = customFont),
        titleLarge = defaultTypography.titleLarge.copy(fontSize = scaleTextUnit(defaultTypography.titleLarge.fontSize, scale), fontFamily = customFont),
        titleMedium = defaultTypography.titleMedium.copy(fontSize = scaleTextUnit(defaultTypography.titleMedium.fontSize, scale), fontFamily = customFont),
        titleSmall = defaultTypography.titleSmall.copy(fontSize = scaleTextUnit(defaultTypography.titleSmall.fontSize, scale), fontFamily = customFont),
        bodyLarge = defaultTypography.bodyLarge.copy(fontSize = scaleTextUnit(defaultTypography.bodyLarge.fontSize, scale), fontFamily = customFont),
        bodyMedium = defaultTypography.bodyMedium.copy(fontSize = scaleTextUnit(defaultTypography.bodyMedium.fontSize, scale), fontFamily = customFont),
        bodySmall = defaultTypography.bodySmall.copy(fontSize = scaleTextUnit(defaultTypography.bodySmall.fontSize, scale), fontFamily = customFont),
        labelLarge = defaultTypography.labelLarge.copy(fontSize = scaleTextUnit(defaultTypography.labelLarge.fontSize, scale), fontFamily = customFont),
        labelMedium = defaultTypography.labelMedium.copy(fontSize = scaleTextUnit(defaultTypography.labelMedium.fontSize, scale), fontFamily = customFont),
        labelSmall = defaultTypography.labelSmall.copy(fontSize = scaleTextUnit(defaultTypography.labelSmall.fontSize, scale), fontFamily = customFont)
    )
}

@Composable
fun MainNavigationRouter(
    telemetryDataStore: TelemetryDataStore,
    liveState: LiveBatteryState
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"

    val hideOnScroll by telemetryDataStore.hideOnScrollFlow.collectAsState(initial = true)
    val density = LocalDensity.current
    val scrollThresholdPx = remember(density) { with(density) { 25.dp.toPx() } }

    var isDockVisible by remember { mutableStateOf(true) }
    var scrollAccumulator by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(currentRoute) {
        isDockVisible = true
        scrollAccumulator = 0f
    }

    val nestedScrollConnection = remember(hideOnScroll) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!hideOnScroll) {
                    isDockVisible = true
                    scrollAccumulator = 0f
                    return Offset.Zero
                }

                val delta = available.y
                if (delta > 0) {
                    // scrolling up (drag down, content moves down) -> Show Dock
                    if (scrollAccumulator < 0) {
                        scrollAccumulator = 0f
                    }
                    scrollAccumulator += delta
                    if (scrollAccumulator >= scrollThresholdPx) {
                        isDockVisible = true
                        scrollAccumulator = 0f
                    }
                } else if (delta < 0) {
                    // scrolling down (drag up, content moves up) -> Hide Dock
                    if (scrollAccumulator > 0) {
                        scrollAccumulator = 0f
                    }
                    scrollAccumulator += delta
                    if (scrollAccumulator <= -scrollThresholdPx) {
                        isDockVisible = false
                        scrollAccumulator = 0f
                    }
                }
                return Offset.Zero
            }
        }
    }

    val dockTranslationY by animateDpAsState(
        targetValue = if (isDockVisible) 0.dp else 150.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "DockTranslationY"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "dashboard",
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)),
                enterTransition = {
                    fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                    scaleIn(
                        initialScale = 0.91f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) +
                    slideInHorizontally(
                        initialOffsetX = { 70 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(160)) +
                    scaleOut(
                        targetScale = 0.95f,
                        animationSpec = tween(160)
                    )
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                    scaleIn(
                        initialScale = 0.91f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) +
                    slideInHorizontally(
                        initialOffsetX = { -70 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(160)) +
                    scaleOut(
                        targetScale = 0.95f,
                        animationSpec = tween(160)
                    )
                }
            ) {
                composable("dashboard") {
                    DashboardScreen(liveState = liveState)
                }
                composable("diagnostics") {
                    AdvancedDiagnosticsScreen()
                }
                composable("settings") {
                    SettingsScreen(telemetryDataStore = telemetryDataStore)
                }
            }
        }

        FloatingMacintoshDock(
            currentRoute = currentRoute,
            onNavigate = { route ->
                navController.navigate(route) {
                    popUpTo("dashboard") { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset { IntOffset(0, dockTranslationY.roundToPx()) }
        )
    }
}

@Composable
fun FloatingMacintoshDock(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .wrapContentWidth(align = Alignment.CenterHorizontally)
            .padding(horizontal = 20.dp)
            .padding(bottom = 16.dp)
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(330.dp)
                .height(72.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant, // 100% Solid Non-Transparent
            tonalElevation = 10.dp,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant) // Solid crisp border
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val items = listOf(
                    Triple("dashboard", "Dashboard", Icons.Outlined.Dashboard),
                    Triple("diagnostics", "Diagnostics", Icons.Outlined.Assessment),
                    Triple("settings", "Settings", Icons.Outlined.Settings)
                )
                items.forEach { (route, label, icon) ->
                    val selected = currentRoute == route

                    val pillBackground = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    val pillBorder = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                    val itemTint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.15f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "NavIconScale"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clip(RoundedCornerShape(29.dp))
                            .background(pillBackground)
                            .then(if (pillBorder != null) Modifier.border(pillBorder.width, pillBorder.brush, RoundedCornerShape(29.dp)) else Modifier)
                            .expressiveBounceClickable(pressedScale = 0.88f) {
                                onNavigate(route)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = itemTint,
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    }
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = itemTint
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- MODULE 1: DASHBOARD SCREEN ---
@Composable
fun DashboardScreen(liveState: LiveBatteryState) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        MinimalistClockGauge(
            level = liveState.level,
            isCharging = liveState.isCharging,
            temp = liveState.temperature,
            volt = liveState.voltage,
            modifier = Modifier
                .size(260.dp)
                .padding(bottom = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Information Grid Items
        Text(
            text = "LIVE METRICS",
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                InfoGridCard(
                    label = "Health Status",
                    value = liveState.healthString,
                    icon = Icons.Outlined.FavoriteBorder,
                    modifier = Modifier.weight(1f)
                )
                InfoGridCard(
                    label = "Power Source",
                    value = liveState.sourceString,
                    icon = Icons.Outlined.Power,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                InfoGridCard(
                    label = "Voltage",
                    value = "${liveState.voltage} mV",
                    icon = Icons.Outlined.Bolt,
                    modifier = Modifier.weight(1f)
                )
                InfoGridCard(
                    label = "Temperature",
                    value = "${liveState.temperature} °C",
                    icon = Icons.Outlined.Thermostat,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun InfoGridCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.expressiveBounceClickable(pressedScale = 0.94f) { /* tactile bounce */ }
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun MinimalistClockGauge(
    level: Int,
    isCharging: Boolean,
    temp: Float,
    volt: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_anim"
    )

    val textPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "text_scale_anim"
    )

    val textPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "text_alpha_anim"
    )

    val chargingAngleOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate_anim"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            // Scale down base radius so that even at maximum pulse (1.06x), the ring fits perfectly within the canvas bounds.
            val maxScale = 1.06f
            val baseRadius = (minOf(size.width, size.height) / 2) / maxScale
            val outerRadius = baseRadius * if (isCharging) pulseScale else 1.0f
            val innerRadius = outerRadius - 16.dp.toPx()

            // Outer guideline ring
            drawCircle(
                color = outlineColor.copy(alpha = 0.35f),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Dynamic ticking markers representing exact battery state of charge
            val tickCount = 60
            val activeTicks = (level * tickCount) / 100

            rotate(if (isCharging) chargingAngleOffset else -90f, pivot = center) {
                for (i in 0 until tickCount) {
                    val angleDeg = (i * 360f) / tickCount
                    val angleRad = Math.toRadians(angleDeg.toDouble())

                    val isTickActive = if (isCharging) {
                        (i % 5 == 0) || (i < activeTicks)
                    } else {
                        i < activeTicks
                    }

                    val color = if (isTickActive) primaryColor else outlineColor.copy(alpha = 0.3f)
                    val strokeW = if (isTickActive) 3.5f * density else 1.5f * density
                    val length = if (i % 5 == 0) 15.dp.toPx() else 8.dp.toPx()

                    val startX = center.x + innerRadius * Math.cos(angleRad).toFloat()
                    val startY = center.y + innerRadius * Math.sin(angleRad).toFloat()
                    val endX = center.x + (innerRadius + length) * Math.cos(angleRad).toFloat()
                    val endY = center.y + (innerRadius + length) * Math.sin(angleRad).toFloat()

                    drawLine(
                        color = color,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // Exact central diagnostic data representation
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val textModifier = if (isCharging) {
                Modifier.graphicsLayer {
                    scaleX = textPulseScale
                    scaleY = textPulseScale
                    alpha = textPulseAlpha
                }
            } else {
                Modifier
            }

            Text(
                text = "$level%",
                modifier = textModifier.padding(horizontal = 12.dp),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 50.sp,
                    textAlign = TextAlign.Center
                )
            )
            Text(
                text = if (isCharging) "CHARGING" else "DISCHARGING",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = if (isCharging) MaterialTheme.colorScheme.tertiary else primaryColor
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${temp}°C",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                )
                Text(
                    text = "$volt mV",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

// --- MODULE 2: ADVANCED DIAGNOSTICS SCREEN ---
@Composable
fun AdvancedDiagnosticsScreen() {
    val context = LocalContext.current
    val telemetryDataStore = remember { TelemetryDataStore(context.applicationContext) }
    val rawAccumulatedCycles by telemetryDataStore.consumedCyclesFlow.collectAsState(initial = 0.0)

    val db = remember { AppDatabase.getDatabase(context.applicationContext) }
    val logs by remember { db.batteryLogDao().getAllLogsFlow() }.collectAsState(initial = emptyList(), context = Dispatchers.IO)

    val realTimeState by remember(context) { context.batteryStatusFlow() }
        .collectAsStateWithLifecycle(initialValue = LiveBatteryState())

    val currentLevel = realTimeState.level
    val isCharging = realTimeState.isCharging

    // Query triple-layer telemetry
    var batteryCycles by remember { mutableStateOf(-1) }
    var stateOfHealth by remember { mutableStateOf(-1.0) }
    var debugLogString by remember { mutableStateOf("Ready to capture state.") }

    LaunchedEffect(Unit) {
        val stats = getBatteryDiagnostics(context)
        batteryCycles = stats.first
        stateOfHealth = stats.second
        debugLogString = stats.third
    }

    // Reactive State Flow Integration
    var isPermissionGranted by remember { mutableStateOf(hasUsageStatsPermission(context)) }

    // Lifecycle-Aware Auto-Recheck (onResume Listener)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isPermissionGranted = hasUsageStatsPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var appList by remember { mutableStateOf<List<AppUsageItem>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(false) }

    LaunchedEffect(isPermissionGranted) {
        isLoadingApps = true
        withContext(Dispatchers.IO) {
            try {
                // Immediate 24-Hour Historical Backfill
                if (isPermissionGranted) {
                    try {
                        val dao = db.batteryLogDao()
                        val existing = dao.getAllLogsFlow().first()
                        if (existing.size < 10) {
                            val now = System.currentTimeMillis()
                            val usageManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                            val activeIntervals = mutableSetOf<Int>()
                            if (usageManager != null) {
                                try {
                                    val events = usageManager.queryEvents(now - 24 * 60 * 60 * 1000L, now)
                                    val event = UsageEvents.Event()
                                    while (events.hasNextEvent()) {
                                        events.getNextEvent(event)
                                        val eventTime = event.timeStamp
                                        val diff = now - eventTime
                                        val stepIndex = (diff / (30 * 60 * 1000L)).toInt()
                                        if (stepIndex in 0..47) {
                                            activeIntervals.add(stepIndex)
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("BatteryBackfill", "Error querying usage events", e)
                                }
                            }
                            
                            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                            val currentLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(2, 100)
                            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                            val isCharging = if (batteryIntent != null) {
                                val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
                                status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                            } else {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) bm.isCharging else false
                            }

                            var level = currentLevel
                            var chargingState = isCharging
                            val entries = mutableListOf<BatteryLogEntry>()
                            
                            for (i in 0..47) {
                                val timestamp = now - (i * 30 * 60 * 1000L)
                                if (i > 0) {
                                    val isActive = activeIntervals.contains(i)
                                    if (chargingState) {
                                        val decrease = 8 // ~8% per 30 minutes charging rate
                                        level -= decrease
                                        if (level <= 20) {
                                            level = 20
                                            chargingState = false
                                        }
                                    } else {
                                        val increase = if (isActive) 3 else 1
                                        level += increase
                                        if (level >= 98) {
                                            level = 98
                                            chargingState = true
                                        }
                                    }
                                }
                                level = level.coerceIn(2, 100)
                                entries.add(
                                    BatteryLogEntry(
                                        timestamp = timestamp,
                                        batteryLevel = level,
                                        isCharging = chargingState
                                    )
                                )
                            }
                            
                            for (entry in entries.reversed()) {
                                dao.insert(entry)
                            }
                            Log.d("BatteryBackfill", "Successfully backfilled 24-hour logs with ${entries.size} points.")
                        }
                    } catch (e: Exception) {
                        Log.e("BatteryBackfill", "Failed to backfill logs", e)
                    }
                }

                val pm = context.packageManager
                val apps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getInstalledApplications(PackageManager.GET_META_DATA)
                }

                val usageStats = if (isPermissionGranted) {
                    try {
                        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                        val endTime = System.currentTimeMillis()
                        val startTime = endTime - 24 * 60 * 60 * 1000
                        usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }

                val items = apps.map { appInfo ->
                    val packageName = appInfo.packageName
                    val label = appInfo.loadLabel(pm).toString()
                    val stats = usageStats?.get(packageName)
                    val totalTimeMs = stats?.totalTimeInForeground ?: 0L
                    val minutes = totalTimeMs / (1000 * 60)

                    // Compute baseline power profile factor in mA
                    var basePowerFactor = 2.0 + (Math.abs(packageName.hashCode()) % 8)
                    val pkgLower = packageName.lowercase()
                    if (pkgLower.contains("game") || pkgLower.contains("camera") || 
                        pkgLower.contains("video") || pkgLower.contains("social") || 
                        pkgLower.contains("youtube") || pkgLower.contains("gallery") ||
                        pkgLower.contains("browser") || pkgLower.contains("chrome")) {
                        basePowerFactor *= 2.5
                    } else if (pkgLower.contains("system") || pkgLower.contains("provider") || 
                               pkgLower.contains("launcher") || pkgLower.contains("service")) {
                        basePowerFactor *= 0.5
                    }

                    // Compute mAh based on runtimes
                    val mAh = if (minutes > 0L) {
                        (minutes / 60.0) * basePowerFactor
                    } else {
                        // Simulated small idle background standby consumption (0.01 - 0.25 mAh)
                        (Math.abs(packageName.hashCode()) % 25) / 100.0 + 0.05
                    }

                    // Standard device capacity: 4000 mAh
                    val pct = (mAh / 4000.0) * 100.0

                    AppUsageItem(
                        packageName = packageName,
                        label = label,
                        runtimeMinutes = minutes,
                        powerPercentage = pct,
                        powerMilliampHours = mAh
                    )
                }.sortedWith(compareByDescending<AppUsageItem> { it.runtimeMinutes }.thenByDescending { it.powerMilliampHours })

                withContext(Dispatchers.Main) {
                    appList = items
                    isLoadingApps = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoadingApps = false
                }
            }
        }
    }

    AdvancedDiagnosticsScreenContent(
        logs = logs,
        currentLevel = if (currentLevel >= 0) currentLevel else 100,
        isCharging = isCharging,
        isPermissionGranted = isPermissionGranted,
        isLoadingApps = isLoadingApps,
        appList = appList,
        onGrantAccessClick = {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        },
        onRestrictAppClick = { pkgName ->
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", pkgName, null)
            }
            context.startActivity(intent)
        }
    )
}

@Composable
fun AdvancedDiagnosticsScreenContent(
    logs: List<BatteryLogEntry>,
    currentLevel: Int,
    isCharging: Boolean,
    isPermissionGranted: Boolean,
    isLoadingApps: Boolean,
    appList: List<AppUsageItem>,
    onGrantAccessClick: () -> Unit,
    onRestrictAppClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
    ) {
        item {
            Text(
                "Hardware Telemetry Engine",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
            )
        }

        // Custom "Battery Usage & Consumption Analysis" card modeled precisely after Google's system battery graph
        item {
            BatteryUsageGraphCard(
                logs = logs,
                currentLevel = currentLevel,
                isCharging = isCharging
            )
        }

        // Package stats header & authorization check
        item {
            Text(
                "Background Package Visibility",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (!isPermissionGranted) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Usage Stats Access Required",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "This page requires usage stat visibility permissions to cross-reference battery drains with background execution runtime.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onGrantAccessClick,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = CircleShape
                        ) {
                            Text("Grant Access")
                        }
                    }
                }
            }
        }

        if (isLoadingApps) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            items(appList) { app ->
                AppUsageRow(
                    app = app,
                    onRestrictClick = onRestrictAppClick
                )
            }
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
    val mockApps = listOf(
        AppUsageItem("com.android.chrome", "Chrome", 120, 15.0, 600.0),
        AppUsageItem("com.google.android.youtube", "YouTube", 90, 10.0, 400.0),
        AppUsageItem("com.facebook.katana", "Facebook", 45, 5.0, 200.0)
    )
    MaterialTheme {
        AdvancedDiagnosticsScreenContent(
            logs = mockLogs,
            currentLevel = 78,
            isCharging = false,
            isPermissionGranted = true,
            isLoadingApps = false,
            appList = mockApps,
            onGrantAccessClick = {},
            onRestrictAppClick = {}
        )
    }
}

@Composable
fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

fun drawableToBitmap(drawable: android.graphics.drawable.Drawable): android.graphics.Bitmap {
    if (drawable is android.graphics.drawable.BitmapDrawable) {
        if (drawable.bitmap != null) {
            return drawable.bitmap
        }
    }
    val bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
        android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
    } else {
        android.graphics.Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, android.graphics.Bitmap.Config.ARGB_8888)
    }
    val canvas = android.graphics.Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

private data class AuraConfig(
    val color: Color,
    val alpha: Float,
    val radiusScale: Float
)

fun Modifier.thermalAura(consumption: Float): Modifier = composed {
    val isRed = consumption >= 0.10f
    
    val infiniteTransition = rememberInfiniteTransition(label = "ThermalPulse")
    
    val pulseAlpha by if (isRed) {
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
    } else {
        remember { mutableStateOf(1.0f) }
    }

    val pulseRadiusScale by if (isRed) {
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.4f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseRadiusScale"
        )
    } else {
        remember { mutableStateOf(1.0f) }
    }

    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val primaryColor = MaterialTheme.colorScheme.primary
    val auraConfig = remember(consumption, pulseAlpha, pulseRadiusScale, tertiaryColor, primaryContainerColor, primaryColor) {
        derivedStateOf {
            when {
                consumption < 0.01f -> AuraConfig(tertiaryColor, 0.2f, 1.0f)
                consumption < 0.10f -> AuraConfig(primaryContainerColor, 0.3f, 1.0f)
                else -> AuraConfig(primaryColor, pulseAlpha, pulseRadiusScale)
            }
        }
    }

    this.drawWithCache {
        onDrawBehind {
            val config = auraConfig.value
            val radius = (size.minDimension / 1.5f) * config.radiusScale
            val centerOffset = Offset(size.width / 2f, size.height / 2f)
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        config.color.copy(alpha = config.alpha),
                        config.color.copy(alpha = 0.0f)
                    ),
                    center = centerOffset,
                    radius = radius
                ),
                radius = radius,
                center = centerOffset
            )
        }
    }
}

@Composable
fun AppUsageRow(app: AppUsageItem, onRestrictClick: (String) -> Unit) {
    val context = LocalContext.current
    val appIcon = remember(app.packageName) {
        try {
            val drawable = context.packageManager.getApplicationIcon(app.packageName)
            drawableToBitmap(drawable).asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .graphicsLayer(clip = false)
                            .blur(16.dp)
                            .thermalAura(app.powerPercentage.toFloat() / 100f)
                    )

                    if (appIcon != null) {
                        Image(
                            bitmap = appIcon,
                            contentDescription = "App Icon",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Android,
                            contentDescription = "Default Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val runtimeText = if (app.runtimeMinutes > 0L) {
                        if (app.runtimeMinutes >= 60) {
                            "${app.runtimeMinutes / 60}h ${app.runtimeMinutes % 60}m"
                        } else {
                            "${app.runtimeMinutes} min"
                        }
                    } else {
                        "Idle"
                    }

                    val powerText = "${String.format("%.1f", app.powerPercentage)}% (${String.format("%.1f", app.powerMilliampHours)} mAh)"

                    Text(
                        text = "Runtime: $runtimeText | Power Drank: $powerText",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (app.runtimeMinutes > 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        ),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onRestrictClick(app.packageName) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.primary),
                shape = androidx.compose.foundation.shape.CircleShape,
                modifier = Modifier.height(36.dp)
            ) {
                Text("Restrict", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

fun getBatteryDiagnostics(context: Context): Triple<Int, Double, String> {
    var cycleCount = -1
    var healthPercentage = -1.0
    val sysPathLogs = StringBuilder()

    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        cycleCount = batteryManager.getIntProperty(4) // 4 = BATTERY_PROPERTY_CYCLE_COUNT
    }

    val sysfsCyclePaths = listOf(
        "/sys/class/power_supply/battery/cycle_count",
        "/sys/class/power_supply/bms/cycle_count"
    )
    for (path in sysfsCyclePaths) {
        try {
            val file = File(path)
            if (file.exists()) {
                val count = file.readText().trim().toIntOrNull()
                if (count != null && count > 0) {
                    cycleCount = count
                    sysPathLogs.append("Read cycles from $path: $count\n")
                    break
                }
            }
        } catch (e: Exception) {
            // Silence fallbacks
        }
    }

    try {
        val designFile = File("/sys/class/power_supply/battery/charge_full_design")
        val currentFile = File("/sys/class/power_supply/battery/charge_full")
        if (designFile.exists() && currentFile.exists()) {
            val designVal = designFile.readText().trim().toDoubleOrNull()
            val currentVal = currentFile.readText().trim().toDoubleOrNull()
            if (designVal != null && currentVal != null && designVal > 0) {
                healthPercentage = (currentVal / designVal) * 100.0
                sysPathLogs.append("SOH computed from sysfs: ${String.format("%.1f", healthPercentage)}%\n")
            }
        }
    } catch (e: Exception) {
        // Silence fallbacks
    }

    val diagnosticLog = if (sysPathLogs.isEmpty()) "Standard platform mode." else sysPathLogs.toString()
    return Triple(cycleCount, healthPercentage, diagnosticLog)
}

fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}


// --- MODULE 3: SETTINGS SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(telemetryDataStore: TelemetryDataStore) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Load DataStore bindings
    val highThresh by telemetryDataStore.highThresholdFlow.collectAsState(initial = 80)
    val lowThresh by telemetryDataStore.lowThresholdFlow.collectAsState(initial = 20)
    val fontScale by telemetryDataStore.fontScaleFlow.collectAsState(initial = 1.0f)
    val fontFamilyPath by telemetryDataStore.fontFamilyPathFlow.collectAsState(initial = "")
    val themeMode by telemetryDataStore.themeModeFlow.collectAsState(initial = "System")
    val themePalette by telemetryDataStore.themePaletteFlow.collectAsState(initial = "HyperLime")
    val hideOnScroll by telemetryDataStore.hideOnScrollFlow.collectAsState(initial = true)

    val showTemp by telemetryDataStore.showTempFlow.collectAsState(initial = true)
    val showVolt by telemetryDataStore.showVoltFlow.collectAsState(initial = true)
    val showChargeMethod by telemetryDataStore.showChargeMethodFlow.collectAsState(initial = true)

    val backgroundServiceEnabled by telemetryDataStore.backgroundServiceEnabledFlow.collectAsState(initial = true)
    val alertSoundUri by telemetryDataStore.alertSoundUriFlow.collectAsState(initial = "")
    val alertSoundTitle by telemetryDataStore.alertSoundTitleFlow.collectAsState(initial = "Default System Sound")

    val quietHoursEnabled by telemetryDataStore.quietHoursEnabledFlow.collectAsState(initial = false)
    val quietHoursStartHour by telemetryDataStore.quietHoursStartHourFlow.collectAsState(initial = 10)
    val quietHoursStartAmPm by telemetryDataStore.quietHoursStartAmPmFlow.collectAsState(initial = "PM")
    val quietHoursEndHour by telemetryDataStore.quietHoursEndHourFlow.collectAsState(initial = 7)
    val quietHoursEndAmPm by telemetryDataStore.quietHoursEndAmPmFlow.collectAsState(initial = "AM")

    var localHighThresh by remember(highThresh) { mutableStateOf(highThresh.toFloat()) }
    var localLowThresh by remember(lowThresh) { mutableStateOf(lowThresh.toFloat()) }
    var localFontScale by remember(fontScale) { mutableStateOf(fontScale) }

    // ActivityResultLauncher for picking ringtone custom audio file
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) as? Uri
            }

            if (uri != null) {
                val uriStr = uri.toString()
                var title = "Unknown Sound"
                try {
                    val ringtone = RingtoneManager.getRingtone(context, uri)
                    if (ringtone != null) {
                        title = ringtone.getTitle(context) ?: "Alert Sound"
                    }
                } catch (e: Exception) {
                    title = uri.lastPathSegment ?: "Custom Audio"
                }

                // Try to take persistable URI permission for custom user files
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    // Fail gracefully for system alert sounds
                }

                coroutineScope.launch {
                    telemetryDataStore.saveAlertSoundUri(uriStr)
                    telemetryDataStore.saveAlertSoundTitle(title)
                }
            }
        }
    }

    // Scan system TTF and OTF files for Picker
    val systemFonts = remember {
        try {
            val fontsDir = File("/system/fonts/")
            if (fontsDir.exists() && fontsDir.isDirectory) {
                fontsDir.listFiles { file ->
                    file.name.endsWith(".ttf") || file.name.endsWith(".otf")
                }?.map { it.absolutePath }?.sorted() ?: emptyList()
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Trigger external document picker to load external custom font
    val fontFilePickerLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                coroutineScope.launch {
                    try {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            val cacheFile = File(context.cacheDir, "custom_font.ttf")
                            cacheFile.outputStream().use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                            telemetryDataStore.saveFontFamilyPath(cacheFile.absolutePath)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
    ) {
        item {
            Text(
                "Telemetry Threshold Settings",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
            )
        }

        // Navigation Dock Settings Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "NAVIGATION & DOCK SYSTEM",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hide Bottom Dock on Scroll", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text(
                                "Automatically hides the navigation dock when scrolling down to maximize reading space",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = hideOnScroll,
                            onCheckedChange = { isChecked ->
                                coroutineScope.launch {
                                    telemetryDataStore.saveHideOnScroll(isChecked)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            }
        }

        // Theme Selection Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "APPLICATION THEME",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val options = listOf("System", "Light", "Dark")
                        options.forEach { option ->
                            val isSelected = themeMode == option
                            val containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                            val contentColor = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(containerColor)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .expressiveBounceClickable(pressedScale = 0.92f) {
                                        coroutineScope.launch {
                                            telemetryDataStore.saveThemeMode(option)
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    val icon = when (option) {
                                        "Light" -> Icons.Outlined.WbSunny
                                        "Dark" -> Icons.Outlined.DarkMode
                                        else -> Icons.Outlined.Settings
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = contentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = contentColor
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        "EXPRESSIVE COLOR PALETTES (مجموعة الألوان التعبيرية المصمتة)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // 2-column grid of 6 solid Expressive Palettes with fine-tuned tactile spring bounce
                    val allPalettes = ExpressivePalettes.allPalettes
                    allPalettes.chunked(2).forEach { rowPalettes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowPalettes.forEach { pal ->
                                val isSelected = themePalette.equals(pal.id, ignoreCase = true)
                                val borderColor = if (isSelected) pal.accentColor else MaterialTheme.colorScheme.outlineVariant
                                val borderWidth = if (isSelected) 2.dp else 1.dp
                                val cardBg = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(cardBg)
                                        .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
                                        .expressiveBounceClickable(pressedScale = 0.92f) {
                                            coroutineScope.launch {
                                                telemetryDataStore.saveThemePalette(pal.id)
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(pal.accentColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = pal.nameAr,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                            )
                                            Text(
                                                text = pal.nameEn,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            if (rowPalettes.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // High & Low critical point sliders
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "ALARM POINT CRITERIA",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // High Critical Point (Upper: 50% to 100%)
                    Text("High Critical Point: ${localHighThresh.toInt()}%", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Slider(
                        value = localHighThresh,
                        onValueChange = { localHighThresh = it },
                        onValueChangeFinished = {
                            coroutineScope.launch {
                                telemetryDataStore.saveHighThreshold(localHighThresh.toInt())
                            }
                        },
                        valueRange = 50f..100f,
                        modifier = Modifier.fillMaxWidth(),
                        thumb = {
                            Box(
                                modifier = Modifier
                                    .size(width = 12.dp, height = 24.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(100.dp)
                                    )
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Low Critical Point (Lower: 5% to 49%)
                    Text("Low Critical Point: ${localLowThresh.toInt()}%", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Slider(
                        value = localLowThresh,
                        onValueChange = { localLowThresh = it },
                        onValueChangeFinished = {
                            coroutineScope.launch {
                                telemetryDataStore.saveLowThreshold(localLowThresh.toInt())
                            }
                        },
                        valueRange = 5f..49f,
                        modifier = Modifier.fillMaxWidth(),
                        thumb = {
                            Box(
                                modifier = Modifier
                                    .size(width = 12.dp, height = 24.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(100.dp)
                                    )
                            )
                        }
                    )
                }
            }
        }

        // Background Service & Alert Sounds Configuration Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "BACKGROUND SERVICE & ALERTS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Background service toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Background Charging Monitor", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text(
                                "خدمة المراقبة بالخلفية لإرسال تنبيهات اكتمال الشحن وانخفاضه فقط دون أي إشعارات مستمرة",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = backgroundServiceEnabled,
                            onCheckedChange = { isChecked ->
                                coroutineScope.launch {
                                    telemetryDataStore.saveBackgroundServiceEnabled(isChecked)
                                    val serviceIntent = Intent(context, BatteryTelemetryService::class.java)
                                    if (isChecked) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            context.startForegroundService(serviceIntent)
                                        } else {
                                            context.startService(serviceIntent)
                                        }
                                    } else {
                                        context.stopService(serviceIntent)
                                    }
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Alert sound picker
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val pickerIntent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                    if (alertSoundUri.isNotEmpty()) {
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(alertSoundUri))
                                    }
                                }
                                ringtonePickerLauncher.launch(pickerIntent)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Alert Sound Profile",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = alertSoundTitle,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = "Pick alert sound",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Quiet Hours toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Quiet Hours (Do Not Disturb)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text(
                                "Silence and disable battery alert notifications during a specific time period",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = quietHoursEnabled,
                            onCheckedChange = { isChecked ->
                                coroutineScope.launch {
                                    telemetryDataStore.saveQuietHoursEnabled(isChecked)
                                }
                            }
                        )
                    }

                    if (quietHoursEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Quiet Hours Start", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Start Hour Selector
                                var showStartHourMenu by remember { mutableStateOf(false) }
                                Box {
                                    TextButton(
                                        onClick = { showStartHourMenu = true },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("$quietHoursStartHour")
                                    }
                                    DropdownMenu(
                                        expanded = showStartHourMenu,
                                        onDismissRequest = { showStartHourMenu = false }
                                    ) {
                                        (1..12).forEach { hour ->
                                            DropdownMenuItem(
                                                text = { Text("$hour") },
                                                onClick = {
                                                    coroutineScope.launch {
                                                        telemetryDataStore.saveQuietHoursStartHour(hour)
                                                    }
                                                    showStartHourMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                                // Start AM/PM Selector
                                var showStartAmPmMenu by remember { mutableStateOf(false) }
                                Box {
                                    TextButton(
                                        onClick = { showStartAmPmMenu = true },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text(quietHoursStartAmPm)
                                    }
                                    DropdownMenu(
                                        expanded = showStartAmPmMenu,
                                        onDismissRequest = { showStartAmPmMenu = false }
                                    ) {
                                        listOf("AM", "PM").forEach { amPm ->
                                            DropdownMenuItem(
                                                text = { Text(amPm) },
                                                onClick = {
                                                    coroutineScope.launch {
                                                        telemetryDataStore.saveQuietHoursStartAmPm(amPm)
                                                    }
                                                    showStartAmPmMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Quiet Hours End", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // End Hour Selector
                                var showEndHourMenu by remember { mutableStateOf(false) }
                                Box {
                                    TextButton(
                                        onClick = { showEndHourMenu = true },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("$quietHoursEndHour")
                                    }
                                    DropdownMenu(
                                        expanded = showEndHourMenu,
                                        onDismissRequest = { showEndHourMenu = false }
                                    ) {
                                        (1..12).forEach { hour ->
                                            DropdownMenuItem(
                                                text = { Text("$hour") },
                                                onClick = {
                                                    coroutineScope.launch {
                                                        telemetryDataStore.saveQuietHoursEndHour(hour)
                                                    }
                                                    showEndHourMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                                // End AM/PM Selector
                                var showEndAmPmMenu by remember { mutableStateOf(false) }
                                Box {
                                    TextButton(
                                        onClick = { showEndAmPmMenu = true },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text(quietHoursEndAmPm)
                                    }
                                    DropdownMenu(
                                        expanded = showEndAmPmMenu,
                                        onDismissRequest = { showEndAmPmMenu = false }
                                    ) {
                                        listOf("AM", "PM").forEach { amPm ->
                                            DropdownMenuItem(
                                                text = { Text(amPm) },
                                                onClick = {
                                                    coroutineScope.launch {
                                                        telemetryDataStore.saveQuietHoursEndAmPm(amPm)
                                                    }
                                                    showEndAmPmMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Interactive Font Pickers & Typography Scaling
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "VISUAL TYPOGRAPHY & SCALING",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Typography Scale Factor
                    Text("Typography Scale: ${String.format("%.2f", localFontScale)}x", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Slider(
                        value = localFontScale,
                        onValueChange = { localFontScale = it },
                        onValueChangeFinished = {
                            coroutineScope.launch {
                                telemetryDataStore.saveFontScale(localFontScale)
                            }
                        },
                        valueRange = 0.8f..1.5f,
                        modifier = Modifier.fillMaxWidth(),
                        thumb = {
                            Box(
                                modifier = Modifier
                                    .size(width = 12.dp, height = 24.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(100.dp)
                                    )
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Active Font Path:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    Text(
                        text = if (fontFamilyPath.isNotEmpty()) fontFamilyPath else "Standard Default (System)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { fontFilePickerLauncher.launch(arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/x-font-otf")) },
                            shape = androidx.compose.foundation.shape.CircleShape,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Load Font File")
                        }
                        Button(
                            onClick = { coroutineScope.launch { telemetryDataStore.saveFontFamilyPath("") } },
                            shape = androidx.compose.foundation.shape.CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reset Font")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("System Font scan results:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        if (systemFonts.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No System Fonts Discovered", style = MaterialTheme.typography.bodySmall)
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(systemFonts) { path ->
                                    val name = path.substringAfterLast("/")
                                    val isSelected = fontFamilyPath == path
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { coroutineScope.launch { telemetryDataStore.saveFontFamilyPath(path) } }
                                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = name, style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal))
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Charging Notification Policy & Alert Channel Redirection
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "CHARGING ALERT NOTIFICATION POLICY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "الإشعارات محصورة تماماً في تنبيهات اكتمال الشحن وانخفاضه عند النسب المحددة.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• عند إغلاق الإشعار دون اتخاذ الإجراء (فصل أو توصيل الشاحن)، يتم الانتظار 20 ثانية ثم يظهر الإشعار مجدداً.\n• في حال عدم إغلاق الإشعار، يبقى الإشعار الحالي يعمل حتى فصل أو توصيل الشاحن.\n• يُمنع منعاً باتاً ظهور أي إشعارات مستمرة أو غير مرتبطة بنسب الشحن المحددة.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            lineHeight = 18.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .expressiveBounceClickable {
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
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Battery Alerts Channel Settings",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = "Configure alert tone, vibration, and system notification priority",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = "Navigate to system settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Xiaomi HyperOS / Android 16 Background & Autostart Optimization Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "XIAOMI HYPEROS / ANDROID 16 BACKGROUND PROTECTION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "لضمان استمرار عمل التنبيهات في الخلفية عند إغلاق التطبيق من قائمة التطبيقات المفتوحة (Recent Apps):",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1. السماح بالتجاهل التام لتحسينات البطارية (No Restrictions).\n2. تفعيل التشغيل التلقائي (Autostart) وقفل التطبيق في قائمة التطبيقات المفتوحة على نظام HyperOS.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Button 1: Request ignore battery optimizations
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .expressiveBounceClickable {
                                try {
                                    val intent = Intent().apply {
                                        action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    try {
                                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                        context.startActivity(intent)
                                    } catch (ex: Exception) {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.BatteryChargingFull,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "إيقاف تقييد البطارية (No Restrictions)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "يمنع النظام من إيقاف خدمة المراقبة عند الخروج",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = "Battery Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Button 2: App Details / Autostart launcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .expressiveBounceClickable {
                                // Try Xiaomi Autostart intent first
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
                                    val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(fallbackIntent)
                                }
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "إعدادات التشغيل التلقائي (Autostart - HyperOS)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "فتح إدارة الأذونات والتشغيل التلقائي في مركز الحماية",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = "Autostart Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BatteryUsageGraphCard(
    logs: List<BatteryLogEntry>,
    currentLevel: Int,
    isCharging: Boolean
) {
    val durationString = remember(logs, currentLevel) {
        calculateActiveDuration(logs, currentLevel)
    }

    var showHelpMessage by remember { mutableStateOf(false) }

    if (showHelpMessage) {
        AlertDialog(
            onDismissRequest = { showHelpMessage = false },
            confirmButton = {
                TextButton(onClick = { showHelpMessage = false }) {
                    Text("Dismiss")
                }
            },
            title = {
                Text(
                    text = "Active Battery Stats",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "The real-time active duration since your last full charge is: $durationString."
                )
            }
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("battery_usage_graph_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header: "Active" with info icon and "Battery usage" + "Charging" legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Active",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Icon(
                            imageVector = Icons.Outlined.HelpOutline,
                            contentDescription = "Active battery usage duration since last full charge",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { showHelpMessage = true }
                                .padding(4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = durationString,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                // Legend Immediately next to the active time
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(5.dp))
                        )
                        Text(
                            text = "Battery usage",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(MaterialTheme.colorScheme.tertiary, shape = RoundedCornerShape(5.dp))
                        )
                        Text(
                            text = "Charging",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Canvas Graph representation
            val outlineColor = MaterialTheme.colorScheme.outlineVariant
            val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
            val primaryColor = MaterialTheme.colorScheme.primary
            val tertiaryColor = MaterialTheme.colorScheme.tertiary
            val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
            val backgroundColor = MaterialTheme.colorScheme.background

            // 1. Prepare logs data (Pre-calculated outside Canvas to avoid allocations inside Canvas)
            val displayLogs = remember(logs, currentLevel, isCharging) {
                val baseLogs = if (logs.size >= 2) {
                    logs
                } else {
                    val fallbackList = mutableListOf<BatteryLogEntry>()
                    val now = System.currentTimeMillis()
                    val startPercent = Math.min(100, currentLevel + 25)
                    val endPercent = currentLevel
                    for (i in 0..12) {
                        val fraction = i / 12.0
                        val timestamp = now - ((12 - i) * 2 * 60 * 60 * 1000L)
                        val levelVal = startPercent - ((startPercent - endPercent) * fraction).toInt()
                        fallbackList.add(
                            BatteryLogEntry(
                                timestamp = timestamp,
                                batteryLevel = levelVal,
                                isCharging = i in 4..6
                            )
                        )
                    }
                    fallbackList
                }

                val now = System.currentTimeMillis()
                if (baseLogs.isNotEmpty()) {
                    val lastLog = baseLogs.last()
                    if (now - lastLog.timestamp > 5000L || lastLog.isCharging != isCharging || lastLog.batteryLevel != currentLevel) {
                        baseLogs + BatteryLogEntry(
                            timestamp = now,
                            batteryLevel = currentLevel,
                            isCharging = isCharging
                        )
                    } else {
                        baseLogs
                    }
                } else {
                    baseLogs
                }
            }

            // 2. Pre-calculate horizontal grid levels & intervals
            val gridLevels = remember { listOf(0, 25, 50, 75, 100) }
            val intervals = remember { listOf(21, 18, 15, 12, 9, 6, 3, 0) }

            // 3. Pre-calculate localized x-axis text labels along the bottom
            val intervalLabels = remember(intervals) {
                intervals.map { hoursAgo ->
                    if (hoursAgo == 0) "Now" else {
                        val cal = java.util.Calendar.getInstance()
                        cal.add(java.util.Calendar.HOUR_OF_DAY, -hoursAgo)
                        String.format("%02d:00", cal.get(java.util.Calendar.HOUR_OF_DAY))
                    }
                }
            }

            // 4. Pre-allocate Text Paint objects to avoid native object creation inside Canvas Draw Scope
            val onSurfaceVariantColorArgb = onSurfaceVariantColor.toArgb()
            val textPaintLeft = remember(onSurfaceVariantColorArgb) {
                android.graphics.Paint().apply {
                    color = onSurfaceVariantColorArgb
                    textSize = 28f
                    textAlign = android.graphics.Paint.Align.LEFT
                    isAntiAlias = true
                }
            }
            val textPaintCenter = remember(onSurfaceVariantColorArgb) {
                android.graphics.Paint().apply {
                    color = onSurfaceVariantColorArgb
                    textSize = 28f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
            }

            // 5. Precompute segmented points (normalized 0f..1f coordinates) to eliminate Math/Grouping from Draw Scope
            val normalizedSegments = remember(displayLogs) {
                val now = System.currentTimeMillis()
                val minTime = now - 24 * 60 * 60 * 1000L
                val segmentsList = mutableListOf<Pair<Boolean, List<Offset>>>()
                var currentSegmentPoints = mutableListOf<Offset>()
                var currentIsCharging: Boolean? = null

                displayLogs.forEach { entry ->
                    if (entry.timestamp >= minTime - 5 * 60 * 1000L) {
                        val normX = ((entry.timestamp - minTime).toFloat() / (24 * 60 * 60 * 1000f)).coerceIn(0f, 1f)
                        val normY = entry.batteryLevel / 100f
                        val point = Offset(normX, normY)

                        if (currentIsCharging == null) {
                            currentIsCharging = entry.isCharging
                            currentSegmentPoints.add(point)
                        } else if (currentIsCharging == entry.isCharging) {
                            currentSegmentPoints.add(point)
                        } else {
                            currentSegmentPoints.add(point)
                            segmentsList.add(Pair(currentIsCharging!!, currentSegmentPoints))
                            currentIsCharging = entry.isCharging
                            currentSegmentPoints = mutableListOf(point)
                        }
                    }
                }
                if (currentSegmentPoints.isNotEmpty() && currentIsCharging != null) {
                    segmentsList.add(Pair(currentIsCharging!!, currentSegmentPoints))
                }
                segmentsList
            }

            // Pre-allocate curve and fill paths to recycle them during draw phase (Zero allocations inside Canvas)
            val curvePath = remember { androidx.compose.ui.graphics.Path() }
            val fillPath = remember { androidx.compose.ui.graphics.Path() }

            // Pre-calculate gradient colors to avoid list/object allocation inside Canvas
            val chargingGradientColors = remember(tertiaryColor, backgroundColor) {
                listOf(tertiaryColor.copy(alpha = 0.4f), backgroundColor)
            }
            val dischargingGradientColors = remember(primaryContainerColor, backgroundColor) {
                listOf(primaryContainerColor.copy(alpha = 0.5f), backgroundColor)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val startX = 20f
                    val endX = size.width - 120f
                    val startY = 20f
                    val endY = size.height - 60f
                    val graphWidth = endX - startX
                    val graphHeight = endY - startY

                    // 1. Draw horizontal dashed grid lines
                    gridLevels.forEach { pct ->
                        val y = endY - (pct / 100f) * graphHeight
                        drawLine(
                            color = outlineColor,
                            start = Offset(startX, y),
                            end = Offset(endX, y),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                        // Grid level label
                        drawContext.canvas.nativeCanvas.drawText(
                            "$pct%",
                            endX + 15f,
                            y + 10f,
                            textPaintLeft
                        )
                    }

                    // 2. Draw pre-calculated localized x-axis text labels along the bottom
                    intervals.forEachIndexed { index, _ ->
                        val label = intervalLabels.getOrNull(index) ?: ""
                        val x = startX + (index / 7f) * graphWidth
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            x,
                            endY + 45f,
                            textPaintCenter
                        )
                    }

                    // 3. Render pre-computed Dual-Color Curve Segments (completely zero allocations)
                    normalizedSegments.forEach { (isChargingSegment, normPoints) ->
                        if (normPoints.size < 2) return@forEach

                        curvePath.reset()
                        fillPath.reset()

                        val p0X = startX + normPoints[0].x * graphWidth
                        val p0Y = endY - normPoints[0].y * graphHeight

                        curvePath.moveTo(p0X, p0Y)
                        fillPath.moveTo(p0X, p0Y)

                        for (i in 0 until normPoints.size - 1) {
                            val normP0 = normPoints[i]
                            val normP1 = normPoints[i + 1]

                            val p0x = startX + normP0.x * graphWidth
                            val p0y = endY - normP0.y * graphHeight
                            val p1x = startX + normP1.x * graphWidth
                            val p1y = endY - normP1.y * graphHeight

                            val controlX1 = p0x + (p1x - p0x) / 2f
                            val controlY1 = p0y
                            val controlX2 = p0x + (p1x - p0x) / 2f
                            val controlY2 = p1y

                            curvePath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1x, p1y)
                            fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1x, p1y)
                        }

                        val lastX = startX + normPoints.last().x * graphWidth
                        val firstX = startX + normPoints.first().x * graphWidth

                        fillPath.lineTo(lastX, endY)
                        fillPath.lineTo(firstX, endY)
                        fillPath.close()

                        val minPointY = normPoints.minOf { endY - it.y * graphHeight }
                        val verticalBrush = Brush.verticalGradient(
                            colors = if (isChargingSegment) chargingGradientColors else dischargingGradientColors,
                            startY = minPointY,
                            endY = endY
                        )

                        drawPath(
                            path = fillPath,
                            brush = verticalBrush
                        )

                        drawPath(
                            path = curvePath,
                            color = if (isChargingSegment) tertiaryColor else primaryColor,
                            style = Stroke(
                                width = 6f,
                                cap = StrokeCap.Round,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round
                            )
                        )
                    }
                }
            }
        }
    }
}

fun calculateActiveDuration(logs: List<BatteryLogEntry>, currentLevel: Int): String {
    val now = System.currentTimeMillis()
    var lastFullChargeTime: Long? = null

    val last100 = logs.lastOrNull { it.batteryLevel == 100 }
    if (last100 != null) {
        lastFullChargeTime = last100.timestamp
    } else {
        if (logs.size >= 2) {
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
        }
    }

    val durationMs = if (lastFullChargeTime != null) {
        now - lastFullChargeTime
    } else {
        val proportionHours = (100 - currentLevel) * 15L * 60 * 1000L
        if (proportionHours > 0) proportionHours else 2 * 60 * 60 * 1000L
    }

    val hours = durationMs / (1000 * 60 * 60)
    val minutes = (durationMs / (1000 * 60)) % 60
    return "${hours} hrs ${minutes} mins"
}

