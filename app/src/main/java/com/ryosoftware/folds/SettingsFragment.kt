package com.ryosoftware.folds;

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.Preference
import androidx.preference.SeekBarPreference
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var prefs: SharedPreferences

    @SuppressLint("UseKtx")
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        prefs = requireContext().getSharedPreferences(FoldCounterService.PREFS_NAME, Context.MODE_PRIVATE)

        setPreferencesFromResource(R.xml.settings_preferences, rootKey)

        val appVersionPref = findPreference<Preference>("app-version")
        appVersionPref?.summary = getString(R.string.app_version_description, BuildConfig.VERSION_NAME, getDateTimeFromTimeStamp(BuildConfig.VERSION_CODE.toLong() * 1000))
        appVersionPref?.setOnPreferenceClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context?.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            true
        }

        val thresholdPref = findPreference<SeekBarPreference>(FoldCounterService.UNFOLDED_MIN_THRESHOLD_KEY)
        thresholdPref?.summary = getString(R.string.min_threshold_description, prefs.getFloat(FoldCounterService.UNFOLDED_MIN_THRESHOLD_KEY, FoldCounterService.UNFOLDED_MIN_THRESHOLD_DEFAULT).toInt())
        thresholdPref?.max = prefs.getFloat(FoldCounterService.UNFOLDED_MAX_RANGE_KEY, 360.0f).toInt()
        thresholdPref?.value = prefs.getFloat(FoldCounterService.UNFOLDED_MIN_THRESHOLD_KEY, FoldCounterService.UNFOLDED_MIN_THRESHOLD_DEFAULT).toInt()
        thresholdPref?.seekBarIncrement = prefs.getFloat(FoldCounterService.UNFOLDED_RANGE_RESOLUTION_KEY, 1.0f).toInt()
        thresholdPref?.setOnPreferenceChangeListener { _, newValue ->
            val value = (newValue as? Int)?.toFloat()
            if (value != null) {
                prefs.edit().putFloat(FoldCounterService.UNFOLDED_MIN_THRESHOLD_KEY, value)?.apply()
                thresholdPref.summary = getString(R.string.min_threshold_description, value.toInt())
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()

        showCurrentValues()

        prefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()

        prefs.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
        when (key) {
            FoldCounterService.FOLD_STATUS_CURRENT_THRESHOLD -> {
                showCurrentThreshold()
            }
            FoldCounterService.UNFOLDS_COUNT_KEY -> {
                showFoldsCount()
            }
        }
    }

    private fun showCurrentThreshold() {
        findPreference<Preference>(FoldCounterService.FOLD_STATUS_CURRENT_THRESHOLD)?.summary = prefs.getFloat(FoldCounterService.FOLD_STATUS_CURRENT_THRESHOLD, 0.0f).toString()
    }

    private fun getDateTimeFromTimeStamp(timestamp: Long) : String {
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        val dateTime = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()

        return dateTime.format(formatter)
    }

    private fun showFoldsCount() {
        val foldCountPref = findPreference<Preference>(FoldCounterService.UNFOLDS_COUNT_KEY)
        var foldCountPrefSummary = getString(R.string.unfolds_count_none)
        val unfoldsCount = prefs.getInt(FoldCounterService.UNFOLDS_COUNT_KEY, 0)

        if (unfoldsCount != 0) {
            foldCountPrefSummary = getString(R.string.unfolds_count_other, unfoldsCount, getDateTimeFromTimeStamp(prefs.getLong(FoldCounterService.UNFOLDS_COUNT_START_TIME_KEY, 0)))
        }

        foldCountPref?.summary = foldCountPrefSummary
    }
    private fun showCurrentValues() {
        showCurrentThreshold()

        showFoldsCount()
    }
}
