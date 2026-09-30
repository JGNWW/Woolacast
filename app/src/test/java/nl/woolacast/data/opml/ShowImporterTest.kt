package nl.woolacast.data.opml

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.io.IOException
import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import nl.woolacast.data.apple.AppleCatalogApi
import nl.woolacast.data.apple.LookupResponse
import nl.woolacast.data.apple.LookupResult
import nl.woolacast.data.feed.FeedClient
import nl.woolacast.data.local.FollowedShow
import nl.woolacast.data.local.LocalStore
import nl.woolacast.data.local.SavedEpisode
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShowImporterTest {

    private fun store() = LocalStore(File.createTempFile("store", ".json").apply { delete() }).also { runBlocking { it.load() } }

    private val rss = """<rss><channel><title>De Deadline</title><item><title>Afl 1</title>
        <enclosure url="https://x.test/1.mp3" type="audio/mpeg"/></item></channel></rss>"""

    private fun feeds() = FeedClient(OkHttpClient.Builder().addInterceptor { chain ->
        val url = chain.request().url.toString()
        val (code, body) = when {
            "weg" in url -> 404 to ""
            "geenaudio" in url -> 200 to "<rss><channel><title>Tekst</title><item><title>x</title></item></channel></rss>"
            else -> 200 to rss
        }
        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("x")
            .body(body.toResponseBody("application/rss+xml".toMediaType())).build()
    }.build())

    private fun catalog(hits: Map<String, Pair<Long, String>>) = Proxy.newProxyInstance(
        AppleCatalogApi::class.java.classLoader, arrayOf(AppleCatalogApi::class.java)
    ) { _, method, args ->
        when (method.name) {
            "search" -> LookupResponse(results = hits.filterKeys { (args[0] as String).contains(it) }.values.map { (id, feed) ->
                LookupResult(collectionId = id, collectionName = "x", feedUrl = feed)
            })
            else -> throw IOException("geen netwerk")
        }
    } as AppleCatalogApi

    @Test
    fun `volgt wat werkt, slaat over wat je al volgt, en zegt waarom de rest faalt`() = runBlocking {
        val store = store()
        store.followAll(listOf(FollowedShow("123", "Al gevolgd", "", null, "https://www.al.test/feed/")))
        val importer = ShowImporter(feeds(), catalog(emptyMap()), store)
        val seen = mutableListOf<ImportOutcome>()
        val outcomes = importer.import(
            listOf(
                OpmlFeed("https://deadline.test/rss", null),
                OpmlFeed("http://al.test/feed", "Al gevolgd"),
                OpmlFeed("https://weg.test/rss", "Weg"),
                OpmlFeed("https://geenaudio.test/rss", "Tekst")
            )
        ) { seen += it }
        assertEquals(4, seen.size)
        assertTrue(outcomes[0] is ImportOutcome.Followed)
        assertTrue(outcomes[1] is ImportOutcome.Known)
        assertEquals("Feed bestaat niet meer (404)", (outcomes[2] as ImportOutcome.Failed).reason)
        assertEquals("Geen afleveringen met audio", (outcomes[3] as ImportOutcome.Failed).reason)
        // Meteen gevolgd, niet pas aan het eind.
        assertEquals(listOf("123", Opml.feedId("https://deadline.test/rss")), store.follows.value.map { it.id })
    }

    @Test
    fun `koppelt alleen bij precies dezelfde feed, en neemt alles mee`() = runBlocking {
        val store = store()
        val importer = ShowImporter(
            feeds(),
            catalog(mapOf("De Deadline" to (777L to "http://www.deadline.test/rss/"))),
            store
        )
        importer.import(listOf(OpmlFeed("https://deadline.test/rss", null), OpmlFeed("https://ander.test/rss", null)))
        val oldId = Opml.feedId("https://deadline.test/rss")
        store.setAutoDownload(oldId, 2)
        store.toggleQueue(SavedEpisode("e1", oldId, "De Deadline", "Afl 1"))
        // Pauze tussen zoekopdrachten maakt deze test traag; twee shows is genoeg.
        importer.link("nl")
        val ids = store.follows.value.map { it.id }
        assertTrue("777" in ids)
        assertTrue(Opml.feedId("https://ander.test/rss") in ids) // "ander" heet ook De Deadline, maar een andere feed
        assertEquals(mapOf("777" to 2), store.autoDownload.value)
        assertEquals("777", store.queue.value.single().showId)
    }

    @Test
    fun `ontvolgen zet automatisch downloaden uit`() = runBlocking {
        val store = store()
        val show = FollowedShow("9", "Show", "", null, "https://s.test/rss")
        store.toggleFollow(show)
        store.setAutoDownload("9", 1)
        store.toggleFollow(show)
        assertEquals(emptyMap<String, Int>(), store.autoDownload.value)
    }
}
