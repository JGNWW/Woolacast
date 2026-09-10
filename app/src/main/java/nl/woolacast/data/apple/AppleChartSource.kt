package nl.woolacast.data.apple

import nl.woolacast.domain.Chart
import nl.woolacast.domain.ChartEntry
import nl.woolacast.domain.ChartLevel
import nl.woolacast.domain.ChartQuery
import nl.woolacast.domain.ChartSource
import nl.woolacast.domain.ChartUnavailable
import nl.woolacast.domain.SourceCapabilities
import nl.woolacast.domain.SourceId

/**
 * Apple is de enige bron met een publieke, sleutelloze lijst. Twee endpoints:
 * de marketing-feed voor de ongefilterde top, en de oude rss-generator zodra
 * er een categorie gekozen is — die laatste is de enige die per genre filtert.
 */
class AppleChartSource(
    private val marketing: AppleMarketingApi,
    private val catalog: AppleCatalogApi
) : ChartSource {

    override val id = SourceId.APPLE

    override val capabilities = SourceCapabilities(
        levels = setOf(ChartLevel.SHOWS, ChartLevel.EPISODES),
        categoryLevels = setOf(ChartLevel.SHOWS),
        countryCount = 175,
        cadence = "Dagelijks",
        summary = "Shows en afleveringen · 175 landen · categorieen alleen op showniveau"
    )

    override suspend fun load(query: ChartQuery): Chart = when {
        query.level == ChartLevel.EPISODES && !query.category.isAll ->
            throw ChartUnavailable(
                "Apple publiceert geen afleveringenlijst per categorie. " +
                    "De lijst hieronder is alle categorieen samen."
            )

        query.level == ChartLevel.EPISODES -> marketingChart(query, feed = "podcast-episodes")

        query.category.isAll -> marketingChart(query, feed = "podcasts")

        else -> legacyChart(query)
    }

    private suspend fun marketingChart(query: ChartQuery, feed: String): Chart {
        val response = marketing.top(query.country.code, query.limit, feed)
        val entries = response.feed.results.mapIndexed { index, result ->
            ChartEntry(
                rank = index + 1,
                id = result.id,
                title = result.name,
                publisher = result.artistName.orEmpty(),
                artworkUrl = result.artworkUrl100?.let(::upscaleArtwork),
                genre = result.genres.firstOrNull()?.name,
                storeUrl = result.url,
                showId = if (feed == "podcast-episodes") result.url?.let(::showIdFromUrl) else result.id,
                feedUrl = null
            )
        }
        return Chart(query, entries, response.feed.updated)
    }

    private suspend fun legacyChart(query: ChartQuery): Chart {
        val genreId = query.category.appleGenreId
            ?: throw ChartUnavailable("Deze categorie heeft geen genre-id.")
        val url = "https://itunes.apple.com/${query.country.code}/rss/toppodcasts/" +
            "limit=${query.limit}/genre=$genreId/json"

        val response = catalog.legacyTop(url)
        val entries = response.feed.entry.mapIndexedNotNull { index, entry ->
            val entryId = entry.id?.attributes?.id ?: return@mapIndexedNotNull null
            ChartEntry(
                rank = index + 1,
                id = entryId,
                title = entry.name?.label.orEmpty(),
                publisher = entry.artist?.label.orEmpty(),
                artworkUrl = entry.images.lastOrNull()?.label?.let(::upscaleArtwork),
                genre = entry.category?.attributes?.label,
                storeUrl = entry.link?.attributes?.href,
                showId = entryId
            )
        }
        return Chart(query, entries, response.feed.updated?.label)
    }

    private companion object {
        /**
         * De feed levert 55 tot 170 px. De maat zit letterlijk in het pad, dus
         * die vervangen we door iets dat op een telefoon scherp is.
         */
        val ARTWORK_SIZE = Regex("""/\d+x\d+bb\.(png|jpg)$""")

        fun upscaleArtwork(url: String) = ARTWORK_SIZE.replace(url) { "/600x600bb.${it.groupValues[1]}" }

        /** Een afleverings-URL draagt het show-id in het pad: .../id1513807137?i=100078... */
        fun showIdFromUrl(url: String) = Regex("""/id(\d+)""").find(url)?.groupValues?.get(1)
    }
}
