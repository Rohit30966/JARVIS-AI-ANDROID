package com.jarvis.personalassistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jarvis.personalassistant.ai.AiModelId
import com.jarvis.personalassistant.ai.ModelRouter
import com.jarvis.personalassistant.ai.RoutingContext
import com.jarvis.personalassistant.automation.AutomationCenterManager
import com.jarvis.personalassistant.control.AndroidPhoneController
import com.jarvis.personalassistant.control.JarvisAccessibilityService
import com.jarvis.personalassistant.data.local.MemoryFactEntity
import com.jarvis.personalassistant.data.local.WhatsAppNotificationEntity
import com.jarvis.personalassistant.files.FileIntelligenceAnalyzer
import com.jarvis.personalassistant.i18n.AppLanguage
import com.jarvis.personalassistant.i18n.LocalizationManager
import com.jarvis.personalassistant.i18n.ParsedDeviceCommand
import com.jarvis.personalassistant.research.WebResearchAgent
import com.jarvis.personalassistant.security.SecureApiKeyVault
import com.jarvis.personalassistant.ui.components.GlassPanel
import com.jarvis.personalassistant.ui.components.JarvisOrbView
import com.jarvis.personalassistant.ui.components.VoiceWaveformAnimation
import com.jarvis.personalassistant.ui.theme.JarvisPalette
import com.jarvis.personalassistant.whatsapp.WhatsAppIntelligenceEngine
import kotlinx.coroutines.launch

enum class JarvisModule(val title: String, val shortBadge: String) {
    COMMAND_CENTER("1. Command Center", "HUD"),
    AI_CHAT("2. AI Chat", "CHAT"),
    MODEL_HUB("3. Multi-AI Model Hub", "MODELS"),
    VOICE_ASSISTANT("4. Voice Assistant", "VOICE"),
    WHATSAPP_INTEL("5. WhatsApp Intelligence", "WA-AI"),
    PHONE_CONTROL("6. Android Phone Control", "CTRL"),
    WEB_RESEARCH("7. Web Research Agent", "WEB"),
    AUTOMATION("8. Automation Center", "AUTO"),
    MEMORY_CENTER("9. Memory Center", "MEM"),
    FILE_INTEL("10. File Intelligence", "FILES"),
    SECURITY("11. Security & Permissions", "SEC")
}

