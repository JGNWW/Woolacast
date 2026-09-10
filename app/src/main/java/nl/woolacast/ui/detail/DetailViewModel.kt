package nl.woolacast.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.woolacast.data.PodcastRepository
import nl.woolacast.data.dataset.ChartsDataset
import nl.woolacast.data.dataset.ShowPosition
import nl.woolacast.data.local.FollowedShow
import nl.woolacast.data.local.LocalStore
import nl.woolacast.domain.Episode
import nl.woolacast.domain.Podcast

data class DetailUiState(
    val loading: Boolean = true,
    val podcast: Podcast? = null,
    val episodes: List<Episode> = emptyList(),
    val error: String? = null,
    /** Waar deze show noteert; leeg als er nog niets is vastgelegd. */
    val positions: List<ShowPosition> = emptyList(),
    val countryCount: Int = 0
)

class DetailViewModel(
    private val showId: String,
    private val countryCode: String,
    private val feedUrl: String?,
    private val title: String?,
    private val repository: PodcastRepository,
    private val store: LocalStore,
    private val dataset: ChartsDataset? = null
) : ViewModel() {

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    val follows: StateFlow<List<FollowedShow>> = store.follows

    init {
        viewModelScope.launch {
            runCatching { repository.detail(showId, countryCode, feedUrl, title) }
                .onSuccess { detail ->
                    _state.value = DetailUiState(
                        loading = false,
                        podcast = detail.podcast,
                        episodes = detail.episodes
                    )
                }
                .onFailure { error ->
                    _state.value = DetailUiState(
                        loading = false,
                        error = error.message ?: "Kon deze podcast niet laden."
                    )
                }
        }

        viewModelScope.launch {
            val tracking = dataset?.tracking(showId) ?: return@launch
            _state.value = _state.value.copy(
                positions = tracking.positions,
                countryCount = tracking.positions.map { it.country }.distinct().size
            )
        }
    }

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
