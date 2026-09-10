package nl.woolacast.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.woolacast.data.dataset.ChartsDataset
import nl.woolacast.data.dataset.DatasetMover
import nl.woolacast.data.local.LocalStore

/** Een show die je volgt en die deze week bewoog. */
data class ChartAlert(
    val showId: String,
    val title: String,
    val artworkUrl: String?,
    val feedUrl: String?,
    val rank: Int,
    val move: Int,
    val source: String,
    val country: String
)

class LibraryViewModel(
    private val store: LocalStore,
    private val dataset: ChartsDataset
) : ViewModel() {

    val follows = store.follows
    val queue = store.queue
    val saved = store.saved

    private val _alerts = MutableStateFlow<List<ChartAlert>>(emptyList())
    val alerts: StateFlow<List<ChartAlert>> = _alerts.asStateFlow()

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
                .sortedByDescending { it.move }
                .take(6)
        }
    }

    private fun DatasetMover.toAlert(countryCode: String) = ChartAlert(
        showId = showId.orEmpty(),
        title = title,
        artworkUrl = artworkUrl,
        feedUrl = feedUrl,
        rank = rank,
        move = move,
        source = if (source == "spotify") "Spotify" else "Apple",
        country = countryCode.uppercase()
    )

    fun removeFromQueue(episodeId: String) {
        val episode = store.queue.value.firstOrNull { it.id == episodeId } ?: return
        viewModelScope.launch { store.toggleQueue(episode) }
    }

    fun removeSaved(episodeId: String) {
        val episode = store.saved.value.firstOrNull { it.id == episodeId } ?: return
        viewModelScope.launch { store.toggleSaved(episode) }
    }
}
