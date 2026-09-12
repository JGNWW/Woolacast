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
import nl.woolacast.data.dataset.MediaTip
import nl.woolacast.data.dataset.ShowPosition
import nl.woolacast.data.local.FollowedShow
import nl.woolacast.data.local.CachedTips
import nl.woolacast.data.local.LocalStore
import nl.woolacast.data.local.SavedEpisode
import nl.woolacast.data.local.toSaved
import nl.woolacast.domain.Catalog
import nl.woolacast.domain.ChartLevel
import nl.woolacast.domain.ChartQuery
import nl.woolacast.domain.Episode
import nl.woolacast.domain.Podcast
import nl.woolacast.domain.SourceId
import nl.woolacast.data.reco.RecoRepository
import nl.woolacast.data.reco.Suggestion
import nl.woolacast.data.tips.LiveTipsReader
import nl.woolacast.ui.common.cadence
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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
    val countryCode: String = Catalog.defaultCountry.code,
    /** Media die deze podcast tipten. */
    val tips: List<MediaTip> = emptyList(),
    /** Podcasts die hierop lijken. */
    val similar: List<Suggestion> = emptyList()
)

class DetailViewModel(
    private val showId: String,
    private val countryCode: String,
    private val feedUrl: String?,
    private val title: String?,
    private val repository: PodcastRepository,
    private val store: LocalStore,
    private val dataset: ChartsDataset? = null,
    private val charts: ChartRepository? = null,
    private val liveTips: LiveTipsReader? = null,
    private val reco: RecoRepository? = null
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
            val known = dataset?.tipsFor(showId, countryCode).orEmpty()
            if (known.isNotEmpty()) _state.value = _state.value.copy(tips = known)
            addFoundTips(known)
        }

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

    /**
     * Kijkt of een medium deze podcast ergens heeft aangeraden. Dat is een
     * vraag aan Google Nieuws over deze ene titel, zonder tijdsgrens: een goede
     * tip veroudert niet. Een keer per dag per podcast; daarna komt het antwoord
     * uit het geheugen.
     */
    private suspend fun addFoundTips(known: List<MediaTip>) {
        val reader = liveTips ?: return
        // De sleutel draagt de versie van de zoekregels mee. Veranderen die
        // regels, dan is een bewaard antwoord van gisteren onbruikbaar; zo
        // vervalt het vanzelf in plaats van dat het een dag blijft hangen.
        val key = "v2:" + showId
        val cached = store.cachedShowTips(key)
        val found = if (cached != null && sameDay(cached.fetchedAt)) {
            cached.entries
        } else {
            val title = _state.value.podcast?.title ?: title ?: return
            val publisher = _state.value.podcast?.publisher.orEmpty()
            val catalogue = dataset?.feeds(countryCode) ?: return
            val fresh = runCatching {
                reader.forShow(catalogue, showId, title, publisher)
            }.getOrDefault(emptyList())
            store.cacheShowTips(key, CachedTips(Instant.now().toString(), fresh))
            fresh
        }
        if (found.isEmpty()) return
        val seen = known.map { it.outlet.lowercase() }.toMutableSet()
        val extra = found.filter { seen.add(it.outlet.lowercase()) }
        if (extra.isNotEmpty()) {
            _state.value = _state.value.copy(tips = _state.value.tips + extra)
        }
    }

    private fun sameDay(stamp: String): Boolean = runCatching {
        Instant.parse(stamp).atZone(ZoneId.systemDefault()).toLocalDate() == LocalDate.now()
    }.getOrDefault(false)

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
                    findSimilar(detail.podcast)
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        loading = false,
                        error = error.message ?: "Kon deze podcast niet laden."
                    )
                }
        }
    }

    /**
     * Podcasts die op deze lijken. Het rekenwerk gebeurt op het toestel: het
     * genre en de maker van deze show tegen de lijsten die de app toch al
     * heeft, plus een zoekopdracht op de naam van de maker.
     */
    private fun findSimilar(podcast: Podcast) {
        val repository = reco ?: return
        if (_state.value.similar.isNotEmpty()) return
        viewModelScope.launch {
            val found = runCatching {
                repository.similarTo(
                    showId = podcast.id,
                    title = podcast.title,
                    publisher = podcast.publisher,
                    genre = podcast.genre,
                    description = podcast.description,
                    countryCode = countryCode
                )
            }.getOrDefault(emptyList())
            if (found.isNotEmpty()) _state.value = _state.value.copy(similar = found)
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
