package nl.woolacast.data

import nl.woolacast.domain.Chart
import nl.woolacast.domain.ChartLevel
import nl.woolacast.domain.ChartQuery
import nl.woolacast.domain.ChartSource
import nl.woolacast.domain.ChartUnavailable
import nl.woolacast.domain.SourceCapabilities
import nl.woolacast.domain.SourceId

/**
 * Spotify en YouTube publiceren hun lijsten wel, maar alleen als webpagina —
 * er is geen open API en scrapen is tegen hun voorwaarden. De bronnen staan
 * er daarom in met hun echte eigenschappen en een uitleg in plaats van data,
 * zodat de app ze kan tonen zodra er wel een weg is.
 */
class UnsupportedChartSource(
    override val id: SourceId,
    override val capabilities: SourceCapabilities,
    private val reason: String
) : ChartSource {

    override suspend fun load(query: ChartQuery): Chart = throw ChartUnavailable(reason)

    companion object {
        fun spotify() = UnsupportedChartSource(
            id = SourceId.SPOTIFY,
            capabilities = SourceCapabilities(
                levels = setOf(ChartLevel.SHOWS, ChartLevel.EPISODES),
                categoryLevels = setOf(ChartLevel.SHOWS),
                countryCount = 60,
                cadence = "Dagelijks",
                summary = "Top 200 shows overal · afleveringen en categorieen in geselecteerde landen"
            ),
            reason = "Spotify publiceert zijn lijsten alleen als webpagina, zonder open API. " +
                "Zodra daar een weg voor is, verschijnt de lijst hier."
        )

        fun youtube() = UnsupportedChartSource(
            id = SourceId.YOUTUBE,
            capabilities = SourceCapabilities(
                levels = setOf(ChartLevel.SHOWS),
                categoryLevels = emptySet(),
                countryCount = 38,
                cadence = "Wekelijks, op woensdag",
                summary = "Alleen shows · wekelijks · circa 38 landen"
            ),
            reason = "YouTube publiceert een wekelijkse showlijst, maar alleen als webpagina. " +
                "Afleveringen en categorieen kent die lijst sowieso niet."
        )
    }
}
