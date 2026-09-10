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
    val artworkUrl: String? = null
)

@Serializable
private data class StoreData(
    val follows: List<FollowedShow> = emptyList(),
    val snapshots: Map<String, List<DaySnapshot>> = emptyMap()
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

    suspend fun load() = withContext(Dispatchers.IO) {
        mutex.withLock {
            data = runCatching {
                if (file.exists()) json.decodeFromString(StoreData.serializer(), file.readText()) else StoreData()
            }.getOrElse { StoreData() }
            _follows.value = data.follows
        }
    }

    /* ---- volgen ---- */

    fun isFollowed(id: String) = _follows.value.any { it.id == id }

    suspend fun toggleFollow(show: FollowedShow) = mutate {
        val existing = it.follows.any { followed -> followed.id == show.id }
        val follows = if (existing) it.follows.filterNot { f -> f.id == show.id } else it.follows + show
        it.copy(follows = follows)
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

    private suspend fun mutate(block: (StoreData) -> StoreData) = withContext(Dispatchers.IO) {
        mutex.withLock {
            data = block(data)
            _follows.value = data.follows
            runCatching {
                file.parentFile?.mkdirs()
                file.writeText(json.encodeToString(StoreData.serializer(), data))
            }
        }
        Unit
    }

    private companion object {
        const val HISTORY_DAYS = 30
    }
}
