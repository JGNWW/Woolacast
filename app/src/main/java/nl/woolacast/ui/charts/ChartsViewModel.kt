package nl.woolacast.ui.charts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.woolacast.data.ChartRepository
import nl.woolacast.data.PodcastRepository
import nl.woolacast.data.maker.MakerRank
import nl.woolacast.data.maker.MakerRepository
import nl.woolacast.domain.Catalog
import nl.woolacast.domain.Category
import nl.woolacast.domain.Chart
import nl.woolacast.domain.ChartEntry
import nl.woolacast.domain.ChartLevel
import nl.woolacast.domain.Episode
import nl.woolacast.domain.ChartQuery
import nl.woolacast.domain.ChartUnavailable
import nl.woolacast.domain.Country
import nl.woolacast.domain.SourceId
import nl.woolacast.domain.WayOut

/** Wat de gebruiker kan doen als een lijst niets oplevert. */
enum class Suggestion { ALL_CATEGORIES, SWITCH_TO_APPLE, SWITCH_TO_SPOTIFY, SWITCH_TO_SHOWS }

data class Notice(val title: String, val message: String, val suggestion: Suggestion? = null)

data class ChartsUiState(
    val query: ChartQuery,
    val loading: Boolean = false,
    val chart: Chart? = null,
    val notice: Notice? = null,
    /** De aflevering waarvan de audio nu wordt opgezocht in de feed. */
    val resolvingId: String? = null,
    /** Korte melding onderin, bijvoorbeeld als een aflevering niet te vinden is. */
    val toast: String? = null,
    /** Wanneer deze lijst is opgehaald, als "17:53". */
    val loadedAt: String? = null,
    /** De lijst per maker in plaats van per show; alleen op het tabblad Podcasts. */
    val byMaker: Boolean = false,
    /** De makersranglijst van de huidige lijst; null zolang hij nog geteld wordt. */
    val makers: List<MakerRank>? = null,
    /** Er staat al een bewaarde lijst; de verse komt er stil overheen, zonder draaiend wieltje. */
    val silent: Boolean = false
)

/** Een aflevering uit de lijst die speelbaar is gemaakt, met waar hij vandaan komt. */
data class PlayRequest(val episode: Episode, val label: String?)

