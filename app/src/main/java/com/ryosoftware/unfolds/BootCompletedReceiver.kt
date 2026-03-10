package com.ryosoftware.unfolds

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == Intent.ACTION_REBOOT ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d(TAG, "Disposition reinitialised, starting service")

            val serviceIntent = Intent(context, UnfoldsCounterService::class.java)
            context.startService(serviceIntent)
        }
    }

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }
}
