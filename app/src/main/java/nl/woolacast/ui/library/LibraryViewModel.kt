package nl.woolacast.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import nl.woolacast.data.PodcastRepository
import nl.woolacast.data.dataset.ChartsDataset
import nl.woolacast.data.dataset.DatasetMover
import nl.woolacast.data.local.FollowedShow
import nl.woolacast.data.local.LocalStore
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

/** Wat de feed van een gevolgde show zegt: wanneer de laatste kwam en hoeveel je nog niet zag. */
data class FeedStatus(val latestDate: String?, val newCount: Int)

enum class LibrarySort(val label: String) { RECENT("Nieuwste eerst"), NAME("Op naam") }

class LibraryViewModel(
    private val store: LocalStore,
    private val dataset: ChartsDataset,
    private val podcasts: PodcastRepository
) : ViewModel() {

    val follows = store.follows
    val queue = store.queue
    val saved = store.saved

    private val _alerts = MutableStateFlow<List<ChartAlert>>(emptyList())
    val alerts: StateFlow<List<ChartAlert>> = _alerts.asStateFlow()

    private val _feeds = MutableStateFlow<Map<String, FeedStatus>>(emptyMap())
    val feeds: StateFlow<Map<String, FeedStatus>> = _feeds.asStateFlow()

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
     * Leest de kop van elke gevolgde feed, hooguit vier tegelijk, om "2 nieuw"
     * en "bijgewerkt di" onder de tegels te zetten. Eén keer per set gevolgde
     * shows; wie wil verversen trekt de lijst naar beneden.
     */
    fun refreshFeeds(force: Boolean = false) {
        val followed = store.follows.value
        val ids = followed.map { it.id }.toSet()
        if (!force && ids == feedsLoadedFor) return
        feedsLoadedFor = ids

        viewModelScope.launch {
            _refreshing.value = true
            val gate = Semaphore(4)
            val statuses = followed.map { show ->
                async {
                    val feedUrl = show.feedUrl ?: return@async null
                    gate.withPermit {
                        val episodes = podcasts.latestEpisodes(feedUrl, limit = 12)
                        if (episodes.isEmpty()) return@withPermit null
                        show.id to FeedStatus(
                            latestDate = episodes.firstOrNull()?.releaseDate,
                            newCount = episodes.count { isNewFor(show, it.releaseDate) }
                        )
                    }
                }
            }.awaitAll().filterNotNull().toMap()
            _feeds.value = statuses
            _refreshing.value = false
        }
    }

    /** Nieuw is: verschenen na je laatste bezoek, of — nooit geopend — in de laatste week. */
    private fun isNewFor(show: FollowedShow, releaseDate: String?): Boolean {
        val date = parseDate(releaseDate) ?: return false
        val since = parseDate(show.lastOpened) ?: LocalDate.now().minusDays(7)
        return date.isAfter(since)
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

    fun removeFromQueue(episodeId: String) {
        viewModelScope.launch { store.removeFromQueue(episodeId) }
    }

    fun removeSaved(episodeId: String) {
        viewModelScope.launch { store.removeSaved(episodeId) }
    }

    fun unfollow(show: FollowedShow) {
        viewModelScope.launch { store.toggleFollow(show) }
    }
}
