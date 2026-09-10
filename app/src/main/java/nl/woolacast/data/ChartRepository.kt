package nl.woolacast.data

import java.time.LocalDate
import nl.woolacast.data.local.CachedChart
import nl.woolacast.data.local.CachedEntry
import nl.woolacast.data.dataset.ChartsDataset
import nl.woolacast.data.local.LocalStore
import nl.woolacast.domain.Catalog
import nl.woolacast.domain.Chart
import nl.woolacast.domain.ChartEntry
import nl.woolacast.domain.ChartLevel
import nl.woolacast.domain.ChartQuery
import nl.woolacast.domain.ChartSource
import nl.woolacast.domain.ChartUnavailable
import nl.woolacast.domain.Movement
import nl.woolacast.domain.SourceId
import nl.woolacast.domain.WayOut

class ChartRepository(
    private val sources: List<ChartSource>,
    private val store: LocalStore,
    private val dataset: ChartsDataset? = null
) {

    fun source(id: SourceId): ChartSource =
        sources.firstOrNull { it.id == id } ?: error("Onbekende bron: $id")

    fun allSources(): List<ChartSource> = sources

    /**
     * Haalt de lijst op en legt hem naast de meest recente eerdere momentopname
     * om beweging te bepalen. De verse lijst gaat daarna zelf de opslag in.
     */
    suspend fun chart(query: ChartQuery): Chart = when (query.level) {
        ChartLevel.TRENDING -> trending(query)
        ChartLevel.NEW -> fresh(query)
        else -> ranked(query)
    }

    /**
     * Trending: Spotify publiceert er een; bij Apple zijn het de snelste
     * stijgers uit de eigen metingen — alleen mogelijk vanaf de tweede dag.
     */
    private suspend fun trending(query: ChartQuery): Chart {
        val plain = query.copy(category = Catalog.defaultCategory)
        if (source(query.source).capabilities.supports(ChartLevel.TRENDING)) {
            return ranked(plain)
        }
        val top = ranked(plain.copy(level = ChartLevel.SHOWS))
        val risers = top.entries
            .filter { (it.movement as? Movement.Up)?.places != null }
            .sortedWith(compareByDescending<ChartEntry> { (it.movement as Movement.Up).places }.thenBy { it.rank })
        if (risers.isEmpty()) {
            throw ChartUnavailable(
                "${query.source.label} publiceert geen trending-lijst. De app leidt hem af uit " +
                    "de dagelijkse metingen, en daarvoor is een tweede dag nodig.",
                wayOut = if (query.source != SourceId.SPOTIFY) WayOut.SPOTIFY else WayOut.SHOWS
            )
        }
        val entries = risers.mapIndexed { index, entry ->
            entry.copy(rank = index + 1, description = "nu #${entry.rank} in Top ${top.entries.size}")
        }
        return Chart(plain, entries, "Snelste stijgers · uit eigen metingen · ${top.updatedLabel.orEmpty()}".trim(' ', '·'))
    }

    /**
     * Nieuw: Apple heeft een echte, redactionele lijst "Nieuwe programma's"
     * per land; die haalt het klusje op. Anders: wie de laatste dagen de Top
     * 200 binnenkwam, uit de eigen metingen.
     */
    private suspend fun fresh(query: ChartQuery): Chart {
        val plain = query.copy(category = Catalog.defaultCategory)
        if (query.source == SourceId.APPLE) {
            dataset?.newShows(query.country.code)?.let { entries ->
                return Chart(plain, entries, "Apple's eigen lijst Nieuwe programma's · ${query.country.label}")
            }
        }
        val top = ranked(plain.copy(level = ChartLevel.SHOWS))
        val entered = dataset?.enteredOn(plain.copy(level = ChartLevel.SHOWS)).orEmpty()
        val entries = top.entries
            .mapNotNull { entry ->
                val day = entered[entry.id] ?: if (entry.movement == Movement.New) LocalDate.now().toString() else null
                day?.let { entry.copy(enteredOn = it) }
            }
            .sortedWith(compareByDescending<ChartEntry> { it.enteredOn }.thenBy { it.rank })
        if (entries.isEmpty()) {
            throw ChartUnavailable(
                if (query.source == SourceId.APPLE)
                    "Apple's lijst met nieuwe programma's is nog niet opgehaald voor ${query.country.label}, " +
                        "en uit de eigen metingen valt pas vanaf de tweede dag te zeggen wie nieuw is."
                else
                    "Wie nieuw is in de Top 200 volgt uit de dagelijkse metingen, en daarvoor is een tweede dag nodig.",
                wayOut = WayOut.SHOWS
            )
        }
        return Chart(plain, entries, "Nieuw in de Top ${top.entries.size} · uit eigen metingen")
    }

    private suspend fun ranked(query: ChartQuery): Chart {
        val chart = try {
            source(query.source).load(query)
        } catch (error: Throwable) {
            // Geen net? Dan de laatst opgehaalde lijst, met datum erbij.
            return cached(query) ?: throw error
        }

        val baseline = store.baseline(query.key)

        // Op dag één is er nog niets eigen gemeten. Dan telt wat het klusje
        // gisteren heeft vastgelegd, en anders wat de bron zelf meegaf.
        val published = if (baseline == null) {
            dataset?.moves(query).orEmpty()
        } else {
            emptyMap()
        }

        val entries = chart.entries.map { entry ->
            val computed = movement(entry, baseline)
            entry.copy(
                movement = when {
                    computed != Movement.Unknown -> computed
                    published.containsKey(entry.id) -> published.getValue(entry.id).asMovement()
                    else -> entry.movement
                }
            )
        }

        store.record(query.key, chart.entries.associate { it.id to it.rank })
        store.cacheChart(query.key, chart.toCache())
        return chart.copy(entries = entries)
    }

    /** De laatst bewaarde lijst, zodat de app ook zonder verbinding iets toont. */
    fun cached(query: ChartQuery): Chart? {
        val cache = store.cachedChart(query.key) ?: return null
        val baseline = store.baseline(query.key)
        val entries = cache.entries.map { entry ->
            ChartEntry(
                rank = entry.rank,
                id = entry.id,
                title = entry.title,
                publisher = entry.publisher,
                artworkUrl = entry.artworkUrl,
                genre = entry.genre,
                storeUrl = entry.storeUrl,
                showId = entry.showId,
                feedUrl = entry.feedUrl
            ).let { it.copy(movement = movement(it, baseline)) }
        }
        return Chart(query, entries, cache.updatedLabel, cachedAt = cache.fetchedAt)
    }

    private fun Chart.toCache() = CachedChart(
        fetchedAt = LocalDate.now().toString(),
        updatedLabel = updatedLabel,
        entries = entries.map {
            CachedEntry(
                rank = it.rank, id = it.id, title = it.title, publisher = it.publisher,
                artworkUrl = it.artworkUrl, genre = it.genre, storeUrl = it.storeUrl,
                showId = it.showId, feedUrl = it.feedUrl
            )
        }
    )

    private fun Int.asMovement(): Movement = when {
        this > 0 -> Movement.Up(this)
        this < 0 -> Movement.Down(-this)
        else -> Movement.Flat
    }

    private fun movement(entry: ChartEntry, baseline: Map<String, Int>?): Movement {
        if (baseline == null) return Movement.Unknown
        val previous = baseline[entry.id] ?: return Movement.New
        val delta = previous - entry.rank
        return when {
            delta > 0 -> Movement.Up(delta)
            delta < 0 -> Movement.Down(-delta)
            else -> Movement.Flat
        }
    }
}
