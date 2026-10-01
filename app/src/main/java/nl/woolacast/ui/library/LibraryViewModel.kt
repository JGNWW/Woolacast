package nl.woolacast.ui.library

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import nl.woolacast.data.inbox.NewEpisodes
import nl.woolacast.data.inbox.NewFilter
import nl.woolacast.data.inbox.NewRules
import nl.woolacast.data.local.SavedEpisode
import nl.woolacast.data.dataset.ChartsDataset
import nl.woolacast.data.dataset.DatasetMover
import nl.woolacast.data.local.FollowedMaker
import nl.woolacast.data.local.FollowedShow
import nl.woolacast.data.maker.MakerDirectory
import nl.woolacast.data.maker.MakerFace
import nl.woolacast.data.maker.MakerRepository
import nl.woolacast.domain.Maker
import nl.woolacast.data.local.LocalStore
import nl.woolacast.data.opml.Opml
import nl.woolacast.data.opml.ShowImporter
import nl.woolacast.ui.common.parseDate

/** Een show die je volgt en die deze week bewoog. */
data class ChartAlert(
    val showId: String,
    val title: String,
    val artworkUrl: String?,
    val feedUrl: String?,
    val rank: Int,
    val previousRank: Int?,
    val move: Int,
    val source: String,
    val country: String
) {
    val isNew: Boolean get() = previousRank == null
    val reachedTop: Boolean get() = rank == 1
}

/** Het venster om een feed toe te voegen. */
data class AddFeedState(
    val busy: Boolean = false,
    val error: String? = null,
    val added: FollowedShow? = null
)

/** Wat de feed van een gevolgde show zegt: wanneer de laatste kwam en hoeveel je nog niet zag. */
data class FeedStatus(val latestDate: String?, val newCount: Int)

enum class LibrarySort(val label: String) { RECENT("Nieuwste eerst"), NAME("Op naam") }

/** Hoe het met een gevolgde maker staat: hoeveel van zijn shows sinds gisteren een nieuwe aflevering hebben. */
data class MakerStatus(val fresh: Int, val total: Int, val complete: Boolean, val artworks: List<String?>)

/** Een nieuwe show van een maker die je volgt. */
data class MakerNewShow(
    val makerKey: String,
    val makerName: String,
    val showId: String,
    val title: String,
    val artworkUrl: String?,
    val feedUrl: String?,
    val episodes: Int?,
    /** Van een kanaal (Apple zegt dat hij nieuw is) of gevonden in de catalogus. */
    val viaChannel: Boolean,
    /** Zonder kanaal: de dag waarop de app hem vond. */
    val foundOn: String? = null
)

/** Een maker van shows die je al volgt, als voorstel om hem ook te volgen. */
data class MakerSuggestion(
    val key: String,
    val name: String,
    val followedShows: Int,
    val totalShows: Int?,
    val channelId: String?,
    val logoUrl: String?,
    val color: String?,
    /** De hoezen van de shows die je van deze maker volgt. */
    val artworks: List<String?>
)

