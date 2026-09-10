package nl.woolacast.ui.tracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.woolacast.data.dataset.ChartsDataset
import nl.woolacast.data.dataset.ShowPosition
import nl.woolacast.domain.Catalog
import nl.woolacast.domain.SourceId

data class TrackLine(
    val source: SourceId,
    /** Rang per dag, oplopend in de tijd. */
    val points: List<Pair<String, Int>>
) {
    val current: Int? get() = points.lastOrNull()?.second
    val best: Int? get() = points.minOfOrNull { it.second }
    val change: Int?
        get() = if (points.size < 2) null else points.first().second - points.last().second
}

data class TrackerUiState(
    val loading: Boolean = true,
    val title: String = "",
    val publisher: String = "",
    val artworkUrl: String? = null,
    val countryLabel: String = "",
    val lines: List<TrackLine> = emptyList(),
    val positions: List<ShowPosition> = emptyList(),
    val available: Boolean = false
)

class TrackerViewModel(
    private val dataset: ChartsDataset,
    private val showId: String,
    private val countryCode: String,
    title: String,
    publisher: String,
    artworkUrl: String?
) : ViewModel() {

    private val _state = MutableStateFlow(
        TrackerUiState(
            title = title,
            publisher = publisher,
            artworkUrl = artworkUrl,
            countryLabel = Catalog.country(countryCode).label
        )
    )
    val state: StateFlow<TrackerUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val tracking = dataset.tracking(showId)

            val appleLine = async { dataset.rankHistory(SourceId.APPLE, countryCode, showId) }
            val spotifyLine = async {
                tracking?.spotifyUri
                    ?.let { dataset.rankHistory(SourceId.SPOTIFY, countryCode, it) }
                    .orEmpty()
            }

            val lines = listOfNotNull(
                appleLine.await().takeIf { it.isNotEmpty() }?.let { TrackLine(SourceId.APPLE, it) },
                spotifyLine.await().takeIf { it.isNotEmpty() }?.let { TrackLine(SourceId.SPOTIFY, it) }
            )

            _state.value = _state.value.copy(
                loading = false,
                lines = lines,
                positions = tracking?.positions.orEmpty(),
                available = lines.isNotEmpty() || !tracking?.positions.isNullOrEmpty()
            )
        }
    }
}
