package nl.woolacast.data.opml

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OpmlTest {

    @Test
    fun `leest geneste outlines en dubbele feeds maar een keer`() {
        val opml = """
            <?xml version="1.0" encoding="utf-8"?>
            <opml version="1.0"><head><title>Pocket Casts Feeds</title></head><body>
              <outline text="feeds">
                <outline type="rss" text="De Deadline" xmlUrl="https://example.com/deadline.xml" />
                <outline type="rss" text="Koud &amp; Spoor" xmlurl="http://www.example.com/koud/" />
                <outline type="rss" text="Dubbel" xmlUrl="https://example.com/deadline.xml/" />
                <outline text="geen feed" />
              </outline>
            </body></opml>
        """.trimIndent()
        val feeds = Opml.parse(opml.byteInputStream())
        assertEquals(listOf("De Deadline", "Koud & Spoor"), feeds.map { it.title })
    }

    @Test
    fun `wat we schrijven lezen we terug`() {
        val feeds = listOf(OpmlFeed("https://a.example/feed?x=1&y=2", "Tom & \"Jerry\""), OpmlFeed("https://b.example/rss", null))
        val back = Opml.parse(Opml.write(feeds, "2026-10-01").byteInputStream())
        assertEquals(feeds.map { it.url }, back.map { it.url })
        assertEquals("Tom & \"Jerry\"", back[0].title)
    }

    @Test
    fun `een feed-id is vast en herkenbaar`() {
        val a = Opml.feedId("https://www.example.com/feed/")
        assertEquals(a, Opml.feedId("http://example.com/feed"))
        assertTrue(a.startsWith(Opml.FEED_PREFIX))
    }

    @Test
    fun `wat iemand plakt`() {
        assertEquals("https://example.com/rss", ShowImporter.normaliseInput("  example.com/rss "))
        assertEquals("https://example.com/rss", ShowImporter.normaliseInput("podcast://example.com/rss"))
        assertNull(ShowImporter.normaliseInput("geen adres"))
        assertNull(ShowImporter.normaliseInput("localhost"))
    }
}
