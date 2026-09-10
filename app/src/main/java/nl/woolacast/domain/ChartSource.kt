package nl.woolacast.domain

/**
 * Een bron kan om twee redenen niets leveren: de combinatie bestaat niet
 * (YouTube kent geen afleveringen) of er is geen publieke weg naar de data.
 * Beide zijn gewone antwoorden, geen fouten — de UI legt ze uit.
 */
class ChartUnavailable(
    val reason: String,
    /** Wat de gebruiker hieraan kan doen; de UI maakt er een knop van. */
    val wayOut: WayOut? = null
) : Exception(reason)

enum class WayOut { ALL_CATEGORIES, APPLE, SHOWS }

interface ChartSource {
    val id: SourceId
    val capabilities: SourceCapabilities

    /** Levert de lijst zonder beweging; die vult [nl.woolacast.data.ChartRepository] aan. */
    suspend fun load(query: ChartQuery): Chart
}
