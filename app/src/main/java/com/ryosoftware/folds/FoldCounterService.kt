package com.ryosoftware.folds

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.time.Instant

class FoldCounterService : Service(), SensorEventListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var sensorManager: SensorManager
    private var hinge: Sensor? = null
    private lateinit var prefs: SharedPreferences
    private var unfoldsCount = 0
    private var unfoldedMinThreshold = 1.0f

    private var isUnfolded = false
    private var isUnfoldedInit = false

    @SuppressLint("UseKtx")
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_content))
            .setSmallIcon(R.drawable.ic_launcher_foreground)  // Substitua com seu ícone
            .build()

        startForeground(1, notification)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(this)

        unfoldsCount = prefs.getInt(UNFOLDS_COUNT_KEY, 0)
        unfoldedMinThreshold = prefs.getFloat(UNFOLDED_MIN_THRESHOLD_KEY, UNFOLDED_MIN_THRESHOLD_DEFAULT)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            hinge = sensorManager.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE)
        }

        if (hinge == null) {
            val deviceSensors: List<Sensor> = sensorManager.getSensorList(Sensor.TYPE_ALL)

            for (sensor in deviceSensors) {
                if (sensor.name.contains("hinge", ignoreCase = true) ||
                    sensor.name.contains("fold", ignoreCase = true)
                ) {
                    hinge = sensor
                    prefs.edit().apply() {
                        putFloat(UNFOLDED_MAX_RANGE_KEY, sensor.maximumRange)
                        putFloat(UNFOLDED_RANGE_RESOLUTION_KEY, sensor.resolution)
                        apply()
                    }
                    break
                }
            }
        }

        hinge?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d(TAG, "Registered bending sensor: ${it.name}")
        } ?: Log.e(TAG, "No bend sensor found!")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
        when (key) {
            UNFOLDED_MIN_THRESHOLD_KEY -> {
                unfoldedMinThreshold = prefs.getFloat(UNFOLDED_MIN_THRESHOLD_KEY, UNFOLDED_MIN_THRESHOLD_DEFAULT)
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor == hinge) {

                Log.d(TAG, "Sensor value: ${it.values.joinToString()}")

                val isCurrentlyUnfolded = it.values[0] >= unfoldedMinThreshold

                if (isUnfoldedInit && isCurrentlyUnfolded && (! isUnfolded)) {
                    unfoldsCount++
                    Log.d(TAG, "Fold detected! Total: ${unfoldsCount}")
                }

                saveCurrentData(it.values[0], isCurrentlyUnfolded && !isUnfolded)

                isUnfolded = isCurrentlyUnfolded
                isUnfoldedInit = true
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun saveCurrentData(currentThreshold: Float, foldCountChanged: Boolean) {
        prefs.edit().apply() {
            putFloat(FOLD_STATUS_CURRENT_THRESHOLD, currentThreshold)
            if (foldCountChanged) {
                putInt(UNFOLDS_COUNT_KEY, unfoldsCount)
                if (! prefs.contains(UNFOLDS_COUNT_START_TIME_KEY)) { putLong(UNFOLDS_COUNT_START_TIME_KEY, Instant.now().toEpochMilli()) }
            }
            apply()
        }
    }
    @SuppressLint("ObsoleteSdkInt")
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.unregisterOnSharedPreferenceChangeListener(this)

        sensorManager.unregisterListener(this)
        Log.d(TAG, "Service destroyed")

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        private const val TAG = "FoldCounterService"
        public const val PREFS_NAME = "FoldCounterPrefs"
        public const val UNFOLDS_COUNT_KEY = "unfolds-count"
        public const val UNFOLDS_COUNT_START_TIME_KEY = "unfolds-count-start-time"
        public const val UNFOLDED_MIN_THRESHOLD_KEY = "unfolded-min-threshold"

        public const val UNFOLDED_MAX_RANGE_KEY = "unfolded-max-range"

        public const val UNFOLDED_RANGE_RESOLUTION_KEY = "unfolded-range-resolution"
        public const val FOLD_STATUS_CURRENT_THRESHOLD = "current-threshold"
        public const val UNFOLDED_MIN_THRESHOLD_DEFAULT = 40.0f
        private const val CHANNEL_ID = "FoldCounterServiceChannel"
    }
}