package nl.woolacast.data

import nl.woolacast.data.local.LocalStore
import nl.woolacast.domain.Chart
import nl.woolacast.domain.ChartEntry
import nl.woolacast.domain.ChartQuery
import nl.woolacast.domain.ChartSource
import nl.woolacast.domain.Movement
import nl.woolacast.domain.SourceId

class ChartRepository(
    private val sources: List<ChartSource>,
    private val store: LocalStore
) {

    fun source(id: SourceId): ChartSource =
        sources.firstOrNull { it.id == id } ?: error("Onbekende bron: $id")

    fun allSources(): List<ChartSource> = sources

    /**
     * Haalt de lijst op en legt hem naast de meest recente eerdere momentopname
     * om beweging te bepalen. De verse lijst gaat daarna zelf de opslag in.
     */
    suspend fun chart(query: ChartQuery): Chart {
        val chart = source(query.source).load(query)
        val baseline = store.baseline(query.key)

        val entries = chart.entries.map { entry ->
            entry.copy(movement = movement(entry, baseline))
        }

        store.record(query.key, chart.entries.associate { it.id to it.rank })
        return chart.copy(entries = entries)
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
