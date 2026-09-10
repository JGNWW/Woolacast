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
enum class Suggestion { ALL_CATEGORIES, SWITCH_TO_APPLE, SWITCH_TO_SHOWS }

data class Notice(val title: String, val message: String, val suggestion: Suggestion? = null)

data class ChartsUiState(
    val query: ChartQuery,
    val loading: Boolean = false,
    val chart: Chart? = null,
    val notice: Notice? = null,
    /** De aflevering waarvan de audio nu wordt opgezocht in de feed. */
    val resolvingId: String? = null,
    /** Korte melding onderin, bijvoorbeeld als een aflevering niet te vinden is. */
    val toast: String? = null
)

/** Een aflevering uit de lijst die speelbaar is gemaakt, met waar hij vandaan komt. */
data class PlayRequest(val episode: Episode, val label: String?)

class ChartsViewModel(
    private val repository: ChartRepository,
    private val podcasts: PodcastRepository,
    initialQuery: ChartQuery = ChartQuery(
        source = SourceId.APPLE,
        country = Catalog.defaultCountry,
        category = Catalog.defaultCategory,
        level = ChartLevel.SHOWS
    )
) : ViewModel() {

    private val _playRequests = MutableSharedFlow<PlayRequest>(extraBufferCapacity = 1)
    val playRequests: SharedFlow<PlayRequest> = _playRequests.asSharedFlow()

    private val _state = MutableStateFlow(ChartsUiState(query = initialQuery))
    val state: StateFlow<ChartsUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun setSource(source: SourceId) = update { it.copy(source = source) }

    fun setLevel(level: ChartLevel) = update { it.copy(level = level) }

    fun setFilters(country: Country, category: Category) =
        update { it.copy(country = country, category = category) }

    fun clearCategory() = update { it.copy(category = Catalog.defaultCategory) }

    /** Laat land of categorie ongemoeid als er null binnenkomt. */
    fun pick(country: Country?, category: Category?) =
        update { it.copy(country = country ?: it.country, category = category ?: it.category) }

    fun refresh() = load()

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
        load()
    }

    private fun load() {
        val query = _state.value.query
        val source = repository.source(query.source)

        // Wat de bron sowieso niet publiceert vragen we niet op.
        if (!source.capabilities.supports(query.level)) {
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
        _state.value = _state.value.copy(loading = true, notice = null)

        loadJob = viewModelScope.launch {
            runCatching { repository.chart(query) }
                .onSuccess { chart ->
                    _state.value = _state.value.copy(loading = false, chart = chart, notice = null)
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        loading = false,
                        chart = null,
                        notice = error.toNotice(query)
                    )
                }
        }
    }

    private fun Throwable.toNotice(query: ChartQuery): Notice = when (this) {
        is ChartUnavailable -> Notice(
            title = if (query.category.isAll) "Nog geen lijst" else "Niet per categorie",
            message = reason,
            // De bron weet zelf het beste wat de uitweg is.
            suggestion = when (wayOut) {
                WayOut.APPLE -> Suggestion.SWITCH_TO_APPLE
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
        Suggestion.SWITCH_TO_SHOWS -> setLevel(ChartLevel.SHOWS)
    }
}
