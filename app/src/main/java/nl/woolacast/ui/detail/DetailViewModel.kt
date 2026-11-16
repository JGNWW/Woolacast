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
import nl.woolacast.data.download.DownloadProgress
import nl.woolacast.data.download.Downloads
import nl.woolacast.data.local.DownloadRecord
import nl.woolacast.data.local.DownloadSettings
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
    val similar: List<Suggestion> = emptyList(),
    /** De pagina staat er al uit het geheugen; de maker wordt nog gevraagd of er iets nieuws is. */
    val refreshing: Boolean = false
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
    private val reco: RecoRepository? = null,
    private val downloads: Downloads? = null,
    /** Na het aanzetten van automatisch downloaden: meteen kijken wat er klaar moet staan. */
    private val onAutoDownloadChanged: (showId: String) -> Unit = {}
) : ViewModel() {

    private val _state = MutableStateFlow(DetailUiState(countryCode = countryCode))
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    val follows: StateFlow<List<FollowedShow>> = store.follows
    val progress: StateFlow<Map<String, Long>> = store.progress
    val queued: StateFlow<List<SavedEpisode>> = store.queue
    val stored: StateFlow<List<SavedEpisode>> = store.saved
    val downloadRecords: StateFlow<Map<String, DownloadRecord>> = store.downloads
    val downloadProgress: StateFlow<Map<String, DownloadProgress>> =
        downloads?.progress ?: MutableStateFlow(emptyMap<String, DownloadProgress>()).asStateFlow()
    val autoDownload: StateFlow<Map<String, Int>> = store.autoDownload
    val downloadSettings: StateFlow<DownloadSettings> = store.downloadSettings
    val listened: StateFlow<Set<String>> = store.listened
    val showPrefs: StateFlow<Map<String, nl.woolacast.data.local.ShowPrefs>> = store.showPrefs
    val playback: StateFlow<nl.woolacast.data.local.PlaybackSettings> = store.playback

    /** Met de hand als beluisterd markeren, of juist niet meer. */
    fun setListened(episodeIds: Collection<String>, on: Boolean) = viewModelScope.launch { store.setListened(episodeIds, on) }

    /** Van oud naar nieuw, voor een verhaal dat bij deel 1 begint; de show onthoudt het. */
    fun setOldestFirst(on: Boolean) = viewModelScope.launch { store.setShowPrefs(showId) { it.copy(oldestFirst = on) } }

    /** Een eigen snelheid voor deze show; null volgt weer de algemene. */
    fun setShowSpeed(speed: Float?) = viewModelScope.launch { store.setShowPrefs(showId) { it.copy(speed = speed) } }

    /**
     * Nieuwe afleveringen vanzelf achter de wachtrij. Aanzetten volgt de show
     * ook: de app kijkt alleen bij shows die je volgt of er iets nieuws is.
     */
    fun setAutoQueue(on: Boolean) {
        val podcast = _state.value.podcast ?: return
        viewModelScope.launch {
            if (on && store.follows.value.none { it.id == podcast.id }) toggleFollowNow(podcast)
            store.setShowPrefs(showId) { it.copy(autoQueue = on) }
        }
    }

    fun waitingForWifi(): Boolean = downloads?.waitingForWifi() ?: false

    fun download(episode: Episode) = viewModelScope.launch { downloads?.enqueue(episode) }
    fun removeDownload(episodeId: String) = viewModelScope.launch { downloads?.remove(episodeId) }
    fun retryDownload(episodeId: String) = viewModelScope.launch { downloads?.retry(episodeId) }

    /**
     * Automatisch downloaden voor deze show. Aanzetten volgt de show ook: de
     * app kijkt alleen bij shows die je volgt of er iets nieuws is.
     */
    fun setAutoDownload(count: Int?) {
        val podcast = _state.value.podcast ?: return
        viewModelScope.launch {
            if (count != null && count > 0 && store.follows.value.none { it.id == podcast.id }) toggleFollowNow(podcast)
            store.setAutoDownload(podcast.id, count)
            if (count != null && count > 0) onAutoDownloadChanged(podcast.id)
        }
    }

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

    private fun show(detail: nl.woolacast.data.PodcastDetail, refreshing: Boolean) {
        // Het genre staat niet in de feed; wat we eerder wisten, blijft staan.
        val genre = detail.podcast.genre ?: _state.value.podcast?.genre
        _state.value = _state.value.copy(
            loading = false,
            refreshing = refreshing,
            podcast = detail.podcast.copy(genre = genre),
            episodes = detail.episodes,
            cadence = cadence(detail.episodes.map { it.releaseDate })
        )
        reconcile()
    }

    private fun load() {
        _state.value = _state.value.copy(loading = _state.value.podcast == null, error = null)
        viewModelScope.launch {
            // Eerst wat er bewaard is: de pagina staat er dan meteen, en de
            // maker hoeft alleen nog te zeggen of er iets veranderd is.
            if (_state.value.podcast == null) {
                runCatching { repository.cached(showId) }.getOrNull()?.let { kept ->
                    show(kept, refreshing = true)
                    findSimilar(kept.podcast)
                }
            } else {
                _state.value = _state.value.copy(refreshing = true)
            }
            runCatching { repository.detail(showId, countryCode, feedUrl, title) }
                .onSuccess { detail ->
                    show(detail, refreshing = false)
                    findSimilar(detail.podcast)
                }
                .onFailure { error ->
                    // Met een bewaarde pagina in beeld is een mislukte verversing geen fout om te tonen.
                    _state.value = if (_state.value.podcast != null) _state.value.copy(loading = false, refreshing = false)
                    else _state.value.copy(
                        loading = false,
                        refreshing = false,
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

    /** Melding bij een nieuwe aflevering van deze show, aan of uit. */
    fun setNotifyNew(on: Boolean) {
        val podcast = _state.value.podcast ?: return
        viewModelScope.launch { store.setNotifyNew(podcast.id, on) }
    }

    fun toggleFollow() {
        val podcast = _state.value.podcast ?: return
        viewModelScope.launch {
            toggleFollowNow(podcast)
        }
    }

    private suspend fun toggleFollowNow(podcast: Podcast) {
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