class LibraryViewModel(
    private val store: LocalStore,
    private val dataset: ChartsDataset,
    private val makerRepository: MakerRepository,
    private val importer: ShowImporter? = null,
    private val newEpisodes: NewEpisodes? = null
) : ViewModel() {

    val follows = store.follows
    val makers = store.makers
    val makerFaces = store.makerFaces

    private val _makerStatus = MutableStateFlow<Map<String, MakerStatus>>(emptyMap())
    val makerStatus: StateFlow<Map<String, MakerStatus>> = _makerStatus.asStateFlow()

    private val _newShows = MutableStateFlow<List<MakerNewShow>>(emptyList())
    val newShows: StateFlow<List<MakerNewShow>> = _newShows.asStateFlow()

    private val _suggestions = MutableStateFlow<List<MakerSuggestion>>(emptyList())
    val suggestions: StateFlow<List<MakerSuggestion>> = _suggestions.asStateFlow()

    private var makersLoadedFor: Pair<String, Set<String>>? = null

    private val _makersRefreshing = MutableStateFlow(false)
    val makersRefreshing: StateFlow<Boolean> = _makersRefreshing.asStateFlow()
    val queue = store.queue
    val saved = store.saved

    private val _alerts = MutableStateFlow<List<ChartAlert>>(emptyList())
    val alerts: StateFlow<List<ChartAlert>> = _alerts.asStateFlow()

    /**
     * Per gevolgde show wat de feedronde zag: wanneer de laatste kwam en
     * hoeveel er nieuw is. Hetzelfde "nieuw" als de lijst Nieuw, zodat het
     * getal onder een show en de lijst nooit iets anders zeggen.
     */
    val feeds: StateFlow<Map<String, FeedStatus>> =
        combine(store.follows, store.feedChecks, store.listened, store.hiddenNew) { follows, checks, listened, hidden ->
            val today = LocalDate.now()
            follows.mapNotNull { show ->
                val latest = checks[show.id]?.latest ?: return@mapNotNull null
                if (latest.isEmpty()) return@mapNotNull null
                show.id to FeedStatus(
                    latestDate = latest.firstOrNull()?.releaseDate,
                    newCount = latest.count { NewRules.isNew(show, it, listened, hidden, today) }
                )
            }.toMap()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /** Alles wat nieuw is bij de shows die je volgt, de nieuwste eerst. */
    val newEpisodeList: StateFlow<List<SavedEpisode>> =
        combine(store.follows, store.feedChecks, store.listened, store.hiddenNew) { follows, checks, listened, hidden ->
            NewRules.inbox(follows, checks, listened, hidden, LocalDate.now())
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _newFilters = MutableStateFlow<Set<NewFilter>>(emptySet())
    val newFilters: StateFlow<Set<NewFilter>> = _newFilters.asStateFlow()

    /** Nieuw zoals de chips het willen. */
    val shownNew: StateFlow<List<SavedEpisode>> =
        combine(newEpisodeList, _newFilters, store.progress) { items, filters, progress ->
            NewRules.applyFilters(items, filters, progress, LocalDate.now())
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun toggleNewFilter(filter: NewFilter) {
        _newFilters.value = _newFilters.value.let { if (filter in it) it - filter else it + filter }
    }

    /** Naar links geveegd: weg uit Nieuw. Niet als beluisterd gemarkeerd; op de podcastpagina staat hij nog. */
    fun hideNew(episodeId: String) {
        viewModelScope.launch { store.hideNew(episodeId) }
    }

    /** Wat Nieuw nu toont achter de wachtrij, zonder dubbele en hooguit twintig. Meldt hoeveel erbij kwamen. */
    fun queueAllNew(onDone: (Int) -> Unit) {
        viewModelScope.launch {
            val queued = store.queue.value.map { it.id }.toSet()
            onDone(store.enqueueAll(NewRules.forQueue(shownNew.value, queued)))
        }
    }

    private val _sort = MutableStateFlow(LibrarySort.RECENT)
    val sort: StateFlow<LibrarySort> = _sort.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private var feedsLoadedFor: Set<String> = emptySet()

    fun setSort(sort: LibrarySort) {
        _sort.value = sort
    }

    val theme = store.theme

    fun setTheme(mode: String) {
        viewModelScope.launch { store.setTheme(mode) }
    }

    /**
     * Wat de app zelf toevoegt: van de shows die je volgt, wie er bewoog. Dat
     * kan alleen omdat de lijsten dagelijks worden vastgelegd — zonder gisteren
     * is er geen beweging.
     */
    fun loadAlerts(countryCode: String) {
        viewModelScope.launch {
            val followed = store.follows.value.associateBy { it.id }
            if (followed.isEmpty()) {
                _alerts.value = emptyList()
                return@launch
            }

            val movers = runCatching { dataset.movers(countryCode) }.getOrDefault(emptyList())
            _alerts.value = movers
                .filter { it.showId != null && followed.containsKey(it.showId) }
                .map { mover -> mover.toAlert(countryCode) }
                .sortedWith(compareByDescending<ChartAlert> { it.reachedTop }.thenByDescending { it.move })
                .distinctBy { it.showId }
                .take(6)
        }
    }

    /**
     * De feedronde voor alle gevolgde shows: dezelfde als die van de
     * achtergrondtaak, voorwaardelijk en hooguit vier tegelijk. Eén keer per set
     * gevolgde shows; wie wil verversen trekt de lijst naar beneden.
     */
    fun refreshFeeds(force: Boolean = false) {
        val followed = store.follows.value
        val ids = followed.map { it.id }.toSet()
        if (!force && ids == feedsLoadedFor) return
        feedsLoadedFor = ids
        val round = newEpisodes ?: return

        viewModelScope.launch {
            _refreshing.value = true
            try {
                round.refresh(followed)
            } finally {
                _refreshing.value = false
            }
        }
    }

    private fun DatasetMover.toAlert(countryCode: String) = ChartAlert(
        showId = showId.orEmpty(),
        title = title,
        artworkUrl = artworkUrl,
        feedUrl = feedUrl,
        rank = rank,
        previousRank = previousRank,
        move = move,
        source = if (source == "spotify") "Spotify" else "Apple",
        country = countryCode.uppercase()
    )

    /**
     * Per gevolgde maker: hoeveel shows sinds gisteren een nieuwe aflevering
     * hebben, en of er een nieuwe show bij kwam. Met kanaal zijn dat de shows
     * van Apple's kanaal, in één of twee aanroepen; zonder kanaal wat een
     * zoekopdracht op naam vindt. Geen enkele feed hoeft hiervoor open.
     */
    fun refreshMakers(countryCode: String, force: Boolean = false) {
        val followed = store.makers.value
        val signature = countryCode to followed.map { it.key }.toSet()
        if (!force && signature == makersLoadedFor) {
            loadSuggestions(countryCode)
            return
        }
        makersLoadedFor = signature

        viewModelScope.launch {
            _makersRefreshing.value = true
            val directory = makerRepository.directory(countryCode)
            val yesterday = LocalDate.now().minusDays(1)
            val recent = LocalDate.now().minusDays(NEW_SHOW_DAYS)
            val gate = Semaphore(3)
            val results = followed.map { followedMaker ->
                async {
                    gate.withPermit {
                        val channel = followedMaker.channelId?.let(directory::channel)
                            ?: directory.channelNamed(followedMaker.key)
                        val maker = Maker(followedMaker.key, channel?.name ?: followedMaker.name, channel)
                        val found = runCatching { makerRepository.shows(maker, countryCode) }.getOrNull()
                            ?: return@withPermit null
                        val shows = found.shows
                        val status = MakerStatus(
                            fresh = shows.count { show -> parseDate(show.latestRelease?.take(10))?.let { !it.isBefore(yesterday) } == true },
                            total = shows.size,
                            complete = found.complete,
                            artworks = MakerFace.of(shows)
                        )
                        val known = followedMaker.knownShowIds.toSet()
                        // De eerste keer is alles wat er staat al bekend: nieuw is wat daarna komt.
                        if (known.isEmpty()) {
                            store.markMakerSeen(followedMaker.key, shows.map { it.podcast.id })
                            return@withPermit Triple(followedMaker.key, status, emptyList<MakerNewShow>())
                        }
                        // Een show is pas nieuw als hij er bij het volgen nog niet was én echt jong is.
                        val fresh = if (channel != null) {
                            channel.newShows
                                .filter { it.id !in known && parseDate(it.createdDate)?.isBefore(recent) == false }
                                .map { MakerNewShow(maker.key, maker.name, it.id, it.title, it.artworkUrl, it.feedUrl, it.trackCount, viaChannel = true) }
                        } else {
                            shows
                                .filter { show ->
                                    show.podcast.id !in known &&
                                        (show.podcast.episodeCount ?: Int.MAX_VALUE) <= NEW_SHOW_MAX_EPISODES &&
                                        parseDate(show.latestRelease?.take(10))?.isBefore(recent) == false
                                }
                                .map {
                                    MakerNewShow(
                                        maker.key, maker.name, it.podcast.id, it.podcast.title,
                                        it.podcast.artworkUrl, it.podcast.feedUrl, it.podcast.episodeCount, viaChannel = false,
                                        foundOn = followedMaker.foundOn[it.podcast.id] ?: LocalDate.now().toString()
                                    )
                                }
                                .also { found -> if (found.isNotEmpty()) store.markMakerFound(followedMaker.key, found.map { it.showId }) }
                        }
                        Triple(followedMaker.key, status, fresh)
                    }
                }
            }.awaitAll().filterNotNull()
            _makerStatus.value = results.associate { it.first to it.second }
            _newShows.value = results.flatMap { it.third }
            _makersRefreshing.value = false
            loadSuggestions(countryCode, directory)
        }
    }

    /** Makers van shows die je volgt, maar die je zelf nog niet volgt. */
    private fun loadSuggestions(countryCode: String, known: MakerDirectory? = null) {
        viewModelScope.launch {
            val directory = known ?: makerRepository.directory(countryCode)
            val followedKeys = store.makers.value.map { it.key }.toSet()
            val makers = mutableMapOf<String, Maker>()
            _suggestions.value = store.follows.value
                .map { show -> directory.makerOf(show.id, show.publisher) to show }
                .filter { (maker, _) -> maker.key.isNotEmpty() && maker.key !in followedKeys }
                .groupBy { it.first.key }
                .map { (key, pairs) ->
                    val group = pairs.map { it.first }
                    val maker = group.firstOrNull { it.channel != null } ?: group.first()
                    makers[key] = maker
                    MakerSuggestion(
                        key = key,
                        name = maker.name,
                        followedShows = group.size,
                        totalShows = maker.channel?.showCount,
                        channelId = maker.channel?.id,
                        logoUrl = maker.channel?.logoUrl,
                        color = maker.channel?.color,
                        artworks = makerRepository.face(maker, directory) ?: pairs.map { it.second.artworkUrl }
                    )
                }
                .sortedWith(compareByDescending<MakerSuggestion> { it.followedShows }.thenBy { it.name.lowercase() })
                .take(5)
            // Van de shows die je volgt, kennen we vaak maar één hoes; de maker heeft er meer.
            // Zijn gezicht halen we één keer op, daarna onthoudt de opslag het.
            val gate = Semaphore(3)
            _suggestions.value
                .mapNotNull { makers[it.key] }
                .filter { makerRepository.face(it, directory) == null }
                .map { maker -> async { gate.withPermit { runCatching { makerRepository.shows(maker, countryCode) } } } }
                .awaitAll()
        }
    }

    fun followSuggestion(suggestion: MakerSuggestion, countryCode: String) {
        viewModelScope.launch {
            store.toggleMaker(
                FollowedMaker(
                    key = suggestion.key,
                    name = suggestion.name,
                    channelId = suggestion.channelId,
                    logoUrl = suggestion.logoUrl,
                    color = suggestion.color,
                    country = countryCode
                )
            )
        }
    }

    fun unfollowMaker(key: String) {
        viewModelScope.launch {
            store.makers.value.firstOrNull { it.key == key }?.let { store.toggleMaker(it) }
        }
    }

    /** Een nieuwe show die je opent, is daarna niet meer nieuw. */
    fun seen(show: MakerNewShow) {
        _newShows.value = _newShows.value.filterNot { it.showId == show.showId }
        viewModelScope.launch { store.markMakerSeen(show.makerKey, listOf(show.showId)) }
    }

    fun removeFromQueue(episodeId: String) {
        viewModelScope.launch { store.removeFromQueue(episodeId) }
    }

    fun removeSaved(episodeId: String) {
        viewModelScope.launch { store.removeSaved(episodeId) }
    }

    /* ---- je shows: een feed toevoegen, exporteren ---- */

    private val _addFeed = MutableStateFlow(AddFeedState())
    val addFeed: StateFlow<AddFeedState> = _addFeed.asStateFlow()

    fun resetAddFeed() {
        _addFeed.value = AddFeedState()
    }

    /** Volgt één feed; bij succes komt de show in [AddFeedState.added] zodat het scherm hem kan openen. */
    fun addFeed(url: String, countryCode: String, appScope: CoroutineScope) {
        val importer = importer ?: return
        _addFeed.value = AddFeedState(busy = true)
        viewModelScope.launch {
            runCatching { importer.addFeed(url) }
                .onSuccess { show ->
                    _addFeed.value = AddFeedState(added = show)
                    importer.startLinking(appScope, countryCode)
                }
                .onFailure { error -> _addFeed.value = AddFeedState(error = error.message ?: "Dat adres werkt niet.") }
        }
    }

    /** Wat mee kan in een export, en hoeveel shows niet (alleen op Spotify, zonder feed). */
    fun exportCounts(): Pair<Int, Int> = importer?.exportCounts() ?: (0 to 0)

    /** Schrijft de OPML naar het gekozen bestand en geeft een zin terug om te tonen. */
    fun export(uri: Uri, resolver: ContentResolver, countryCode: String, onDone: (String) -> Unit) {
        val importer = importer ?: return
        viewModelScope.launch {
            val (feeds, missing) = importer.exportable(countryCode)
            val text = Opml.write(feeds, OffsetDateTime.now().toString())
            val ok = runCatching {
                withContext(Dispatchers.IO) {
                    resolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) } ?: error("geen bestand")
                }
            }.isSuccess
            onDone(
                when {
                    !ok -> "Opslaan lukte niet. Kies een andere plek."
                    missing.isEmpty() -> "${feeds.size} shows opgeslagen."
                    else -> "${feeds.size} shows opgeslagen. ${missing.size} konden niet mee: geen open feed."
                }
            )
        }
    }

    fun unfollow(show: FollowedShow) {
        viewModelScope.launch { store.toggleFollow(show) }
    }

    private companion object {
        /** Zo jong moet een show zijn om als nieuwe podcast te gelden. */
        const val NEW_SHOW_DAYS = 14L

        /** Zonder kanaal: een show met meer afleveringen is niet nieuw, maar net gevonden. */
        const val NEW_SHOW_MAX_EPISODES = 3
    }
}
