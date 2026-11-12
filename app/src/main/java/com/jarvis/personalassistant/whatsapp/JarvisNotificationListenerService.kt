package com.jarvis.personalassistant.whatsapp

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.jarvis.personalassistant.JarvisApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Listens for permitted incoming notifications from WhatsApp and WhatsApp Business.
 * Stores local summaries in Room Database on-device without transmitting private chats externally.
 */
class JarvisNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val notification = sbn ?: return
        val pkg = notification.packageName ?: return
        if (pkg !in WhatsAppIntelligenceEngine.SUPPORTED_WHATSAPP_PACKAGES) return

        val extras = notification.notification?.extras ?: return
        val sender = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val message = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()

        if (sender.isBlank() || message.isBlank()) return
        if (message.contains("new messages", ignoreCase = true) && sender.equals("WhatsApp", ignoreCase = true)) {
            return
        }

        val entity = WhatsAppIntelligenceEngine.classifyNotification(
            packageName = pkg,
            sender = sender,
            message = message,
            timestampMs = notification.postTime
        )

        val app = applicationContext as? JarvisApplication ?: return
        serviceScope.launch {
            app.database.jarvisDao().insertWhatsAppNotification(entity)
        }
    }
}
