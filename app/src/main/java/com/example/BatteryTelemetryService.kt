package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class BatteryTelemetryService : Service() {

    companion object {
        const val FOREGROUND_CHANNEL_ID = "live_telemetry_channel"
        const val ALERTS_CHANNEL_ID = "battery_alerts_channel"

        const val FOREGROUND_NOTIF_ID = 1001
        const val HIGH_ALERT_ID = 1002
        const val LOW_ALERT_ID = 1003

        const val ACTION_RESTORE_HIGH_ALERT = "com.example.ACTION_RESTORE_HIGH_ALERT"
        const val ACTION_RESTORE_LOW_ALERT = "com.example.ACTION_RESTORE_LOW_ALERT"
        const val ACTION_SNOOZE_ALERT = "com.example.ACTION_SNOOZE_ALERT"

        const val HIGH_ALERT_TITLE = "High Battery Alert"
        const val HIGH_ALERT_BODY = "Battery reached the maximum limit. Unplug the charger now to dismiss this alert."

        const val LOW_ALERT_TITLE = "Low Battery Alert"
        const val LOW_ALERT_BODY = "Battery dropped to the minimum limit. Connect the charger now to dismiss this alert."
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isMonitoring = false

    private lateinit var dataStore: TelemetryDataStore
    private lateinit var notificationManager: NotificationManager
    private lateinit var database: AppDatabase

    // Alarm state tracking
    private var mediaPlayer: MediaPlayer? = null

    // Active threshold alert & cycle/snooze state tracking
    private var isHighAlertActive = false
    private var isLowAlertActive = false
    private var alertCycleJob: Job? = null
    private var snoozeTimerJob: Job? = null

    // Current battery stats
    private var currentLevel = 80
    private var isCharging = false
    private var currentTemp = 25.5f
    private var currentVolt = 4000
    private var currentPlugged = 0

    // Preference cache fields
    private var highThresholdSetting = 80
    private var lowThresholdSetting = 20
    private var quietHoursEnabledSetting = false
    private var quietHoursStartHourSetting = 10
    private var quietHoursStartAmPmSetting = "PM"
    private var quietHoursEndHourSetting = 7
    private var quietHoursEndAmPmSetting = "AM"
    private var customRingtoneUriSetting = ""
    private var alertDurationSecondsSetting = 60
    private var snoozeDurationMinutesSetting = 5
    private var snoozeUntilTimestampSetting = 0L

    private val powerConnectionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_DISCONNECTED -> {
                    isCharging = false
                    // Regardless of snooze or active cycles, immediately dismiss High Alert and clear snooze
                    clearSnoozeState()
                    cancelHighAlert()
                }
                Intent.ACTION_POWER_CONNECTED -> {
                    isCharging = true
                    // Regardless of snooze or active cycles, immediately dismiss Low Alert and clear snooze
                    clearSnoozeState()
                    cancelLowAlert()
                }
            }

            val quietActive = isQuietHoursActive(
                enabled = quietHoursEnabledSetting,
                startHour = quietHoursStartHourSetting,
                startAmPm = quietHoursStartAmPmSetting,
                endHour = quietHoursEndHourSetting,
                endAmPm = quietHoursEndAmPmSetting
            )
            evaluateAlertStateMachine(highThresholdSetting, lowThresholdSetting, quietActive)
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            currentLevel = if (level >= 0 && scale > 0) (level * 100) / scale else currentLevel

            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
            val nowCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL ||
                plugged != 0

            // Detect instant charger unplug/plug transitions from battery broadcast as well
            if (!nowCharging && (isHighAlertActive || isCharging)) {
                isCharging = false
                clearSnoozeState()
                cancelHighAlert()
            } else if (nowCharging && (isLowAlertActive || !isCharging)) {
                isCharging = true
                clearSnoozeState()
                cancelLowAlert()
            }
            isCharging = nowCharging

            val tempRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
            currentTemp = tempRaw / 10.0f
            currentVolt = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
            currentPlugged = plugged

            val quietActive = isQuietHoursActive(
                enabled = quietHoursEnabledSetting,
                startHour = quietHoursStartHourSetting,
                startAmPm = quietHoursStartAmPmSetting,
                endHour = quietHoursEndHourSetting,
                endAmPm = quietHoursEndAmPmSetting
            )
            evaluateAlertStateMachine(highThresholdSetting, lowThresholdSetting, quietActive)
        }
    }

    override fun onCreate() {
        super.onCreate()
        dataStore = TelemetryDataStore(applicationContext)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        database = AppDatabase.getDatabase(applicationContext)

        // Configure channels and ensure any ongoing monitoring notification is completely hidden/removed
        createNotificationChannels()
        hidePersistentMonitoringNotification()

        // Cache initial battery state from sticky intent safely
        try {
            val stickyIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (stickyIntent != null) {
                val level = stickyIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = stickyIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    currentLevel = (level * 100) / scale
                }
                val status = stickyIntent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
                val plugged = stickyIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL ||
                    plugged != 0
                val tempRaw = stickyIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
                currentTemp = tempRaw / 10.0f
                currentVolt = stickyIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
                currentPlugged = plugged
            }
        } catch (e: Exception) {
            Log.e("BatteryTelemetryService", "Error reading initial sticky battery intent", e)
        }

        serviceScope.launch {
            dataStore.highThresholdFlow.collect {
                highThresholdSetting = it
            }
        }
        serviceScope.launch {
            dataStore.lowThresholdFlow.collect {
                lowThresholdSetting = it
            }
        }
        serviceScope.launch {
            dataStore.quietHoursEnabledFlow.collect {
                quietHoursEnabledSetting = it
            }
        }
        serviceScope.launch {
            dataStore.quietHoursStartHourFlow.collect {
                quietHoursStartHourSetting = it
            }
        }
        serviceScope.launch {
            dataStore.quietHoursStartAmPmFlow.collect {
                quietHoursStartAmPmSetting = it
            }
        }
        serviceScope.launch {
            dataStore.quietHoursEndHourFlow.collect {
                quietHoursEndHourSetting = it
            }
        }
        serviceScope.launch {
            dataStore.quietHoursEndAmPmFlow.collect {
                quietHoursEndAmPmSetting = it
            }
        }
        serviceScope.launch {
            dataStore.ringtoneUriFlow.collect {
                customRingtoneUriSetting = it
            }
        }
        serviceScope.launch {
            dataStore.alertDurationSecondsFlow.collect {
                alertDurationSecondsSetting = it
            }
        }
        serviceScope.launch {
            dataStore.snoozeDurationMinutesFlow.collect {
                snoozeDurationMinutesSetting = it
            }
        }
        serviceScope.launch {
            dataStore.snoozeUntilTimestampFlow.collect { snoozeUntil ->
                snoozeUntilTimestampSetting = snoozeUntil
                val now = System.currentTimeMillis()
                if (snoozeUntil > now) {
                    // Suppress active alerts immediately if snoozed from either notification or UI
                    if (isHighAlertActive) cancelHighAlert()
                    if (isLowAlertActive) cancelLowAlert()
                    scheduleSnoozeWakeup(snoozeUntil - now)
                }
            }
        }
        serviceScope.launch {
            dataStore.backgroundServiceEnabledFlow.collect { isEnabled ->
                if (!isEnabled) {
                    isMonitoring = false
                    clearSnoozeState()
                    cancelHighAlert()
                    cancelLowAlert()
                    hidePersistentMonitoringNotification()
                    stopSelf()
                }
            }
        }

        // Register receivers safely
        try {
            val powerFilter = IntentFilter().apply {
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(powerConnectionReceiver, powerFilter, Context.RECEIVER_NOT_EXPORTED)
                registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(powerConnectionReceiver, powerFilter)
                registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            }
        } catch (e: Exception) {
            Log.e("BatteryTelemetryService", "Receiver registration error", e)
        }

        // Start continuous monitoring loop
        startTelemetryLoop()
    }

    /**
     * Ensures no ongoing background monitoring notification is ever visible on the status bar or lock screen.
     */
    private fun hidePersistentMonitoringNotification() {
        try {
            notificationManager.cancel(FOREGROUND_NOTIF_ID)
            if (!isHighAlertActive && !isLowAlertActive) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
            }
        } catch (e: Exception) {
            // Ignore if not currently in foreground
        }
    }

    /**
     * Locks the active threshold alert notification in foreground mode so the system cannot swipe it away
     * until the user performs the required charger action or clicks Snooze.
     */
    private fun lockAlertNotificationInForeground(notificationId: Int, notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    startForeground(
                        notificationId,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } catch (typeError: Throwable) {
                    startForeground(notificationId, notification)
                }
            } else {
                startForeground(notificationId, notification)
            }
        } catch (t: Throwable) {
            try {
                notificationManager.notify(notificationId, notification)
            } catch (e: Exception) {
                Log.e("BatteryTelemetryService", "Failed to notify locked alert", e)
            }
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        hidePersistentMonitoringNotification()

        try {
            if (wakeLock == null) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
                wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AuraBattery::ChargingTelemetryLock")?.apply {
                    setReferenceCounted(false)
                    acquire(10 * 60 * 1000L)
                }
            }
        } catch (t: Throwable) {
            Log.e("BatteryTelemetryService", "WakeLock error", t)
        }

        when (intent?.action) {
            ACTION_SNOOZE_ALERT -> {
                handleSnoozeRequested()
            }
            ACTION_RESTORE_HIGH_ALERT -> {
                if (!isSnoozed() && isCharging && currentLevel >= highThresholdSetting) {
                    isHighAlertActive = false
                    triggerHighAlertNotification()
                }
            }
            ACTION_RESTORE_LOW_ALERT -> {
                if (!isSnoozed() && !isCharging && currentLevel <= lowThresholdSetting) {
                    isLowAlertActive = false
                    triggerLowAlertNotification()
                }
            }
        }
        return START_STICKY
    }

    private fun isSnoozed(): Boolean {
        return System.currentTimeMillis() < snoozeUntilTimestampSetting
    }

    private fun handleSnoozeRequested() {
        val snoozeMillis = snoozeDurationMinutesSetting.coerceIn(1, 60) * 60_000L
        val snoozeUntil = System.currentTimeMillis() + snoozeMillis
        snoozeUntilTimestampSetting = snoozeUntil

        // Immediately silence alarm and dismiss active alert notification
        cancelHighAlert()
        cancelLowAlert()

        serviceScope.launch {
            try {
                dataStore.saveSnoozeUntilTimestamp(snoozeUntil)
            } catch (e: Exception) {
                Log.e("BatteryTelemetryService", "Failed to persist snooze timestamp", e)
            }
        }

        scheduleSnoozeWakeup(snoozeMillis)
    }

    private fun scheduleSnoozeWakeup(delayMillis: Long) {
        snoozeTimerJob?.cancel()
        snoozeTimerJob = serviceScope.launch {
            delay(delayMillis.coerceAtLeast(500L))
            snoozeUntilTimestampSetting = 0L
            try {
                dataStore.saveSnoozeUntilTimestamp(0L)
            } catch (_: Exception) {}

            val quietActive = isQuietHoursActive(
                enabled = quietHoursEnabledSetting,
                startHour = quietHoursStartHourSetting,
                startAmPm = quietHoursStartAmPmSetting,
                endHour = quietHoursEndHourSetting,
                endAmPm = quietHoursEndAmPmSetting
            )
            evaluateAlertStateMachine(highThresholdSetting, lowThresholdSetting, quietActive)
        }
    }

    private fun clearSnoozeState() {
        snoozeTimerJob?.cancel()
        snoozeTimerJob = null
        if (snoozeUntilTimestampSetting != 0L) {
            snoozeUntilTimestampSetting = 0L
            serviceScope.launch {
                try {
                    dataStore.saveSnoozeUntilTimestamp(0L)
                } catch (_: Exception) {}
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isMonitoring = false
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        wakeLock = null
        alertCycleJob?.cancel()
        snoozeTimerJob?.cancel()
        serviceScope.cancel()
        stopAlarmSound()

        try {
            unregisterReceiver(powerConnectionReceiver)
            unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            // Ignore
        }

        try {
            notificationManager.cancelAll()
        } catch (e: Exception) {
            // Ignore
        }

        Log.d("BatteryTelemetryService", "Service Stopped.")
    }

    private fun startTelemetryLoop() {
        if (isMonitoring) return
        isMonitoring = true

        // 2-minute periodic room database logger (runs on IO dispatcher)
        serviceScope.launch(Dispatchers.IO) {
            while (isMonitoring && isActive) {
                try {
                    val entry = BatteryLogEntry(
                        timestamp = System.currentTimeMillis(),
                        batteryLevel = currentLevel,
                        isCharging = isCharging
                    )
                    database.batteryLogDao().insert(entry)

                    val cutoffTime = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
                    database.batteryLogDao().pruneLogs(cutoffTime)
                } catch (e: Exception) {
                    Log.e("BatteryTelemetryService", "Error in database logging loop: ", e)
                }
                delay(120000)
            }
        }

        // Lightweight 1-second state evaluation loop using cached event-driven fields
        serviceScope.launch {
            while (isMonitoring && isActive) {
                try {
                    val quietActive = isQuietHoursActive(
                        enabled = quietHoursEnabledSetting,
                        startHour = quietHoursStartHourSetting,
                        startAmPm = quietHoursStartAmPmSetting,
                        endHour = quietHoursEndHourSetting,
                        endAmPm = quietHoursEndAmPmSetting
                    )

                    evaluateAlertStateMachine(highThresholdSetting, lowThresholdSetting, quietActive)
                } catch (e: Exception) {
                    Log.e("BatteryTelemetryService", "Error in state evaluation cycle: ", e)
                }
                delay(1000)
            }
        }
    }

    private fun isQuietHoursActive(
        enabled: Boolean,
        startHour: Int,
        startAmPm: String,
        endHour: Int,
        endAmPm: String
    ): Boolean {
        if (!enabled) return false

        val calendar = java.util.Calendar.getInstance()
        val currentHour24 = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(java.util.Calendar.MINUTE)

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

    private fun evaluateAlertStateMachine(highThresh: Int, lowThresh: Int, quietActive: Boolean) {
        if (quietActive || isSnoozed()) {
            if (isHighAlertActive) cancelHighAlert()
            if (isLowAlertActive) cancelLowAlert()
            return
        }

        // High Limit Alert: Trigger ONLY when charging and battery reaches highThresh
        if (isCharging && currentLevel >= highThresh) {
            if (isLowAlertActive) cancelLowAlert()
            if (!isHighAlertActive) {
                triggerHighAlertNotification()
            }
        } else {
            if (isHighAlertActive) {
                cancelHighAlert()
            }
        }

        // Low Limit Alert: Trigger ONLY when discharging and battery drops to/below lowThresh
        if (!isCharging && currentLevel <= lowThresh) {
            if (isHighAlertActive) cancelHighAlert()
            if (!isLowAlertActive) {
                triggerLowAlertNotification()
            }
        } else {
            if (isLowAlertActive) {
                cancelLowAlert()
            }
        }
    }

    private fun buildLockedAlertNotification(
        title: String,
        body: String,
        restoreAction: String,
        requestCodeOffset: Int
    ): Notification {
        val restoreIntent = Intent(this, BatteryTelemetryService::class.java).apply {
            action = restoreAction
        }
        val restorePendingIntent = PendingIntent.getService(
            this,
            200 + requestCodeOffset,
            restoreIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, BatteryTelemetryService::class.java).apply {
            action = ACTION_SNOOZE_ALERT
        }
        val snoozePendingIntent = PendingIntent.getService(
            this,
            300 + requestCodeOffset,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            100 + requestCodeOffset,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeLabel = "Snooze (${snoozeDurationMinutesSetting}m)"

        return NotificationCompat.Builder(this, ALERTS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(restorePendingIntent)
            .addAction(
                NotificationCompat.Action.Builder(
                    android.R.drawable.ic_lock_idle_alarm,
                    snoozeLabel,
                    snoozePendingIntent
                ).build()
            )
            .build()
            .apply {
                flags = flags or Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR
            }
    }

    private fun triggerHighAlertNotification() {
        if (isHighAlertActive || isSnoozed()) return
        isHighAlertActive = true
        startAlertAutoCycleLoop(isHighAlert = true)
    }

    private fun cancelHighAlert() {
        if (!isHighAlertActive) return
        isHighAlertActive = false
        alertCycleJob?.cancel()
        alertCycleJob = null
        stopAlarmSound()
        try {
            notificationManager.cancel(HIGH_ALERT_ID)
        } catch (e: Exception) {
            // Ignore
        }
        hidePersistentMonitoringNotification()
    }

    private fun triggerLowAlertNotification() {
        if (isLowAlertActive || isSnoozed()) return
        isLowAlertActive = true
        startAlertAutoCycleLoop(isHighAlert = false)
    }

    private fun cancelLowAlert() {
        if (!isLowAlertActive) return
        isLowAlertActive = false
        alertCycleJob?.cancel()
        alertCycleJob = null
        stopAlarmSound()
        try {
            notificationManager.cancel(LOW_ALERT_ID)
        } catch (e: Exception) {
            // Ignore
        }
        hidePersistentMonitoringNotification()
    }

    /**
     * Alert Duration & Auto-Cycle Engine:
     * - Issues the locked alert notification and runs continuous audio/visual alert for `alertDurationSecondsSetting`.
     * - Once the duration finishes without charger action or snooze, immediately cancels the expired notification (`cancel(ID)`)
     *   and re-issues a fresh notification to start the next cycle.
     */
    private fun startAlertAutoCycleLoop(isHighAlert: Boolean) {
        alertCycleJob?.cancel()
        alertCycleJob = serviceScope.launch {
            val alertId = if (isHighAlert) HIGH_ALERT_ID else LOW_ALERT_ID
            val title = if (isHighAlert) HIGH_ALERT_TITLE else LOW_ALERT_TITLE
            val body = if (isHighAlert) HIGH_ALERT_BODY else LOW_ALERT_BODY
            val restoreAction = if (isHighAlert) ACTION_RESTORE_HIGH_ALERT else ACTION_RESTORE_LOW_ALERT
            val offset = if (isHighAlert) 1 else 2

            while (isActive && !isSnoozed() && ((isHighAlert && isHighAlertActive && isCharging && currentLevel >= highThresholdSetting) ||
                    (!isHighAlert && isLowAlertActive && !isCharging && currentLevel <= lowThresholdSetting))
            ) {
                // Start fresh cycle: play looping alarm sound & issue fresh notification
                playAlarmSound()
                val freshNotification = buildLockedAlertNotification(
                    title = title,
                    body = body,
                    restoreAction = restoreAction,
                    requestCodeOffset = offset
                )
                lockAlertNotificationInForeground(alertId, freshNotification)
                try {
                    notificationManager.notify(alertId, freshNotification)
                } catch (e: Exception) {
                    Log.e("BatteryTelemetryService", "Failed to issue alert cycle notification", e)
                }

                // Run full configured Alert Active Duration (e.g., 30s, 60s, 120s)
                val durationMs = alertDurationSecondsSetting.coerceIn(15, 300) * 1000L
                val tickMs = 2000L
                var elapsedMs = 0L
                while (isActive && elapsedMs < durationMs && !isSnoozed()) {
                    val delayStep = (durationMs - elapsedMs).coerceAtMost(tickMs)
                    delay(delayStep)
                    elapsedMs += delayStep

                    val stillValid = if (isHighAlert) {
                        isHighAlertActive && isCharging && currentLevel >= highThresholdSetting
                    } else {
                        isLowAlertActive && !isCharging && currentLevel <= lowThresholdSetting
                    }
                    if (!stillValid || isSnoozed()) break
                }

                val shouldRecycle = !isSnoozed() && if (isHighAlert) {
                    isHighAlertActive && isCharging && currentLevel >= highThresholdSetting
                } else {
                    isLowAlertActive && !isCharging && currentLevel <= lowThresholdSetting
                }

                if (!shouldRecycle) break

                // Duration finished without action: immediately dismiss the expired notification and reset for next cycle
                stopAlarmSound()
                try {
                    notificationManager.cancel(alertId)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    } else {
                        @Suppress("DEPRECATION")
                        stopForeground(true)
                    }
                } catch (_: Exception) {}

                // Brief cycle reset pause before re-issuing the fresh notification cycle
                delay(1200L)
            }
        }
    }

    private fun playAlarmSound() {
        stopAlarmSound()
        serviceScope.launch {
            try {
                var audioUri: Uri? = null
                if (customRingtoneUriSetting.isNotBlank()) {
                    try {
                        audioUri = Uri.parse(customRingtoneUriSetting)
                    } catch (e: Exception) {
                        Log.w("BatteryTelemetryService", "Failed to parse custom ringtone URI", e)
                    }
                }
                if (audioUri == null) {
                    audioUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                }

                if (audioUri != null) {
                    withContext(Dispatchers.Main) {
                        try {
                            mediaPlayer = MediaPlayer().apply {
                                setDataSource(applicationContext, audioUri)
                                setAudioAttributes(
                                    AudioAttributes.Builder()
                                        .setUsage(AudioAttributes.USAGE_ALARM)
                                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                        .build()
                                )
                                isLooping = true
                                prepare()
                                start()
                            }
                        } catch (playerError: Exception) {
                            Log.w("BatteryTelemetryService", "Failed custom URI playback, falling back to default", playerError)
                            val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                            if (fallbackUri != null) {
                                mediaPlayer = MediaPlayer().apply {
                                    setDataSource(applicationContext, fallbackUri)
                                    setAudioAttributes(
                                        AudioAttributes.Builder()
                                            .setUsage(AudioAttributes.USAGE_ALARM)
                                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                            .build()
                                    )
                                    isLooping = true
                                    prepare()
                                    start()
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("BatteryTelemetryService", "Failed to play alarm ringtone: ", e)
            }
        }
    }

    private fun stopAlarmSound() {
        serviceScope.launch(Dispatchers.Main) {
            try {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        it.stop()
                    }
                    it.release()
                }
            } catch (e: Exception) {
                Log.e("BatteryTelemetryService", "Failed to stop alarm ringtone: ", e)
            }
            mediaPlayer = null
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                notificationManager.cancel(FOREGROUND_NOTIF_ID)
                notificationManager.deleteNotificationChannel(FOREGROUND_CHANNEL_ID)
            } catch (e: Exception) {
                // Ignore
            }

            val alertChannel = NotificationChannel(
                ALERTS_CHANNEL_ID,
                "Battery Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority alarms triggering for high and low battery thresholds."
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannel(alertChannel)
        }
    }
}
