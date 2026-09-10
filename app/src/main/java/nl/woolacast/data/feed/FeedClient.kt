package nl.woolacast.data.feed

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/** Haalt een RSS-feed op en laat de parser er doorheen lopen. */
class FeedClient(
    private val client: OkHttpClient,
    private val parser: RssFeedParser = RssFeedParser()
) {

    suspend fun fetch(feedUrl: String): ParsedFeed = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(feedUrl)
            .header("User-Agent", USER_AGENT)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Feed gaf ${response.code}")
            val body = response.body ?: throw IOException("Lege feed")
            parser.parse(body.byteStream())
        }
    }

    private companion object {
        const val USER_AGENT = "Woolacast/0.1 (+https://github.com/jgnww/Woolacast)"
    }
}
