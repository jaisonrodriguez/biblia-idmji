package org.idmji.biblia.ui.components

import java.net.URI

internal data class TranscriptionParagraph(val text: String, val timestamp: String? = null, val url: String? = null)

internal object TranscriptionParser {
    private val marker = Regex("""\[\[(\d{1,2}:[0-5]\d(?::[0-5]\d)?)\]\]\((https?://[^\)]+)\)""")

    fun parse(text: String): List<TranscriptionParagraph> = buildList {
        for (line in text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }) {
            val matches = marker.findAll(line).toList()
            if (matches.isEmpty()) {
                add(TranscriptionParagraph(line))
                continue
            }
            val prefix = line.substring(0, matches.first().range.first).trim()
            if (prefix.isNotEmpty()) add(TranscriptionParagraph(prefix))
            matches.forEachIndexed { index, match ->
                val label = match.groupValues[1]
                val content = line.substring(match.range.last + 1,
                    matches.getOrNull(index + 1)?.range?.first ?: line.length).trim()
                val url = videoUrl(match.groupValues[2], label)
                add(if (url == null) TranscriptionParagraph("${match.value} $content".trim())
                    else TranscriptionParagraph(content, label, url))
            }
        }
    }

    private fun videoUrl(url: String, label: String): String? {
        val uri = try { URI(url) } catch (_: Exception) { return null }
        if (uri.scheme != "https" || uri.host !in setOf("www.youtube.com", "youtube.com", "m.youtube.com", "youtu.be")) return null
        val parts = label.split(':').map { it.toInt() }
        val seconds = parts.fold(0) { total, part -> total * 60 + part }
        val parameters = uri.rawQuery.orEmpty().split('&').filter { it.isNotEmpty() && it.substringBefore('=') != "t" }
        val query = (parameters + "t=${seconds}s").joinToString("&")
        return url.substringBefore('#').substringBefore('?') + "?" + query + (uri.rawFragment?.let { "#$it" } ?: "")
    }
}
