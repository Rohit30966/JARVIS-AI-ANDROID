package com.jarvis.personalassistant.files

import android.content.Context
import android.net.Uri
import com.jarvis.personalassistant.whatsapp.WhatsAppIntelligenceEngine
import java.io.BufferedReader
import java.io.InputStreamReader

data class LocalFileReport(
    val fileName: String,
    val lineCount: Int,
    val wordCount: Int,
    val detectedType: String,
    val executiveSummary: String
)

object FileIntelligenceAnalyzer {

    fun analyzeTextContent(fileName: String, content: String): LocalFileReport {
        val lines = content.lines()
        val words = content.split(Regex("\\s+")).filter { it.isNotBlank() }
        val isWhatsAppExport = content.contains("Messages and calls are end-to-end encrypted", ignoreCase = true) ||
            lines.take(10).any { Regex("^\\d{1,2}/\\d{1,2}/\\d{2,4},?\\s+\\d{1,2}:\\d{2}").containsMatchIn(it) }

        if (isWhatsAppExport) {
            val waReport = WhatsAppIntelligenceEngine.analyzeExportedChatTxt(content)
            return LocalFileReport(
                fileName = fileName,
                lineCount = lines.size,
                wordCount = words.size,
                detectedType = "WhatsApp Chat Export (.txt)",
                executiveSummary = waReport.summaryReport
            )
        }

        val detectedType = when {
            fileName.endsWith(".csv", ignoreCase = true) -> "Tabular CSV Dataset"
            fileName.endsWith(".json", ignoreCase = true) -> "Structured JSON Payload"
            fileName.endsWith(".md", ignoreCase = true) -> "Markdown Documentation"
            else -> "Plain Text Document"
        }

        val preview = lines.filter { it.isNotBlank() }.take(3).joinToString(" | ").take(240)
        val summary = "File '$fileName' ($detectedType): ${lines.size} lines, ${words.size} words. Key excerpt: $preview"

        return LocalFileReport(
            fileName = fileName,
            lineCount = lines.size,
            wordCount = words.size,
            detectedType = detectedType,
            executiveSummary = summary
        )
    }

    fun analyzeUri(context: Context, uri: Uri): LocalFileReport {
        val text = StringBuilder()
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                var line: String? = reader.readLine()
                var count = 0
                while (line != null && count < 5000) {
                    text.append(line).append("\n")
                    line = reader.readLine()
                    count++
                }
            }
        }
        val name = uri.lastPathSegment ?: "imported_document.txt"
        return analyzeTextContent(name, text.toString())
    }
}
