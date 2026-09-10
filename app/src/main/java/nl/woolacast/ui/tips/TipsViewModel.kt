package nl.woolacast.ui.tips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.woolacast.data.dataset.ChartsDataset
import nl.woolacast.data.dataset.MediaTip

data class TipsUiState(
    val loading: Boolean = true,
    val countryCode: String = "",
    val outlets: List<String> = emptyList(),
    val outlet: String? = null,
    val all: List<MediaTip> = emptyList(),
    val updated: String? = null
) {
    /** Wat er getoond wordt: alles, of alleen het gekozen medium. */
    val visible: List<MediaTip> get() = all.filter { outlet == null || it.outlet == outlet }
}

class TipsViewModel(private val dataset: ChartsDataset) : ViewModel() {

    private val _state = MutableStateFlow(TipsUiState())
    val state: StateFlow<TipsUiState> = _state.asStateFlow()

    fun load(countryCode: String) {
        if (_state.value.countryCode == countryCode && !_state.value.loading) return
        _state.value = TipsUiState(loading = true, countryCode = countryCode)
        viewModelScope.launch {
            val tips = dataset.tips(countryCode)
            _state.value = TipsUiState(
                loading = false,
                countryCode = countryCode,
                outlets = tips?.outlets.orEmpty(),
                all = tips?.entries.orEmpty(),
                updated = tips?.updated
            )
        }
    }

    fun setOutlet(outlet: String?) {
        _state.value = _state.value.copy(outlet = outlet)
    }
}
