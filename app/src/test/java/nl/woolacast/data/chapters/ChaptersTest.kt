package nl.woolacast.data.chapters

import java.io.ByteArrayOutputStream
import nl.woolacast.domain.Chapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChaptersTest {

    @Test
    fun `json-hoofdstukken op volgorde, zonder toc-false`() {
        val json = """
            {"version":"1.2.0","chapters":[
              {"startTime":95.5,"title":"Gast"},
              {"startTime":0,"title":"Opening"},
              {"startTime":60,"title":"Reclame","toc":false},
              {"startTime":300}
            ]}
        """.trimIndent()
        assertEquals(
            listOf(Chapter(0, "Opening"), Chapter(95_500, "Gast"), Chapter(300_000, "Hoofdstuk 3")),
            ChapterJson.parse(json)
        )
    }

    @Test
    fun `id3v2_3 met een grote hoes vooraan en twee CHAP-frames`() {
        val tag = id3(
            version = 3,
            frame("APIC", ByteArray(90_000) { 7 }, 3),
            chap("c1", 0, frame("TIT2", byteArrayOf(3) + "Opening".toByteArray(), 3), 3),
            chap("c2", 61_000, frame("TIT2", byteArrayOf(1) + "Gast".toByteArray(Charsets.UTF_16), 3), 3)
        )
        val reader = CountingReader(tag + ByteArray(200_000))
        val chapters = Id3Chapters.read(reader)
        assertEquals(listOf(Chapter(0, "Opening"), Chapter(61_000, "Gast")), chapters)
        // De hoes wordt overgeslagen, niet opgehaald.
        assertTrue("haalde ${reader.fetched} bytes", reader.fetched < 80_000)
    }

    @Test
    fun `id3v2_4 met syncsafe-maten`() {
        val tag = id3(
            version = 4,
            chap("a", 1_000, frame("TIT2", byteArrayOf(3) + "Een".toByteArray(), 4), 4),
            chap("b", 2_000, frame("TIT2", byteArrayOf(0) + "Twee".toByteArray(Charsets.ISO_8859_1), 4), 4)
        )
        assertEquals(listOf(Chapter(1_000, "Een"), Chapter(2_000, "Twee")), Id3Chapters.read(CountingReader(tag)))
    }

    @Test
    fun `geen ID3-kop is geen hoofdstukken`() {
        assertEquals(emptyList<Chapter>(), Id3Chapters.read(CountingReader(ByteArray(1000) { 0x55 })))
    }

    /* ---- een kop in elkaar zetten ---- */

    private class CountingReader(private val data: ByteArray) : RangeReader {
        var fetched = 0
        override fun read(offset: Long, length: Int): ByteArray {
            if (offset >= data.size) return ByteArray(0)
            val end = minOf(data.size.toLong(), offset + length).toInt()
            fetched += end - offset.toInt()
            return data.copyOfRange(offset.toInt(), end)
        }
    }

    private fun size(n: Int, version: Int): ByteArray =
        if (version == 4) byteArrayOf((n shr 21 and 0x7f).toByte(), (n shr 14 and 0x7f).toByte(), (n shr 7 and 0x7f).toByte(), (n and 0x7f).toByte())
        else int(n)

    private fun int(n: Int) = byteArrayOf((n ushr 24).toByte(), (n ushr 16).toByte(), (n ushr 8).toByte(), n.toByte())

    private fun frame(id: String, body: ByteArray, version: Int): ByteArray =
        id.toByteArray(Charsets.ISO_8859_1) + size(body.size, version) + byteArrayOf(0, 0) + body

    private fun chap(element: String, startMs: Int, sub: ByteArray, version: Int): ByteArray {
        val body = element.toByteArray() + byteArrayOf(0) + int(startMs) + int(startMs + 1000) + int(-1) + int(-1) + sub
        return frame("CHAP", body, version)
    }

    private fun id3(version: Int, vararg frames: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        frames.forEach { out.write(it) }
        out.write(ByteArray(64)) // opvulling
        val body = out.toByteArray()
        return byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(), version.toByte(), 0, 0) + size(body.size, 4) + body
    }
}
