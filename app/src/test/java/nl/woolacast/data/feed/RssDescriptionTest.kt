package nl.woolacast.data.feed

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.woolacast.ui.common.richDescription
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Omschrijvingen houden hun alinea's en links. Het voorbeeld is nagebouwd naar
 * een NPO-feed: platte tekst met witregels in <description>, en dezelfde tekst
 * als HTML met echte links in <content:encoded>.
 */
@RunWith(AndroidJUnit4::class)
class RssDescriptionTest {

    private val feed = """
        <rss xmlns:content="http://purl.org/rss/1.0/modules/content/"><channel>
          <title>Boos</title>
          <item>
            <title>Beide versies</title>
            <guid>afl-1</guid>
            <description>We bespreken de nasleep.

Shownotes:

Het rapport  (https://example.org/rapport)</description>
            <content:encoded>&lt;p&gt;We bespreken de &lt;b&gt;nasleep&lt;/b&gt;.&lt;/p&gt;&#13;
&lt;p&gt;Shownotes: &lt;/p&gt;&#13;
&lt;p&gt;&lt;a href="https://example.org/aflevering"&gt;De aflevering &lt;/a&gt;&lt;br&gt;&lt;br&gt;&lt;a href="https://example.org/rapport"&gt;Het rapport &lt;/a&gt;&lt;/p&gt;</content:encoded>
          </item>
          <item>
            <title>Alleen platte tekst</title>
            <guid>afl-2</guid>
            <description>Eerste alinea.

Meer op https://example.org/meer.</description>
          </item>
        </channel></rss>
    """.trimIndent()

    private val parsed = RssFeedParser().parse(feed.byteInputStream()).episodes

    @Test
    fun `de opgemaakte versie wint, de platte tekst houdt zijn alineas`() {
        val both = parsed[0]
        assertTrue(both.descriptionHtml!!.contains("<a href=\"https://example.org/aflevering\""))
        assertTrue(both.description!!.startsWith("We bespreken de nasleep."))
        assertTrue(both.description!!.contains("\n"))

        val plain = parsed[1]
        assertNull(plain.descriptionHtml)
        assertEquals("Eerste alinea.\n\nMeer op https://example.org/meer.", plain.description)
    }

    @Test
    fun `links zijn aan te tikken`() {
        val both = parsed[0]
        val rich = richDescription(both.descriptionHtml, both.description, Color.Red)!!
        val urls = rich.getLinkAnnotations(0, rich.length).map { (it.item as LinkAnnotation.Url).url to rich.text.substring(it.start, it.end).trim() }
        assertEquals(listOf("https://example.org/aflevering" to "De aflevering", "https://example.org/rapport" to "Het rapport"), urls)
        // Alinea's blijven gescheiden, zonder lege staart.
        assertTrue(rich.text.startsWith("We bespreken de nasleep.\n\nShownotes:"))
        assertTrue(!rich.text.endsWith("\n"))

        // Een kaal adres in platte tekst wordt een link, zonder de punt erachter.
        val plain = parsed[1]
        val linked = richDescription(plain.descriptionHtml, plain.description, Color.Red)!!
        val bare = linked.getLinkAnnotations(0, linked.length).single()
        assertEquals("https://example.org/meer", (bare.item as LinkAnnotation.Url).url)
    }
}
