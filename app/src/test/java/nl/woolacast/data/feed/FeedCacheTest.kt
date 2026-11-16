package nl.woolacast.data.feed

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import nl.woolacast.data.PodcastRepository
import nl.woolacast.data.apple.AppleCatalogApi
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** De podcastpagina uit het geheugen: bewaren, voorwaardelijk navragen en terugvallen zonder verbinding. */
@RunWith(AndroidJUnit4::class)
class FeedCacheTest {

    private val server = MockWebServer()
    private lateinit var dir: File

    @Before fun start() {
        server.start()
        dir = Files.createTempDirectory("feeds").toFile()
    }

    @After fun stop() {
        runCatching { server.shutdown() }
        dir.deleteRecursively()
    }

    private fun feed(vararg guids: String) = """
        <rss><channel><title>De Deadline</title><image><url>https://example.com/art.jpg</url></image>
        ${guids.joinToString("") { "<item><title>$it</title><guid>$it</guid><enclosure url=\"https://example.com/$it.mp3\" type=\"audio/mpeg\"/></item>" }}
        </channel></rss>
    """.trimIndent()

    /** Een catalogus die nooit gevraagd mag worden: het adres van de feed is bekend. */
    private val noCatalog = java.lang.reflect.Proxy.newProxyInstance(
        AppleCatalogApi::class.java.classLoader, arrayOf(AppleCatalogApi::class.java)
    ) { _, method, _ -> error("de catalogus hoort hier niet gevraagd te worden: ${method.name}") } as AppleCatalogApi

    private fun repository() = PodcastRepository(noCatalog, FeedClient(OkHttpClient()), FeedCache(dir))

    @Test
    fun `de tweede keer vraagt alleen of er iets veranderd is`() = runBlocking {
        val url = server.url("/feed.xml").toString()
        val repository = repository()
        assertNull(repository.cached("show"))

        server.enqueue(MockResponse().setBody(feed("a1")).setHeader("ETag", "\"v1\""))
        assertEquals(listOf("a1"), repository.detail("show", "nl", url).episodes.map { it.id })
        server.takeRequest()

        // Een nieuwe start: de pagina staat er zonder het net.
        val later = repository()
        assertEquals(listOf("a1"), later.cached("show")?.episodes?.map { it.id })

        server.enqueue(MockResponse().setResponseCode(304))
        assertEquals(listOf("a1"), later.detail("show", "nl", url).episodes.map { it.id })
        assertEquals("\"v1\"", server.takeRequest().getHeader("If-None-Match"))

        server.enqueue(MockResponse().setBody(feed("a2", "a1")).setHeader("ETag", "\"v2\""))
        assertEquals(listOf("a2", "a1"), later.detail("show", "nl", url).episodes.map { it.id })
    }

    @Test
    fun `net gelezen is goed genoeg, en zonder verbinding blijft de vorige stand`() = runBlocking {
        val url = server.url("/feed.xml").toString()
        val repository = repository()
        server.enqueue(MockResponse().setBody(feed("a1")))
        repository.detail("show", "nl", url)
        assertEquals(1, server.requestCount)

        // Binnen het half uur gaat er niets het net op, ook zonder dat het adres wordt meegegeven.
        assertNotNull(repository.resolveEpisode("show", "nl", null, "De Deadline", "a1"))
        assertEquals(1, server.requestCount)

        server.shutdown()
        assertEquals(listOf("a1"), repository.detail("show", "nl", url).episodes.map { it.id })
    }
}
