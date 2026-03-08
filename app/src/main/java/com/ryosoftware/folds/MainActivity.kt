package com.ryosoftware.folds

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentActivity
import androidx.preference.Preference
import com.google.android.material.color.DynamicColors
import kotlin.system.exitProcess

class MainActivity : FragmentActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    private val batteryOptimizationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(packageName)
        if (isIgnoringBatteryOptimizations) {
            FoldCounterService.startService(this)
        } else {
            killApp()
        }
    }

    private lateinit var prefs: SharedPreferences

    @SuppressLint("ObsoleteSdkInt", "UseKtx", "BatteryLife")
    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)

        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_activity)

        prefs = getSharedPreferences(FoldCounterService.PREFS_NAME, Context.MODE_PRIVATE)

        val container: View = findViewById(R.id.settings_container)
        ViewCompat.setOnApplyWindowInsetsListener(container) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.updatePadding(
                top = insets.top,
                bottom = insets.bottom
            )

            windowInsets
        }

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(packageName)

            if (! isIgnoringBatteryOptimizations) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.permission_required)
                builder.setMessage(R.string.disable_battery_optimization_message)
                builder.setPositiveButton(R.string.allow) { _, _ ->
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${packageName}")
                    }
                    batteryOptimizationLauncher.launch(intent)
                }
                builder.setNegativeButton(R.string.exit) { dialog, _ ->
                    killApp()
                }
                builder.show()
            }
            else {
                FoldCounterService.startService(this)
            }
        } else {
            FoldCounterService.startService(this)
        }

        enableEdgeToEdge()
    }

    private fun killApp() {
        finish()
        exitProcess(0)
    }

    override fun onResume() {
        super.onResume()

        showUnfoldsCount()

        prefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()

        prefs.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
        when (key) {
            FoldCounterService.UNFOLDS_COUNT_KEY -> {
                showUnfoldsCount()
            }
        }
    }

    private fun showUnfoldsCount() {
        val unfoldsCount = prefs.getInt(FoldCounterService.UNFOLDS_COUNT_KEY, 0)
        findViewById<TextView>(R.id.unfolds_count).setText(unfoldsCount.toString())
    }
}

