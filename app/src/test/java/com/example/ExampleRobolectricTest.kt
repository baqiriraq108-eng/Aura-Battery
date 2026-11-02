package com.example

import android.content.Context
import android.os.BatteryManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.ui.StarTopNavigationBar
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AuraBattery", appName)
  }

  @Test
  fun `settings screen renders alert duration snooze and time options without crashing`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dataStore = TelemetryDataStore(context)

    composeTestRule.setContent {
      MyApplicationTheme {
        SettingsScreen(
          telemetryDataStore = dataStore,
          currentRoute = "settings",
          onNavigate = {},
          isDark = false
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Background Telemetry").assertIsDisplayed()
    composeTestRule.onNodeWithText("Critical Thresholds").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("80%").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("20%").performScrollTo().assertIsDisplayed()

    // Verify 3D clay Alert Duration and Snooze Duration selectors
    composeTestRule.onNodeWithTag("alert_duration_30s").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("alert_duration_60s").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("alert_duration_120s").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("snooze_duration_5m").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("snooze_duration_10m").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("snooze_duration_15m").performScrollTo().assertIsDisplayed()

    // Verify consolidated single ringtone card and no duplicate picker button
    composeTestRule.onNodeWithTag("ringtone_current_pod").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("ringtone_picker_button").assertDoesNotExist()

    // Verify synchronized Display Indicators including new Cycles toggle
    composeTestRule.onNodeWithTag("toggle_indicator_voltage").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("toggle_indicator_health").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("toggle_indicator_temp").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("toggle_indicator_source").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("toggle_indicator_cycles").performScrollTo().assertIsDisplayed()

    // Verify CSV / Battery info share tray is completely removed
    composeTestRule.onNodeWithTag("export_data_tray").assertDoesNotExist()

    // Scroll to and tap FROM pod to expand Quiet Hours time selection tray with cascading open animation
    composeTestRule.onNodeWithTag("quiet_from_pod").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("quiet_time_options_tray").performScrollTo().assertIsDisplayed()

    // Tap DONE to close tray with unified close animation
    composeTestRule.onNodeWithTag("quiet_time_options_done").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("quiet_time_options_tray").assertDoesNotExist()
  }

  @Test
  fun `threshold alerts support snooze and auto dismiss on charger action`() {
    composeTestRule.mainClock.autoAdvance = false
    var batteryState by mutableStateOf(LiveBatteryState(level = 65, isCharging = true))

    composeTestRule.setContent {
      MyApplicationTheme {
        DashboardScreen(
          liveState = batteryState,
          highThreshold = 80,
          lowThreshold = 20,
          snoozeDurationMinutes = 5
        )
      }
    }

    composeTestRule.mainClock.advanceTimeBy(100)
    composeTestRule.onNodeWithTag("active_threshold_visual_alert").assertDoesNotExist()

    // Reach High Limit while charging -> triggers High Battery Alert
    batteryState = LiveBatteryState(level = 80, isCharging = true)
    composeTestRule.mainClock.advanceTimeBy(100)
    composeTestRule.onNodeWithText("High Battery Alert").assertIsDisplayed()
    composeTestRule.onNodeWithText(
      "Battery reached the maximum limit. Unplug the charger now to dismiss this alert."
    ).assertIsDisplayed()

    // Tap inline Snooze button -> immediately silences and dismisses visual alert
    composeTestRule.onNodeWithTag("visual_alert_snooze_button").performClick()
    composeTestRule.mainClock.advanceTimeBy(100)
    composeTestRule.onNodeWithTag("active_threshold_visual_alert").assertDoesNotExist()

    // Unplug charger and drop to Low Limit -> triggers Low Battery Alert
    batteryState = LiveBatteryState(level = 20, isCharging = false)
    composeTestRule.mainClock.advanceTimeBy(100)
    composeTestRule.onNodeWithText("Low Battery Alert").assertIsDisplayed()
    composeTestRule.onNodeWithText(
      "Battery dropped to the minimum limit. Connect the charger now to dismiss this alert."
    ).assertIsDisplayed()

    // Connect charger -> immediately auto-dismisses Low Battery Alert
    batteryState = LiveBatteryState(level = 20, isCharging = true)
    composeTestRule.mainClock.advanceTimeBy(100)
    composeTestRule.onNodeWithTag("active_threshold_visual_alert").assertDoesNotExist()
  }

  @Test
  fun `dashboard indicators dynamically reflow and remove restricted or toggled off tiles`() {
    var showVolt by mutableStateOf(true)
    var showHealth by mutableStateOf(true)
    var showCycles by mutableStateOf(true)
    var batteryState by mutableStateOf(
      LiveBatteryState(
        level = 75,
        isCharging = false,
        temperature = 33.5f,
        voltage = 4120,
        health = BatteryManager.BATTERY_HEALTH_GOOD,
        cycleCount = 142
      )
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        DashboardScreen(
          liveState = batteryState,
          showVolt = showVolt,
          showHealth = showHealth,
          showTemp = true,
          showChargeMethod = true,
          showCycles = showCycles
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dashboard_tile_voltage").assertExists()
    composeTestRule.onNodeWithTag("dashboard_tile_health").assertExists()
    composeTestRule.onNodeWithTag("dashboard_tile_temp").assertExists()
    composeTestRule.onNodeWithTag("dashboard_tile_source").assertExists()
    composeTestRule.onNodeWithTag("dashboard_tile_cycles").assertExists()

    // Toggle off Cycles indicator -> tile is removed dynamically
    showCycles = false
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dashboard_tile_cycles").assertDoesNotExist()

    // Toggle off Health indicator -> tile is removed and remaining tiles reflow
    showHealth = false
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dashboard_tile_health").assertDoesNotExist()
    composeTestRule.onNodeWithTag("dashboard_tile_voltage").assertExists()

    // Restrict hardware voltage (null) -> Voltage tile is removed from composition automatically
    batteryState = batteryState.copy(voltage = null)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dashboard_tile_voltage").assertDoesNotExist()
    composeTestRule.onNodeWithTag("dashboard_tile_temp").assertExists()
    composeTestRule.onNodeWithTag("dashboard_tile_source").assertExists()
  }

  @Test
  fun `diagnostics screen collapses SOH arch when restricted by OS and renders when available`() {
    var sohValue by mutableStateOf<Double?>(null)

    composeTestRule.setContent {
      MyApplicationTheme {
        AdvancedDiagnosticsScreenContent(
          logs = emptyList(),
          liveState = LiveBatteryState(level = 78, isCharging = false),
          stateOfHealth = sohValue
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Charge Variation Curve").assertIsDisplayed()
    composeTestRule.onNodeWithText("Battery Health Relief").performScrollTo().assertIsDisplayed()
    // When stateOfHealth is null (restricted by OS), SOH arch is completely removed from composition
    composeTestRule.onNodeWithTag("diagnostics_soh_arch").assertDoesNotExist()

    // When genuine hardware SOH is reported, SOH arch appears with real percentage
    sohValue = 96.0
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("diagnostics_soh_arch").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("96%").assertIsDisplayed()
  }

  @Test
  fun `top navigation bar renders all tabs with distinct identities`() {
    var selectedRoute by mutableStateOf("dashboard")
    composeTestRule.setContent {
      MyApplicationTheme {
        StarTopNavigationBar(
          currentRoute = selectedRoute,
          onNavigate = { selectedRoute = it }
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_dashboard_tab").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_diagnostics_tab").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_settings_tab").assertIsDisplayed()

    // Navigate to Diagnostics
    composeTestRule.onNodeWithTag("nav_diagnostics_tab").performClick()
    assertEquals("diagnostics", selectedRoute)

    // Navigate to Settings
    composeTestRule.onNodeWithTag("nav_settings_tab").performClick()
    assertEquals("settings", selectedRoute)
  }
}
