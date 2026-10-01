package nl.woolacast.data.download

import java.io.File
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AudioFetcherTest {

    private val server = MockWebServer()
    private val client = OkHttpClient()
    private lateinit var part: File
    private val audio = ByteArray(100_000) { (it % 251).toByte() }
    private val otherAudio = ByteArray(120_000) { (it % 13).toByte() }

    @Before fun setUp() {
        server.start()
        part = File.createTempFile("afl", ".mp3.part").apply { delete() }
    }

    @After fun tearDown() {
        server.shutdown()
        part.delete()
        File(part.path + AudioFetcher.META).delete()
    }

    private fun body(bytes: ByteArray) = Buffer().write(bytes)

    @Test
    fun `volgt de doorverwijzing van een meetdienst`() {
        // op3.dev/e/… en podtrac: eerst een 302 naar de echte plek.
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", server.url("/echt/afl.mp3")))
        server.enqueue(MockResponse().setBody(body(audio)).setHeader("ETag", "\"v1\""))
        val done = AudioFetcher(client).fetch(server.url("/e/tracker/afl.mp3").toString(), part)
        assertEquals(audio.size.toLong(), done)
        assertArrayEquals(audio, part.readBytes())
        assertEquals("/echt/afl.mp3", server.takeRequest().let { server.takeRequest().path })
    }

    @Test
    fun `gaat verder met dezelfde versie`() {
        part.writeBytes(audio.copyOfRange(0, 40_000))
        File(part.path + AudioFetcher.META).writeText("\"v1\"\n${audio.size}")
        server.enqueue(
            MockResponse().setResponseCode(206).setBody(body(audio.copyOfRange(40_000, audio.size)))
                .setHeader("Content-Range", "bytes 40000-99999/100000")
        )
        val done = AudioFetcher(client).fetch(server.url("/afl.mp3").toString(), part)
        val request = server.takeRequest()
        assertEquals("bytes=40000-", request.getHeader("Range"))
        assertEquals("\"v1\"", request.getHeader("If-Range"))
        assertEquals(audio.size.toLong(), done)
        assertArrayEquals(audio, part.readBytes())
    }

    @Test
    fun `een andere versie begint opnieuw in plaats van te plakken`() {
        part.writeBytes(audio.copyOfRange(0, 40_000))
        File(part.path + AudioFetcher.META).writeText("\"v1\"\n${audio.size}")
        // If-Range klopt niet meer (andere reclame): de server stuurt alles, met 200.
        server.enqueue(MockResponse().setBody(body(otherAudio)).setHeader("ETag", "\"v2\""))
        val done = AudioFetcher(client).fetch(server.url("/afl.mp3").toString(), part)
        assertEquals(otherAudio.size.toLong(), done)
        assertArrayEquals(otherAudio, part.readBytes())
    }

    @Test
    fun `een 206 die niet aansluit wordt geen half bestand`() {
        part.writeBytes(audio.copyOfRange(0, 40_000))
        File(part.path + AudioFetcher.META).writeText("\"v1\"\n${audio.size}")
        // De server negeert If-Range maar doet de Range wel, voor een andere versie.
        server.enqueue(
            MockResponse().setResponseCode(206).setBody(body(otherAudio.copyOfRange(40_000, otherAudio.size)))
                .setHeader("Content-Range", "bytes 40000-119999/120000")
        )
        try {
            AudioFetcher(client).fetch(server.url("/afl.mp3").toString(), part)
            throw AssertionError("had moeten falen")
        } catch (expected: IOException) {
        }
        assertFalse(part.exists())
        assertFalse(File(part.path + AudioFetcher.META).exists())
    }

    @Test
    fun `zonder bewaarde versie niet verdergaan`() {
        part.writeBytes(audio.copyOfRange(0, 40_000))
        server.enqueue(MockResponse().setBody(body(audio)))
        AudioFetcher(client).fetch(server.url("/afl.mp3").toString(), part)
        assertNull(server.takeRequest().getHeader("Range"))
        assertArrayEquals(audio, part.readBytes())
    }

    @Test
    fun `416 op een compleet deel is klaar`() {
        part.writeBytes(audio)
        File(part.path + AudioFetcher.META).writeText("\"v1\"\n${audio.size}")
        server.enqueue(MockResponse().setResponseCode(416))
        assertEquals(audio.size.toLong(), AudioFetcher(client).fetch(server.url("/afl.mp3").toString(), part))
    }

    @Test
    fun `416 op een half deel begint de volgende keer schoon`() {
        part.writeBytes(audio.copyOfRange(0, 40_000))
        File(part.path + AudioFetcher.META).writeText("\"v1\"\n${audio.size}")
        server.enqueue(MockResponse().setResponseCode(416))
        try {
            AudioFetcher(client).fetch(server.url("/afl.mp3").toString(), part)
            throw AssertionError("had moeten falen")
        } catch (expected: IOException) {
        }
        assertFalse(part.exists())
    }

    @Test
    fun `een foutpagina is geen aflevering`() {
        server.enqueue(MockResponse().setBody("<html>niet gevonden</html>"))
        try {
            AudioFetcher(client).fetch(server.url("/afl.mp3").toString(), part)
            throw AssertionError("had moeten falen")
        } catch (expected: IOException) {
            assertTrue(expected.message!!.contains("te klein"))
        }
    }

    @Test
    fun `stoppen laat het deel staan`() {
        server.enqueue(MockResponse().setBody(body(audio)).setHeader("ETag", "\"v1\""))
        var reads = 0
        val result = AudioFetcher(client).fetch(server.url("/afl.mp3").toString(), part, isStopped = { reads++ > 0 })
        assertNull(result)
        assertTrue(File(part.path + AudioFetcher.META).exists())
    }

    @Test
    fun `foutmeldingen in gewone taal`() {
        assertEquals("Aflevering niet meer te vinden (404)", DownloadWorker.reason(IOException("HTTP 404")))
        assertEquals("Geen verbinding, of de website bestaat niet meer", DownloadWorker.reason(java.net.UnknownHostException("x")))
    }
}
