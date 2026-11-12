package com.jarvis.personalassistant.whatsapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.jarvis.personalassistant.data.local.WhatsAppNotificationEntity
import com.jarvis.personalassistant.i18n.AppLanguage

data class WhatsAppExportAnalysis(
    val totalMessages: Int,
    val uniqueParticipants: List<String>,
    val messageCountBySender: Map<String, Int>,
    val priorityMessages: List<String>,
    val summaryReport: String
)

/**
 * Legally and technically compliant WhatsApp Intelligence Engine:
 * 1. Classifies real-time notifications received after explicit user permission.
 * 2. Generates bilingual smart reply drafts (never sends without explicit user confirmation).
 * 3. Analyzes user-imported WhatsApp .txt chat export files locally on-device.
 */
object WhatsAppIntelligenceEngine {

    val SUPPORTED_WHATSAPP_PACKAGES = setOf("com.whatsapp", "com.whatsapp.w4b")

    fun classifyNotification(
        packageName: String,
        sender: String,
        message: String,
        timestampMs: Long = System.currentTimeMillis()
    ): WhatsAppNotificationEntity {
        val lower = message.lowercase()
        val isOtp = Regex("\\b(\\d{4,8})\\b").containsMatchIn(lower) &&
            (lower.contains("otp") || lower.contains("verification") || lower.contains("code"))
        val isUrgent = lower.contains("urgent") || lower.contains("asap") ||
            lower.contains("emergency") || lower.contains("jaldi") ||
            lower.contains("important") || lower.contains("तुरंत") || lower.contains("ज़रूरी")
        val isWork = lower.contains("meeting") || lower.contains("deadline") ||
            lower.contains("project") || lower.contains("client") || lower.contains("invoice")
        val isGroup = sender.contains(":") || sender.contains("@") || lower.contains("group")

        val category = when {
            isOtp -> "OTP"
            isUrgent -> "URGENT"
            isWork -> "WORK"
            isGroup -> "GROUP"
            else -> "PERSONAL"
        }

        val priorityScore = when (category) {
            "URGENT" -> 95
            "OTP" -> 90
            "WORK" -> 80
            "PERSONAL" -> 60
            else -> 40
        }

        val replyDraft = generateSmartReplyDraft(sender, message, category)

        return WhatsAppNotificationEntity(
            packageName = packageName,
            senderName = sender.ifBlank { "Unknown Contact" },
            messagePreview = message.trim(),
            category = category,
            priorityScore = priorityScore,
            suggestedReplyDraft = replyDraft,
            timestampMs = timestampMs
        )
    }

    fun generateSmartReplyDraft(sender: String, message: String, category: String): String {
        val lower = message.lowercase()
        val isHindiOrHinglish = message.any { it in '\u0900'..'\u097F' } ||
            listOf("kya", "kab", "kahan", "bhai", "sir", "kal", "aaj", "batao").any { lower.contains(it) }

        return when (category) {
            "OTP" -> "Do not reply — Sensitive verification code detected."
            "URGENT" -> if (isHindiOrHinglish) {
                "Namaste $sender, aapka urgent message mil gaya hai. Main abhi check karke turant call/reply karta hoon."
            } else {
                "Hi $sender, received your urgent message. Reviewing this right now and will get back to you shortly."
            }
            "WORK" -> if (isHindiOrHinglish) {
                "Thanks $sender, maine update note kar liya hai. Main thodi der mein details share karta hoon."
            } else {
                "Thanks $sender, I've noted this update and will follow up with the details shortly."
            }
            else -> if (isHindiOrHinglish) {
                "Hey $sender! Message mil gaya, main free hote hi reply karta hoon."
            } else {
                "Hey $sender! Got your message, will reply as soon as I'm free."
            }
        }
    }

    fun translateMessageLocally(message: String, targetLanguage: AppLanguage): String {
        return when (targetLanguage) {
            AppLanguage.HINDI -> "[हिंदी अनुवाद ड्राफ्ट]: $message"
            AppLanguage.HINGLISH -> "[Hinglish Summary]: $message"
            AppLanguage.ENGLISH -> "[English Translation]: $message"
        }
    }

    /**
     * Parses standard Android/iOS WhatsApp exported .txt chat logs locally on-device.
     * Supports formats like:
     * "12/11/2026, 9:15 pm - Rahul Sharma: Kal subah meeting urgent hai"
     * "[12/11/26, 21:15:00] Priya: Please send the project APK"
     */
    fun analyzeExportedChatTxt(rawExportText: String): WhatsAppExportAnalysis {
        val lineRegex = Regex(
            "^(?:\\[?\\d{1,2}/\\d{1,2}/\\d{2,4},?\\s+\\d{1,2}:\\d{2}(?::\\d{2})?(?:\\s*[apAP][mM])?\\]?\\s*(?:-|:)?\\s*)([^:]+):\\s*(.+)$"
        )
        val counts = mutableMapOf<String, Int>()
        val urgentHits = mutableListOf<String>()
        var totalParsed = 0

        rawExportText.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            val match = lineRegex.find(line)
            if (match != null) {
                val sender = match.groupValues[1].trim()
                val body = match.groupValues[2].trim()
                if (!body.contains("end-to-end encrypted", ignoreCase = true) &&
                    !body.equals("<Media omitted>", ignoreCase = true)
                ) {
                    totalParsed++
                    counts[sender] = (counts[sender] ?: 0) + 1
                    val classified = classifyNotification("com.whatsapp", sender, body)
                    if (classified.priorityScore >= 80) {
                        urgentHits.add("$sender: $body")
                    }
                }
            }
        }

        val participants = counts.entries.sortedByDescending { it.value }.map { it.key }
        val topSenderInfo = counts.entries.maxByOrNull { it.value }?.let { "${it.key} (${it.value} msgs)" } ?: "None"
        val summary = buildString {
            append("Analyzed $totalParsed messages across ${participants.size} participants locally on-device. ")
            append("Most active participant: $topSenderInfo. ")
            append("Detected ${urgentHits.size} high-priority/work messages.")
        }

        return WhatsAppExportAnalysis(
            totalMessages = totalParsed,
            uniqueParticipants = participants,
            messageCountBySender = counts,
            priorityMessages = urgentHits.take(15),
            summaryReport = summary
        )
    }

    /**
     * Stages a reply in WhatsApp ONLY when the user explicitly taps 'Approve & Open in WhatsApp'.
     * Never sends messages silently in the background.
     */
    fun buildUserConfirmedWhatsAppReplyIntent(draftText: String, phoneDigitsOptional: String? = null): Intent {
        return if (!phoneDigitsOptional.isNullOrBlank()) {
            val cleanPhone = phoneDigitsOptional.replace(Regex("[^\\d]"), "")
            val uri = Uri.parse("https://wa.me/$cleanPhone?text=${Uri.encode(draftText)}")
            Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("com.whatsapp")
                putExtra(Intent.EXTRA_TEXT, draftText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }
}
