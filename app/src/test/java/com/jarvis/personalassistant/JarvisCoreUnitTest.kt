package com.jarvis.personalassistant

import com.jarvis.personalassistant.ai.AiModelId
import com.jarvis.personalassistant.ai.ModelRouter
import com.jarvis.personalassistant.ai.RoutingContext
import com.jarvis.personalassistant.automation.AutomationCenterManager
import com.jarvis.personalassistant.i18n.AppLanguage
import com.jarvis.personalassistant.i18n.LocalizationManager
import com.jarvis.personalassistant.i18n.ParsedDeviceCommand
import com.jarvis.personalassistant.whatsapp.WhatsAppIntelligenceEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JarvisCoreUnitTest {

    @Test
    fun testHinglishAlarmParsing() {
        val cmd = LocalizationManager.parseCommand("Kal subah 6:45 baje alarm lagao")
        assertTrue(cmd is ParsedDeviceCommand.SetAlarm)
        val alarm = cmd as ParsedDeviceCommand.SetAlarm
        assertEquals(6, alarm.hour24)
        assertEquals(45, alarm.minute)
    }

    @Test
    fun testEveningAlarmParsing() {
        val cmd = LocalizationManager.parseCommand("Set alarm for 7:30 pm")
        assertTrue(cmd is ParsedDeviceCommand.SetAlarm)
        val alarm = cmd as ParsedDeviceCommand.SetAlarm
        assertEquals(19, alarm.hour24)
        assertEquals(30, alarm.minute)
    }

    @Test
    fun testLanguageDetection_EnglishHindiHinglish() {
        assertEquals(AppLanguage.HINDI, LocalizationManager.detectLanguage("आज का मौसम कैसा है?"))
        assertEquals(AppLanguage.HINGLISH, LocalizationManager.detectLanguage("Bhai WhatsApp ke urgent messages dikhao"))
        assertEquals(AppLanguage.ENGLISH, LocalizationManager.detectLanguage("Summarize system telemetry status"))
    }

    @Test
    fun testModelRouter_PrivacyForcesLocalOnDevice() {
        val router = ModelRouter()
        val ctx = RoutingContext(
            isNetworkAvailable = true,
            hasGeminiApiKey = true,
            isLocalGemmaInstalled = true,
            isLocalQwenInstalled = true,
            isPrivateSensitiveData = true,
            dailyCloudRequestsUsed = 10
        )
        val selected = router.selectOptimalModel("Summarize my private WhatsApp chat", ctx)
        assertTrue(selected.isLocalOnDevice)
        assertEquals(AiModelId.LOCAL_QWEN_3_5_0_8B, selected)
    }

    @Test
    fun testModelRouter_QuotaExhaustionFallsBackToLocal() {
        val router = ModelRouter()
        val ctx = RoutingContext(
            isNetworkAvailable = true,
            hasGeminiApiKey = true,
            isLocalGemmaInstalled = true,
            isLocalQwenInstalled = false,
            isPrivateSensitiveData = false,
            dailyCloudRequestsUsed = 1500,
            dailyCloudRequestLimit = 1400
        )
        val selected = router.selectOptimalModel("Explain Android 15 edge-to-edge enforcement", ctx)
        assertEquals(AiModelId.LOCAL_GEMMA_3_1B, selected)
    }

    @Test
    fun testWhatsAppNotificationClassificationAndOtpSafety() {
        val otpEntity = WhatsAppIntelligenceEngine.classifyNotification(
            packageName = "com.whatsapp",
            sender = "HDFC Bank",
            message = "Your OTP verification code is 839201"
        )
        assertEquals("OTP", otpEntity.category)
        assertEquals(90, otpEntity.priorityScore)

        val urgentEntity = WhatsAppIntelligenceEngine.classifyNotification(
            packageName = "com.whatsapp",
            sender = "Team Lead",
            message = "Urgent: Production server deployment needed ASAP"
        )
        assertEquals("URGENT", urgentEntity.category)
        assertEquals(95, urgentEntity.priorityScore)
    }

    @Test
    fun testWhatsAppChatExportParser() {
        val sampleLog = """
            12/11/2026, 9:10 pm - Messages and calls are end-to-end encrypted.
            12/11/2026, 9:12 pm - Rohit: Bhai urgent APK build check karo
            12/11/2026, 9:14 pm - Priya: Sure, checking the deadline now
            12/11/2026, 9:15 pm - Rohit: Thanks!
        """.trimIndent()
        val report = WhatsAppIntelligenceEngine.analyzeExportedChatTxt(sampleLog)
        assertEquals(3, report.totalMessages)
        assertEquals(2, report.uniqueParticipants.size)
        assertEquals(2, report.messageCountBySender["Rohit"])
        assertTrue(report.priorityMessages.isNotEmpty())
    }

    @Test
    fun testAutomationRuleEvaluation() {
        val rules = AutomationCenterManager.defaultRules()
        val triggered = AutomationCenterManager.evaluateRules(
            rules = rules,
            currentTimeHhMm = "07:30",
            batteryPercent = 15,
            highestWhatsAppPriority = 95
        )
        assertEquals(3, triggered.size)
    }
}
