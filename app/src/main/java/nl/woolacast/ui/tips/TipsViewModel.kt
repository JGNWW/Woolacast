package nl.woolacast.ui.tips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.woolacast.data.dataset.ChartsDataset
import nl.woolacast.data.dataset.MediaTip
import nl.woolacast.data.local.CachedTips
import nl.woolacast.data.local.LocalStore
import nl.woolacast.data.tips.LiveTipsReader
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class TipsUiState(
    val loading: Boolean = true,
    val countryCode: String = "",
    val outlets: List<String> = emptyList(),
    val outlet: String? = null,
    val all: List<MediaTip> = emptyList(),
    val updated: String? = null,
    /** De app is nu zelf feeds aan het lezen. */
    val reading: Boolean = false,
    /** Hoeveel tips daarvan uit de eigen leesronde kwamen. */
    val fromFeeds: Int = 0
) {
    /** Het beeldmerk per medium, om de keuzeknoppen bovenaan te vullen. */
    val logos: Map<String, String> get() =
        all.mapNotNull { tip -> tip.logo?.let { tip.outlet to it } }.toMap()

    /** Wat er getoond wordt: alles, of alleen het gekozen medium. */
    val visible: List<MediaTip> get() = all.filter { outlet == null || it.outlet == outlet }
}

/**
 * De tips komen uit twee bronnen, en dat is met opzet.
 *
 * De verzamelaar leest elke nacht de artikelen achter de koppen — dat is het
 * werk dat per site verschilt en het vaakst breekt, en het levert de beste
 * koppelingen op. De app leest daarnaast zelf de feeds, en dat vult het gat van
 * vandaag: een tiplijst die vanochtend verscheen staat er meteen in.
 */
class TipsViewModel(
    private val dataset: ChartsDataset,
    private val reader: LiveTipsReader,
    private val store: LocalStore
) : ViewModel() {

    private val _state = MutableStateFlow(TipsUiState())
    val state: StateFlow<TipsUiState> = _state.asStateFlow()

    private var collected: List<MediaTip> = emptyList()

    fun load(countryCode: String) {
        if (_state.value.countryCode == countryCode && !_state.value.loading) return
        _state.value = TipsUiState(loading = true, countryCode = countryCode)
        viewModelScope.launch {
            val tips = dataset.tips(countryCode)
            collected = tips?.entries.orEmpty()
            val cached = store.cachedTips(countryCode)
            publish(countryCode, cached?.entries.orEmpty(), tips?.updated)
            if (cached == null || stale(cached)) refresh()
        }
    }

    /** Zelf de feeds langslopen. Gebeurt vanzelf, en met de knop bovenin. */
    fun refresh() {
        val countryCode = _state.value.countryCode.ifEmpty { return }
        if (_state.value.reading) return
        _state.value = _state.value.copy(reading = true)
        viewModelScope.launch {
            val feeds = dataset.feeds(countryCode)
            val live = runCatching { reader.read(countryCode, feeds) }.getOrDefault(emptyList())
            if (live.isNotEmpty()) {
                store.cacheTips(countryCode, CachedTips(Instant.now().toString(), live))
            }
            publish(countryCode, live, _state.value.updated)
            _state.value = _state.value.copy(reading = false)
        }
    }

    /** Hetzelfde artikel over dezelfde show is één tip, hoe het medium ook heet. */
    private fun fingerprint(tip: MediaTip) =
        (tip.showId ?: "") + "|" + tip.url + "|" + tip.outlet.lowercase().filter { it.isLetter() }

    private fun publish(countryCode: String, live: List<MediaTip>, updated: String?) {
        // Wat de verzamelaar vond gaat voor: daar is het artikel bij gelezen.
        val seen = collected.map(::fingerprint).toMutableSet()
        val extra = live.filter { seen.add(fingerprint(it)) }
        val all = (collected + extra).sortedByDescending { it.date ?: "" }
        _state.value = _state.value.copy(
            loading = false,
            countryCode = countryCode,
            outlets = all.map { it.outlet }.distinct().sorted(),
            all = all,
            updated = updated,
            fromFeeds = extra.size
        )
    }

    /** Een keer per dag per land. De knop rechtsboven doet het altijd. */
    private fun stale(cached: CachedTips): Boolean {
        val fetched = runCatching { Instant.parse(cached.fetchedAt) }.getOrNull() ?: return true
        return fetched.atZone(ZoneId.systemDefault()).toLocalDate() != LocalDate.now()
    }

    fun setOutlet(outlet: String?) {
        _state.value = _state.value.copy(outlet = outlet)
    }

}
