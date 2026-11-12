package com.jarvis.personalassistant.i18n

enum class AppLanguage(val code: String, val displayName: String, val speechLocale: String) {
    ENGLISH("en", "English", "en-IN"),
    HINDI("hi", "हिंदी (Hindi)", "hi-IN"),
    HINGLISH("hinglish", "Hinglish (हिंग्लिश)", "en-IN")
}

sealed class ParsedDeviceCommand {
    data class SetAlarm(val hour24: Int, val minute: Int, val label: String) : ParsedDeviceCommand()
    data class OpenApp(val targetAppName: String, val resolvedPackage: String?) : ParsedDeviceCommand()
    data class OpenSystemSettings(val settingSection: String) : ParsedDeviceCommand()
    data class PrepareDialCall(val phoneNumber: String) : ParsedDeviceCommand()
    data class SummarizeWhatsApp(val filterPriorityOnly: Boolean) : ParsedDeviceCommand()
    data class GeneralAiQuery(val rawPrompt: String, val detectedLanguage: AppLanguage) : ParsedDeviceCommand()
}

object LocalizationManager {

    fun greeting(language: AppLanguage): String = when (language) {
        AppLanguage.ENGLISH -> "JARVIS Online. All 11 holographic subsystems nominal, Sir."
        AppLanguage.HINDI -> "जार्विस ऑनलाइन है। सभी 11 एआई और फ़ोन नियंत्रण प्रणालियाँ सक्रिय हैं।"
        AppLanguage.HINGLISH -> "JARVIS Online hai boss! Sabhi 11 AI modules aur phone controls ready hain."
    }

    fun detectLanguage(input: String): AppLanguage {
        val hasDevanagari = input.any { it in '\u0900'..'\u097F' }
        if (hasDevanagari) return AppLanguage.HINDI

        val lower = input.lowercase()
        val hinglishMarkers = listOf(
            "karo", "kardo", "batao", "lagao", "dikhao", "kholo", "bhejo",
            "subah", "shaam", "raat", "baje", "mein", "hai", "kya", "kaise", "aur"
        )
        val markerHits = hinglishMarkers.count { Regex("\\b$it\\b").containsMatchIn(lower) }
        return if (markerHits >= 1) AppLanguage.HINGLISH else AppLanguage.ENGLISH
    }

    fun parseCommand(rawInput: String): ParsedDeviceCommand {
        val text = rawInput.trim()
        val lower = text.lowercase()

        // 1. Alarm command in English, Hindi, or Hinglish
        // e.g. "Set alarm for 6:30 AM", "Kal subah 7:15 baje alarm lagao", "सुबह 6 बजे अलार्म लगाओ"
        if (lower.contains("alarm") || text.contains("अलार्म")) {
            val timeRegex = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm|subah|shaam|raat|सुबह|शाम)?")
            val match = timeRegex.find(lower)
            if (match != null) {
                var hour = match.groupValues[1].toIntOrNull() ?: 7
                val minute = match.groupValues[2].toIntOrNull() ?: 0
                val meridiem = match.groupValues[3]
                if (meridiem in listOf("pm", "shaam", "raat", "शाम") && hour < 12) {
                    hour += 12
                } else if (meridiem in listOf("am", "subah", "सुबह") && hour == 12) {
                    hour = 0
                }
                return ParsedDeviceCommand.SetAlarm(
                    hour24 = hour.coerceIn(0, 23),
                    minute = minute.coerceIn(0, 59),
                    label = "JARVIS Assisted Alarm"
                )
            }
        }

        // 2. WhatsApp summary intent
        if ((lower.contains("whatsapp") || text.contains("व्हाट्सएप")) &&
            (lower.contains("summar") || lower.contains("unread") || lower.contains("urgent") ||
                lower.contains("priority") || lower.contains("dikhao") || lower.contains("batao"))
        ) {
            val priorityOnly = lower.contains("urgent") || lower.contains("priority") || lower.contains("important")
            return ParsedDeviceCommand.SummarizeWhatsApp(filterPriorityOnly = priorityOnly)
        }

        // 3. Open Settings intent
        if (lower.contains("setting") || text.contains("सेटिंग")) {
            val section = when {
                lower.contains("wifi") || lower.contains("wi-fi") -> "WIFI"
                lower.contains("bluetooth") -> "BLUETOOTH"
                lower.contains("notification") -> "NOTIFICATION_LISTENER"
                lower.contains("accessibility") -> "ACCESSIBILITY"
                else -> "MAIN_SETTINGS"
            }
            return ParsedDeviceCommand.OpenSystemSettings(section)
        }

        // 4. Open App intent ("Open YouTube", "WhatsApp kholo", "Launch Chrome")
        val openPrefix = Regex("^(?:open|launch|start|kholo)\\s+([a-z0-9\\s]+)$")
        val openSuffix = Regex("^([a-z0-9\\s]+)\\s+(?:kholo|open karo|chalu karo)$")
        val appName = openPrefix.find(lower)?.groupValues?.get(1)?.trim()
            ?: openSuffix.find(lower)?.groupValues?.get(1)?.trim()

        if (!appName.isNullOrBlank()) {
            val knownPackages = mapOf(
                "whatsapp" to "com.whatsapp",
                "whatsapp business" to "com.whatsapp.w4b",
                "youtube" to "com.google.android.youtube",
                "chrome" to "com.android.chrome",
                "gmail" to "com.google.android.gm",
                "maps" to "com.google.android.apps.maps",
                "files" to "com.google.android.apps.nbu.files"
            )
            return ParsedDeviceCommand.OpenApp(
                targetAppName = appName,
                resolvedPackage = knownPackages[appName]
            )
        }

        // 5. Dial phone intent ("Call 9876543210", "Dial 112")
        val dialRegex = Regex("(?:call|dial|phone lagao)\\s+(\\+?\\d[\\d\\s-]{2,14})")
        val dialMatch = dialRegex.find(lower)
        if (dialMatch != null) {
            val cleanDigits = dialMatch.groupValues[1].replace(Regex("[^\\d+]"), "")
            return ParsedDeviceCommand.PrepareDialCall(cleanDigits)
        }

        return ParsedDeviceCommand.GeneralAiQuery(
            rawPrompt = text,
            detectedLanguage = detectLanguage(text)
        )
    }
}
