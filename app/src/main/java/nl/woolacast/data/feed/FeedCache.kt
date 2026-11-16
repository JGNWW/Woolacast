package nl.woolacast.data.feed

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import nl.woolacast.domain.Episode
import nl.woolacast.domain.Podcast

/** Een podcastpagina zoals hij de laatste keer uit de feed kwam. */
@Serializable
data class CachedFeed(
    val feedUrl: String,
    val etag: String? = null,
    val lastModified: String? = null,
    /** Wanneer de feed voor het laatst bij de maker is nagekeken. */
    val checkedAtMs: Long = 0L,
    val podcast: Podcast,
    val episodes: List<Episode> = emptyList()
)

/**
 * Bewaart per show de laatst gelezen feed, zodat de podcastpagina er meteen
 * staat en de app de maker alleen vraagt of er iets veranderd is. Een bestand
 * per show; de grootste feeds zijn een megabyte of meer, dus het blijft bij de
 * shows waar je het laatst was.
 */
class FeedCache(private val dir: File, private val keep: Int = 80) {

    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    /** De laatste paar in het geheugen: terug naar een pagina waar je net was kost dan niets. */
    private val recent = object : LinkedHashMap<String, CachedFeed>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedFeed>?) = size > 6
    }

    private fun fileFor(showId: String) =
        File(dir, showId.replace(Regex("[^A-Za-z0-9]+"), "_").take(60) + "-" + Integer.toHexString(showId.hashCode()) + ".json")

    suspend fun get(showId: String): CachedFeed? {
        if (showId.isBlank()) return null
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                recent[showId] ?: runCatching {
                    val file = fileFor(showId)
                    if (file.exists()) json.decodeFromString(CachedFeed.serializer(), file.readText()) else null
                }.getOrNull()?.also { recent[showId] = it }
            }
        }
    }

    suspend fun put(showId: String, feed: CachedFeed) {
        if (showId.isBlank()) return
        withContext(Dispatchers.IO) {
            mutex.withLock {
                recent[showId] = feed
                runCatching {
                    dir.mkdirs()
                    val target = fileFor(showId)
                    val temp = File(target.path + ".tmp")
                    temp.writeText(json.encodeToString(CachedFeed.serializer(), feed))
                    if (!temp.renameTo(target)) {
                        target.writeText(temp.readText())
                        temp.delete()
                    }
                    // De oudste gaan weg zodra het er te veel worden.
                    val files = dir.listFiles { file -> file.name.endsWith(".json") }.orEmpty()
                    if (files.size > keep) {
                        files.sortedBy { it.lastModified() }.take(files.size - keep).forEach { it.delete() }
                    }
                }
            }
        }
    }
}
