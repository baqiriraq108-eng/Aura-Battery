package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.pm.ServiceInfo
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import android.app.usage.UsageStatsManager
import android.app.usage.UsageStats
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.UUID

class BatteryTelemetryService : Service() {

    companion object {
        const val FOREGROUND_CHANNEL_ID = "live_telemetry_channel"
        const val ALERTS_CHANNEL_ID = "battery_alerts_channel"
        
        const val FOREGROUND_NOTIF_ID = 1001
        const val HIGH_ALERT_ID = 1002
        const val LOW_ALERT_ID = 1003

        const val ACTION_MUTE_DISMISS_HIGH = "com.example.ACTION_MUTE_DISMISS_HIGH"
        const val ACTION_MUTE_DISMISS_LOW = "com.example.ACTION_MUTE_DISMISS_LOW"
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isMonitoring = false

    private lateinit var dataStore: TelemetryDataStore
    private lateinit var notificationManager: NotificationManager
    private lateinit var database: AppDatabase

    // Alarm state tracking
    private var mediaPlayer: MediaPlayer? = null

    // Alert active and retry tracking
    private var isHighAlertActive = false
    private var isLowAlertActive = false
    private var highAlertRetryJob: Job? = null
    private var lowAlertRetryJob: Job? = null

    // Current battery stats
    private var currentLevel = 50
    private var isCharging = false
    private var currentTemp = 25.5f
    private var currentVolt = 4000
    private var currentPlugged = 0
    private var previousLevel: Int? = null

    // Preference cache fields
    private var highThresholdSetting = 80
    private var lowThresholdSetting = 20
    private var quietHoursEnabledSetting = false
    private var quietHoursStartHourSetting = 10
    private var quietHoursStartAmPmSetting = "PM"
    private var quietHoursEndHourSetting = 7
    private var quietHoursEndAmPmSetting = "AM"

    // Receive power state changes for auto-dismissal
    private val powerConnectionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == Intent.ACTION_POWER_DISCONNECTED) {
                isCharging = false
                // User took the recommended action: unplugged charger -> cancel high alert & retry immediately
                cancelHighAlert()
            } else if (action == Intent.ACTION_POWER_CONNECTED) {
                isCharging = true
                // User took the recommended action: plugged in charger -> cancel low alert & retry immediately
                cancelLowAlert()
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

    // Capture system battery states
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            currentLevel = if (level >= 0 && scale > 0) (level * 100) / scale else currentLevel
            
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            
            val tempRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
            currentTemp = tempRaw / 10.0f
            currentVolt = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
            currentPlugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)

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
            dataStore.backgroundServiceEnabledFlow.collect { isEnabled ->
                if (!isEnabled) {
                    isMonitoring = false
                    stopAlarmSound()
                    highAlertRetryJob?.cancel()
                    lowAlertRetryJob?.cancel()
                    try {
                        notificationManager.cancelAll()
                    } catch (e: Exception) {
                        // Ignore
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    } else {
                        @Suppress("DEPRECATION")
                        stopForeground(true)
                    }
                    stopSelf()
                }
            }
        }

        // Set up notification channels (removes persistent channel, configures alerts channel)
        createNotificationChannels()

        // Register power & battery state receivers
        val powerFilter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(powerConnectionReceiver, powerFilter)
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        // Fulfill startForegroundService requirement without leaving any persistent notification
        val startupNotification = NotificationCompat.Builder(this, ALERTS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("")
            .setContentText("")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                FOREGROUND_NOTIF_ID,
                startupNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                FOREGROUND_NOTIF_ID,
                startupNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(FOREGROUND_NOTIF_ID, startupNotification)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        try {
            notificationManager.cancel(FOREGROUND_NOTIF_ID)
        } catch (e: Exception) {
            // Ignore
        }

        // Start infinite 1-second monitoring loop
        startTelemetryLoop()
    }

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Acquire partial wakelock to ensure CPU keeps monitoring even if app is swiped away
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Xiaomi14TPro::ChargingTelemetryLock")?.apply {
                setReferenceCounted(false)
                acquire()
            }
        }

        when (intent?.action) {
            ACTION_MUTE_DISMISS_HIGH -> {
                handleHighAlertDismissed()
            }
            ACTION_MUTE_DISMISS_LOW -> {
                handleLowAlertDismissed()
            }
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d("BatteryTelemetryService", "onTaskRemoved called - ensuring service stays alive or restarts immediately")
        // Schedule immediate resurrection if the OS attempts to terminate the process on task swipe
        try {
            val restartServiceIntent = Intent(applicationContext, BatteryTelemetryService::class.java).apply {
                setPackage(packageName)
            }
            val restartServicePendingIntent = PendingIntent.getService(
                applicationContext,
                999,
                restartServiceIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
            alarmManager?.set(
                android.app.AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 1000,
                restartServicePendingIntent
            )
        } catch (e: Exception) {
            Log.e("BatteryTelemetryService", "Failed to schedule restart alarm: ", e)
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
        highAlertRetryJob?.cancel()
        lowAlertRetryJob?.cancel()
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

    private fun getTopActivePackage(): String? {
        try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
            val endTime = System.currentTimeMillis()
            val startTime = endTime - 120000 // past 2 minutes
            val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
            if (stats.isNullOrEmpty()) return null
            
            // Find packages that were active in the last 2 minutes
            val activeStats = stats.filter { it.lastTimeUsed >= startTime }
            if (activeStats.isEmpty()) {
                // Fallback to max by lastTimeUsed
                return stats.maxByOrNull { it.lastTimeUsed }?.packageName
            }
            return activeStats.maxByOrNull { it.totalTimeInForeground }?.packageName
        } catch (e: Exception) {
            Log.e("BatteryTelemetryService", "Error querying top active package", e)
            return null
        }
    }

    private fun startTelemetryLoop() {
        if (isMonitoring) return
        isMonitoring = true

        // High-resolution Room logging loop: Captures battery state every 2 minutes and prunes older logs
        serviceScope.launch(Dispatchers.IO) {
            while (isMonitoring && isActive) {
                try {
                    val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                    val batteryIntent = withContext(Dispatchers.Main) {
                        registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    }

                    val level = if (batteryIntent != null) {
                        val rawLevel = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                        if (rawLevel >= 0 && scale > 0) (rawLevel * 100) / scale else bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    } else {
                        bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    }

                    val charging = if (batteryIntent != null) {
                        val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
                        status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) bm.isCharging else false
                    }

                    val topPkg = getTopActivePackage()

                    // Write log entry to database
                    val entry = BatteryLogEntry(
                        timestamp = System.currentTimeMillis(),
                        batteryLevel = level,
                        isCharging = charging,
                        topPackageName = topPkg
                    )
                    database.batteryLogDao().insert(entry)

                    // Prune logs older than exactly 24 hours
                    val cutoffTime = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
                    database.batteryLogDao().pruneLogs(cutoffTime)
                } catch (e: Exception) {
                    Log.e("BatteryTelemetryService", "Error in database logging loop: ", e)
                }
                delay(120000) // Execute exactly once every 2 minutes (120,000ms)
            }
        }

        serviceScope.launch {
            while (isMonitoring && isActive) {
                try {
                    val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                    
                    // Fetch sticky battery state on the Main thread to ensure 100% thread safety and accuracy
                    val batteryIntent = withContext(Dispatchers.Main) {
                        registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    }

                    val level = if (batteryIntent != null) {
                        val rawLevel = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                        if (rawLevel >= 0 && scale > 0) (rawLevel * 100) / scale else bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    } else {
                        bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    }

                    val charging = if (batteryIntent != null) {
                        val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
                        status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) bm.isCharging else false
                    }

                    val tempRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
                    val temp = tempRaw / 10.0f
                    val volt = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 4000
                    val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0

                    // Update local telemetry values
                    currentLevel = level
                    isCharging = charging
                    currentTemp = temp
                    currentVolt = volt
                    currentPlugged = plugged

                    val currentPercentage = level.toDouble()
                    val lastKnownPercentage = previousLevel?.toDouble()

                    // Check and accumulate consumed micro-cycles
                    if (lastKnownPercentage != null && charging) {
                        if (currentPercentage > lastKnownPercentage) {
                            val delta = (currentPercentage - lastKnownPercentage) / 100.0
                            if (delta > 0.0) {
                                val currentCycles = dataStore.consumedCyclesFlow.first()
                                dataStore.saveConsumedCycles(currentCycles + delta)
                                Log.d("BatteryTelemetryService", "Delta Charge accumulated: $delta, total cycles: ${currentCycles + delta}")
                            }
                        }
                    }
                    previousLevel = level

                    // Check configurations using cached, reactive fields
                    val quietActive = isQuietHoursActive(
                        enabled = quietHoursEnabledSetting,
                        startHour = quietHoursStartHourSetting,
                        startAmPm = quietHoursStartAmPmSetting,
                        endHour = quietHoursEndHourSetting,
                        endAmPm = quietHoursEndAmPmSetting
                    )

                    // Evaluate state machine for alerts
                    evaluateAlertStateMachine(highThresholdSetting, lowThresholdSetting, quietActive)

                } catch (e: Exception) {
                    Log.e("TelemetryService", "Error in monitoring cycle: ", e)
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
            true // If start and end times are identical, quiet hours are active for the full 24-hour period
        }
    }

    private fun evaluateAlertStateMachine(highThresh: Int, lowThresh: Int, quietActive: Boolean) {
        if (quietActive) {
            cancelHighAlert()
            cancelLowAlert()
            return
        }

        // --- 1. HIGH ALERT STATE MACHINE ---
        if (isCharging && currentLevel >= highThresh) {
            // Only trigger if not already active and not waiting for retry
            if (!isHighAlertActive && highAlertRetryJob == null) {
                triggerHighAlertNotification()
            }
        } else {
            // Condition no longer met (charger unplugged or battery dropped below high threshold)
            if (isHighAlertActive || highAlertRetryJob != null) {
                cancelHighAlert()
            }
        }

        // --- 2. LOW ALERT STATE MACHINE ---
        if (!isCharging && currentLevel <= lowThresh) {
            // Only trigger if not already active and not waiting for retry
            if (!isLowAlertActive && lowAlertRetryJob == null) {
                triggerLowAlertNotification()
            }
        } else {
            // Condition no longer met (charger plugged in or battery rose above low threshold)
            if (isLowAlertActive || lowAlertRetryJob != null) {
                cancelLowAlert()
            }
        }
    }

    private fun triggerHighAlertNotification() {
        if (isHighAlertActive) return // Notification stays alive until action taken or dismissed
        isHighAlertActive = true
        highAlertRetryJob?.cancel()
        highAlertRetryJob = null

        playAlarmSound()

        val dismissIntent = Intent(this, BatteryTelemetryService::class.java).apply {
            action = ACTION_MUTE_DISMISS_HIGH
        }
        val dismissPendingIntent = PendingIntent.getService(
            this, 201, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, 101, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, ALERTS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("تنبيه اكتمال الشحن • High Battery Alert")
            .setContentText("وصل مستوى الشحن إلى $currentLevel%، يرجى فصل الشاحن الآن!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(dismissPendingIntent) // Triggered when user swipes away/dismisses notification
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إغلاق التنبيه", dismissPendingIntent)
            .setAutoCancel(true)

        val notification = builder.build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    HIGH_ALERT_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    HIGH_ALERT_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(HIGH_ALERT_ID, notification)
            }
        } catch (e: Exception) {
            notificationManager.notify(HIGH_ALERT_ID, notification)
        }
    }

    private fun handleHighAlertDismissed() {
        stopAlarmSound()
        isHighAlertActive = false
        notificationManager.cancel(HIGH_ALERT_ID)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        highAlertRetryJob?.cancel()
        // If recommended action has NOT been taken (still charging and >= threshold), wait 20s and re-alert!
        if (isCharging && currentLevel >= highThresholdSetting) {
            highAlertRetryJob = serviceScope.launch {
                delay(20000L) // Wait exactly 20 seconds
                if (isCharging && currentLevel >= highThresholdSetting && !isHighAlertActive) {
                    withContext(Dispatchers.Main) {
                        triggerHighAlertNotification()
                    }
                }
            }
        }
    }

    private fun cancelHighAlert() {
        highAlertRetryJob?.cancel()
        highAlertRetryJob = null
        isHighAlertActive = false
        stopAlarmSound()
        notificationManager.cancel(HIGH_ALERT_ID)
        if (!isLowAlertActive) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        }
    }

    private fun triggerLowAlertNotification() {
        if (isLowAlertActive) return // Notification stays alive until action taken or dismissed
        isLowAlertActive = true
        lowAlertRetryJob?.cancel()
        lowAlertRetryJob = null

        playAlarmSound()

        val dismissIntent = Intent(this, BatteryTelemetryService::class.java).apply {
            action = ACTION_MUTE_DISMISS_LOW
        }
        val dismissPendingIntent = PendingIntent.getService(
            this, 202, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, 102, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, ALERTS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("تنبيه انخفاض الشحن • Low Battery Alert")
            .setContentText("وصل مستوى الشحن إلى $currentLevel%، يرجى توصيل الشاحن الآن!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(dismissPendingIntent) // Triggered when user swipes away/dismisses notification
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إغلاق التنبيه", dismissPendingIntent)
            .setAutoCancel(true)

        val notification = builder.build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    LOW_ALERT_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    LOW_ALERT_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(LOW_ALERT_ID, notification)
            }
        } catch (e: Exception) {
            notificationManager.notify(LOW_ALERT_ID, notification)
        }
    }

    private fun handleLowAlertDismissed() {
        stopAlarmSound()
        isLowAlertActive = false
        notificationManager.cancel(LOW_ALERT_ID)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        lowAlertRetryJob?.cancel()
        // If recommended action has NOT been taken (still not charging and <= threshold), wait 20s and re-alert!
        if (!isCharging && currentLevel <= lowThresholdSetting) {
            lowAlertRetryJob = serviceScope.launch {
                delay(20000L) // Wait exactly 20 seconds
                if (!isCharging && currentLevel <= lowThresholdSetting && !isLowAlertActive) {
                    withContext(Dispatchers.Main) {
                        triggerLowAlertNotification()
                    }
                }
            }
        }
    }

    private fun cancelLowAlert() {
        lowAlertRetryJob?.cancel()
        lowAlertRetryJob = null
        isLowAlertActive = false
        stopAlarmSound()
        notificationManager.cancel(LOW_ALERT_ID)
        if (!isHighAlertActive) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        }
    }

    private fun playAlarmSound() {
        stopAlarmSound()
        serviceScope.launch {
            try {
                val savedUriStr = dataStore.alertSoundUriFlow.first()
                val uri = if (!savedUriStr.isNullOrEmpty()) {
                    try {
                        android.net.Uri.parse(savedUriStr)
                    } catch (e: Exception) {
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    }
                } else {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                }
                withContext(Dispatchers.Main) {
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(applicationContext, uri)
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        isLooping = true
                        prepare()
                        start()
                    }
                }
            } catch (e: Exception) {
                Log.e("TelemetryService", "Failed to play alarm ringtone: ", e)
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
                Log.e("TelemetryService", "Failed to stop alarm ringtone: ", e)
            }
            mediaPlayer = null
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Delete persistent live telemetry channel if previously created
            try {
                notificationManager.deleteNotificationChannel(FOREGROUND_CHANNEL_ID)
            } catch (e: Exception) {
                // Ignore
            }

            // High priority alerts channel exclusively for high and low battery events
            val alertChannel = NotificationChannel(
                ALERTS_CHANNEL_ID,
                "Battery Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority alarms triggering for high and low battery thresholds."
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(alertChannel)
        }
    }
}
