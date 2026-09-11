package nl.woolacast.data.feed

import android.util.Xml
import nl.woolacast.data.Html
import java.io.InputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.xmlpull.v1.XmlPullParser

data class ParsedEpisode(
    val guid: String,
    val title: String,
    val description: String?,
    val audioUrl: String?,
    val durationMillis: Long?,
    val releaseDate: String?,
    val imageUrl: String?,
    val link: String?,
    /** Bij een nieuwsbundelaar: van welk medium deze kop is. */
    val sourceUrl: String? = null,
    val sourceName: String? = null
)

data class ParsedFeed(
    val title: String?,
    val author: String?,
    val description: String?,
    val imageUrl: String?,
    val episodes: List<ParsedEpisode>
)

/**
 * Leest de RSS van een podcast rechtstreeks. Dat is de enige route naar
 * afleveringen die van niemand toestemming nodig heeft: de feed is van de maker
 * zelf en staat open. Elke podcastapp die geen eigen backend heeft doet dit zo.
 *
 * Namespaces staan uit, zodat tags binnenkomen als "itunes:duration" — dat
 * scheelt het uitzoeken van welke namespace-URI een feed toevallig gebruikt.
 */
class RssFeedParser {

    fun parse(input: InputStream): ParsedFeed {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, null)

        var channelTitle: String? = null
        var channelAuthor: String? = null
        var channelDescription: String? = null
        var channelImage: String? = null

        val episodes = mutableListOf<ParsedEpisode>()
        var item: MutableItem? = null
        var inChannelImage = false

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            val name = parser.name?.lowercase()

            if (event == XmlPullParser.START_TAG) {
                when (name) {
                    // RSS noemt het <item>, Atom <entry>. Verder lijken ze
                    // genoeg op elkaar om dezelfde weg te volgen.
                    "item", "entry" -> item = MutableItem()
                    "image" -> if (item == null) inChannelImage = true

                    "title" -> if (item != null) item.title = text(parser) else channelTitle = text(parser)

                    "description", "itunes:summary", "summary", "content", "content:encoded" ->
                        if (item != null) {
                            if (item.description.isNullOrBlank()) item.description = text(parser)
                        } else if (channelDescription.isNullOrBlank()) {
                            channelDescription = text(parser)
                        }

                    "itunes:author" -> if (item == null) channelAuthor = text(parser)

                    "itunes:image" -> {
                        val href = parser.getAttributeValue(null, "href")
                        if (item != null) item.imageUrl = href else if (href != null) channelImage = href
                    }

                    // <image><url>…</url></image> op kanaalniveau
                    "url" -> if (inChannelImage && channelImage == null) channelImage = text(parser)

                    "enclosure" -> if (item != null) {
                        val type = parser.getAttributeValue(null, "type").orEmpty()
                        if (type.isEmpty() || type.startsWith("audio")) {
                            item.audioUrl = parser.getAttributeValue(null, "url")
                        }
                    }

                    "itunes:duration" -> if (item != null) item.duration = parseDuration(text(parser))
                    "pubdate", "published", "updated", "dc:date" ->
                        if (item != null && item.releaseDate == null) {
                            item.releaseDate = parseDate(text(parser))
                        }
                    "guid" -> if (item != null) item.guid = text(parser)

                    // Google Nieuws zet in <source url="..."> bij welk medium
                    // een kop hoort. Een podcastfeed heeft dit niet.
                    "source" -> if (item != null) {
                        item.sourceUrl = parser.getAttributeValue(null, "url")
                        item.sourceName = text(parser)
                    }
                    // Atom zet het adres in een attribuut in plaats van in de tekst.
                    "link" -> if (item != null && item.link == null) {
                        val href = parser.getAttributeValue(null, "href")
                        item.link = href ?: text(parser)
                    }
                }
            } else if (event == XmlPullParser.END_TAG) {
                when (name) {
                    "item", "entry" -> {
                        item?.build()?.let(episodes::add)
                        item = null
                    }

                    "image" -> inChannelImage = false
                }
            }

            event = parser.next()
        }

        return ParsedFeed(
            title = Html.toPlainText(channelTitle),
            author = Html.toPlainText(channelAuthor),
            description = Html.toPlainText(channelDescription),
            imageUrl = channelImage?.trim(),
            episodes = episodes
        )
    }

    private fun text(parser: XmlPullParser): String? =
        runCatching { parser.nextText()?.trim()?.takeIf { it.isNotEmpty() } }.getOrNull()

    private class MutableItem {
        var guid: String? = null
        var title: String? = null
        var description: String? = null
        var audioUrl: String? = null
        var duration: Long? = null
        var releaseDate: String? = null
        var imageUrl: String? = null
        var link: String? = null
        var sourceUrl: String? = null
        var sourceName: String? = null

        fun build(): ParsedEpisode? {
            val heading = title ?: return null
            return ParsedEpisode(
                guid = guid ?: audioUrl ?: heading,
                title = Html.toPlainText(heading) ?: heading,
                description = Html.toPlainText(description),
                audioUrl = audioUrl,
                durationMillis = duration,
                releaseDate = releaseDate,
                imageUrl = imageUrl,
                link = link?.takeIf { it.startsWith("http") },
                sourceUrl = sourceUrl,
                sourceName = Html.toPlainText(sourceName)
            )
        }
    }

    private companion object {
        val RFC_1123: DateTimeFormatter = DateTimeFormatter.RFC_1123_DATE_TIME

        /** itunes:duration is seconden, of mm:ss, of hh:mm:ss. */
        fun parseDuration(raw: String?): Long? {
            val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            if (!value.contains(':')) return value.toLongOrNull()?.times(1000)
            val parts = value.split(':').mapNotNull { it.trim().toLongOrNull() }
            return when (parts.size) {
                2 -> (parts[0] * 60 + parts[1]) * 1000
                3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000
                else -> null
            }
        }

        /**
         * RSS schrijft "Wed, 02 Sep 2026 07:00:00 GMT", Atom
         * "2026-09-02T07:00:00Z". Beide moeten een datum opleveren, want op de
         * datum staat of een tip nog actueel is.
         */
        fun parseDate(raw: String?): String? {
            val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            runCatching {
                return Instant.from(RFC_1123.parse(value))
                    .atZone(ZoneId.systemDefault()).toLocalDate().toString()
            }
            runCatching {
                return java.time.OffsetDateTime.parse(value).toLocalDate().toString()
            }
            // Een kale datum vooraan telt ook: "2026-09-02T07:00:00+02:00".
            if (value.length >= 10 && value[4] == '-' && value[7] == '-') return value.take(10)
            return value.take(16)
        }
    }
}
