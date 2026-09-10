package nl.woolacast.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.woolacast.data.dataset.ChartsDataset
import nl.woolacast.data.dataset.DatasetMover

data class DiscoverUiState(
    val loading: Boolean = true,
    val country: String = "",
    val movers: List<DatasetMover> = emptyList()
)

class DiscoverViewModel(private val dataset: ChartsDataset) : ViewModel() {

    private val _state = MutableStateFlow(DiscoverUiState())
    val state: StateFlow<DiscoverUiState> = _state.asStateFlow()

    fun load(countryCode: String) {
        if (_state.value.country == countryCode && !_state.value.loading) return
        _state.value = DiscoverUiState(loading = true, country = countryCode)
        viewModelScope.launch {
            val movers = runCatching { dataset.movers(countryCode) }.getOrDefault(emptyList())
            _state.value = DiscoverUiState(loading = false, country = countryCode, movers = movers)
        }
    }
}
