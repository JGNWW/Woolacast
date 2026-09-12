package nl.woolacast.ui.maker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.woolacast.data.SearchRepository
import nl.woolacast.domain.Podcast

data class MakerUiState(
    val loading: Boolean = true,
    val publisher: String = "",
    val shows: List<Podcast> = emptyList(),
    /** De show waar je vandaan kwam; die krijgt een merkteken. */
    val currentId: String? = null,
    val error: String? = null
)

/** Alles van één maker, opgehaald bij Apple zodra je het scherm opent. */
class MakerViewModel(
    private val repository: SearchRepository,
    private val publisher: String,
    private val countryCode: String,
    private val fromShowId: String?
) : ViewModel() {

    private val _state = MutableStateFlow(MakerUiState(publisher = publisher))
    val state: StateFlow<MakerUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { repository.byMaker(publisher, countryCode) }
                .onSuccess { shows ->
                    _state.value = MakerUiState(
                        loading = false,
                        publisher = publisher,
                        shows = shows,
                        currentId = fromShowId
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        loading = false,
                        error = error.message ?: "Kon de podcasts van deze maker niet laden."
                    )
                }
        }
    }
}
