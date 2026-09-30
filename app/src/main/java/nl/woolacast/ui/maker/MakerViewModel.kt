package nl.woolacast.ui.maker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.woolacast.data.local.FollowedMaker
import nl.woolacast.data.local.LocalStore
import nl.woolacast.data.maker.MakerRepository
import nl.woolacast.data.maker.Placement
import nl.woolacast.domain.Catalog
import nl.woolacast.domain.Maker
import nl.woolacast.domain.MakerShow
import nl.woolacast.domain.Makers
import nl.woolacast.domain.SourceId

/** De drie manieren om de shows van een maker te ordenen. */
enum class MakerSort(val label: String) { POPULAR("Populair"), RECENT("Recent"), NAME("A–Z") }

data class MakerUiState(
    val loading: Boolean = true,
    val maker: Maker,
    val countryCode: String = "",
    val shows: List<MakerShow> = emptyList(),
    /** False als de shows uit een zoekopdracht op naam komen: dan is het wat de catalogus vond. */
    val complete: Boolean = false,
    val sort: MakerSort = MakerSort.POPULAR,
    val source: SourceId = SourceId.APPLE,
    val placement: Placement? = null,
    val placementLoading: Boolean = false,
    /** De show waar je vandaan kwam; die krijgt een merkteken. */
    val currentId: String? = null,
    val error: String? = null
)

/**
 * Alles van één maker. De maker komt uit het kanaal van de show waar je vandaan
 * kwam, of anders uit de naam; zijn shows uit het kanaal of een zoekopdracht;
 * zijn plekken uit de lijst die op Hitlijsten gekozen is.
 */
class MakerViewModel(
    private val repository: MakerRepository,
    private val store: LocalStore,
    private val publisher: String,
    private val countryCode: String,
    private val fromShowId: String?,
    initialSource: SourceId,
    /** De show waar je vandaan kwam; alleen vanaf een podcastpagina, niet vanuit een lijst. */
    highlightId: String? = null,
    initialSort: MakerSort = MakerSort.POPULAR
) : ViewModel() {

    private val _state = MutableStateFlow(
        MakerUiState(
            maker = Maker(Makers.keyOf(publisher), Makers.name(publisher)),
            countryCode = countryCode,
            source = initialSource,
            sort = initialSort,
            currentId = highlightId
        )
    )
    val state: StateFlow<MakerUiState> = _state.asStateFlow()

    val following: StateFlow<Boolean> = combine(store.makers, _state) { makers, state ->
        makers.any { it.key == state.maker.key }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)


    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                val maker = repository.identify(publisher, countryCode, fromShowId)
                _state.value = _state.value.copy(maker = maker)
                maker to repository.shows(maker, countryCode)
            }.onSuccess { (maker, found) ->
                _state.value = _state.value.copy(loading = false, maker = maker, shows = found.shows, complete = found.complete)
                // Wie deze maker volgt, heeft zijn shows nu gezien.
                store.markMakerSeen(maker.key, found.shows.map { it.podcast.id })
                loadPlacement()
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    loading = false,
                    error = error.message ?: "Kon de podcasts van deze maker niet laden."
                )
            }
        }
    }

    fun setSort(sort: MakerSort) {
        _state.value = _state.value.copy(sort = sort)
    }

    fun setSource(source: SourceId) {
        if (source == _state.value.source) return
        _state.value = _state.value.copy(source = source, placement = null)
        loadPlacement()
    }

    fun toggleFollow() {
        val state = _state.value
        val channel = state.maker.channel
        viewModelScope.launch {
            store.toggleMaker(
                FollowedMaker(
                    key = state.maker.key,
                    name = state.maker.name,
                    channelId = channel?.id,
                    logoUrl = channel?.logoUrl,
                    color = channel?.color,
                    knownShowIds = state.shows.map { it.podcast.id },
                    country = countryCode
                )
            )
        }
    }

    private fun loadPlacement() {
        val state = _state.value
        if (state.shows.isEmpty()) return
        _state.value = state.copy(placementLoading = true)
        viewModelScope.launch {
            val placement = repository.placement(state.source, Catalog.country(countryCode), state.maker, state.shows)
            // Intussen van bron gewisseld? Dan hoort dit antwoord niet meer bij het scherm.
            if (_state.value.source == state.source) {
                _state.value = _state.value.copy(placement = placement, placementLoading = false)
            }
        }
    }
}
