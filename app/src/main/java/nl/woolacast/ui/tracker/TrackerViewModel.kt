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
import nl.woolacast.data.local.FollowedShow
import nl.woolacast.data.local.LocalStore
import nl.woolacast.domain.Catalog
import nl.woolacast.domain.SourceId

data class TrackLine(
    val source: SourceId,
    /** Rang per dag, oplopend in de tijd. */
    val points: List<Pair<String, Int>>
) {
    val current: Int? get() = points.lastOrNull()?.second
    val best: Pair<String, Int>? get() = points.minByOrNull { it.second }
    val change: Int?
        get() = if (points.size < 2) null else points.first().second - points.last().second
    val days: Int get() = points.size
}

/** Hoogste notering ooit binnen wat we hebben vastgelegd. */
data class Peak(val rank: Int, val source: SourceId, val date: String)

data class TrackerUiState(
    val loading: Boolean = true,
    val title: String = "",
    val publisher: String = "",
    val genre: String? = null,
    val artworkUrl: String? = null,
    val countryLabel: String = "",
    val countryCode: String = "",
    val lines: List<TrackLine> = emptyList(),
    val positions: List<ShowPosition> = emptyList(),
    val peak: Peak? = null,
    val available: Boolean = false
)

class TrackerViewModel(
    private val dataset: ChartsDataset,
    private val store: LocalStore,
    val showId: String,
    private val countryCode: String,
    title: String,
    publisher: String,
    artworkUrl: String?,
    genre: String?,
    private val feedUrl: String?
) : ViewModel() {

    private val _state = MutableStateFlow(
        TrackerUiState(
            title = title,
            publisher = publisher,
            genre = genre,
            artworkUrl = artworkUrl,
            countryLabel = Catalog.country(countryCode).label,
            countryCode = countryCode
        )
    )
    val state: StateFlow<TrackerUiState> = _state.asStateFlow()

    val follows: StateFlow<List<FollowedShow>> = store.follows

    fun toggleFollow() {
        val current = _state.value
        viewModelScope.launch {
            store.toggleFollow(
                FollowedShow(
                    id = showId,
                    title = current.title,
                    publisher = current.publisher,
                    artworkUrl = current.artworkUrl,
                    feedUrl = feedUrl
                )
            )
        }
    }

    /** Korte samenvatting om te delen. */
    fun shareText(): String {
        val s = _state.value
        val lines = s.lines.joinToString("\n") { line ->
            "${line.source.label}: #${line.current ?: "–"}" +
                (line.change?.let { c -> if (c > 0) " (▲$c in ${line.days} dgn)" else if (c < 0) " (▼${-c} in ${line.days} dgn)" else " (stabiel)" } ?: "")
        }
        return "${s.title} in ${s.countryLabel}\n$lines\n— Woolacast"
    }

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

            val peak = lines.mapNotNull { line -> line.best?.let { (date, rank) -> Peak(rank, line.source, date) } }
                .minWithOrNull(compareBy<Peak> { it.rank }.thenByDescending { it.date })

            _state.value = _state.value.copy(
                loading = false,
                lines = lines,
                positions = tracking?.positions.orEmpty(),
                peak = peak,
                available = lines.isNotEmpty() || !tracking?.positions.isNullOrEmpty()
            )
        }
    }
}