class ChartsViewModel(
    private val repository: ChartRepository,
    private val podcasts: PodcastRepository,
    private val makers: MakerRepository? = null,
    initialQuery: ChartQuery = ChartQuery(
        source = SourceId.APPLE,
        country = Catalog.defaultCountry,
        category = Catalog.defaultCategory,
        level = ChartLevel.SHOWS
    ),
    /** Met een opslag onthoudt dit scherm je keuzes: bron, land, categorie en tabblad. */
    private val store: nl.woolacast.data.local.LocalStore? = null
) : ViewModel() {

    private val _playRequests = MutableSharedFlow<PlayRequest>(extraBufferCapacity = 1)
    val playRequests: SharedFlow<PlayRequest> = _playRequests.asSharedFlow()

    private val _state = MutableStateFlow(ChartsUiState(query = initialQuery))
    val state: StateFlow<ChartsUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var prefetchJob: Job? = null

    init {
        if (store == null) load() else viewModelScope.launch {
            // De bibliotheek is klein en staat er in een oogwenk; daarna begint de app waar je was.
            store.ensureLoaded()
            val prefs = store.prefs.value
            val source = runCatching { SourceId.valueOf(prefs.chartSource) }.getOrDefault(initialQuery.source)
            val level = runCatching { ChartLevel.valueOf(prefs.chartLevel) }.getOrDefault(initialQuery.level)
            _state.value = _state.value.copy(
                query = initialQuery.copy(
                    source = source,
                    country = if (prefs.chartCountry.isBlank()) initialQuery.country else Catalog.country(prefs.chartCountry),
                    category = if (level.isRanking) Catalog.category(prefs.chartGenre.takeIf { it != 0 }) else Catalog.defaultCategory,
                    level = level
                ),
                byMaker = prefs.chartByMaker
            )
            load()
        }
    }

    private fun remember() {
        val store = store ?: return
        val state = _state.value
        viewModelScope.launch {
            store.setPrefs {
                it.copy(
                    chartSource = state.query.source.name,
                    chartCountry = state.query.country.code,
                    chartGenre = state.query.category.appleGenreId ?: 0,
                    chartLevel = state.query.level.name,
                    chartByMaker = state.byMaker
                )
            }
        }
    }

    fun setSource(source: SourceId) = update { it.copy(source = source) }

    /** Trending en Nieuw gaan over de hele lijst; een categorie valt dan weg. */
    fun setLevel(level: ChartLevel) = update {
        it.copy(level = level, category = if (level.isRanking) it.category else Catalog.defaultCategory)
    }

    fun setFilters(country: Country, category: Category) =
        update { it.copy(country = country, category = category) }

    fun clearCategory() = update { it.copy(category = Catalog.defaultCategory) }

    /** Laat land of categorie ongemoeid als er null binnenkomt. */
    fun pick(country: Country?, category: Category?) =
        update { it.copy(country = country ?: it.country, category = category ?: it.category) }

    /** Naar beneden getrokken: echt opnieuw bij de bron vragen. */
    fun refresh() = load(force = true)

    /** Wisselt tussen de lijst per show en per maker; dezelfde lijst, anders geteld. */
    fun setByMaker(byMaker: Boolean) {
        _state.value = _state.value.copy(byMaker = byMaker)
        remember()
    }

    fun dismissToast() {
        _state.value = _state.value.copy(toast = null)
    }

    /**
     * Speelt een aflevering rechtstreeks uit de lijst. Hitlijsten geven geen
     * audio; die staat in de feed van de show, dus daar zoeken we hem op.
     */
    fun play(entry: ChartEntry) {
        val query = _state.value.query
        if (_state.value.resolvingId != null) return
        _state.value = _state.value.copy(resolvingId = entry.id)
        viewModelScope.launch {
            val showId = entry.showId
            val episode = if (showId != null) {
                podcasts.resolveEpisode(
                    showId = showId,
                    countryCode = query.country.code,
                    feedUrl = entry.feedUrl,
                    showTitle = entry.publisher,
                    episodeTitle = entry.title
                )
            } else null

            if (episode == null) {
                _state.value = _state.value.copy(
                    resolvingId = null,
                    toast = "Niet gevonden in de feed van ${entry.publisher}. Open de podcast om hem daar te kiezen."
                )
                return@launch
            }

            val where = if (query.category.isAll) "Top afleveringen" else query.category.label
            _playRequests.tryEmit(
                PlayRequest(episode, "#${entry.rank} in $where ${query.country.code.uppercase()}")
            )
            _state.value = _state.value.copy(resolvingId = null)
        }
    }

    private fun update(transform: (ChartQuery) -> ChartQuery) {
        val next = transform(_state.value.query)
        if (next == _state.value.query) return
        _state.value = _state.value.copy(query = next)
        remember()
        load()
    }

    private fun load(force: Boolean = false) {
        val query = _state.value.query
        val source = repository.source(query.source)

        // Wat de bron sowieso niet publiceert vragen we niet op. Trending en
        // Nieuw leidt de repository zelf af als de bron ze niet heeft.
        if (query.level.isRanking && !source.capabilities.supports(query.level)) {
            _state.value = _state.value.copy(
                loading = false,
                chart = null,
                notice = Notice(
                    title = "Geen ${query.level.label.lowercase()}lijst",
                    message = "${source.id.label} publiceert alleen een lijst met " +
                        "${ChartLevel.SHOWS.label.lowercase()}, niet met losse afleveringen.",
                    suggestion = Suggestion.SWITCH_TO_SHOWS
                )
            )
            return
        }

        loadJob?.cancel()
        prefetchJob?.cancel()
        _state.value = _state.value.copy(loading = true, notice = null)

        loadJob = viewModelScope.launch {
            // Eerst wat er bewaard is: dan staat er meteen een lijst, en komt de verse eroverheen.
            if (!force && _state.value.chart?.query != query) {
                val kept = runCatching { repository.cached(query) }.getOrNull()
                if (kept != null && _state.value.query == query) {
                    _state.value = _state.value.copy(chart = kept.copy(cachedAt = null), makers = null, silent = true)
                }
            }
            runCatching { repository.chart(query, force) }
                .onSuccess { chart ->
                    _state.value = _state.value.copy(
                        loading = false, chart = chart, notice = null, makers = null, silent = false,
                        loadedAt = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                    )
                    if (query.level == ChartLevel.EPISODES && chart.cachedAt == null) prefetch(chart, query)
                    // Ook als je hem niet bekijkt: zo is er morgen een makersranglijst om mee te vergelijken.
                    if (query.level == ChartLevel.SHOWS && makers != null) {
                        val rows = runCatching { makers.rankMakers(chart) }.getOrDefault(emptyList())
                        if (_state.value.query == query) _state.value = _state.value.copy(makers = rows)
                    }
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        loading = false,
                        chart = null,
                        silent = false,
                        notice = error.toNotice(query)
                    )
                }
        }
    }

    /**
     * De feeds van de bovenste afleveringen alvast binnenhalen: een tik op
     * afspelen hoeft dan niet eerst op de maker te wachten. Eén tegelijk, en
     * een feed die net gelezen is, wordt niet opnieuw gevraagd.
     */
    private fun prefetch(chart: Chart, query: ChartQuery) {
        prefetchJob = viewModelScope.launch {
            chart.entries.asSequence().filter { it.showId != null }.distinctBy { it.showId }.take(PREFETCH).forEach { entry ->
                podcasts.prefetch(entry.showId.orEmpty(), query.country.code, entry.feedUrl, entry.publisher)
            }
        }
    }

    private companion object {
        const val PREFETCH = 5
    }

    private fun Throwable.toNotice(query: ChartQuery): Notice = when (this) {
        is ChartUnavailable -> Notice(
            title = when {
                query.level == ChartLevel.TRENDING -> "Nog geen trending"
                query.level == ChartLevel.NEW -> "Nog geen nieuwe binnenkomers"
                query.category.isAll -> "Nog geen lijst"
                else -> "Niet per categorie"
            },
            message = reason,
            // De bron weet zelf het beste wat de uitweg is.
            suggestion = when (wayOut) {
                WayOut.APPLE -> Suggestion.SWITCH_TO_APPLE
                WayOut.SPOTIFY -> Suggestion.SWITCH_TO_SPOTIFY
                WayOut.ALL_CATEGORIES -> Suggestion.ALL_CATEGORIES
                WayOut.SHOWS -> Suggestion.SWITCH_TO_SHOWS
                null -> when {
                    !query.category.isAll -> Suggestion.ALL_CATEGORIES
                    query.source != SourceId.APPLE -> Suggestion.SWITCH_TO_APPLE
                    else -> null
                }
            }
        )

        else -> Notice(
            title = "Lijst niet opgehaald",
            message = message ?: "Er ging iets mis bij het ophalen. Controleer je verbinding."
        )
    }

    fun applySuggestion(suggestion: Suggestion) = when (suggestion) {
        Suggestion.ALL_CATEGORIES -> clearCategory()
        Suggestion.SWITCH_TO_APPLE -> setSource(SourceId.APPLE)
        Suggestion.SWITCH_TO_SPOTIFY -> setSource(SourceId.SPOTIFY)
        Suggestion.SWITCH_TO_SHOWS -> setLevel(ChartLevel.SHOWS)
    }
}
