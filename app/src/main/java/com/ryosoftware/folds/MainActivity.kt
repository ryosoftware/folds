package com.ryosoftware.folds

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentActivity
import kotlin.system.exitProcess

class MainActivity : FragmentActivity() {
    @SuppressLint("ObsoleteSdkInt", "UseKtx", "BatteryLife")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_activity)

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

            if (!isIgnoringBatteryOptimizations) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.permission_required)
                builder.setMessage(R.string.disable_battery_optimization_message)
                builder.setPositiveButton(R.string.allow) { _, _ ->
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${packageName}")
                    }
                    startActivity(intent)
                }

                builder.setNegativeButton(R.string.exit) { dialog, _ ->
                    dialog.dismiss()
                    finish()
                    exitProcess(0)
                }

                builder.show()

            }
        }

        enableEdgeToEdge()
    }
}

