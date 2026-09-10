package nl.woolacast.data.local

import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class DaySnapshot(val date: String, val ranks: Map<String, Int>)

@Serializable
data class FollowedShow(
    val id: String,
    val title: String,
    val publisher: String,
    val artworkUrl: String? = null,
    val feedUrl: String? = null
)

@Serializable
data class CachedEntry(
    val rank: Int,
    val id: String,
    val title: String,
    val publisher: String,
    val artworkUrl: String? = null,
    val genre: String? = null,
    val storeUrl: String? = null,
    val showId: String? = null,
    val feedUrl: String? = null
)

@Serializable
data class CachedChart(
    val fetchedAt: String,
    val updatedLabel: String? = null,
    val entries: List<CachedEntry> = emptyList()
)

@Serializable
data class SavedEpisode(
    val id: String,
    val showId: String,
    val showTitle: String,
    val title: String,
    val artworkUrl: String? = null,
    val audioUrl: String? = null,
    val durationMillis: Long? = null,
    val releaseDate: String? = null
)

@Serializable
private data class StoreData(
    val follows: List<FollowedShow> = emptyList(),
    val snapshots: Map<String, List<DaySnapshot>> = emptyMap(),
    val charts: Map<String, CachedChart> = emptyMap(),
    val queue: List<SavedEpisode> = emptyList(),
    val saved: List<SavedEpisode> = emptyList(),
    /** Waar je gebleven bent, per aflevering, in milliseconden. */
    val progress: Map<String, Long> = emptyMap()
)

/**
 * Alles wat de app onthoudt staat in een enkel JSON-bestand: welke shows je
 * volgt, en per lijst een momentopname per dag. Die momentopnames zijn wat
 * beweging (stijgers en dalers) mogelijk maakt — zonder gisteren is er niets
 * om vandaag mee te vergelijken.
 *
 * Bewust geen database in deze eerste versie: het gaat om een paar honderd
 * regels. Zodra er echte historie overheen gaat — grafieken over maanden —
 * is dit het punt om naar Room te verhuizen.
 */
class LocalStore(private val file: File) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val mutex = Mutex()
    private var data = StoreData()

    private val _follows = MutableStateFlow<List<FollowedShow>>(emptyList())
    val follows: StateFlow<List<FollowedShow>> = _follows.asStateFlow()

    private val _queue = MutableStateFlow<List<SavedEpisode>>(emptyList())
    val queue: StateFlow<List<SavedEpisode>> = _queue.asStateFlow()

    private val _saved = MutableStateFlow<List<SavedEpisode>>(emptyList())
    val saved: StateFlow<List<SavedEpisode>> = _saved.asStateFlow()

    private val _progress = MutableStateFlow<Map<String, Long>>(emptyMap())
    val progress: StateFlow<Map<String, Long>> = _progress.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        mutex.withLock {
            data = runCatching {
                if (file.exists()) json.decodeFromString(StoreData.serializer(), file.readText()) else StoreData()
            }.getOrElse { StoreData() }
            publish()
        }
    }

    /* ---- volgen ---- */

    fun isFollowed(id: String) = _follows.value.any { it.id == id }

    suspend fun toggleFollow(show: FollowedShow) = mutate {
        val existing = it.follows.any { followed -> followed.id == show.id }
        val follows = if (existing) it.follows.filterNot { f -> f.id == show.id } else it.follows + show
        it.copy(follows = follows)
    }

    /* ---- wachtrij en bewaard ---- */

    suspend fun toggleQueue(episode: SavedEpisode) = mutate {
        val present = it.queue.any { queued -> queued.id == episode.id }
        it.copy(queue = if (present) it.queue.filterNot { q -> q.id == episode.id }
                        else it.queue + episode)
    }

    suspend fun toggleSaved(episode: SavedEpisode) = mutate {
        val present = it.saved.any { stored -> stored.id == episode.id }
        it.copy(saved = if (present) it.saved.filterNot { q -> q.id == episode.id }
                        else it.saved + episode)
    }

    /** Alleen bewaren als er iets te onthouden valt; anders groeit dit eindeloos. */
    suspend fun rememberProgress(episodeId: String, positionMs: Long, durationMs: Long) {
        if (positionMs < 30_000L) return
        val finished = durationMs > 0L && positionMs > durationMs - 60_000L
        mutate { data ->
            data.copy(progress = if (finished) data.progress - episodeId
                                 else data.progress + (episodeId to positionMs))
        }
    }

    /* ---- momentopnames ---- */

    /**
     * De meest recente opname van een *eerdere* dag. Vandaag telt niet mee:
     * anders wist een verversing de basislijn en stond alles op nul.
     */
    fun baseline(queryKey: String): Map<String, Int>? {
        val today = LocalDate.now().toString()
        return data.snapshots[queryKey]
            ?.filter { it.date != today }
            ?.maxByOrNull { it.date }
            ?.ranks
    }

    suspend fun record(queryKey: String, ranks: Map<String, Int>) = mutate { current ->
        val today = LocalDate.now().toString()
        val existing = current.snapshots[queryKey].orEmpty().filterNot { it.date == today }
        val updated = (existing + DaySnapshot(today, ranks))
            .sortedByDescending { it.date }
            .take(HISTORY_DAYS)
        current.copy(snapshots = current.snapshots + (queryKey to updated))
    }

    /* ---- lijsten offline ---- */

    fun cachedChart(queryKey: String): CachedChart? = data.charts[queryKey]

    suspend fun cacheChart(queryKey: String, chart: CachedChart) = mutate {
        it.copy(charts = it.charts + (queryKey to chart))
    }

    private suspend fun mutate(block: (StoreData) -> StoreData) = withContext(Dispatchers.IO) {
        mutex.withLock {
            data = block(data)
            publish()
            runCatching {
                file.parentFile?.mkdirs()
                file.writeText(json.encodeToString(StoreData.serializer(), data))
            }
        }
        Unit
    }

    private fun publish() {
        _follows.value = data.follows
        _queue.value = data.queue
        _saved.value = data.saved
        _progress.value = data.progress
    }

    private companion object {
        const val HISTORY_DAYS = 30
    }
}
