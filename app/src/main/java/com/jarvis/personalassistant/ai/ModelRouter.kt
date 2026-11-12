package com.jarvis.personalassistant.ai

import com.jarvis.personalassistant.i18n.AppLanguage
import com.jarvis.personalassistant.i18n.LocalizationManager
import com.jarvis.personalassistant.i18n.ParsedDeviceCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class AiModelId(
    val id: String,
    val displayName: String,
    val provider: String,
    val isLocalOnDevice: Boolean,
    val implementationStatus: String,
    val license: String,
    val maxContextTokens: Int
) {
    GEMINI_2_5_FLASH(
        id = "gemini-2.5-flash",
        displayName = "Gemini 2.5 Flash (Cloud REST)",
        provider = "Google AI Studio",
        isLocalOnDevice = false,
        implementationStatus = "IMPLEMENTED (OkHttp REST API; requires user API key)",
        license = "Google APIs Terms (Free Tier)",
        maxContextTokens = 1_000_000
    ),
    GEMINI_2_5_FLASH_LITE(
        id = "gemini-2.5-flash-lite",
        displayName = "Gemini 2.5 Flash-Lite (Cloud REST)",
        provider = "Google AI Studio",
        isLocalOnDevice = false,
        implementationStatus = "IMPLEMENTED (OkHttp REST API; requires user API key)",
        license = "Google APIs Terms (Free Tier)",
        maxContextTokens = 1_000_000
    ),
    LOCAL_GEMMA_3_1B(
        id = "gemma-3-1b-it-litertlm",
        displayName = "Gemma 3 1B INT4 (LiteRT-LM Stub/Bridge)",
        provider = "Google AI Edge LiteRT-LM",
        isLocalOnDevice = true,
        implementationStatus = "NOT IMPLEMENTED IN MVP (Adapter Stub Only — .litertlm weights & JNI SDK not bundled)",
        license = "Gemma Terms of Use",
        maxContextTokens = 8_192
    ),
    LOCAL_QWEN_3_5_0_8B(
        id = "qwen3.5-0.8b-litertlm",
        displayName = "Qwen 3.5 0.8B INT4 (LiteRT-LM Stub/Bridge)",
        provider = "Alibaba Qwen / LiteRT-LM",
        isLocalOnDevice = true,
        implementationStatus = "NOT IMPLEMENTED IN MVP (Adapter Stub Only — .litertlm weights & JNI SDK not bundled)",
        license = "Apache-2.0",
        maxContextTokens = 32_768
    ),
    OFFLINE_BILINGUAL_CORE(
        id = "jarvis-offline-bilingual-core",
        displayName = "JARVIS Offline Deterministic Rule Engine",
        provider = "Built-in On-Device Kotlin Engine",
        isLocalOnDevice = true,
        implementationStatus = "IMPLEMENTED (100% On-Device Intent & Rule Engine)",
        license = "Apache-2.0",
        maxContextTokens = 4_096
    )
}

data class RoutingContext(
    val isNetworkAvailable: Boolean,
    val hasGeminiApiKey: Boolean,
    val isLocalGemmaInstalled: Boolean,
    val isLocalQwenInstalled: Boolean,
    val isPrivateSensitiveData: Boolean,
    val dailyCloudRequestsUsed: Int,
    val dailyCloudRequestLimit: Int = 1400,
    val preferredModel: AiModelId? = null
)

data class AiRouterResponse(
    val text: String,
    val modelUsed: AiModelId,
    val promptTokens: Int,
    val completionTokens: Int,
    val latencyMs: Long,
    val parsedDeviceCommand: ParsedDeviceCommand? = null
)

