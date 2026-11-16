package nl.woolacast.data.local

import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    /** Datum waarop je de podcastpagina voor het laatst opende. Oude volgers zonder [followedAt] tellen nieuw vanaf hier. */
    val lastOpened: String? = null,
    /** Datum waarop je de show ging volgen; wat daarna verschijnt, is nieuw. */
    val followedAt: String? = null,
    /** Een melding bij een nieuwe aflevering; standaard uit. */
    val notifyNew: Boolean = false
)

/**
 * Wat de feedronde de laatste keer van een show zag: de kenmerken voor een
 * voorwaardelijk verzoek, en de nieuwste afleveringen. Daaruit komen de lijst
 * Nieuw, het getal "2 nieuw" en de meldingen, zonder dat er een feed open hoeft.
 */
@Serializable
data class FeedCheck(
    val checkedAt: String = "",
    val etag: String? = null,
    val lastModified: String? = null,
    /** De nieuwste eerst. */
    val latest: List<SavedEpisode> = emptyList()
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
    val feedUrl: String? = null,
    val description: String? = null,
    val durationMillis: Long? = null,
    val releaseDate: String? = null,
    /** De beweging zoals hij bij het ophalen berekend werd: "up:3", "down:", "new", "flat". */
    val move: String? = null
)

@Serializable
data class CachedChart(
    val fetchedAt: String,
    val updatedLabel: String? = null,
    val entries: List<CachedEntry> = emptyList(),
    /** Het moment van ophalen; binnen een kwartier vragen we de bron niet opnieuw. */
    val fetchedAtMs: Long = 0L
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

/**
 * Wat de app van je keuzes onthoudt: de lijst waar je het laatst naar keek, en
 * een paar schakelaars. Bij de volgende start staat alles weer zoals je het liet.
 */
@Serializable
data class Prefs(
    val chartSource: String = "APPLE",
    /** Leeg betekent: het land waarmee de app begint. */
    val chartCountry: String = "",
    /** 0 is alle categorieën. */
    val chartGenre: Int = 0,
    val chartLevel: String = "SHOWS",
    val chartByMaker: Boolean = false,
    val libraryTab: String = "",
    val librarySection: String = "",
    val librarySort: String = "RECENT",
    /** Afspelen klapt het spelerscherm uit; uit betekent alleen de balk onderin. */
    val openPlayerOnPlay: Boolean = false,
    /** Een melding als een show die je volgt opvallend beweegt in de hitlijst. */
    val chartAlertNotify: Boolean = false
)

/** Hoe de speler klinkt, voor alles wat geen eigen instelling heeft. */
@Serializable
data class PlaybackSettings(
    val skipSilence: Boolean = false,
    val volumeBoost: Boolean = false,
    val speed: Float = 1f
)

/** Wat je per show anders wilt dan de rest. */
@Serializable
data class ShowPrefs(
    /** Een eigen snelheid; null volgt de algemene. */
    val speed: Float? = null,
    /** Afleveringen van oud naar nieuw, voor een verhaal dat bij deel 1 begint. */
    val oldestFirst: Boolean = false,
    /** Nieuwe afleveringen gaan vanzelf achter de wachtrij. */
    val autoQueue: Boolean = false
)

/** Een aflevering die je afspeelde, met wanneer voor het laatst. */
@Serializable
data class HistoryItem(val episode: SavedEpisode, val at: String, val finished: Boolean = false)

/** Een plek in een aflevering die je wilt terugvinden. */
@Serializable
data class Bookmark(
    val id: String,
    val episode: SavedEpisode,
    val positionMs: Long,
    val createdAt: String
)

/** Hoe lang je naar een show luisterde, opgeteld. */
@Serializable
data class ShowListening(
    val title: String,
    val artworkUrl: String? = null,
    val ms: Long = 0L,
    val lastAt: String = ""
)

/** De momentopnames en de laatst opgehaalde stand van één lijst; elke lijst heeft zijn eigen bestand. */
@Serializable
private data class ListFile(val snapshots: List<DaySnapshot> = emptyList(), val chart: CachedChart? = null)

/** Tips die de app zelf las; los van de bibliotheek, want het is alleen geheugen. */
@Serializable
private data class TipsFile(
    val liveTips: Map<String, CachedTips> = emptyMap(),
    val showTips: Map<String, CachedTips> = emptyMap()
)

/**
 * Een reservekopie: de bibliotheek met een versie erbij. De versie heeft met
 * opzet geen standaardwaarde: zo staat hij altijd in het bestand, en is een
 * bestand zonder dat veld geen reservekopie.
 */
@Serializable
private data class BackupFile(val toadcastBackup: Int, val madeAt: String = "", val data: StoreData = StoreData())

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
    /** Stond vroeger hier; verhuist bij het laden naar een bestand per lijst. */
    val snapshots: Map<String, List<DaySnapshot>> = emptyMap(),
    val charts: Map<String, CachedChart> = emptyMap(),
    val queue: List<SavedEpisode> = emptyList(),
    val saved: List<SavedEpisode> = emptyList(),
    /** Waar je gebleven bent, per aflevering, in milliseconden. */
    val progress: Map<String, Long> = emptyMap(),
    /** "light", "dark" of "system"; standaard volgt de app het toestel. */
    val theme: String = "system",
    /** Tips die de app zelf uit de feeds las, per land. Verhuisd naar een eigen bestand. */
    val liveTips: Map<String, CachedTips> = emptyMap(),
    /** En hetzelfde per podcast: wie deze show ergens aanraadde. */
    val showTips: Map<String, CachedTips> = emptyMap(),
    /** Per aflevering-id wat er gedownload is of wordt. */
    val downloads: Map<String, DownloadRecord> = emptyMap(),
    val downloadSettings: DownloadSettings = DownloadSettings(),
    /** Per show hoeveel nieuwe afleveringen de app automatisch klaarzet. */
    val autoDownload: Map<String, Int> = emptyMap(),
    /** Afleveringen die je uitluisterde, de nieuwste achteraan. */
    val listened: List<String> = emptyList(),
    /** Automatische downloads die wegens ruimte opgeruimd zijn; die komen niet vanzelf terug. */
    val evicted: List<String> = emptyList(),
    /** Gewiste downloads die nog speelden: bestandsnaam → aflevering. */
    val orphans: Map<String, String> = emptyMap(),
    /** Per gevolgde show wat de feedronde laatst zag. */
    val feedChecks: Map<String, FeedCheck> = emptyMap(),
    /** Uit Nieuw weggeveegd; dat is niet hetzelfde als beluisterd. De nieuwste achteraan. */
    val hiddenNew: List<String> = emptyList(),
    /** Afleveringen waar al een melding over ging. De nieuwste achteraan. */
    val notifiedNew: List<String> = emptyList(),
    val prefs: Prefs = Prefs(),
    val playback: PlaybackSettings = PlaybackSettings(),
    val showPrefs: Map<String, ShowPrefs> = emptyMap(),
    /** Wat je afspeelde, de nieuwste achteraan. */
    val history: List<HistoryItem> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList(),
    /** Luistertijd per show. */
    val listening: Map<String, ShowListening> = emptyMap(),
    /** Luistertijd per dag, in milliseconden. */
    val listeningDays: Map<String, Long> = emptyMap(),
    /** Chart-alerts waar al een melding over ging, als "dag|show". De nieuwste achteraan. */
    val notifiedAlerts: List<String> = emptyList()
)

