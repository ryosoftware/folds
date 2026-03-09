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
import com.ryosoftware.folds.ui.theme.FoldsTheme

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var prefs: SharedPreferences

    private var unfoldedRangeResolution = 1.0f

    @SuppressLint("UseKtx")
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        prefs = requireContext().getSharedPreferences(FoldCounterService.PREFS_NAME, Context.MODE_PRIVATE)

        unfoldedRangeResolution = prefs.getFloat(FoldCounterService.UNFOLDED_RANGE_RESOLUTION_KEY, 1.0f)

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
        thresholdPref?.summary = getString(R.string.min_threshold_description, getString(R.string.degrees_from_int, prefs.getFloat(FoldCounterService.UNFOLDED_MIN_THRESHOLD_KEY, FoldCounterService.UNFOLDED_MIN_THRESHOLD_DEFAULT).toInt()))
        thresholdPref?.max = prefs.getFloat(FoldCounterService.UNFOLDED_MAX_RANGE_KEY, 360.0f).toInt()
        thresholdPref?.value = prefs.getFloat(FoldCounterService.UNFOLDED_MIN_THRESHOLD_KEY, FoldCounterService.UNFOLDED_MIN_THRESHOLD_DEFAULT).toInt()
        thresholdPref?.seekBarIncrement = unfoldedRangeResolution.toInt()
        thresholdPref?.setOnPreferenceChangeListener { _, newValue ->
            val value = (newValue as? Int)?.toFloat()
            if (value != null) {
                prefs.edit().putFloat(FoldCounterService.UNFOLDED_MIN_THRESHOLD_KEY, value)?.apply()
                thresholdPref.summary = getString(R.string.min_threshold_description, getString(R.string.degrees_from_int, value.toInt()))
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
                showUnfoldsCount()
            }
            FoldCounterService.TIME_UNFOLDED_KEY -> {
                showUnfoldsCount()
            }
            FoldCounterService.TIME_FOLDED_KEY -> {
                showUnfoldsCount()
            }
        }
    }

    private fun showCurrentThreshold() {
        val value = prefs.getFloat(FoldCounterService.FOLD_STATUS_CURRENT_THRESHOLD, 0.0f)
        val currentThresholdPref = findPreference<Preference>(FoldCounterService.FOLD_STATUS_CURRENT_THRESHOLD)
        if (unfoldedRangeResolution < 1.0f) {
            currentThresholdPref?.summary = getString(R.string.degrees_from_float, value)
        } else {
            currentThresholdPref?.summary = getString(R.string.degrees_from_int, value.toInt())
        }
    }

    private fun getDateTimeFromTimeStamp(timestamp: Long) : String {
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        val dateTime = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()

        return dateTime.format(formatter)
    }

    private fun getDurationString(timeInSeconds : Long) : String {
        var seconds = timeInSeconds
        val days = seconds / 86400L
        seconds %= 86400L
        val hours = seconds / 3600L
        seconds %= 3600L
        val minutes = seconds / 60L
        seconds %= 60L
        val parts = mutableListOf<String>()

        if (days > 0) parts.add(resources.getQuantityString(R.plurals.days, days.toInt(), days))
        if (hours > 0) parts.add(resources.getQuantityString(R.plurals.hours, hours.toInt(), hours))
        if ((minutes > 0) || (parts.isEmpty())) parts.add(resources.getQuantityString(R.plurals.minutes, minutes.toInt(), minutes))

        if (parts.size == 1) return parts[0]

        val string = parts.subList(0, parts.size - 1).joinToString(getString(R.string.middle_lists_separator))
        return string + getString(R.string.last_lists_separator) + parts.last()
    }

    private fun showUnfoldsCount() {
        val foldCountPref = findPreference<Preference>(FoldCounterService.UNFOLDS_COUNT_KEY)
        val foldTimePref = findPreference<Preference>(FoldCounterService.TIME_UNFOLDED_KEY)
        var foldCountPrefSummary = getString(R.string.unfolds_count_none)
        val unfoldsCount = prefs.getInt(FoldCounterService.UNFOLDS_COUNT_KEY, 0)
        val unfoldedTime = prefs.getLong(FoldCounterService.TIME_UNFOLDED_KEY, 0)
        val foldedTime = prefs.getLong(FoldCounterService.TIME_FOLDED_KEY, 0)
        val totalTime = unfoldedTime + foldedTime

        if (unfoldsCount != 0) {
            foldCountPrefSummary = getString(R.string.unfolds_count_other, unfoldsCount, getDateTimeFromTimeStamp(prefs.getLong(FoldCounterService.UNFOLDS_COUNT_START_TIME_KEY, 0)))
        }

        foldCountPref?.summary = foldCountPrefSummary
        foldTimePref?.summary = getString(R.string.unfolded_time_description, getDurationString(unfoldedTime), getString(if (totalTime == 0L) R.string.percent_from_int else R.string.percent_from_float, if (totalTime == 0L) 100 else (unfoldedTime.toFloat() / totalTime.toFloat()) * 100))
    }
    private fun showCurrentValues() {
        showCurrentThreshold()

        showUnfoldsCount()
    }
}
