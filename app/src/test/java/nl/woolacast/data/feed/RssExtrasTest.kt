package nl.woolacast.data.feed

import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.woolacast.domain.Chapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RssExtrasTest {

    private val feed = """
        <rss xmlns:podcast="https://podcastindex.org/namespace/1.0" xmlns:psc="http://podlove.org/simple-chapters"><channel>
          <title>De Deadline</title>
          <item>
            <title>Live vanuit Paradiso</title>
            <guid>afl-1</guid>
            <enclosure url="https://op3.dev/e/example.com/afl1.mp3" type="audio/mpeg" length="1"/>
            <podcast:chapters url="https://example.com/afl1.json" type="application/json+chapters"/>
            <podcast:transcript url="https://example.com/afl1.html" type="text/html"/>
            <podcast:transcript url="https://example.com/afl1.vtt" type="text/vtt" language="nl"/>
            <psc:chapters version="1.2">
              <psc:chapter start="00:06:12.000" title="Gast"/>
              <psc:chapter start="0" title="Opening"/>
            </psc:chapters>
          </item>
          <item>
            <title>Zonder extra's</title>
            <enclosure url="http://example.com/afl2.mp3" type="audio/mpeg"/>
          </item>
        </channel></rss>
    """.trimIndent()

    @Test
    fun `hoofdstukken en de beste transcriptie uit de feed`() {
        val parsed = RssFeedParser().parse(feed.byteInputStream())
        val first = parsed.episodes[0]
        assertEquals("https://example.com/afl1.json", first.chaptersUrl)
        assertEquals(listOf(Chapter(0, "Opening"), Chapter(372_000, "Gast")), first.inlineChapters)
        assertEquals("text/vtt", first.transcript?.type)
        assertEquals("nl", first.transcript?.language)
        val second = parsed.episodes[1]
        assertNull(second.transcript)
        assertEquals(emptyList<Chapter>(), second.inlineChapters)
    }
}
