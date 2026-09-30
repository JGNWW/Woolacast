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
import nl.woolacast.domain.Chapter
import nl.woolacast.domain.Episode
import nl.woolacast.domain.TranscriptRef

@Serializable
data class DaySnapshot(val date: String, val ranks: Map<String, Int>)

@Serializable
data class FollowedShow(
    val id: String,
    val title: String,
    val publisher: String,
    val artworkUrl: String? = null,
    val feedUrl: String? = null,
    /** Datum waarop je de podcastpagina voor het laatst opende; nieuwer is 'nieuw'. */
    val lastOpened: String? = null
)

/**
 * Een maker die je volgt. [knownShowIds] zijn de shows die hij had toen je hem
 * voor het laatst bekeek: wat er daarna bij komt, is nieuw.
 */
@Serializable
data class FollowedMaker(
    val key: String,
    val name: String,
    val channelId: String? = null,
    val logoUrl: String? = null,
    val color: String? = null,
    val followedOn: String = "",
    val knownShowIds: List<String> = emptyList(),
    /** Het land waar je hem volgde: daar kijkt de dagelijkse controle. */
    val country: String = "nl",
    /** Nieuwe shows waar al een melding over ging; die komt geen tweede keer. */
    val notifiedShowIds: List<String> = emptyList(),
    /** Per nieuwe show zonder kanaal: de dag waarop de app hem vond. */
    val foundOn: Map<String, String> = emptyMap()
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
    val releaseDate: String? = null,
    val link: String? = null,
    val chaptersUrl: String? = null,
    val chapters: List<Chapter> = emptyList(),
    val transcript: TranscriptRef? = null
) {
    fun toEpisode() = Episode(
        id = id, showId = showId, showTitle = showTitle, title = title,
        description = null, artworkUrl = artworkUrl, audioUrl = audioUrl,
        durationMillis = durationMillis, releaseDate = releaseDate, link = link,
        chaptersUrl = chaptersUrl, inlineChapters = chapters, transcript = transcript
    )
}

fun Episode.toSaved() = SavedEpisode(
    id = id, showId = showId, showTitle = showTitle, title = title,
    artworkUrl = artworkUrl, audioUrl = audioUrl,
    durationMillis = durationMillis, releaseDate = releaseDate, link = link,
    chaptersUrl = chaptersUrl, chapters = inlineChapters, transcript = transcript
)

/**
 * Een gedownloade (of nog te downloaden) aflevering. [fileName] staat in de map
 * downloads van de app; [auto] zegt of de app hem zelf klaarzette.
 */
@Serializable
data class DownloadRecord(
    val episode: SavedEpisode,
    val fileName: String,
    val state: DownloadState = DownloadState.QUEUED,
    val bytes: Long = 0L,
    val auto: Boolean = false,
    /** Wanneer hij in de rij kwam, als ISO-moment; de oudste ruimt de app het eerst op. */
    val addedAt: String = "",
    /** Wanneer je hem uitluisterde; een dag later mag hij weg. */
    val listenedAt: String? = null,
    val error: String? = null
)

@Serializable
enum class DownloadState { QUEUED, DONE, FAILED }

@Serializable
data class DownloadSettings(
    /** Hoeveel ruimte downloads samen mogen innemen. */
    val limitMb: Int = 2048,
    /** Alleen op wifi (of een ander onbeperkt netwerk). */
    val wifiOnly: Boolean = true,
    /** Uitgeluisterd wissen, een dag na het uitluisteren. */
    val deleteListened: Boolean = true
)

/** Wat de app zelf uit de feeds haalde, met het moment erbij. */
@Serializable
data class CachedTips(
    val fetchedAt: String,
    val entries: List<nl.woolacast.data.dataset.MediaTip> = emptyList()
)

