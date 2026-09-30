package nl.woolacast.data.chapters

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.charset.Charset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import nl.woolacast.domain.Chapter
import nl.woolacast.domain.Episode
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Hoofdstukken van een aflevering, van drie plekken, in deze volgorde:
 *
 * 1. in de feed zelf (Podlove Simple Chapters), dus al binnen;
 * 2. een JSON-bestand waar de feed naar wijst (podcast:chapters);
 * 3. de ID3-kop van het mp3-bestand (CHAP-frames).
 *
 * Voor de derde halen we alleen het begin van het bestand op, met een
 * range-verzoek, of lezen we het gedownloade bestand. Een aflevering zonder
 * hoofdstukken kost zo hooguit één klein verzoek.
 */
class ChapterRepository(private val client: OkHttpClient) {

    private val cache = object : LinkedHashMap<String, List<Chapter>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<Chapter>>?) = size > 40
    }

    suspend fun chapters(episode: Episode, localFile: File?): List<Chapter> {
        synchronized(cache) { cache[episode.id] }?.let { return it }
        val found = find(episode, localFile)
        // Een netwerkfout onthouden we niet: de volgende keer kan het wel lukken.
        if (found != null) synchronized(cache) { cache[episode.id] = found }
        return found.orEmpty()
    }

    /** null als het niet te zeggen was (netwerkfout); leeg als er echt geen hoofdstukken zijn. */
    private suspend fun find(episode: Episode, localFile: File?): List<Chapter>? = withContext(Dispatchers.IO) {
        if (episode.inlineChapters.size >= 2) return@withContext episode.inlineChapters
        var failed = false
        episode.chaptersUrl?.let { url ->
            runCatching { ChapterJson.parse(get(url)) }
                .onFailure { failed = true }
                .getOrNull()?.takeIf { it.size >= 2 }
                ?.let { return@withContext it }
        }
        val reader: RangeReader? = when {
            localFile != null && localFile.exists() -> FileRangeReader(localFile)
            episode.audioUrl != null -> HttpRangeReader(client, episode.audioUrl)
            else -> null
        }
        val fromId3 = reader?.use { runCatching { Id3Chapters.read(it) } }
        when {
            fromId3 == null -> if (failed) null else emptyList()
            fromId3.isFailure -> null
            else -> fromId3.getOrThrow().takeIf { it.isNotEmpty() } ?: if (failed) null else emptyList()
        }
    }

    private fun get(url: String): String {
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Hoofdstukken gaven ${response.code}")
            return response.body?.string() ?: throw IOException("Lege hoofdstukken")
        }
    }
}

/** Het JSON-formaat van Podcasting 2.0: {"chapters":[{"startTime":0,"title":"…"}]}. */
object ChapterJson {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(body: String): List<Chapter> {
        val root = json.parseToJsonElement(body)
        val list = (root as? JsonObject)?.get("chapters") as? JsonArray ?: return emptyList()
        return list.mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            // "toc": false is een markering, geen hoofdstuk om naartoe te springen.
            if ((item["toc"] as? JsonPrimitive)?.booleanOrNull == false) return@mapNotNull null
            val start = (item["startTime"] as? JsonPrimitive)?.doubleOrNull ?: return@mapNotNull null
            val title = (item["title"] as? JsonPrimitive)?.content?.trim().orEmpty()
            Chapter((start * 1000).toLong(), title)
        }.let(::tidy)
    }
}

/** Op volgorde, geen dubbele begintijden, en een naam voor elk hoofdstuk. */
internal fun tidy(chapters: List<Chapter>): List<Chapter> =
    chapters.filter { it.startMs >= 0 }
        .sortedBy { it.startMs }
        .distinctBy { it.startMs }
        .mapIndexed { index, chapter ->
            if (chapter.title.isBlank()) chapter.copy(title = "Hoofdstuk ${index + 1}") else chapter
        }

/** Iets waar je op een plek een aantal bytes uit kunt lezen: een bestand of een URL. */
interface RangeReader : AutoCloseable {
    /** Hooguit [length] bytes vanaf [offset]; minder als het bestand ophoudt. */
    fun read(offset: Long, length: Int): ByteArray
    override fun close() {}
}

class FileRangeReader(file: File) : RangeReader {
    private val raf = RandomAccessFile(file, "r")
    override fun read(offset: Long, length: Int): ByteArray {
        if (offset >= raf.length()) return ByteArray(0)
        raf.seek(offset)
        val out = ByteArray(minOf(length.toLong(), raf.length() - offset).toInt())
        raf.readFully(out)
        return out
    }
    override fun close() = raf.close()
}

/**
 * Leest stukken van een audiobestand op het net met een Range-kop. Een server
 * die dat niet kent stuurt het hele bestand; dan lezen we het begin en
 * breken af. Doorverwijzingen (meetdiensten als OP3 en Podtrac) volgt OkHttp.
 */
class HttpRangeReader(private val client: OkHttpClient, private val url: String) : RangeReader {
    override fun read(offset: Long, length: Int): ByteArray {
        val request = Request.Builder().url(url)
            .header("Range", "bytes=$offset-${offset + length - 1}")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Audio gaf ${response.code}")
            val stream = response.body?.byteStream() ?: return ByteArray(0)
            // 200 in plaats van 206: de server stuurt alles vanaf het begin.
            if (response.code == 200 && offset > 0) stream.skipNBytesCompat(offset)
            return stream.readUpTo(length)
        }
    }

    private fun java.io.InputStream.skipNBytesCompat(n: Long) {
        var left = n
        while (left > 0) {
            val skipped = skip(left)
            if (skipped <= 0) { if (read() == -1) return else left-- } else left -= skipped
        }
    }

    private fun java.io.InputStream.readUpTo(n: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream(minOf(n, 64 * 1024))
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (total < n) {
            val read = read(buffer, 0, minOf(buffer.size, n - total))
            if (read == -1) break
            out.write(buffer, 0, read)
            total += read
        }
        return out.toByteArray()
    }
}

