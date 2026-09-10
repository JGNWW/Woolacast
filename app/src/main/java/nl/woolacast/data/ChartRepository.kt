package nl.woolacast.data

import java.time.LocalDate
import nl.woolacast.data.local.CachedChart
import nl.woolacast.data.local.CachedEntry
import nl.woolacast.data.dataset.ChartsDataset
import nl.woolacast.data.local.LocalStore
import nl.woolacast.domain.Chart
import nl.woolacast.domain.ChartEntry
import nl.woolacast.domain.ChartQuery
import nl.woolacast.domain.ChartSource
import nl.woolacast.domain.Movement
import nl.woolacast.domain.SourceId

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
    suspend fun chart(query: ChartQuery): Chart {
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