@Composable
fun JarvisMasterHud(
    keyVault: SecureApiKeyVault,
    onStartVoice: (AppLanguage) -> Unit,
    onStopVoice: () -> Unit,
    onSpeakText: (String, AppLanguage) -> Unit,
    isListening: Boolean,
    isSpeaking: Boolean,
    rmsLevel: Float
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val modelRouter = remember { ModelRouter() }
    val researchAgent = remember { WebResearchAgent() }

    var selectedModule by remember { mutableStateOf(JarvisModule.COMMAND_CENTER) }
    var currentLanguage by remember { mutableStateOf(AppLanguage.HINGLISH) }
    var selectedModel by remember { mutableStateOf(AiModelId.GEMINI_2_5_FLASH) }
    var strictPrivacyMode by remember { mutableStateOf(keyVault.isStrictLocalPrivacyMode()) }
    var totalTokensUsed by remember { mutableStateOf(1240) }
    var commandInput by remember { mutableStateOf("") }

    val chatHistory = remember {
        mutableStateListOf(
            "JARVIS" to LocalizationManager.greeting(AppLanguage.HINGLISH)
        )
    }

    val sampleWhatsAppItems = remember {
        mutableStateListOf(
            WhatsAppIntelligenceEngine.classifyNotification(
                "com.whatsapp",
                "Rohit (Project Lead)",
                "Bhai kal subah 9 baje Android 15 APK release urgent hai, please check GitHub Actions build!"
            ),
            WhatsAppIntelligenceEngine.classifyNotification(
                "com.whatsapp",
                "Mom",
                "Beta shaam ko ghar aate waqt fruits lete aana."
            ),
            WhatsAppIntelligenceEngine.classifyNotification(
                "com.whatsapp.w4b",
                "Cloud Bank Alert",
                "Your verification OTP code is 482910. Do not share this code."
            )
        )
    }

    val memories = remember {
        mutableStateListOf(
            MemoryFactEntity(1, "PROJECT", "JARVIS Android 15 Target", "Package com.jarvis.personalassistant compiled via GitHub Actions cloud workflow.", true),
            MemoryFactEntity(2, "PREFERENCE", "Bilingual Hinglish Mode", "User prefers Hindi, English, and Hinglish code-switched responses.", true)
        )
    }

    val automationRules = remember {
        mutableStateListOf(*AutomationCenterManager.defaultRules().toTypedArray())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisPalette.DeepVoid)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // Top Futuristic Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "J.A.R.V.I.S. // OS-15 HUD",
                    style = MaterialTheme.typography.headlineLarge,
                    color = JarvisPalette.ArcCyan
                )
                Text(
                    text = "MODEL: ${selectedModel.displayName} • LANG: ${currentLanguage.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = JarvisPalette.TextSecondary
                )
            }
            Button(
                onClick = {
                    currentLanguage = when (currentLanguage) {
                        AppLanguage.ENGLISH -> AppLanguage.HINDI
                        AppLanguage.HINDI -> AppLanguage.HINGLISH
                        AppLanguage.HINGLISH -> AppLanguage.ENGLISH
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = JarvisPalette.CyberSlate)
            ) {
                Text(text = currentLanguage.code.uppercase(), color = JarvisPalette.ArcCyan)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal 11-Module Holographic Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            JarvisModule.entries.forEach { module ->
                val isSelected = module == selectedModule
                val bg = if (isSelected) JarvisPalette.ArcCyan else JarvisPalette.CyberSlate
                val fg = if (isSelected) JarvisPalette.DeepVoid else JarvisPalette.ArcCyan
                Text(
                    text = "${module.shortBadge} • ${module.title}",
                    style = MaterialTheme.typography.labelSmall,
                    color = fg,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg)
                        .clickable { selectedModule = module }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Main Module Viewport
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedModule) {
                JarvisModule.COMMAND_CENTER -> {
                    GlassPanel(title = "Central AI Reactor Core", badgeText = "API 35 ONLINE") {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            JarvisOrbView(
                                isListening = isListening,
                                isSpeaking = isSpeaking,
                                audioRmsLevel = rmsLevel
                            )
                            VoiceWaveformAnimation(isActive = isListening || isSpeaking, rmsLevel = rmsLevel)
                            Text(
                                text = LocalizationManager.greeting(currentLanguage),
                                style = MaterialTheme.typography.bodyMedium,
                                color = JarvisPalette.TextPrimary
                            )
                        }
                    }

                    GlassPanel(title = "Quick Holographic Command Bar", badgeText = "TRILINGUAL") {
                        OutlinedTextField(
                            value = commandInput,
                            onValueChange = { commandInput = it },
                            label = { Text("Ask in English, हिंदी, or Hinglish (e.g. 'Kal subah 7 baje alarm lagao')") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (commandInput.isNotBlank()) {
                                        val prompt = commandInput
                                        commandInput = ""
                                        chatHistory.add("USER" to prompt)
                                        scope.launch {
                                            val resp = modelRouter.generateResponse(
                                                userPrompt = prompt,
                                                conversationHistory = chatHistory,
                                                routingContext = RoutingContext(
                                                    isNetworkAvailable = true,
                                                    hasGeminiApiKey = keyVault.getGeminiApiKey() != null,
                                                    isLocalGemmaInstalled = keyVault.getLocalModelPath("gemma3") != null,
                                                    isLocalQwenInstalled = keyVault.getLocalModelPath("qwen35") != null,
                                                    isPrivateSensitiveData = strictPrivacyMode,
                                                    dailyCloudRequestsUsed = 12,
                                                    preferredModel = selectedModel
                                                ),
                                                geminiApiKey = keyVault.getGeminiApiKey()
                                            )
                                            totalTokensUsed += (resp.promptTokens + resp.completionTokens)
                                            chatHistory.add("JARVIS" to resp.text)
                                            onSpeakText(resp.text, currentLanguage)
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = JarvisPalette.ArcCyan)
                            ) {
                                Text("EXECUTE COMMAND", color = JarvisPalette.DeepVoid, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { if (isListening) onStopVoice() else onStartVoice(currentLanguage) },
                                colors = ButtonDefaults.buttonColors(containerColor = JarvisPalette.HologramBlue)
                            ) {
                                Text(if (isListening) "STOP MIC" else "VOICE MIC", color = JarvisPalette.DeepVoid)
                            }
                        }
                    }

                    GlassPanel(title = "Real-Time Telemetry & Subsystems", badgeText = "11 MODULES") {
                        Text("• Active Model Router: ${selectedModel.displayName}", style = MaterialTheme.typography.bodyMedium)
                        Text("• Session Token Ledger: $totalTokensUsed tokens tracked", style = MaterialTheme.typography.bodyMedium)
                        Text("• WhatsApp Priority Queue: ${sampleWhatsAppItems.count { it.priorityScore >= 80 }} high-priority notifications", style = MaterialTheme.typography.bodyMedium)
                        Text("• Strict On-Device Privacy Mode: ${if (strictPrivacyMode) "ENABLED" else "HYBRID CLOUD/LOCAL"}", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                JarvisModule.AI_CHAT -> {
                    GlassPanel(title = "Multi-Turn Bilingual AI Chat", badgeText = currentLanguage.displayName) {
                        chatHistory.forEach { (speaker, msg) ->
                            val color = if (speaker == "USER") JarvisPalette.NeonTeal else JarvisPalette.ArcCyan
                            Text(
                                text = "[$speaker]: $msg",
                                style = MaterialTheme.typography.bodyLarge,
                                color = color,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                JarvisModule.MODEL_HUB -> {
                    GlassPanel(title = "Provider-Independent Model Router", badgeText = "ZERO LOCK-IN") {
                        AiModelId.entries.forEach { model ->
                            val active = model == selectedModel
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable { selectedModel = model },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (active) "★ ${model.displayName}" else model.displayName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (active) JarvisPalette.ArcCyan else JarvisPalette.TextPrimary
                                    )
                                    Text(
                                        text = "Status: ${model.implementationStatus} | Provider: ${model.provider} | License: ${model.license}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }

                JarvisModule.VOICE_ASSISTANT -> {
                    GlassPanel(title = "Neural Voice Assistant (hi-IN / en-IN)", badgeText = "SPEECH + TTS") {
                        JarvisOrbView(isListening = isListening, isSpeaking = isSpeaking, audioRmsLevel = rmsLevel, orbSize = 140.dp)
                        VoiceWaveformAnimation(isActive = isListening || isSpeaking, rmsLevel = rmsLevel)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onStartVoice(currentLanguage) }) { Text("Start Listening (${currentLanguage.speechLocale})") }
                            Button(onClick = { onSpeakText(LocalizationManager.greeting(currentLanguage), currentLanguage) }) { Text("Test TTS Voice") }
                        }
                    }
                }

                JarvisModule.WHATSAPP_INTEL -> {
                    var exportSampleInput by remember {
                        mutableStateOf(
                            "12/11/2026, 9:10 pm - Rohit: Kal subah 9 baje Android 15 APK build urgent hai\n" +
                                "12/11/2026, 9:12 pm - Ananya: Meeting presentation deck ready hai\n" +
                                "12/11/2026, 9:15 pm - Rohit: Great, GitHub Actions artifact verify kar lena"
                        )
                    }
                    val exportAnalysis = remember(exportSampleInput) {
                        WhatsAppIntelligenceEngine.analyzeExportedChatTxt(exportSampleInput)
                    }

                    GlassPanel(title = "Permitted WhatsApp Notification Intelligence", badgeText = "CONFIRM GATE") {
                        sampleWhatsAppItems.forEach { item ->
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Text(
                                    text = "[${item.category} • Score ${item.priorityScore}] ${item.senderName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (item.priorityScore >= 90) JarvisPalette.ReactorGold else JarvisPalette.ArcCyan
                                )
                                Text(text = item.messagePreview, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = "Suggested Draft: ${item.suggestedReplyDraft}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Button(
                                    onClick = {
                                        val intent = WhatsAppIntelligenceEngine.buildUserConfirmedWhatsAppReplyIntent(item.suggestedReplyDraft)
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text("Approve & Open Draft in WhatsApp")
                                }
                            }
                        }
                    }

                    GlassPanel(title = "Local WhatsApp Chat Export (.txt) Analyzer", badgeText = "100% ON-DEVICE") {
                        OutlinedTextField(
                            value = exportSampleInput,
                            onValueChange = { exportSampleInput = it },
                            label = { Text("Paste or Import WhatsApp Exported .txt Log") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = exportAnalysis.summaryReport, style = MaterialTheme.typography.bodyLarge, color = JarvisPalette.NeonTeal)
                    }
                }

                JarvisModule.PHONE_CONTROL -> {
                    GlassPanel(title = "Official Android 15 Intent Control", badgeText = "SAFE API") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { AndroidPhoneController.createSystemAlarm(context, 7, 0, "JARVIS Morning Briefing") }) {
                                Text("Set 07:00 Alarm")
                            }
                            Button(onClick = { AndroidPhoneController.openSystemSettings(context, "WIFI") }) {
                                Text("Wi-Fi Settings")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { AndroidPhoneController.openSystemSettings(context, "NOTIFICATION_LISTENER") }) {
                                Text("Notification Access")
                            }
                            Button(onClick = { AndroidPhoneController.openSystemSettings(context, "ACCESSIBILITY") }) {
                                Text("Accessibility Setup")
                            }
                        }
                    }
                }

                JarvisModule.WEB_RESEARCH -> {
                    var researchQuery by remember { mutableStateOf("LiteRT-LM Android 15 NPU acceleration") }
                    val plan = remember(researchQuery) { researchAgent.buildSearchPlan(researchQuery) }
                    GlassPanel(title = "Autonomous Web Research Planner", badgeText = "CITATIONS") {
                        OutlinedTextField(
                            value = researchQuery,
                            onValueChange = { researchQuery = it },
                            label = { Text("Research Topic or Documentation URL") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        plan.forEachIndexed { idx, step ->
                            Text(text = "${idx + 1}. $step", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

                JarvisModule.AUTOMATION -> {
                    GlassPanel(title = "WorkManager & Rule-Based Automation Center", badgeText = "PERSISTENT") {
                        automationRules.forEachIndexed { index, rule ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = rule.ruleName, style = MaterialTheme.typography.bodyLarge)
                                    Text(text = "Trigger: ${rule.triggerType} (${rule.triggerCondition}) -> ${rule.actionCommand}", style = MaterialTheme.typography.bodyMedium)
                                }
                                Switch(
                                    checked = rule.isEnabled,
                                    onCheckedChange = { checked ->
                                        automationRules[index] = rule.copy(isEnabled = checked)
                                    }
                                )
                            }
                        }
                    }
                }

                JarvisModule.MEMORY_CENTER -> {
                    GlassPanel(title = "Room Persistent Long-Term Memory Center", badgeText = "${memories.size} FACTS") {
                        memories.forEach { fact ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(text = "[${fact.category}] ${fact.title}", style = MaterialTheme.typography.titleMedium, color = JarvisPalette.ArcCyan)
                                Text(text = fact.factContent, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                JarvisModule.FILE_INTEL -> {
                    val sampleReport = remember {
                        FileIntelligenceAnalyzer.analyzeTextContent(
                            "project_notes.md",
                            "# JARVIS Android 15\nAll 11 modules verified with zero hardcoded keys."
                        )
                    }
                    GlassPanel(title = "On-Device File Intelligence (SAF)", badgeText = "ZERO STORAGE PERM") {
                        Text(text = sampleReport.executiveSummary, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                JarvisModule.SECURITY -> {
                    var apiKeyInput by remember { mutableStateOf("") }
                    GlassPanel(title = "Hardware Keystore Vault & Android 15 Permissions", badgeText = "AES-256") {
                        Text(
                            text = "• Microphone Permission: ${if (keyVault.isMicrophoneGranted()) "GRANTED" else "NOT GRANTED"}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "• Notification Listener: ${if (keyVault.isNotificationListenerEnabled()) "ENABLED" else "DISABLED (Enable in Settings)"}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "• Assistive Accessibility: ${if (keyVault.isAccessibilityServiceEnabled(JarvisAccessibilityService::class.java)) "ENABLED" else "DISABLED"}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Strict 100% On-Device Privacy Mode", modifier = Modifier.weight(1f))
                            Switch(
                                checked = strictPrivacyMode,
                                onCheckedChange = {
                                    strictPrivacyMode = it
                                    keyVault.setStrictLocalPrivacyMode(it)
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            label = { Text("Enter Personal Gemini Free-Tier API Key (Encrypted in Hardware Keystore)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                if (apiKeyInput.isNotBlank()) {
                                    keyVault.saveGeminiApiKey(apiKeyInput)
                                    apiKeyInput = ""
                                }
                            }) {
                                Text("Save Key to AES-256 Vault")
                            }
                            Button(
                                onClick = { keyVault.wipeAllSecrets() },
                                colors = ButtonDefaults.buttonColors(containerColor = JarvisPalette.CrimsonAlert)
                            ) {
                                Text("Wipe Vault")
                            }
                        }
                    }
                }
            }
        }
    }
}