/**
 * De CHAP-frames uit een ID3v2.3- of v2.4-kop. Een kop kan groot zijn door een
 * ingesloten hoes; die slaan we over door op de plek erna verder te lezen, in
 * plaats van hem op te halen.
 */
object Id3Chapters {
    private const val WINDOW = 32 * 1024
    /** Nooit meer dan dit ophalen, hoe de kop ook is opgebouwd. */
    private const val MAX_FETCH = 512 * 1024
    private const val MAX_FRAMES = 400

    fun read(reader: RangeReader): List<Chapter> {
        // Eén venster vanaf het begin: de kop en meestal ook alle hoofdstukken in één verzoek.
        val window = Window(reader)
        val header = window.bytes(0, 10) ?: return emptyList()
        if (header.size < 10 || header[0] != 'I'.code.toByte() || header[1] != 'D'.code.toByte() || header[2] != '3'.code.toByte()) {
            return emptyList()
        }
        val version = header[3].toInt()
        if (version != 3 && version != 4) return emptyList()
        val flags = header[5].toInt()
        // Onsynchronisatie over de hele kop komt bijna niet voor; dan niet.
        if (flags and 0x80 != 0) return emptyList()
        val tagEnd = 10L + syncsafe(header, 6)

        var offset = 10L
        if (flags and 0x40 != 0) {
            val ext = window.bytes(offset, 4) ?: return emptyList()
            offset += if (version == 3) 4L + int32(ext, 0) else syncsafe(ext, 0).toLong()
        }

        val chapters = mutableListOf<Chapter>()
        var frames = 0
        while (offset + 10 <= tagEnd && frames++ < MAX_FRAMES && window.fetched < MAX_FETCH) {
            val frameHeader = window.bytes(offset, 10) ?: break
            if (frameHeader[0].toInt() == 0) break // opvulling: einde van de frames
            val id = String(frameHeader, 0, 4, Charsets.ISO_8859_1)
            val size = if (version == 4) syncsafe(frameHeader, 4) else int32(frameHeader, 4)
            if (size <= 0 || offset + 10 + size > tagEnd) break
            if (id == "CHAP" && size < 64 * 1024) {
                window.bytes(offset + 10, size)?.let { body -> parseChap(body, version)?.let(chapters::add) }
            }
            offset += 10 + size
        }
        return tidy(chapters)
    }

    private fun parseChap(body: ByteArray, version: Int): Chapter? {
        var i = 0
        while (i < body.size && body[i].toInt() != 0) i++ // element-id, met een nul erachter
        i++
        if (i + 16 > body.size) return null
        val start = int32(body, i).toLong() and 0xffffffffL
        i += 16 // begin, eind, beginbyte, eindbyte
        var title: String? = null
        while (i + 10 <= body.size) {
            val id = String(body, i, 4, Charsets.ISO_8859_1)
            val size = if (version == 4) syncsafe(body, i + 4) else int32(body, i + 4)
            if (size <= 0 || i + 10 + size > body.size) break
            if (id == "TIT2") title = text(body.copyOfRange(i + 10, i + 10 + size))
            i += 10 + size
        }
        return Chapter(start, title.orEmpty())
    }

    /** Een tekstframe: eerst een byte voor de codering, dan de tekst. */
    internal fun text(frame: ByteArray): String? {
        if (frame.isEmpty()) return null
        val charset: Charset = when (frame[0].toInt()) {
            1 -> Charsets.UTF_16
            2 -> Charsets.UTF_16BE
            3 -> Charsets.UTF_8
            else -> Charsets.ISO_8859_1
        }
        return String(frame, 1, frame.size - 1, charset).trimEnd('\u0000').trim().takeIf { it.isNotEmpty() }
    }

    private fun syncsafe(b: ByteArray, at: Int): Int =
        (b[at].toInt() and 0x7f shl 21) or (b[at + 1].toInt() and 0x7f shl 14) or
            (b[at + 2].toInt() and 0x7f shl 7) or (b[at + 3].toInt() and 0x7f)

    private fun int32(b: ByteArray, at: Int): Int =
        (b[at].toInt() and 0xff shl 24) or (b[at + 1].toInt() and 0xff shl 16) or
            (b[at + 2].toInt() and 0xff shl 8) or (b[at + 3].toInt() and 0xff)

    /**
     * Houdt één venster van de kop vast. Wat erin valt komt uit het geheugen;
     * wat erbuiten valt, haalt een nieuw venster op vanaf die plek.
     */
    private class Window(private val reader: RangeReader) {
        private var start = 0L
        private var data = ByteArray(0)
        var fetched = 0
            private set

        fun bytes(offset: Long, length: Int): ByteArray? {
            if (offset < start || offset + length > start + data.size) {
                data = reader.read(offset, maxOf(length, WINDOW))
                start = offset
                fetched += data.size
                if (data.size < length) return null
            }
            val from = (offset - start).toInt()
            return data.copyOfRange(from, from + length)
        }
    }
}
