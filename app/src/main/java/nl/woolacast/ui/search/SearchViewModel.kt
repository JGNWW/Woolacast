package nl.woolacast.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.woolacast.data.SearchRepository
import nl.woolacast.data.SearchResults

data class SearchUiState(
    val term: String = "",
    val loading: Boolean = false,
    val searched: Boolean = false,
    val results: SearchResults = SearchResults()
)

class SearchViewModel(
    private val repository: SearchRepository,
    private val countryCode: String
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var job: Job? = null

    fun onTermChanged(term: String) {
        _state.value = _state.value.copy(term = term)
        job?.cancel()

        if (term.trim().length < 2) {
            _state.value = _state.value.copy(loading = false, searched = false, results = SearchResults())
            return
        }

        job = viewModelScope.launch {
            delay(350)  // wacht tot het typen even stilvalt
            _state.value = _state.value.copy(loading = true)
            val results = repository.search(term, countryCode)
            _state.value = _state.value.copy(loading = false, searched = true, results = results)
        }
    }
}
