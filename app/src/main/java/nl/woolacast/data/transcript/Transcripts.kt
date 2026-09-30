package nl.woolacast.data.transcript

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import nl.woolacast.domain.TranscriptFormats
import nl.woolacast.domain.TranscriptRef
import okhttp3.OkHttpClient
import okhttp3.Request

/** Eén regel uit een transcriptie. Zonder tijden is [startMs] -1. */
data class TranscriptLine(
    val startMs: Long,
    val endMs: Long,
    val speaker: String?,
    val text: String
)

data class Transcript(val lines: List<TranscriptLine>, val timed: Boolean) {
    /** De regel die bij [positionMs] hoort: de laatste die al begonnen is. */
    fun indexAt(positionMs: Long): Int {
        if (!timed || lines.isEmpty()) return -1
        var low = 0
        var high = lines.size - 1
        var found = -1
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (lines[mid].startMs <= positionMs) { found = mid; low = mid + 1 } else high = mid - 1
        }
        return found
    }
}

/**
 * Haalt de transcriptie op die de maker in de feed aanwijst. De app maakt er
 * zelf geen: dat vraagt een server of veel rekenkracht op het toestel.
 */
class TranscriptRepository(private val client: OkHttpClient) {

    private companion object {
        const val MAX_BYTES = 5L * 1024 * 1024
    }

    private val cache = object : LinkedHashMap<String, Transcript>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Transcript>?) = size > 6
    }

    suspend fun load(ref: TranscriptRef): Transcript {
        synchronized(cache) { cache[ref.url] }?.let { return it }
        val body = withContext(Dispatchers.IO) {
            client.newCall(Request.Builder().url(ref.url).build()).execute().use { response ->
                if (!response.isSuccessful) throw IOException("De tekst is niet te vinden bij de maker (${response.code}).")
                val body = response.body ?: throw IOException("Het tekstbestand is leeg.")
                // Een transcriptie van drie uur is een paar honderd kilobyte; wat veel groter is, is iets anders.
                val source = body.source()
                if (source.request(MAX_BYTES + 1)) throw IOException("Het tekstbestand is te groot om te tonen.")
                source.buffer.readString(body.contentType()?.charset() ?: Charsets.UTF_8)
            }
        }
        val parsed = withContext(Dispatchers.Default) { TranscriptParser.parse(body, ref.type) }
        if (parsed.lines.isEmpty()) throw IOException("Er staat geen tekst in dit bestand.")
        synchronized(cache) { cache[ref.url] = parsed }
        return parsed
    }
}

object TranscriptParser {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val timing = Regex("""((?:\d+:)?\d{1,2}:\d{2}[.,]\d{1,3})\s*-->\s*((?:\d+:)?\d{1,2}:\d{2}[.,]\d{1,3})""")
    private val voice = Regex("""<v(?:\.[^\s>]+)*\s+([^>]+)>""")
    private val tag = Regex("""<[^>]+>""")
    private val sentenceEnd = Regex("""[.!?…]["'”’)]?$""")

    fun parse(body: String, type: String): Transcript {
        val text = body.removePrefix("\uFEFF")
        return when (TranscriptFormats.normalise(type)) {
            "application/json" -> Transcript(merge(json(text)), timed = true)
            "text/vtt" -> Transcript(merge(cues(text, vtt = true)), timed = true)
            "application/x-subrip", "application/srt", "text/srt" -> Transcript(merge(cues(text, vtt = false)), timed = true)
            "text/html" -> Transcript(paragraphs(htmlToText(text)), timed = false)
            else -> sniff(text)
        }
    }

    /** Een onbekend type: kijk of het op een bekende vorm lijkt. */
    private fun sniff(text: String): Transcript {
        val start = text.trimStart()
        return when {
            start.startsWith("WEBVTT") -> Transcript(merge(cues(text, vtt = true)), timed = true)
            start.startsWith("{") -> Transcript(merge(json(text)), timed = true)
            timing.containsMatchIn(text) -> Transcript(merge(cues(text, vtt = false)), timed = true)
            start.startsWith("<") -> Transcript(paragraphs(htmlToText(text)), timed = false)
            else -> Transcript(paragraphs(text), timed = false)
        }
    }

