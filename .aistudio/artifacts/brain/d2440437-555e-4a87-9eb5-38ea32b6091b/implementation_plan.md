# Instant Cold-Start & Jitter-Free Launch Implementation Plan

A comprehensive plan to eliminate startup flicker, layout shifts, late theme popping, and piece-by-piece UI construction during cold launch.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> Based on your clarifying choices:
> 1. **Frame Blocking & Readiness**: The app will block its initial frame until saved user preferences, theme, and battery status are fully resolved, guaranteeing that the very first frame displayed is 100% complete.
> 2. **Synchronous Hardware Battery Reading**: Real-time battery hardware status will be retrieved synchronously at app launch using Android's cached sticky broadcast (`ACTION_BATTERY_CHANGED`) with a strict safety timeout (max 250ms) to ensure the app never hangs or stutters under any circumstances.
> 3. **Preserved Visuals & Zero Animations at Launch**: No animations will play during cold start. The home screen will immediately appear in its final, calm state with your saved palette, thresholds, and battery metrics.

---

## 1. Problem Diagnosis & Root Causes

1. **Fallback Dummy States in Compose**:
   - `batteryStatusFlow()` initialized with `LiveBatteryState(level = 80, isCharging = false, temperature = 34.3f, voltage = 4107)`. When the true hardware state arrived moments later, the battery hero medallion, charging icon, and metric chips abruptly morphed.
   - `themeModeFlow` and `themePaletteFlow` initialized with `"System"` and `"AuraEarthy"`. If the user configured Dark mode or a different palette (e.g., Slate or Sunburst), the app drew in Earthy Light on frame 1, then flipped to Dark/Slate on frame 2.
   - Room database `logs` initialized with `emptyList()`, causing the 25h activity ribbon to render empty and expand after disk I/O resolved, pushing other dashboard content down.

2. **Window Preview & Theme Flash**:
   - `themes.xml` had `android:windowDisablePreview` set to `true`. This prevented the Android Window Manager from displaying a preview window during process startup, resulting in an empty white/black flash before Compose rendered.
   - Window status and decor backgrounds were applied in Compose `SideEffect`, causing the OS window to lag behind Compose drawing.

---

## 2. Proposed Architecture & Solution

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              APP COLD START                                 │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
            ┌──────────────────────────┴──────────────────────────┐
            ▼                                                     ▼
┌───────────────────────────────┐             ┌───────────────────────────────┐
│ Synchronous Battery Telemetry │             │ Pre-warmed DataStore & Room   │
│ registerReceiver(null,        │             │ withTimeoutOrNull(250ms)      │
│   ACTION_BATTERY_CHANGED)     │             │ - Theme mode & palette        │
│   ↳ Immediate 0ms retrieval   │             │ - Thresholds & toggles        │
└───────────┬───────────────────┘             │ - 25h Room log snapshot       │
            │                                 └───────────────┬───────────────┘
            └──────────────────────────┬──────────────────────┘
                                       ▼
             ┌──────────────────────────────────────────────────┐
             │ Composite `StartupPayload` Fully Assembled       │
             └─────────────────────────┬────────────────────────┘
                                       ▼
             ┌──────────────────────────────────────────────────┐
             │ Window Decor & Background set synchronously      │
             │ android:windowDisablePreview removed             │
             └─────────────────────────┬────────────────────────┘
                                       ▼
             ┌──────────────────────────────────────────────────┐
             │ setContent { ... }                               │
             │ Initial states = Pre-warmed `StartupPayload`     │
             │   ↳ Zero layout shift                            │
             │   ↳ Zero color/theme flip                        │
             │   ↳ Zero metric morphing                         │
             │   ↳ Perfect, calm first frame                    │
             └──────────────────────────────────────────────────┘
```

---

## 3. Detailed File Changes

### A. `app/src/main/java/com/example/TelemetryDataStore.kt`
- Create `data class StartupPayload(...)` holding all saved settings (theme mode, palette, thresholds, toggles, quiet hours, ringtone URI/name, snooze timestamps) and default fallback values.
- Add `suspend fun loadStartupPayload(context: Context): StartupPayload` that reads `dataStore.data.first()` safely and populates `StartupPayload`.

### B. `app/src/main/java/com/example/BatteryLogDao.kt`
- Add a synchronous or direct suspend snapshot query:
  ```kotlin
  @Query("SELECT * FROM battery_logs WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
  suspend fun getRecentLogsSnapshot(sinceTimestamp: Long): List<BatteryLogEntity>
  ```
  This allows fetching the 25-hour activity logs immediately during pre-warming without waiting for the Flow collector.

### C. `app/src/main/java/com/example/MainActivity.kt`
- Add `fun readInitialBatteryState(context: Context): LiveBatteryState`:
  Synchronously queries `context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))` and falls back to `BatteryManager.getIntProperty(...)`. Completes in < 1ms.
- In `onCreate()`:
  - Read initial battery state synchronously.
  - Run bounded pre-warm `runBlocking(Dispatchers.IO) { withTimeoutOrNull(250L) { ... } }` to fetch DataStore preferences and recent logs. If the timeout triggers (rare), fall back to safe defaults so the app never hangs.
  - Synchronously apply the matching window background color drawable to `window.decorView` and `window.setBackgroundDrawable(...)` before `setContent`.
  - Pass the resolved `StartupPayload` into `setContent` and `MainNavigationRouter`.
  - Supply the pre-warmed values as `initialValue` for `collectAsStateWithLifecycle` and `collectAsState`.
  - Ensure zero entry animation plays on the first frame.

### D. `app/src/main/res/values/themes.xml`
- Remove `<item name="android:windowDisablePreview">true</item>`.
- Set `android:windowBackground` and `android:colorBackground` to the soothing parchment background (`@color/parchment_background_light`) to ensure seamless continuity from the OS launch window into Compose.

---

## 4. Verification Plan

1. **Compilation & Syntax**: Run `compile_applet` to ensure error-free compilation with Kotlin DSL and AGP.
2. **Cold Start Latency & Timeout Safety**: Verify that the pre-warming coroutine operates with strict timeout boundaries (250ms) and that `readInitialBatteryState` executes in 0ms without UI thread blocking.
3. **Frame 0 State Consistency**: Verify that `themeMode`, `themePalette`, `liveBatteryState`, and `logs` match the user's saved state on the very first rendered frame.
4. **Visual & Behavioral Invariance**: Verify that all screens, controls, Xiaomi ringtone store integration, and tactile styling remain completely intact.
