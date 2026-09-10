package nl.woolacast.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.woolacast.data.ChartRepository
import nl.woolacast.data.PodcastRepository
import nl.woolacast.data.dataset.ChartsDataset
import nl.woolacast.data.dataset.ShowPosition
import nl.woolacast.data.local.FollowedShow
import nl.woolacast.data.local.LocalStore
import nl.woolacast.data.local.SavedEpisode
import nl.woolacast.data.local.toSaved
import nl.woolacast.domain.Catalog
import nl.woolacast.domain.ChartLevel
import nl.woolacast.domain.ChartQuery
import nl.woolacast.domain.Episode
import nl.woolacast.domain.Podcast
import nl.woolacast.domain.SourceId
import nl.woolacast.ui.common.cadence

data class DetailUiState(
    val loading: Boolean = true,
    val podcast: Podcast? = null,
    val episodes: List<Episode> = emptyList(),
    val error: String? = null,
    /** Waar deze show noteert; leeg als er nog niets is vastgelegd. */
    val positions: List<ShowPosition> = emptyList(),
    val countryCount: Int = 0,
    /** "wekelijks", afgeleid uit de tussenpozen van de afleveringen. */
    val cadence: String? = null,
    /** Aflevering-id → plek in Apple's afleveringenlijst van dit land. */
    val episodeRanks: Map<String, Int> = emptyMap(),
    val countryCode: String = Catalog.defaultCountry.code
)

class DetailViewModel(
    private val showId: String,
    private val countryCode: String,
    private val feedUrl: String?,
    private val title: String?,
    private val repository: PodcastRepository,
    private val store: LocalStore,
    private val dataset: ChartsDataset? = null,
    private val charts: ChartRepository? = null
) : ViewModel() {

    private val _state = MutableStateFlow(DetailUiState(countryCode = countryCode))
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    val follows: StateFlow<List<FollowedShow>> = store.follows
    val progress: StateFlow<Map<String, Long>> = store.progress
    val queued: StateFlow<List<SavedEpisode>> = store.queue
    val stored: StateFlow<List<SavedEpisode>> = store.saved

    /** Titels uit de afleveringenlijst van het land, om rijen een "#3 NL" te geven. */
    private var chartTitles: Map<String, Int> = emptyMap()

    fun toggleQueue(episode: Episode) = viewModelScope.launch {
        store.toggleQueue(episode.toSaved())
    }

    fun playNext(episode: Episode) = viewModelScope.launch {
        store.playNext(episode.toSaved())
    }

    fun toggleSaved(episode: Episode) = viewModelScope.launch {
        store.toggleSaved(episode.toSaved())
    }

    init {
        load()
        viewModelScope.launch { store.markOpened(showId) }

        viewModelScope.launch {
            val tracking = dataset?.tracking(showId) ?: return@launch
            _state.value = _state.value.copy(
                positions = tracking.positions,
                countryCount = tracking.positions.map { it.country }.distinct().size
            )
        }

        viewModelScope.launch {
            // De afleveringenlijst van het land is één verzoek; de rijen krijgen
            // er hun notering van. Mislukt het, dan ontbreekt alleen dat pilletje.
            val query = ChartQuery(
                source = SourceId.APPLE,
                country = Catalog.country(countryCode),
                category = Catalog.defaultCategory,
                level = ChartLevel.EPISODES
            )
            val chart = runCatching { charts?.chart(query) }.getOrNull() ?: return@launch
            chartTitles = chart.entries
                .filter { it.showId == null || it.showId == showId }
                .associate { normalise(it.title) to it.rank }
            reconcile()
        }
    }

    fun refresh() = load()

    private fun load() {
        _state.value = _state.value.copy(loading = _state.value.podcast == null, error = null)
        viewModelScope.launch {
            runCatching { repository.detail(showId, countryCode, feedUrl, title) }
                .onSuccess { detail ->
                    _state.value = _state.value.copy(
                        loading = false,
                        podcast = detail.podcast,
                        episodes = detail.episodes,
                        cadence = cadence(detail.episodes.map { it.releaseDate })
                    )
                    reconcile()
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        loading = false,
                        error = error.message ?: "Kon deze podcast niet laden."
                    )
                }
        }
    }

    private fun reconcile() {
        if (chartTitles.isEmpty()) return
        val ranks = _state.value.episodes.mapNotNull { episode ->
            chartTitles[normalise(episode.title)]?.let { episode.id to it }
        }.toMap()
        _state.value = _state.value.copy(episodeRanks = ranks)
    }

    private fun normalise(text: String) =
        text.lowercase().replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()

    fun toggleFollow() {
        val podcast = _state.value.podcast ?: return
        viewModelScope.launch {
            store.toggleFollow(
                FollowedShow(
                    id = podcast.id,
                    title = podcast.title,
                    publisher = podcast.publisher,
                    artworkUrl = podcast.artworkUrl,
                    feedUrl = podcast.feedUrl
                )
            )
        }
    }
}
