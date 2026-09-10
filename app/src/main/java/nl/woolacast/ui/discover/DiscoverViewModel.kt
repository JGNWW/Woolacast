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

data class DiscoverUiState(
    val loading: Boolean = true,
    val country: String = "",
    val movers: List<DatasetMover> = emptyList(),
    val tips: List<MediaTip> = emptyList(),
    val tipCount: Int = 0,
    val outlets: List<String> = emptyList()
)

class DiscoverViewModel(private val dataset: ChartsDataset) : ViewModel() {

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
    }
}