    /** SRT en WebVTT: blokken met een tijdregel en daaronder de tekst. */
    private fun cues(text: String, vtt: Boolean): List<TranscriptLine> {
        val lines = mutableListOf<TranscriptLine>()
        text.replace("\r\n", "\n").replace('\r', '\n').split(Regex("\n\\s*\n")).forEach { block ->
            val rows = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val at = rows.indexOfFirst { timing.containsMatchIn(it) }
            if (at == -1) return@forEach // WEBVTT-kop, NOTE, STYLE, of een los nummer
            val match = timing.find(rows[at]) ?: return@forEach
            val raw = rows.drop(at + 1).joinToString(" ")
            if (raw.isEmpty()) return@forEach
            var speaker: String? = null
            if (vtt) voice.find(raw)?.let { speaker = it.groupValues[1].trim() }
            val clean = decode(raw.replace(tag, "")).replace(Regex("\\s+"), " ").trim()
            if (clean.isEmpty()) return@forEach
            lines += TranscriptLine(clock(match.groupValues[1]), clock(match.groupValues[2]), speaker, clean)
        }
        return lines.sortedBy { it.startMs }
    }

    /** Podcasting 2.0-JSON: {"segments":[{"speaker","startTime","endTime","body"}]}. */
    private fun json(text: String): List<TranscriptLine> {
        val root = json.parseToJsonElement(text) as? JsonObject ?: return emptyList()
        val segments = root["segments"] as? JsonArray ?: return emptyList()
        return segments.mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            val start = (item["startTime"] as? JsonPrimitive)?.doubleOrNull ?: return@mapNotNull null
            val end = (item["endTime"] as? JsonPrimitive)?.doubleOrNull ?: start
            val words = (item["body"] as? JsonPrimitive)?.content?.trim().orEmpty()
            if (words.isEmpty()) return@mapNotNull null
            TranscriptLine(
                (start * 1000).toLong(), (end * 1000).toLong(),
                (item["speaker"] as? JsonPrimitive)?.content?.trim()?.takeIf { it.isNotEmpty() },
                words
            )
        }.sortedBy { it.startMs }
    }

    /**
     * Voegt korte stukjes samen tot zinnen. JSON-transcripties staan vaak per
     * woord, en ondertitels breken midden in een zin af; meelezen gaat beter
     * per zin. Een nieuwe spreker of een lange stilte begint altijd een nieuwe regel.
     */
    internal fun merge(parts: List<TranscriptLine>): List<TranscriptLine> {
        val out = mutableListOf<TranscriptLine>()
        var current: TranscriptLine? = null
        for (part in parts) {
            val open = current
            val joinable = open != null &&
                !sentenceEnd.containsMatchIn(open.text) &&
                (part.speaker == null || part.speaker == open.speaker) &&
                part.startMs - open.endMs < 2_000 &&
                open.text.length + part.text.length < 280
            current = if (joinable && open != null) {
                open.copy(endMs = maxOf(open.endMs, part.endMs), text = join(open.text, part.text))
            } else {
                open?.let(out::add)
                part
            }
        }
        current?.let(out::add)
        return out
    }

    /** Woorden aan elkaar, zonder spatie voor leestekens. */
    private fun join(a: String, b: String): String =
        if (b.firstOrNull()?.let { it in ".,!?;:…)" } == true) a + b else "$a $b"

    private fun paragraphs(text: String): List<TranscriptLine> =
        text.replace("\r\n", "\n").split(Regex("\n\\s*\n|\n"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { TranscriptLine(-1, -1, null, it) }

    private fun htmlToText(html: String): String =
        decode(
            html.replace(Regex("(?is)<(script|style)[^>]*>.*?</\\1>"), "")
                .replace(Regex("(?i)<br\\s*/?>|</p>|</div>|</h\\d>|</li>"), "\n")
                .replace(tag, "")
        )

    private fun decode(text: String): String =
        // &amp; als laatste, anders wordt "&amp;lt;" twee keer ontsleuteld.
        text.replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"").replace("&#39;", "'").replace("&apos;", "'").replace("&amp;", "&")

    /** "01:02:03,500" of "02:03.500" in milliseconden. */
    internal fun clock(value: String): Long {
        val parts = value.replace(',', '.').split(':')
        var seconds = 0.0
        parts.forEach { seconds = seconds * 60 + (it.toDoubleOrNull() ?: 0.0) }
        return (seconds * 1000).toLong()
    }
}
