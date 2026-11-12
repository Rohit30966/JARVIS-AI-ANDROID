package com.jarvis.personalassistant.automation

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.jarvis.personalassistant.data.local.AutomationRuleEntity
import java.util.concurrent.TimeUnit

object AutomationCenterManager {

    private const val BRIEFING_WORK_NAME = "jarvis_periodic_briefing_work"

    fun defaultRules(): List<AutomationRuleEntity> = listOf(
        AutomationRuleEntity(
            id = 1,
            ruleName = "Morning Executive Briefing (07:30)",
            triggerType = "TIME_DAILY",
            triggerCondition = "07:30",
            actionCommand = "Summarize overnight priority WhatsApp notifications and battery status",
            isEnabled = true
        ),
        AutomationRuleEntity(
            id = 2,
            ruleName = "Low Battery AI Power Saver (< 20%)",
            triggerType = "BATTERY_LOW",
            triggerCondition = "20",
            actionCommand = "Switch ModelRouter to On-Device Offline Core to conserve battery",
            isEnabled = true
        ),
        AutomationRuleEntity(
            id = 3,
            ruleName = "Urgent WhatsApp Alert Digest",
            triggerType = "WHATSAPP_URGENT",
            triggerCondition = "PRIORITY>=90",
            actionCommand = "Prepare bilingual reply draft and pin to Command Center HUD",
            isEnabled = true
        )
    )

    fun evaluateRules(
        rules: List<AutomationRuleEntity>,
        currentTimeHhMm: String,
        batteryPercent: Int,
        highestWhatsAppPriority: Int
    ): List<AutomationRuleEntity> {
        return rules.filter { rule ->
            if (!rule.isEnabled) return@filter false
            when (rule.triggerType) {
                "TIME_DAILY" -> rule.triggerCondition == currentTimeHhMm
                "BATTERY_LOW" -> {
                    val threshold = rule.triggerCondition.toIntOrNull() ?: 20
                    batteryPercent <= threshold
                }
                "WHATSAPP_URGENT" -> {
                    val minScore = rule.triggerCondition.removePrefix("PRIORITY>=").toIntOrNull() ?: 90
                    highestWhatsAppPriority >= minScore
                }
                else -> false
            }
        }
    }

    fun schedulePeriodicBriefing(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<JarvisBriefingWorker>(6, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            BRIEFING_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
