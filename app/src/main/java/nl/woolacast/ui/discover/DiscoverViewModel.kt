package nl.woolacast.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.woolacast.data.dataset.ChartsDataset
import nl.woolacast.data.dataset.DatasetMover
import nl.woolacast.data.dataset.MediaTip
import nl.woolacast.data.reco.RecoRepository
import nl.woolacast.data.reco.Suggestion

data class DiscoverUiState(
    val loading: Boolean = true,
    val country: String = "",
    val movers: List<DatasetMover> = emptyList(),
    val tips: List<MediaTip> = emptyList(),
    val tipCount: Int = 0,
    val outlets: List<String> = emptyList(),
    /** Voorstellen op grond van je eigen bibliotheek. */
    val forYou: List<Suggestion> = emptyList()
)

class DiscoverViewModel(
    private val dataset: ChartsDataset,
    private val reco: RecoRepository? = null
) : ViewModel() {

    private val _state = MutableStateFlow(DiscoverUiState())
    val state: StateFlow<DiscoverUiState> = _state.asStateFlow()

    fun load(countryCode: String) {
        if (_state.value.country == countryCode && !_state.value.loading) return
        _state.value = DiscoverUiState(loading = true, country = countryCode)
        viewModelScope.launch {
            val movers = runCatching { dataset.movers(countryCode) }.getOrDefault(emptyList())
            val tips = runCatching { dataset.tips(countryCode) }.getOrNull()
            _state.value = DiscoverUiState(
                loading = false, country = countryCode, movers = movers,
                tips = tips?.entries.orEmpty().take(10),
                tipCount = tips?.count ?: 0,
                outlets = tips?.outlets.orEmpty()
            )
        }
        suggest(countryCode)
    }

    /**
     * "Misschien vind je dit leuk": wat je volgt en bewaard hebt, afgezet tegen
     * de lijsten die de app al heeft. Het profiel blijft op het toestel.
     */
    private fun suggest(countryCode: String) {
        val repository = reco ?: return
        viewModelScope.launch {
            val found = runCatching { repository.forLibrary(countryCode) }
                .getOrDefault(emptyList())
            if (found.isNotEmpty() && _state.value.country == countryCode) {
                _state.value = _state.value.copy(forYou = found)
            }
        }
    }
}
