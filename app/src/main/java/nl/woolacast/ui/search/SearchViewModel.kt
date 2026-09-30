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
import nl.woolacast.data.maker.MakerRepository
import nl.woolacast.domain.Maker
import nl.woolacast.domain.Makers

/** Een maker in de zoekresultaten, met hoeveel van zijn podcasts erbij zitten. */
data class MakerHit(val maker: Maker, val found: Int, val firstShowId: String)

data class SearchUiState(
    val term: String = "",
    val loading: Boolean = false,
    val searched: Boolean = false,
    val results: SearchResults = SearchResults(),
    val makers: List<MakerHit> = emptyList()
)

class SearchViewModel(
    private val repository: SearchRepository,
    private val countryCode: String,
    private val makerRepository: MakerRepository? = null
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
            _state.value = _state.value.copy(loading = false, searched = true, results = results, makers = makersIn(term, results))
        }
    }

    /**
     * Makers die bij de gevonden podcasts horen: wie de zoekterm in zijn naam
     * heeft, of met twee of meer podcasts in de resultaten staat. Hooguit drie.
     */
    private suspend fun makersIn(term: String, results: SearchResults): List<MakerHit> {
        val repository = makerRepository ?: return emptyList()
        if (results.podcasts.isEmpty()) return emptyList()
        val directory = repository.directory(countryCode)
        val wanted = Makers.key(term)
        return results.podcasts
            .map { directory.makerOf(it.id, it.publisher) to it }
            .filter { (maker, _) -> maker.key.isNotEmpty() }
            .groupBy { it.first.key }
            .values
            .map { group ->
                val maker = group.map { it.first }.firstOrNull { it.channel != null } ?: group.first().first
                MakerHit(maker, group.size, group.first().second.id)
            }
            .filter { it.maker.key.contains(wanted) || it.found >= 2 }
            .sortedWith(compareByDescending<MakerHit> { it.maker.key.contains(wanted) }.thenByDescending { it.found })
            .take(3)
    }
}
