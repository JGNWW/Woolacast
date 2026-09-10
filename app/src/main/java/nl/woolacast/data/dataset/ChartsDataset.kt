package nl.woolacast.data.dataset

import kotlinx.serialization.Serializable
import nl.woolacast.domain.Chart
import nl.woolacast.domain.ChartEntry
import nl.woolacast.domain.ChartQuery
import retrofit2.http.GET
import retrofit2.http.Url

@Serializable
data class DatasetEntry(
    val rank: Int = 0,
    val id: String = "",
    val title: String = "",
    val showId: String? = null,
    val showTitle: String? = null,
    val publisher: String? = null,
    val artworkUrl: String? = null,
    val feedUrl: String? = null,
    val durationMs: Long? = null,
    val releaseDate: String? = null
)

@Serializable
data class DatasetChart(
    val country: String = "",
    val genreId: Int = 0,
    val genreLabel: String = "",
    val updated: String? = null,
    val resolved: Int = 0,
    val entries: List<DatasetEntry> = emptyList()
)

interface ChartsDatasetApi {
    @GET
    suspend fun chart(@Url url: String): DatasetChart
}

/**
 * Apple's afleveringenlijst per categorie is publiek, maar alleen als losse ids:
 * elk id kost een eigen aanroep, en dat zijn er tweehonderd per lijst. Te veel
 * voor een telefoon, prima voor een klusje dat af en toe draait.
 *
 * Die verzamelde lijsten staan als platte JSON in een publieke repo. De app
 * leest daar alleen van — geen sleutel, geen account, geen server die draait.
 * Staat er niets, dan valt [nl.woolacast.data.apple.AppleChartSource] terug op
 * schiften uit de algemene top honderd.
 *
 * De verzamelaar zelf staat in charts-service/ in deze repo.
 */
class ChartsDataset(
    private val api: ChartsDatasetApi,
    private val baseUrl: String = DEFAULT_BASE_URL
) {

    suspend fun episodes(query: ChartQuery, genreId: Int): Chart? {
        val url = "$baseUrl/${query.country.code}/$genreId/episodes.json"
        val dataset = runCatching { api.chart(url) }.getOrNull() ?: return null
        if (dataset.entries.isEmpty()) return null

        val entries = dataset.entries.map { entry ->
            ChartEntry(
                rank = entry.rank,
                id = entry.id,
                title = entry.title,
                publisher = entry.showTitle ?: entry.publisher.orEmpty(),
                artworkUrl = entry.artworkUrl,
                genre = dataset.genreLabel,
                storeUrl = null,
                showId = entry.showId,
                feedUrl = entry.feedUrl
            )
        }

        val day = dataset.updated?.take(10)
        return Chart(
            query = query,
            entries = entries,
            updatedLabel = "Apple's eigen categorielijst · verzameld ${day.orEmpty()}".trim()
        )
    }

    companion object {
        /**
         * Wijs dit naar je eigen kopie als je de verzamelaar zelf draait. De
         * repo moet publiek zijn: raw.githubusercontent geeft privérepo's niet
         * zonder token vrij.
         */
        const val DEFAULT_BASE_URL =
            "https://raw.githubusercontent.com/JGNWW/Woolacast/" +
                "claude/android-podcast-app-mockup-weuhxw/charts-service/charts"
    }
}
