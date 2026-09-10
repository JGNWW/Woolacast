package nl.woolacast.data.apple

import nl.woolacast.data.Html
import nl.woolacast.data.dataset.ChartsDataset
import nl.woolacast.domain.Chart
import nl.woolacast.domain.ChartEntry
import nl.woolacast.domain.ChartLevel
import nl.woolacast.domain.ChartQuery
import nl.woolacast.domain.ChartSource
import nl.woolacast.domain.ChartUnavailable
import nl.woolacast.domain.SourceCapabilities
import nl.woolacast.domain.SourceId
import nl.woolacast.domain.WayOut

/**
 * Apple is de enige bron met een publieke, sleutelloze lijst. Drie routes:
 *
 * - de marketing-feed voor de ongefilterde top, shows en afleveringen;
 * - de oudere rss-generator zodra er een categorie gekozen is op showniveau,
 *   want dat is de enige die per genre filtert;
 * - voor afleveringen per categorie is er geen gefilterde feed. Apple toont die
 *   lijst wel op podcasts.apple.com, maar alleen via een API met token. Wat wel
 *   kan: de top 100 ophalen en zelf op categorie schiften. Elke aflevering
 *   draagt namelijk een genre — soms een subgenre, en die knoopt [genreTree]
 *   aan zijn hoofdgenre. Dat is dus een *afgeleide* lijst, geen kopie van
 *   Apple's eigen categorielijst, en de app zegt dat er ook bij.
 */
class AppleChartSource(
    private val marketing: AppleMarketingApi,
    private val catalog: AppleCatalogApi,
    private val genreTree: AppleGenreTree,
    private val dataset: ChartsDataset? = null
) : ChartSource {

    override val id = SourceId.APPLE

    override val capabilities = SourceCapabilities(
        levels = setOf(ChartLevel.SHOWS, ChartLevel.EPISODES),
        categoryLevels = setOf(ChartLevel.SHOWS, ChartLevel.EPISODES),
        countryCount = 175,
        cadence = "Dagelijks",
        summary = "Shows en afleveringen · 175 landen · alle categorieen"
    )

    override suspend fun load(query: ChartQuery): Chart = when (query.level) {
        ChartLevel.SHOWS -> showsChart(query)
        ChartLevel.EPISODES ->
            if (query.category.isAll) marketingChart(query, feed = EPISODES_FEED)
            else episodesByCategory(query)
    }

    /**
     * De winkel-ranglijst geeft alleen ids, maar gaat 200 diep en kent elke
     * categorie. Eén batch-lookup maakt er volledige vermeldingen van — mét
     * feed-URL, zodat de podcastpagina meteen naar de RSS kan.
     */
    private suspend fun showsChart(query: ChartQuery): Chart {
        val genreId = query.category.appleGenreId ?: ROOT_GENRE
        val ids = catalog.charts(
            country = query.country.code,
            genreId = genreId,
            name = "Podcasts",
            limit = query.limit.coerceAtMost(MAX_CHART)
        ).resultIds

        if (ids.isEmpty()) throw ChartUnavailable("Apple heeft geen lijst voor deze combinatie.")

        val byId = catalog.lookupMany(ids.joinToString(","), query.country.code)
            .results.associateBy { it.collectionId?.toString() }

        // De lookup laat er soms een paar vallen; de volgorde van de lijst is leidend.
        val entries = ids.mapNotNull { byId[it] }.mapIndexed { index, result ->
            ChartEntry(
                rank = index + 1,
                id = result.collectionId?.toString() ?: return@mapIndexed null,
                title = result.collectionName ?: result.trackName.orEmpty(),
                publisher = result.artistName.orEmpty(),
                artworkUrl = result.artworkUrl600 ?: result.artworkUrl100,
                genre = result.primaryGenreName,
                storeUrl = null,
                showId = result.collectionId?.toString(),
                feedUrl = result.feedUrl,
                description = Html.toPlainText(result.description)
            )
        }.filterNotNull()

        return Chart(query, entries, updatedLabel = "Apple Podcasts · dagelijks bijgewerkt")
    }

    private suspend fun marketingChart(query: ChartQuery, feed: String): Chart {
        val response = marketing.top(query.country.code, query.limit.coerceAtMost(MAX_FEED), feed)
        val entries = response.feed.results.mapIndexed { index, result ->
            result.toEntry(rank = index + 1, episodes = feed == EPISODES_FEED)
        }
        return Chart(query, entries, response.feed.updated)
    }

    private suspend fun episodesByCategory(query: ChartQuery): Chart {
        val genreId = query.category.appleGenreId
            ?: throw ChartUnavailable("Deze categorie heeft geen genre-id.")

        // Is de lijst al ergens verzameld, dan is dat Apple's echte volgorde.
        dataset?.episodesByCategory(query)?.let { return it }

        val response = marketing.top(query.country.code, MAX_FEED, EPISODES_FEED)
        val names = genreTree.topLevelByName(query.country.code)

        val entries = response.feed.results
            .filter { result ->
                val genre = result.genres.firstOrNull() ?: return@filter false
                val topLevel = genre.genreId?.toIntOrNull() ?: names[genre.name]
                topLevel == genreId
            }
            .mapIndexed { index, result -> result.toEntry(rank = index + 1, episodes = true) }

        if (entries.isEmpty()) {
            throw ChartUnavailable(
                "Er staat op dit moment geen ${query.category.label.lowercase()} in de top " +
                    "$MAX_FEED afleveringen van ${query.country.label}.",
                wayOut = WayOut.ALL_CATEGORIES
            )
        }

        return Chart(
            query = query,
            entries = entries,
            updatedLabel = "${entries.size} uit de top $MAX_FEED afleveringen · geschift op categorie"
        )
    }

    @Suppress("unused")
    private suspend fun legacyChart(query: ChartQuery): Chart {
        val genreId = query.category.appleGenreId
            ?: throw ChartUnavailable("Deze categorie heeft geen genre-id.")
        val url = "https://itunes.apple.com/${query.country.code}/rss/toppodcasts/" +
            "limit=${query.limit.coerceAtMost(MAX_FEED)}/genre=$genreId/json"

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
                showId = entryId,
                description = Html.toPlainText(entry.summary?.label)
            )
        }
        return Chart(query, entries, response.feed.updated?.label)
    }

    private fun MarketingResult.toEntry(rank: Int, episodes: Boolean) = ChartEntry(
        rank = rank,
        id = id,
        title = name,
        publisher = artistName.orEmpty(),
        artworkUrl = artworkUrl100?.let(::upscaleArtwork),
        genre = genres.firstOrNull()?.name,
        storeUrl = url,
        showId = if (episodes) url?.let(::showIdFromUrl) else id
    )

    private companion object {
        const val SHOWS_FEED = "podcasts"
        const val EPISODES_FEED = "podcast-episodes"

        /** Het genre-id van de wortel: alle podcastcategorieen samen. */
        const val ROOT_GENRE = 26

        /** De winkel-ranglijst gaat minstens zo diep; verder heeft weinig zin. */
        const val MAX_CHART = 200

        /** Boven de honderd geeft de feed een serverfout. */
        const val MAX_FEED = 100

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
