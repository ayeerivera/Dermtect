package com.example.dermtect

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import org.osmdroid.config.Configuration

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // --- OSMdroid config (yours) ---
        Configuration.getInstance().apply {
            userAgentValue = "Dermtect/1.0 ($packageName)"
            load(this@App, getSharedPreferences("osmdroid", MODE_PRIVATE))
        }

        // --- Create a NON-RESERVED notification channel (Android 8+) ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Use your own ID; avoid "miscellaneous" and DEFAULT_CHANNEL_ID
            val channelId = CHANNEL_ID   // "dermtect_general_v1"
            val channelName = "General Notifications"
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Dermtect general notifications"
            }
            val nm = getSystemService(NotificationManager::class.java)
            try {
                nm.createNotificationChannel(channel)
            } catch (t: Throwable) {
                // Fallback: log only; don't crash the app on misconfigured devices
                t.printStackTrace()
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "dermtect_general_v1"
    }
}
