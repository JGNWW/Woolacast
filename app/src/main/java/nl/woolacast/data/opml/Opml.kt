package nl.woolacast.data.opml

import android.util.Xml
import java.io.InputStream
import java.security.MessageDigest
import org.xmlpull.v1.XmlPullParser

/** Een feed uit een OPML-bestand: het adres, en de naam als die erbij staat. */
data class OpmlFeed(val url: String, val title: String?)

/**
 * OPML is hoe podcastapps hun abonnementen uitwisselen: een lijst
 * &lt;outline type="rss" xmlUrl="…"&gt;. Pocket Casts, AntennaPod, Overcast en
 * Podcast Addict schrijven het allemaal net iets anders, dus we lezen ruim: elke
 * outline met een xmlUrl telt, genest of niet.
 */
object Opml {

    fun parse(input: InputStream): List<OpmlFeed> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, null)
        val feeds = LinkedHashMap<String, OpmlFeed>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name.equals("outline", ignoreCase = true)) {
                val url = attribute(parser, "xmlUrl") ?: attribute(parser, "url")?.takeIf {
                    attribute(parser, "type").equals("rss", ignoreCase = true)
                }
                val clean = url?.trim()?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
                if (clean != null) {
                    val title = (attribute(parser, "title") ?: attribute(parser, "text"))?.trim()?.takeIf { it.isNotEmpty() }
                    feeds.putIfAbsent(key(clean), OpmlFeed(clean, title))
                }
            }
            event = parser.next()
        }
        return feeds.values.toList()
    }

    /** Attributen zonder op hoofdletters te letten: xmlUrl, xmlurl, XMLURL. */
    private fun attribute(parser: XmlPullParser, name: String): String? {
        for (i in 0 until parser.attributeCount) {
            if (parser.getAttributeName(i).equals(name, ignoreCase = true)) return parser.getAttributeValue(i)
        }
        return null
    }

    fun write(feeds: List<OpmlFeed>, created: String): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        append("<opml version=\"2.0\">\n")
        append("  <head>\n    <title>Toadcast</title>\n    <dateCreated>").append(escape(created)).append("</dateCreated>\n  </head>\n")
        append("  <body>\n")
        feeds.forEach { feed ->
            val title = escape(feed.title ?: feed.url)
            append("    <outline type=\"rss\" text=\"").append(title).append("\" title=\"").append(title)
                .append("\" xmlUrl=\"").append(escape(feed.url)).append("\" />\n")
        }
        append("  </body>\n</opml>\n")
    }

    private fun escape(text: String) = text
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&apos;")
        .filter { it == '\n' || it == '\t' || it >= ' ' }

    /**
     * Twee adressen voor dezelfde feed: http of https, met of zonder www en een
     * slash aan het eind. Die tellen als één.
     */
    fun key(url: String): String = url.trim().lowercase()
        .removePrefix("http://").removePrefix("https://").removePrefix("www.")
        .trimEnd('/')

    /**
     * De id van een show die alleen een feed heeft. Vast voor hetzelfde adres,
     * zodat instellingen en voortgang blijven kloppen, en herkenbaar aan het
     * voorvoegsel: een Apple-id bestaat alleen uit cijfers.
     */
    fun feedId(url: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(key(url).toByteArray())
        return FEED_PREFIX + digest.take(8).joinToString("") { "%02x".format(it) }
    }

    const val FEED_PREFIX = "feed-"
}
