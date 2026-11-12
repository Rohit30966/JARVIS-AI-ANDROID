package com.jarvis.personalassistant

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.jarvis.personalassistant.automation.AutomationCenterManager
import com.jarvis.personalassistant.data.local.JarvisDatabase
import com.jarvis.personalassistant.security.SecureApiKeyVault

class JarvisApplication : Application() {

    lateinit var database: JarvisDatabase
        private set

    lateinit var keyVault: SecureApiKeyVault
        private set

    override fun onCreate() {
        super.onCreate()
        database = JarvisDatabase.getInstance(this)
        keyVault = SecureApiKeyVault(this)
        createNotificationChannels()
        AutomationCenterManager.schedulePeriodicBriefing(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val voiceChannel = NotificationChannel(
                CHANNEL_VOICE_HUD,
                "JARVIS Active Voice HUD",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active when JARVIS voice recognition is listening in foreground"
            }
            val briefingChannel = NotificationChannel(
                CHANNEL_BRIEFING,
                "JARVIS Briefings & Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Scheduled briefings, priority WhatsApp digests, and battery alerts"
            }
            manager.createNotificationChannel(voiceChannel)
            manager.createNotificationChannel(briefingChannel)
        }
    }

    companion object {
        const val CHANNEL_VOICE_HUD = "jarvis_voice_hud_channel"
        const val CHANNEL_BRIEFING = "jarvis_briefing_channel"
    }
}