/**
 * Wat de app onthoudt, in drie soorten bestanden naast elkaar.
 *
 * Het hoofdbestand is je bibliotheek: wat je volgt, de wachtrij, waar je
 * gebleven bent, je instellingen. Dat is klein en staat bij de start meteen
 * klaar. De momentopnames per lijst — wat beweging (stijgers en dalers)
 * mogelijk maakt — staan in een eigen bestandje per lijst, en worden pas
 * gelezen als die lijst gevraagd wordt. Vroeger stond alles in één bestand van
 * een paar megabyte, dat bij elke tik in zijn geheel opnieuw geschreven werd;
 * een oud bestand wordt bij het laden vanzelf uit elkaar gehaald.
 *
 * Een wijziging aan de bibliotheek is meteen overal te zien, maar gaat pas even
 * later naar schijf, met alles wat er intussen bij kwam in één keer. Wie zeker
 * wil weten dat het er staat (de app gaat naar de achtergrond, een klus is
 * klaar), roept [flush].
 */
class LocalStore(private val file: File) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val mutex = Mutex()
    private var data = StoreData()

    private val home: File = file.absoluteFile.parentFile ?: File(".")
    private val listsDir = File(home, file.nameWithoutExtension + "-lists")
    private val tipsFile = File(home, file.nameWithoutExtension + "-tips.json")

    /** Bewaakt de bestanden per lijst en de tips; los van [mutex], zodat een lijst lezen de bibliotheek niet ophoudt. */
    private val sideMutex = Mutex()
    private val lists = HashMap<String, ListFile>()
    private var tips: TipsFile? = null

    /** Schrijft weg los van wie de wijziging deed, zodat een gesloten scherm niets tegenhoudt. */
    private val writer = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pendingWrite: Job? = null
    private var writeAt = Long.MAX_VALUE
    private var dirty = false

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

    private val _evicted = MutableStateFlow<Set<String>>(emptySet())
    val evicted: StateFlow<Set<String>> = _evicted.asStateFlow()

    private val _orphans = MutableStateFlow<Map<String, String>>(emptyMap())
    val orphans: StateFlow<Map<String, String>> = _orphans.asStateFlow()

    private val _feedChecks = MutableStateFlow<Map<String, FeedCheck>>(emptyMap())
    val feedChecks: StateFlow<Map<String, FeedCheck>> = _feedChecks.asStateFlow()

    private val _hiddenNew = MutableStateFlow<Set<String>>(emptySet())
    val hiddenNew: StateFlow<Set<String>> = _hiddenNew.asStateFlow()

    private val _theme = MutableStateFlow("system")
    val theme: StateFlow<String> = _theme.asStateFlow()

    private val _prefs = MutableStateFlow(Prefs())
    val prefs: StateFlow<Prefs> = _prefs.asStateFlow()

    private val _playback = MutableStateFlow(PlaybackSettings())
    val playback: StateFlow<PlaybackSettings> = _playback.asStateFlow()

    private val _showPrefs = MutableStateFlow<Map<String, ShowPrefs>>(emptyMap())
    val showPrefs: StateFlow<Map<String, ShowPrefs>> = _showPrefs.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    /** De nieuwste achteraan. */
    val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    val bookmarks: StateFlow<List<Bookmark>> = _bookmarks.asStateFlow()

    private val _listening = MutableStateFlow<Map<String, ShowListening>>(emptyMap())
    val listening: StateFlow<Map<String, ShowListening>> = _listening.asStateFlow()

    private val _listeningDays = MutableStateFlow<Map<String, Long>>(emptyMap())
    val listeningDays: StateFlow<Map<String, Long>> = _listeningDays.asStateFlow()

    suspend fun setTheme(mode: String) = mutate { it.copy(theme = mode) }

    @Volatile
    private var loaded = false

    /** Laadt het bestand als dat nog niet gebeurd is; voor werk buiten de app om. */
    suspend fun ensureLoaded() {
        if (!loaded) load()
    }

    /** Leest de bibliotheek. Een tweede keer doet niets: wat in het geheugen staat is dan nieuwer dan de schijf. */
    suspend fun load() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (loaded) return@withLock
            val read = runCatching {
                if (file.exists()) json.decodeFromString(StoreData.serializer(), file.readText()) else StoreData()
            }.getOrElse {
                // Onleesbaar: opzij zetten in plaats van er bij de eerste wijziging overheen te schrijven.
                runCatching { file.copyTo(File(file.path + ".onleesbaar"), overwrite = true) }
                StoreData()
            }
            data = splitOff(read)
            loaded = true
            publish()
        }
        runCatching { prune() }
        Unit
    }

    /**
     * Een bestand van vóór de splitsing draagt de momentopnames en de tips nog
     * in zich. Die gaan naar hun eigen bestanden; het hoofdbestand wordt daarna
     * zonder hen opnieuw geschreven.
     */
    private suspend fun splitOff(read: StoreData): StoreData {
        if (read.snapshots.isEmpty() && read.charts.isEmpty() && read.liveTips.isEmpty() && read.showTips.isEmpty()) return read
        sideMutex.withLock {
            (read.snapshots.keys + read.charts.keys).forEach { key ->
                val entry = ListFile(read.snapshots[key].orEmpty(), read.charts[key])
                lists[key] = entry
                writeAside(listFile(key), json.encodeToString(ListFile.serializer(), entry))
            }
            if (read.liveTips.isNotEmpty() || read.showTips.isNotEmpty()) {
                val moved = TipsFile(read.liveTips, read.showTips)
                tips = moved
                writeAside(tipsFile, json.encodeToString(TipsFile.serializer(), moved))
            }
        }
        dirty = true
        scheduleWrite(WRITE_MS)
        return read.copy(snapshots = emptyMap(), charts = emptyMap(), liveTips = emptyMap(), showTips = emptyMap())
    }

    /** Een lijst waar anderhalve maand niet naar gekeken is, heeft geen bruikbare momentopnames meer. */
    private fun prune() {
        val limit = System.currentTimeMillis() - LIST_KEPT_DAYS * 24L * 3_600_000L
        listsDir.listFiles()?.forEach { if (it.lastModified() in 1 until limit) it.delete() }
    }

    /* ---- keuzes en instellingen ---- */

    suspend fun setPrefs(change: (Prefs) -> Prefs) = mutate {
        val next = change(it.prefs)
        if (next == it.prefs) it else it.copy(prefs = next)
    }

    suspend fun setPlayback(change: (PlaybackSettings) -> PlaybackSettings) = mutate {
        val next = change(it.playback)
        if (next == it.playback) it else it.copy(playback = next)
    }

    /** Wat alleen voor deze show geldt. Is er niets bijzonders meer, dan verdwijnt de regel. */
    suspend fun setShowPrefs(showId: String, change: (ShowPrefs) -> ShowPrefs) = mutate {
        if (showId.isBlank()) return@mutate it
        val before = it.showPrefs[showId] ?: ShowPrefs()
        val next = change(before)
        when {
            next == before -> it
            next == ShowPrefs() -> it.copy(showPrefs = it.showPrefs - showId)
            else -> it.copy(showPrefs = it.showPrefs + (showId to next))
        }
    }

    /* ---- geschiedenis, luistertijd en bladwijzers ---- */

    /** Deze aflevering speelt nu: hij komt vooraan in de geschiedenis te staan. */
    suspend fun touchHistory(episode: SavedEpisode) = mutate { data ->
        val before = data.history.firstOrNull { it.episode.id == episode.id }
        val item = HistoryItem(episode, java.time.Instant.now().toString(), finished = false)
        // Een uitgeluisterde aflevering die opnieuw begint, is weer bezig.
        data.copy(
            history = (data.history.filterNot { it.episode.id == episode.id } + item).takeLast(HISTORY_KEPT),
            listened = if (before?.finished == true) data.listened - episode.id else data.listened
        )
    }

    suspend fun removeHistory(episodeId: String) = mutate { data ->
        if (data.history.none { it.episode.id == episodeId }) data
        else data.copy(history = data.history.filterNot { it.episode.id == episodeId }, progress = data.progress - episodeId)
    }

    /** Wist de geschiedenis en de luistertijd; waar je in een aflevering gebleven bent, blijft staan. */
    suspend fun clearHistory() = mutate {
        it.copy(history = it.history.filter { item -> item.episode.id in it.progress }, listening = emptyMap(), listeningDays = emptyMap())
    }

    /** Telt geluisterde tijd op, per show en per dag. */
    suspend fun addListening(showId: String, title: String, artworkUrl: String?, ms: Long) {
        if (ms <= 0L) return
        mutate(PLAYING_WRITE_MS) { data ->
            val today = LocalDate.now().toString()
            val key = showId.ifBlank { title }
            val before = data.listening[key] ?: ShowListening(title)
            val days = data.listeningDays + (today to (data.listeningDays[today] ?: 0L) + ms)
            data.copy(
                listening = data.listening + (key to before.copy(
                    title = title.ifBlank { before.title },
                    artworkUrl = artworkUrl ?: before.artworkUrl,
                    ms = before.ms + ms,
                    lastAt = today
                )),
                listeningDays = if (days.size > DAYS_KEPT) days.entries.sortedBy { it.key }.takeLast(DAYS_KEPT).associate { it.key to it.value } else days
            )
        }
    }

    /** Met de hand als beluisterd markeren, of juist niet meer. */
    suspend fun setListened(episodeIds: Collection<String>, on: Boolean) = mutate { data ->
        val ids = episodeIds.toSet()
        if (ids.isEmpty()) return@mutate data
        data.copy(
            listened = if (on) (data.listened - ids + ids).takeLast(LISTENED_KEPT) else data.listened - ids,
            progress = if (on) data.progress - ids else data.progress,
            history = data.history.map { if (it.episode.id in ids) it.copy(finished = on) else it }
        )
    }

    suspend fun addBookmark(episode: SavedEpisode, positionMs: Long) = mutate { data ->
        val id = episode.id + "@" + positionMs / 1000
        if (data.bookmarks.any { it.id == id }) data
        else data.copy(bookmarks = (data.bookmarks + Bookmark(id, episode, positionMs, java.time.Instant.now().toString())).takeLast(BOOKMARKS_KEPT))
    }

    suspend fun removeBookmark(id: String) = mutate { data ->
        if (data.bookmarks.none { it.id == id }) data else data.copy(bookmarks = data.bookmarks.filterNot { it.id == id })
    }

    fun notifiedAlerts(): Set<String> = data.notifiedAlerts.toSet()

    suspend fun markAlertsNotified(keys: Collection<String>) = mutate {
        it.copy(notifiedAlerts = (it.notifiedAlerts + keys).distinct().takeLast(NEW_IDS_KEPT))
    }

    /* ---- reservekopie ---- */

    /**
     * De hele bibliotheek als tekst, om zelf te bewaren of mee te nemen naar een
     * ander toestel. Downloads gaan niet mee: de bestanden zelf zitten er niet in.
     */
    suspend fun exportBackup(): String = withContext(Dispatchers.IO) {
        mutex.withLock {
            json.encodeToString(
                BackupFile.serializer(),
                BackupFile(
                    toadcastBackup = 1,
                    madeAt = java.time.Instant.now().toString(),
                    data = data.copy(downloads = emptyMap(), orphans = emptyMap(), evicted = emptyList())
                )
            )
        }
    }

    /**
     * Zet een reservekopie terug, in de plaats van wat er nu staat. Wat er op
     * dit toestel gedownload is, blijft. Geeft false terug als de tekst geen
     * reservekopie van Toadcast is; dan verandert er niets.
     */
    suspend fun importBackup(text: String): Boolean = withContext(Dispatchers.IO) {
        val backup = runCatching { json.decodeFromString(BackupFile.serializer(), text) }.getOrNull()
        if (backup == null) return@withContext false
        mutex.withLock {
            data = backup.data.copy(
                snapshots = emptyMap(), charts = emptyMap(), liveTips = emptyMap(), showTips = emptyMap(),
                downloads = data.downloads, orphans = data.orphans, evicted = data.evicted,
                downloadSettings = backup.data.downloadSettings
            )
            loaded = true
            publish()
            dirty = true
            scheduleWrite(0L)
        }
        true
    }

    /* ---- volgen ---- */

    fun isFollowed(id: String) = _follows.value.any { it.id == id }

    suspend fun toggleFollow(show: FollowedShow) = mutate {
        val existing = it.follows.any { followed -> followed.id == show.id }
        val follows = if (existing) it.follows.filterNot { f -> f.id == show.id } else it.follows + show.stamped()
        // Wie ontvolgt, wil ook niet dat de app nog afleveringen van die show binnenhaalt.
        it.copy(
            follows = follows,
            autoDownload = if (existing) it.autoDownload - show.id else it.autoDownload,
            feedChecks = if (existing) it.feedChecks - show.id else it.feedChecks
        )
    }

    private fun FollowedShow.stamped() = if (followedAt == null) copy(followedAt = LocalDate.now().toString()) else this

    /** Volgt een rij shows tegelijk; wat je al volgde blijft zoals het was. */
    suspend fun followAll(shows: List<FollowedShow>) = mutate { data ->
        val have = data.follows.map { it.id }.toSet()
        data.copy(follows = data.follows + shows.filter { it.id !in have }.distinctBy { it.id }.map { it.stamped() })
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
                      else data.follows.map {
                          if (it.id == oldId) show.copy(lastOpened = it.lastOpened, followedAt = it.followedAt, notifyNew = it.notifyNew)
                          else it
                      },
            autoDownload = data.autoDownload[oldId]?.let { count -> data.autoDownload - oldId + (show.id to count) }
                ?: data.autoDownload,
            downloads = data.downloads.mapValues { (_, record) -> record.copy(episode = record.episode.moved()) },
            feedChecks = data.feedChecks[oldId]?.let { check ->
                data.feedChecks - oldId + (show.id to check.copy(latest = check.latest.map { it.moved() }))
            } ?: data.feedChecks,
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

    /** Melding bij een nieuwe aflevering van deze show, aan of uit. */
    suspend fun setNotifyNew(showId: String, on: Boolean) = mutate { data ->
        // Wat er nu al staat, is geen nieuws meer: alleen wat hierna verschijnt krijgt een melding.
        val already = if (on) data.feedChecks[showId]?.latest.orEmpty().map { it.id } else emptyList()
        data.copy(
            follows = data.follows.map { if (it.id == showId) it.copy(notifyNew = on) else it },
            notifiedNew = (data.notifiedNew + already).distinct().takeLast(NEW_IDS_KEPT)
        )
    }

    /* ---- nieuwe afleveringen ---- */

    fun feedCheck(showId: String): FeedCheck? = data.feedChecks[showId]

    fun notifiedNew(): Set<String> = data.notifiedNew.toSet()

    suspend fun putFeedCheck(showId: String, check: FeedCheck) = mutate { data ->
        // Een show die intussen ontvolgd is, krijgt geen plek meer.
        if (data.follows.none { it.id == showId }) data
        else data.copy(feedChecks = data.feedChecks + (showId to check))
    }

    suspend fun hideNew(episodeId: String) = mutate {
        it.copy(hiddenNew = (it.hiddenNew - episodeId + episodeId).takeLast(NEW_IDS_KEPT))
    }

    suspend fun markNotified(episodeIds: Collection<String>) = mutate {
        it.copy(notifiedNew = (it.notifiedNew + episodeIds).distinct().takeLast(NEW_IDS_KEPT))
    }

    /** Zet afleveringen achter de wachtrij, zonder dubbele. Geeft terug hoeveel erbij kwamen. */
    suspend fun enqueueAll(episodes: List<SavedEpisode>): Int {
        var added = 0
        mutate { data ->
            val have = data.queue.map { it.id }.toSet()
            val fresh = episodes.filter { it.id !in have }.distinctBy { it.id }
            added = fresh.size
            data.copy(queue = data.queue + fresh)
        }
        return added
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

    /** Verplaatst een aflevering binnen de wachtrij naar plek [toIndex]. */
    suspend fun moveInQueue(episodeId: String, toIndex: Int) = mutate { data ->
        val from = data.queue.indexOfFirst { it.id == episodeId }
        if (from == -1) return@mutate data
        val to = toIndex.coerceIn(0, data.queue.lastIndex)
        if (from == to) return@mutate data
        val list = data.queue.toMutableList()
        list.add(to, list.removeAt(from))
        data.copy(queue = list)
    }

    suspend fun clearQueue() = mutate { if (it.queue.isEmpty()) it else it.copy(queue = emptyList()) }

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
    suspend fun rememberProgress(
        episodeId: String,
        positionMs: Long,
        durationMs: Long,
        /** De vaste tik tijdens het spelen: die mag wat langer op schijf wachten. */
        whilePlaying: Boolean = false
    ): Boolean {
        if (positionMs < 30_000L) return false
        val finished = durationMs > 0L && positionMs > durationMs - 60_000L
        mutate(if (whilePlaying && !finished) PLAYING_WRITE_MS else WRITE_MS) { data ->
            data.copy(
                progress = if (finished) data.progress - episodeId else data.progress + (episodeId to positionMs),
                listened = if (finished) (data.listened - episodeId + episodeId).takeLast(LISTENED_KEPT) else data.listened,
                history = if (finished) data.history.map { if (it.episode.id == episodeId) it.copy(finished = true) else it } else data.history,
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

    suspend fun addEvicted(ids: List<String>) = mutate {
        it.copy(evicted = (it.evicted - ids.toSet() + ids).takeLast(LISTENED_KEPT))
    }

    suspend fun addOrphan(fileName: String, episodeId: String) = mutate { it.copy(orphans = it.orphans + (fileName to episodeId)) }

    suspend fun removeOrphan(fileName: String) = mutate { it.copy(orphans = it.orphans - fileName) }

    suspend fun setDownloadSettings(settings: DownloadSettings) = mutate { it.copy(downloadSettings = settings) }

    /** [count] null of 0 zet automatisch downloaden voor deze show uit. */
    suspend fun setAutoDownload(showId: String, count: Int?) = mutate {
        it.copy(autoDownload = if (count == null || count <= 0) it.autoDownload - showId
                               else it.autoDownload + (showId to count))
    }

    /* ---- momentopnames en de laatst opgehaalde lijst: een bestand per lijst ---- */

    private fun listFile(key: String) =
        File(listsDir, key.replace(Regex("[^A-Za-z0-9]+"), "_").take(80) + "-" + Integer.toHexString(key.hashCode()) + ".json")

    /** Alleen onder [sideMutex]. */
    private fun readList(key: String): ListFile = lists.getOrPut(key) {
        runCatching {
            val source = listFile(key)
            if (source.exists()) json.decodeFromString(ListFile.serializer(), source.readText()) else ListFile()
        }.getOrElse { ListFile() }
    }

    private suspend fun list(key: String): ListFile = withContext(Dispatchers.IO) { sideMutex.withLock { readList(key) } }

    private suspend fun changeList(key: String, change: (ListFile) -> ListFile) = withContext(Dispatchers.IO + NonCancellable) {
        sideMutex.withLock {
            val before = readList(key)
            val after = change(before)
            if (after != before) {
                lists[key] = after
                writeAside(listFile(key), json.encodeToString(ListFile.serializer(), after))
            }
        }
        Unit
    }

    /**
     * De meest recente opname van een *eerdere* dag. Vandaag telt niet mee:
     * anders wist een verversing de basislijn en stond alles op nul.
     */
    suspend fun baseline(queryKey: String): Map<String, Int>? {
        val today = LocalDate.now().toString()
        return list(queryKey).snapshots
            .filter { it.date != today }
            .maxByOrNull { it.date }
            ?.ranks
    }

    private fun ListFile.recorded(ranks: Map<String, Int>): ListFile {
        val today = LocalDate.now().toString()
        return copy(
            snapshots = (snapshots.filterNot { it.date == today } + DaySnapshot(today, ranks))
                .sortedByDescending { it.date }
                .take(HISTORY_DAYS)
        )
    }

    suspend fun record(queryKey: String, ranks: Map<String, Int>) = changeList(queryKey) { it.recorded(ranks) }

    /* ---- lijsten offline ---- */

    suspend fun cachedChart(queryKey: String): CachedChart? = list(queryKey).chart

    suspend fun cacheChart(queryKey: String, chart: CachedChart) = changeList(queryKey) { it.copy(chart = chart) }

    /** De momentopname van vandaag en de lijst zelf, in één keer naar schijf. */
    suspend fun recordChart(queryKey: String, ranks: Map<String, Int>, chart: CachedChart) =
        changeList(queryKey) { it.recorded(ranks).copy(chart = chart) }

    /* ---- tips die de app zelf ophaalde ---- */

    /** Alleen onder [sideMutex]. */
    private fun readTips(): TipsFile = tips ?: runCatching {
        if (tipsFile.exists()) json.decodeFromString(TipsFile.serializer(), tipsFile.readText()) else TipsFile()
    }.getOrElse { TipsFile() }.also { tips = it }

    private suspend fun changeTips(change: (TipsFile) -> TipsFile) = withContext(Dispatchers.IO + NonCancellable) {
        sideMutex.withLock {
            val after = change(readTips())
            tips = after
            writeAside(tipsFile, json.encodeToString(TipsFile.serializer(), after))
        }
        Unit
    }

    suspend fun cachedTips(countryCode: String): CachedTips? =
        withContext(Dispatchers.IO) { sideMutex.withLock { readTips().liveTips[countryCode] } }

    suspend fun cacheTips(countryCode: String, tips: CachedTips) =
        changeTips { it.copy(liveTips = it.liveTips + (countryCode to tips)) }

    suspend fun cachedShowTips(showId: String): CachedTips? =
        withContext(Dispatchers.IO) { sideMutex.withLock { readTips().showTips[showId] } }

    /** Hooguit een paar honderd shows onthouden; de oudste vallen eraf. */
    suspend fun cacheShowTips(showId: String, tips: CachedTips) = changeTips { current ->
        val trimmed = if (current.showTips.size < SHOW_TIPS_KEPT) current.showTips
        else current.showTips.entries
            .sortedByDescending { it.value.fetchedAt }
            .take(SHOW_TIPS_KEPT / 2)
            .associate { it.key to it.value }
        current.copy(showTips = trimmed + (showId to tips))
    }

    /** Eerst ernaast, dan erover: een app die halverwege stopt, laat zo nooit een half bestand achter. */
    private fun writeAside(target: File, text: String) {
        runCatching {
            target.parentFile?.mkdirs()
            val temp = File(target.path + ".tmp")
            temp.writeText(text)
            if (!temp.renameTo(target)) {
                target.writeText(text)
                temp.delete()
            }
        }
    }

    private suspend fun mutate(
        writeWithinMs: Long = WRITE_MS,
        block: (StoreData) -> StoreData
    ) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val before = data
            data = block(data)
            // Niets veranderd (een show die je niet volgt als geopend markeren): niets te schrijven.
            if (data === before) return@withLock
            publish()
            dirty = true
            scheduleWrite(writeWithinMs)
        }
        Unit
    }

    /** Alleen onder [mutex]. Een eerder gepland moment blijft staan; een later schuift naar voren. */
    private fun scheduleWrite(withinMs: Long) {
        val at = System.nanoTime() / 1_000_000 + withinMs
        if (pendingWrite != null && writeAt <= at) return
        pendingWrite?.cancel()
        writeAt = at
        pendingWrite = writer.launch {
            delay(withinMs)
            flush()
        }
    }

    /** Zet wat nog niet op schijf staat er nu op. */
    suspend fun flush() = withContext(Dispatchers.IO + NonCancellable) {
        mutex.withLock {
            pendingWrite = null
            writeAt = Long.MAX_VALUE
            if (!dirty) return@withLock
            dirty = false
            writeAside(file, json.encodeToString(StoreData.serializer(), data))
        }
        Unit
    }

    /** Voor klussen buiten de app om: wat [block] veranderde, staat op schijf als hij klaar is. */
    suspend fun <T> flushAfter(block: suspend () -> T): T = try {
        block()
    } finally {
        flush()
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
        _evicted.value = data.evicted.toSet()
        _orphans.value = data.orphans
        _feedChecks.value = data.feedChecks
        _hiddenNew.value = data.hiddenNew.toSet()
        _prefs.value = data.prefs
        _playback.value = data.playback
        _showPrefs.value = data.showPrefs
        _history.value = data.history
        _bookmarks.value = data.bookmarks
        _listening.value = data.listening
        _listeningDays.value = data.listeningDays
    }

    private companion object {
        const val HISTORY_DAYS = 30

        /** Zo lang mag een wijziging op schijf wachten, zodat wat tegelijk gebeurt samen gaat. */
        const val WRITE_MS = 2_000L

        /** En de voortgang tijdens het spelen: die tikt elke tien seconden. */
        const val PLAYING_WRITE_MS = 30_000L

        /** Zoveel uitgeluisterde afleveringen onthouden we. */
        const val LISTENED_KEPT = 2000

        /** Zoveel podcasts onthouden we hun tips van; daarna vallen de oudste af. */
        const val SHOW_TIPS_KEPT = 300

        /** Zoveel weggeveegde en gemelde afleveringen onthouden we. */
        const val NEW_IDS_KEPT = 1000

        /** Zoveel afgespeelde afleveringen onthouden we. */
        const val HISTORY_KEPT = 300

        const val BOOKMARKS_KEPT = 500

        /** Zoveel dagen luistertijd blijven staan. */
        const val DAYS_KEPT = 400

        /** Een lijst waar zo lang niet naar gekeken is, ruimen we op. */
        const val LIST_KEPT_DAYS = 45L

        /** Zoveel makers onthouden we hun gezicht van. */
        const val MAKER_FACES_KEPT = 200
    }
}