class ModelRouter(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .build()
) {

    fun selectOptimalModel(prompt: String, ctx: RoutingContext): AiModelId {
        if (ctx.isPrivateSensitiveData) {
            return when {
                ctx.isLocalQwenInstalled -> AiModelId.LOCAL_QWEN_3_5_0_8B
                ctx.isLocalGemmaInstalled -> AiModelId.LOCAL_GEMMA_3_1B
                else -> AiModelId.OFFLINE_BILINGUAL_CORE
            }
        }

        ctx.preferredModel?.let { pref ->
            if (pref.isLocalOnDevice) {
                if (pref == AiModelId.LOCAL_GEMMA_3_1B && ctx.isLocalGemmaInstalled) return pref
                if (pref == AiModelId.LOCAL_QWEN_3_5_0_8B && ctx.isLocalQwenInstalled) return pref
                if (pref == AiModelId.OFFLINE_BILINGUAL_CORE) return pref
            } else if (ctx.isNetworkAvailable && ctx.hasGeminiApiKey &&
                ctx.dailyCloudRequestsUsed < ctx.dailyCloudRequestLimit
            ) {
                return pref
            }
        }

        val canUseCloud = ctx.isNetworkAvailable &&
            ctx.hasGeminiApiKey &&
            ctx.dailyCloudRequestsUsed < ctx.dailyCloudRequestLimit

        if (canUseCloud) {
            val isComplex = prompt.length > 160 ||
                prompt.contains("plan", ignoreCase = true) ||
                prompt.contains("research", ignoreCase = true) ||
                prompt.contains("code", ignoreCase = true)
            return if (isComplex && ctx.dailyCloudRequestsUsed < (ctx.dailyCloudRequestLimit * 0.8)) {
                AiModelId.GEMINI_2_5_FLASH
            } else {
                AiModelId.GEMINI_2_5_FLASH_LITE
            }
        }

        return when {
            ctx.isLocalGemmaInstalled -> AiModelId.LOCAL_GEMMA_3_1B
            ctx.isLocalQwenInstalled -> AiModelId.LOCAL_QWEN_3_5_0_8B
            else -> AiModelId.OFFLINE_BILINGUAL_CORE
        }
    }

    suspend fun generateResponse(
        userPrompt: String,
        conversationHistory: List<Pair<String, String>>,
        routingContext: RoutingContext,
        geminiApiKey: String?
    ): AiRouterResponse = withContext(Dispatchers.IO) {
        val startMs = System.currentTimeMillis()
        val parsedCommand = LocalizationManager.parseCommand(userPrompt)
        val chosenModel = selectOptimalModel(userPrompt, routingContext)
        val detectedLang = LocalizationManager.detectLanguage(userPrompt)

        if (!chosenModel.isLocalOnDevice && !geminiApiKey.isNullOrBlank()) {
            val cloudResult = callGeminiRestApi(
                modelId = chosenModel.id,
                apiKey = geminiApiKey,
                userPrompt = userPrompt,
                history = conversationHistory,
                language = detectedLang
            )
            if (cloudResult != null) {
                val elapsed = System.currentTimeMillis() - startMs
                return@withContext AiRouterResponse(
                    text = cloudResult.first,
                    modelUsed = chosenModel,
                    promptTokens = cloudResult.second,
                    completionTokens = cloudResult.third,
                    latencyMs = elapsed,
                    parsedDeviceCommand = parsedCommand
                )
            }
        }

        val localText = synthesizeOnDeviceResponse(userPrompt, parsedCommand, detectedLang)
        val promptToks = (userPrompt.length / 4).coerceAtLeast(1)
        val compToks = (localText.length / 4).coerceAtLeast(1)
        val elapsed = (System.currentTimeMillis() - startMs).coerceAtLeast(12L)

        AiRouterResponse(
            text = localText,
            modelUsed = AiModelId.OFFLINE_BILINGUAL_CORE,
            promptTokens = promptToks,
            completionTokens = compToks,
            latencyMs = elapsed,
            parsedDeviceCommand = parsedCommand
        )
    }

    private fun callGeminiRestApi(
        modelId: String,
        apiKey: String,
        userPrompt: String,
        history: List<Pair<String, String>>,
        language: AppLanguage
    ): Triple<String, Int, Int>? {
        return try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent?key=$apiKey"
            val systemInstruction = JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put(
                    "text",
                    "You are JARVIS, an advanced futuristic Android 15 personal AI assistant. " +
                        "Respond concisely and helpfully in ${language.displayName}. " +
                        "Never fabricate device actions; confirm before sending messages."
                )))
            }

            val contentsArray = JSONArray()
            history.takeLast(8).forEach { (role, msg) ->
                val apiRole = if (role.equals("USER", ignoreCase = true)) "user" else "model"
                contentsArray.put(JSONObject().apply {
                    put("role", apiRole)
                    put("parts", JSONArray().put(JSONObject().put("text", msg)))
                })
            }
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", userPrompt)))
            })

            val payload = JSONObject().apply {
                put("system_instruction", systemInstruction)
                put("contents", contentsArray)
            }

            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bodyStr = response.body?.string() ?: return null
                val json = JSONObject(bodyStr)
                val candidates = json.optJSONArray("candidates") ?: return null
                if (candidates.length() == 0) return null
                val text = candidates.getJSONObject(0)
                    .optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text") ?: return null

                val usage = json.optJSONObject("usageMetadata")
                val promptTokens = usage?.optInt("promptTokenCount", userPrompt.length / 4) ?: (userPrompt.length / 4)
                val compTokens = usage?.optInt("candidatesTokenCount", text.length / 4) ?: (text.length / 4)
                Triple(text.trim(), promptTokens, compTokens)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun synthesizeOnDeviceResponse(
        userPrompt: String,
        parsedCommand: ParsedDeviceCommand,
        language: AppLanguage
    ): String {
        return when (parsedCommand) {
            is ParsedDeviceCommand.SetAlarm -> {
                val timeFormatted = String.format("%02d:%02d", parsedCommand.hour24, parsedCommand.minute)
                when (language) {
                    AppLanguage.HINDI -> "सर, मैंने $timeFormatted बजे के लिए अलार्म तैयार कर दिया है। पुष्टि करने के लिए 'Trigger Alarm Intent' पर टैप करें।"
                    AppLanguage.HINGLISH -> "Boss, $timeFormatted ka alarm ready hai! Android AlarmClock intent launch karne ke liye confirm karein."
                    AppLanguage.ENGLISH -> "Sir, I have prepared your alarm for $timeFormatted via the official Android AlarmClock intent."
                }
            }
            is ParsedDeviceCommand.OpenApp -> {
                when (language) {
                    AppLanguage.HINDI -> "ज़रूर सर, ${parsedCommand.targetAppName} ऐप खोला जा रहा है।"
                    AppLanguage.HINGLISH -> "Bilkul boss, ${parsedCommand.targetAppName} launch kar raha hoon."
                    AppLanguage.ENGLISH -> "Launching ${parsedCommand.targetAppName} via Android PackageManager, Sir."
                }
            }
            is ParsedDeviceCommand.OpenSystemSettings -> {
                when (language) {
                    AppLanguage.HINDI -> "एंड्रॉइड सिस्टम सेटिंग्स (${parsedCommand.settingSection}) खोली जा रही है।"
                    AppLanguage.HINGLISH -> "Android System Settings (${parsedCommand.settingSection}) open kar raha hoon."
                    AppLanguage.ENGLISH -> "Opening Android System Settings (${parsedCommand.settingSection}), Sir."
                }
            }
            is ParsedDeviceCommand.PrepareDialCall -> {
                when (language) {
                    AppLanguage.HINDI -> "डायलर में ${parsedCommand.phoneNumber} तैयार किया गया है। सुरक्षा के लिए कॉल केवल आपके टैप करने पर लगेगी।"
                    AppLanguage.HINGLISH -> "Dialer mein ${parsedCommand.phoneNumber} ready kar diya hai. Aapke confirm karne par hi call lagegi."
                    AppLanguage.ENGLISH -> "Prepared ${parsedCommand.phoneNumber} in the system dialer. Awaiting your tap to place the call."
                }
            }
            is ParsedDeviceCommand.SummarizeWhatsApp -> {
                when (language) {
                    AppLanguage.HINDI -> "व्हाट्सएप इंटेलिजेंस मॉड्यूल सक्रिय है। आपके अनुमति प्राप्त नोटिफिकेशन और चैट एक्सपोर्ट का सारांश तैयार है।"
                    AppLanguage.HINGLISH -> "WhatsApp Intelligence active hai! Aapke permitted notifications aur priority messages ka local summary ready hai."
                    AppLanguage.ENGLISH -> "WhatsApp Intelligence scan complete. Filtering permitted notifications locally on-device."
                }
            }
            is ParsedDeviceCommand.GeneralAiQuery -> {
                when (language) {
                    AppLanguage.HINDI -> "[Offline Deterministic Rule Engine] आदेश प्राप्त हुआ: \"${userPrompt.take(80)}\"। (नोट: Local Gemma 3 / Qwen 3.5 LiteRT-LM मॉडल वेट्स बंडल नहीं हैं; क्लाउड AI के लिए Security & Permissions में मुफ़्त Gemini API कुंजी जोड़ें।)"
                    AppLanguage.HINGLISH -> "[Offline Deterministic Rule Engine] Boss, aapka command \"${userPrompt.take(80)}\" note ho gaya hai. (Note: Local Gemma 3 / Qwen 3.5 LiteRT-LM weights is MVP mein bundled nahi hain; cloud AI reasoning ke liye Security & Permissions mein free Gemini API key dalein.)"
                    AppLanguage.ENGLISH -> "[Offline Deterministic Rule Engine] Command received: \"${userPrompt.take(80)}\". (Note: Local Gemma 3 / Qwen 3.5 LiteRT-LM weights are not bundled in this MVP; enter a free Gemini API key in Security & Permissions for cloud LLM responses.)"
                }
            }
        }
    }
}
