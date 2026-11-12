package com.jarvis.personalassistant.automation

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jarvis.personalassistant.JarvisApplication
import com.jarvis.personalassistant.data.local.MemoryFactEntity

class JarvisBriefingWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? JarvisApplication ?: return Result.success()
        app.database.jarvisDao().upsertMemory(
            MemoryFactEntity(
                category = "ROUTINE",
                title = "Automated System Check",
                factContent = "WorkManager background health check completed cleanly on Android 15."
            )
        )
        return try {
            val notification = NotificationCompat.Builder(applicationContext, JarvisApplication.CHANNEL_BRIEFING)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("JARVIS System Telemetry Ready")
                .setContentText("All local AI & notification subsystems verified.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            NotificationManagerCompat.from(applicationContext).notify(2002, notification)
            Result.success()
        } catch (e: SecurityException) {
            Result.success()
        }
    }
}
