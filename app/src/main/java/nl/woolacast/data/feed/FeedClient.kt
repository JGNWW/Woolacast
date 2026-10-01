package nl.woolacast.data.feed

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/** Wat een voorwaardelijk verzoek opleverde: niets nieuws, of de feed met zijn nieuwe kenmerken. */
sealed interface FeedFetch {
    data object NotModified : FeedFetch
    data class Fetched(val feed: ParsedFeed, val etag: String?, val lastModified: String?) : FeedFetch
}

/** Haalt een RSS-feed op en laat de parser er doorheen lopen. */
class FeedClient(
    private val client: OkHttpClient,
    private val parser: RssFeedParser = RssFeedParser()
) {

    suspend fun fetch(feedUrl: String): ParsedFeed =
        (fetchIfChanged(feedUrl, null, null) as FeedFetch.Fetched).feed

    /**
     * Haalt de feed alleen op als hij veranderd is sinds [etag] en
     * [lastModified] van de vorige keer. Een ongewijzigde feed is dan een 304
     * zonder inhoud: dat scheelt de host en de bundel van de luisteraar bij elke
     * ronde langs alle gevolgde shows. Servers die die koppen negeren, geven
     * gewoon de hele feed.
     */
    suspend fun fetchIfChanged(feedUrl: String, etag: String?, lastModified: String?): FeedFetch =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(feedUrl)
                .header("User-Agent", USER_AGENT)
                .apply {
                    etag?.let { header("If-None-Match", it) }
                    lastModified?.let { header("If-Modified-Since", it) }
                }
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 304) return@withContext FeedFetch.NotModified
                if (!response.isSuccessful) throw IOException("Feed gaf ${response.code}")
                val body = response.body ?: throw IOException("Lege feed")
                FeedFetch.Fetched(
                    feed = parser.parse(body.byteStream()),
                    etag = response.header("ETag"),
                    lastModified = response.header("Last-Modified")
                )
            }
        }

    private companion object {
        const val USER_AGENT = "Toadcast/0.1 (+https://github.com/jgnww/Woolacast)"
    }
}
