package nl.woolacast.data.inbox

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import nl.woolacast.data.feed.FeedClient
import nl.woolacast.data.feed.FeedFetch
import nl.woolacast.data.local.FollowedShow
import nl.woolacast.data.local.LocalStore
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** De feedronde tegen een nagebootste host: voorwaardelijk ophalen en wat er binnenkwam. */
@RunWith(AndroidJUnit4::class)
class NewEpisodesTest {

    private val server = MockWebServer()
    private lateinit var dir: File

    @Before fun start() {
        server.start()
        dir = Files.createTempDirectory("store").toFile()
    }

    @After fun stop() {
        server.shutdown()
        dir.deleteRecursively()
    }

    private fun feed(vararg items: Pair<String, String>) = """
        <rss><channel><title>De Deadline</title>
        ${items.joinToString("") { (guid, date) ->
            "<item><title>$guid</title><guid>$guid</guid><pubDate>$date</pubDate>" +
                "<enclosure url=\"https://example.com/$guid.mp3\" type=\"audio/mpeg\"/></item>"
        }}
        <item><title>zonder audio</title><guid>leeg</guid><pubDate>Tue, 30 Sep 2026 06:00:00 GMT</pubDate></item>
        </channel></rss>
    """.trimIndent()

    @Test
    fun `een ongewijzigde feed is een 304 en een nieuwe aflevering komt binnen`() = runBlocking {
        val client = FeedClient(OkHttpClient())
        val store = LocalStore(File(dir, "store.json")).apply { load() }
        val show = FollowedShow("deadline", "De Deadline", "Kade", feedUrl = server.url("/feed.xml").toString())
        store.toggleFollow(show)
        val round = NewEpisodes(client, store)

        server.enqueue(MockResponse().setBody(feed("a1" to "Mon, 29 Sep 2026 06:00:00 GMT")).setHeader("ETag", "\"v1\""))
        val first = round.refresh(store.follows.value)
        assertEquals("de eerste keer komt er niets binnen", emptyList<String>(), first["deadline"].orEmpty().map { it.id })
        assertEquals(listOf("a1"), store.feedCheck("deadline")?.latest?.map { it.id })
        assertNull(server.takeRequest().getHeader("If-None-Match"))

        server.enqueue(MockResponse().setResponseCode(304))
        val second = round.refresh(store.follows.value)
        assertEquals("\"v1\"", server.takeRequest().getHeader("If-None-Match"))
        assertTrue(second["deadline"].orEmpty().isEmpty())
        assertEquals("na een 304 blijft de vorige stand", listOf("a1"), store.feedCheck("deadline")?.latest?.map { it.id })

        server.enqueue(MockResponse().setBody(feed("a2" to "Wed, 01 Oct 2026 06:00:00 GMT", "a1" to "Mon, 29 Sep 2026 06:00:00 GMT")))
        val third = round.refresh(store.follows.value)
        assertEquals(listOf("a2"), third["deadline"].orEmpty().map { it.id })
        assertEquals(listOf("a2", "a1"), store.feedCheck("deadline")?.latest?.map { it.id })
    }

    @Test
    fun `een kapotte feed laat de vorige stand staan`() = runBlocking {
        val store = LocalStore(File(dir, "store.json")).apply { load() }
        val show = FollowedShow("s", "S", "", feedUrl = server.url("/f.xml").toString())
        store.toggleFollow(show)
        server.enqueue(MockResponse().setResponseCode(500))
        val result = NewEpisodes(FeedClient(OkHttpClient()), store).refresh(store.follows.value)
        assertTrue(result.isEmpty())
        assertNull(store.feedCheck("s"))
    }

    @Test
    fun `volgen zet de volgdag, en de melding aanzetten meldt de achterstand niet`() = runBlocking {
        val store = LocalStore(File(dir, "store.json")).apply { load() }
        store.toggleFollow(FollowedShow("s", "S", "", feedUrl = server.url("/f.xml").toString()))
        assertEquals(java.time.LocalDate.now().toString(), store.follows.value.single().followedAt)

        server.enqueue(MockResponse().setBody(feed("x" to "Wed, 01 Oct 2026 06:00:00 GMT")))
        NewEpisodes(FeedClient(OkHttpClient()), store).refresh(store.follows.value)
        store.setNotifyNew("s", true)
        assertTrue(store.follows.value.single().notifyNew)
        assertTrue("wat er al stond, is geen nieuws", "x" in store.notifiedNew())
    }

    @Test
    fun `fetchIfChanged stuurt beide kenmerken mee`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(304))
        val result = FeedClient(OkHttpClient()).fetchIfChanged(server.url("/f.xml").toString(), "\"e\"", "Tue, 30 Sep 2026 06:00:00 GMT")
        val request = server.takeRequest()
        assertEquals("\"e\"", request.getHeader("If-None-Match"))
        assertEquals("Tue, 30 Sep 2026 06:00:00 GMT", request.getHeader("If-Modified-Since"))
        assertEquals(FeedFetch.NotModified, result)
    }
}
