package com.jarvis.personalassistant.research

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class ResearchBrief(
    val topic: String,
    val subQueries: List<String>,
    val extractedSummary: String,
    val sourceUrl: String,
    val timestampMs: Long = System.currentTimeMillis()
)

class WebResearchAgent(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {

    fun buildSearchPlan(topic: String): List<String> {
        val clean = topic.trim()
        return listOf(
            "$clean official documentation overview",
            "$clean latest specifications and benchmarks",
            "$clean open source implementation best practices"
        )
    }

    suspend fun fetchUrlReadableExcerpt(url: String): ResearchBrief = withContext(Dispatchers.IO) {
        val subQueries = buildSearchPlan(url)
        return@withContext try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "JarvisPersonalAssistant/1.0 (Android 15)")
                .build()
            client.newCall(req).execute().use { resp ->
                val rawHtml = resp.body?.string().orEmpty()
                val textOnly = rawHtml
                    .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
                    .replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
                    .replace(Regex("<[^>]+>"), " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                    .take(900)
                ResearchBrief(
                    topic = url,
                    subQueries = subQueries,
                    extractedSummary = textOnly.ifBlank { "Page fetched (HTTP ${resp.code}), no plain text body found." },
                    sourceUrl = url
                )
            }
        } catch (e: Exception) {
            ResearchBrief(
                topic = url,
                subQueries = subQueries,
                extractedSummary = "Offline or unreachable URL (${e.javaClass.simpleName}). Saved query plan for next online sync.",
                sourceUrl = url
            )
        }
    }
}