@Serializable
private data class StoreData(
    val follows: List<FollowedShow> = emptyList(),
    val makers: List<FollowedMaker> = emptyList(),
    /**
     * Per maker de hoezen die zijn gezicht vormen, in de volgorde van zijn
     * shows. Zo ziet een maker er op elk scherm hetzelfde uit, ook als dat
     * scherm zelf maar een deel van zijn shows kent.
     */
    val makerFaces: Map<String, List<String>> = emptyMap(),
    val snapshots: Map<String, List<DaySnapshot>> = emptyMap(),
    val charts: Map<String, CachedChart> = emptyMap(),
    val queue: List<SavedEpisode> = emptyList(),
    val saved: List<SavedEpisode> = emptyList(),
    /** Waar je gebleven bent, per aflevering, in milliseconden. */
    val progress: Map<String, Long> = emptyMap(),
    /** "light", "dark" of "system"; standaard volgt de app het toestel. */
    val theme: String = "system",
    /** Tips die de app zelf uit de feeds las, per land. */
    val liveTips: Map<String, CachedTips> = emptyMap(),
    /** En hetzelfde per podcast: wie deze show ergens aanraadde. */
    val showTips: Map<String, CachedTips> = emptyMap(),
    /** Per aflevering-id wat er gedownload is of wordt. */
    val downloads: Map<String, DownloadRecord> = emptyMap(),
    val downloadSettings: DownloadSettings = DownloadSettings(),
    /** Per show hoeveel nieuwe afleveringen de app automatisch klaarzet. */
    val autoDownload: Map<String, Int> = emptyMap(),
    /** Afleveringen die je uitluisterde, de nieuwste achteraan. */
    val listened: List<String> = emptyList()
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

    private val _makers = MutableStateFlow<List<FollowedMaker>>(emptyList())
    val makers: StateFlow<List<FollowedMaker>> = _makers.asStateFlow()

    private val _makerFaces = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val makerFaces: StateFlow<Map<String, List<String>>> = _makerFaces.asStateFlow()

    private val _queue = MutableStateFlow<List<SavedEpisode>>(emptyList())
    val queue: StateFlow<List<SavedEpisode>> = _queue.asStateFlow()

    private val _saved = MutableStateFlow<List<SavedEpisode>>(emptyList())
    val saved: StateFlow<List<SavedEpisode>> = _saved.asStateFlow()

    private val _progress = MutableStateFlow<Map<String, Long>>(emptyMap())
    val progress: StateFlow<Map<String, Long>> = _progress.asStateFlow()

    private val _downloads = MutableStateFlow<Map<String, DownloadRecord>>(emptyMap())
    val downloads: StateFlow<Map<String, DownloadRecord>> = _downloads.asStateFlow()

    private val _downloadSettings = MutableStateFlow(DownloadSettings())
    val downloadSettings: StateFlow<DownloadSettings> = _downloadSettings.asStateFlow()

    private val _autoDownload = MutableStateFlow<Map<String, Int>>(emptyMap())
    val autoDownload: StateFlow<Map<String, Int>> = _autoDownload.asStateFlow()

    private val _listened = MutableStateFlow<Set<String>>(emptySet())
    val listened: StateFlow<Set<String>> = _listened.asStateFlow()

    private val _theme = MutableStateFlow("system")
    val theme: StateFlow<String> = _theme.asStateFlow()

    suspend fun setTheme(mode: String) = mutate { it.copy(theme = mode) }

    private var loaded = false

    /** Laadt het bestand als dat nog niet gebeurd is; voor werk buiten de app om. */
    suspend fun ensureLoaded() {
        if (!loaded) load()
    }

    suspend fun load() = withContext(Dispatchers.IO) {
        mutex.withLock {
            loaded = true
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

    /** Volgt een rij shows tegelijk; wat je al volgde blijft zoals het was. */
    suspend fun followAll(shows: List<FollowedShow>) = mutate { data ->
        val have = data.follows.map { it.id }.toSet()
        data.copy(follows = data.follows + shows.filter { it.id !in have }.distinctBy { it.id })
    }

    /**
     * Een show die op zijn feed gevolgd werd, heeft nu een Apple-id. Alles wat
     * aan de oude id hing, verhuist mee: automatisch downloaden, downloads en de
     * wachtrij. Voortgang hangt aan afleveringen en blijft vanzelf.
     */
    suspend fun relinkFollow(oldId: String, show: FollowedShow) = mutate { data ->
        fun SavedEpisode.moved() = if (showId == oldId) copy(showId = show.id) else this
        val already = data.follows.any { it.id == show.id }
        data.copy(
            follows = if (already) data.follows.filterNot { it.id == oldId }
                      else data.follows.map { if (it.id == oldId) show.copy(lastOpened = it.lastOpened) else it },
            autoDownload = data.autoDownload[oldId]?.let { count -> data.autoDownload - oldId + (show.id to count) }
                ?: data.autoDownload,
            downloads = data.downloads.mapValues { (_, record) -> record.copy(episode = record.episode.moved()) },
            queue = data.queue.map { it.moved() },
            saved = data.saved.map { it.moved() }
        )
    }

    /** Onthoudt wanneer je een gevolgde show voor het laatst bekeek. */
    suspend fun markOpened(showId: String) = mutate { data ->
        if (data.follows.none { it.id == showId }) return@mutate data
        val today = LocalDate.now().toString()
        data.copy(follows = data.follows.map { if (it.id == showId) it.copy(lastOpened = today) else it })
    }

    /* ---- makers ---- */

    fun isMakerFollowed(key: String) = _makers.value.any { it.key == key }

    suspend fun toggleMaker(maker: FollowedMaker) = mutate { data ->
        val present = data.makers.any { it.key == maker.key }
        data.copy(makers = if (present) data.makers.filterNot { it.key == maker.key }
                           else data.makers + maker.copy(followedOn = LocalDate.now().toString()))
    }

    /** Wat je nu van deze maker gezien hebt; alleen wat daarna komt heet nieuw. */
    suspend fun markMakerSeen(key: String, showIds: Collection<String>) = mutate { data ->
        if (data.makers.none { it.key == key }) return@mutate data
        data.copy(makers = data.makers.map {
            if (it.key == key) it.copy(knownShowIds = (it.knownShowIds + showIds).distinct()) else it
        })
    }

    /** Onthoudt wanneer een nieuwe show voor het eerst gevonden werd; een tweede keer verandert niets. */
    suspend fun markMakerFound(key: String, showIds: Collection<String>) = mutate { data ->
        val today = LocalDate.now().toString()
        data.copy(makers = data.makers.map { maker ->
            if (maker.key != key) maker
            else maker.copy(foundOn = maker.foundOn + showIds.filterNot { it in maker.foundOn }.associateWith { today })
        })
    }

    fun makerFace(key: String): List<String>? = _makerFaces.value[key]

    /**
     * Onthoudt het gezicht van een maker. Het nieuwste staat achteraan; boven
     * [MAKER_FACES_KEPT] vallen de oudste af, behalve die van makers die je volgt.
     */
    suspend fun rememberMakerFace(key: String, covers: List<String>) = mutate { data ->
        if (covers.isEmpty() || data.makerFaces[key] == covers) return@mutate data
        val faces = LinkedHashMap(data.makerFaces).apply { remove(key); put(key, covers) }
        val followed = data.makers.map { it.key }.toSet()
        faces.keys.filterNot { it in followed }.take((faces.size - MAKER_FACES_KEPT).coerceAtLeast(0)).forEach(faces::remove)
        data.copy(makerFaces = faces)
    }

    /** Onthoudt over welke nieuwe shows al een melding ging. */
    suspend fun markMakerNotified(key: String, showIds: Collection<String>) = mutate { data ->
        data.copy(makers = data.makers.map {
            if (it.key == key) it.copy(notifiedShowIds = (it.notifiedShowIds + showIds).distinct()) else it
        })
    }

    /* ---- wachtrij en bewaard ---- */

    fun isQueued(id: String) = _queue.value.any { it.id == id }
    fun isSaved(id: String) = _saved.value.any { it.id == id }

    suspend fun removeFromQueue(episodeId: String) = mutate {
        it.copy(queue = it.queue.filterNot { q -> q.id == episodeId })
    }

    suspend fun removeSaved(episodeId: String) = mutate {
        it.copy(saved = it.saved.filterNot { q -> q.id == episodeId })
    }

    /** Verplaatst een aflevering naar de kop van de wachtrij (of zet hem erin). */
    suspend fun playNext(episode: SavedEpisode) = mutate {
        it.copy(queue = listOf(episode) + it.queue.filterNot { q -> q.id == episode.id })
    }

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

    /**
     * Alleen bewaren als er iets te onthouden valt; anders groeit dit eindeloos.
     * Geeft true terug als de aflevering daarmee uitgeluisterd is.
     */
    suspend fun rememberProgress(episodeId: String, positionMs: Long, durationMs: Long): Boolean {
        if (positionMs < 30_000L) return false
        val finished = durationMs > 0L && positionMs > durationMs - 60_000L
        mutate { data ->
            data.copy(
                progress = if (finished) data.progress - episodeId else data.progress + (episodeId to positionMs),
                listened = if (finished) (data.listened - episodeId + episodeId).takeLast(LISTENED_KEPT) else data.listened,
                downloads = if (finished) data.downloads[episodeId]?.let { record ->
                    data.downloads + (episodeId to record.copy(listenedAt = record.listenedAt ?: java.time.Instant.now().toString()))
                } ?: data.downloads else data.downloads
            )
        }
        return finished
    }

    /* ---- downloads ---- */

    fun download(episodeId: String): DownloadRecord? = data.downloads[episodeId]

    suspend fun putDownload(record: DownloadRecord) = mutate {
        it.copy(downloads = it.downloads + (record.episode.id to record))
    }

    /** Past een download aan, als hij nog bestaat. */
    suspend fun updateDownload(episodeId: String, change: (DownloadRecord) -> DownloadRecord) = mutate { data ->
        val record = data.downloads[episodeId] ?: return@mutate data
        data.copy(downloads = data.downloads + (episodeId to change(record)))
    }

    suspend fun removeDownload(episodeId: String) = mutate { it.copy(downloads = it.downloads - episodeId) }

    suspend fun setDownloadSettings(settings: DownloadSettings) = mutate { it.copy(downloadSettings = settings) }

    /** [count] null of 0 zet automatisch downloaden voor deze show uit. */
    suspend fun setAutoDownload(showId: String, count: Int?) = mutate {
        it.copy(autoDownload = if (count == null || count <= 0) it.autoDownload - showId
                               else it.autoDownload + (showId to count))
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

    /* ---- tips die de app zelf ophaalde ---- */

    fun cachedTips(countryCode: String): CachedTips? = data.liveTips[countryCode]

    suspend fun cacheTips(countryCode: String, tips: CachedTips) = mutate {
        it.copy(liveTips = it.liveTips + (countryCode to tips))
    }

    fun cachedShowTips(showId: String): CachedTips? = data.showTips[showId]

    /** Hooguit een paar honderd shows onthouden; de oudste vallen eraf. */
    suspend fun cacheShowTips(showId: String, tips: CachedTips) = mutate { current ->
        val trimmed = if (current.showTips.size < SHOW_TIPS_KEPT) current.showTips
        else current.showTips.entries
            .sortedByDescending { it.value.fetchedAt }
            .take(SHOW_TIPS_KEPT / 2)
            .associate { it.key to it.value }
        current.copy(showTips = trimmed + (showId to tips))
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
        _makers.value = data.makers
        _makerFaces.value = data.makerFaces
        _queue.value = data.queue
        _saved.value = data.saved
        _progress.value = data.progress
        _theme.value = data.theme
        _downloads.value = data.downloads
        _downloadSettings.value = data.downloadSettings
        _autoDownload.value = data.autoDownload
        _listened.value = data.listened.toSet()
    }

    private companion object {
        const val HISTORY_DAYS = 30

        /** Zoveel uitgeluisterde afleveringen onthouden we. */
        const val LISTENED_KEPT = 2000

        /** Zoveel podcasts onthouden we hun tips van; daarna vallen de oudste af. */
        const val SHOW_TIPS_KEPT = 300

        /** Zoveel makers onthouden we hun gezicht van. */
        const val MAKER_FACES_KEPT = 200
    }
}
