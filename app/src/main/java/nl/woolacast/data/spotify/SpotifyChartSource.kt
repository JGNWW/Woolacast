package nl.woolacast.data.spotify

import nl.woolacast.data.Html
import nl.woolacast.domain.Chart
import nl.woolacast.domain.ChartEntry
import nl.woolacast.domain.ChartLevel
import nl.woolacast.domain.ChartQuery
import nl.woolacast.domain.ChartSource
import nl.woolacast.domain.ChartUnavailable
import nl.woolacast.domain.Movement
import nl.woolacast.domain.SourceCapabilities
import nl.woolacast.domain.SourceId
import nl.woolacast.domain.WayOut

class SpotifyChartSource(private val api: SpotifyChartsApi) : ChartSource {

    override val id = SourceId.SPOTIFY

    override val capabilities = SourceCapabilities(
        // Trending is een echte Spotify-lijst; Nieuw leidt de app zelf af.
        levels = setOf(ChartLevel.SHOWS, ChartLevel.EPISODES, ChartLevel.TRENDING),
        categoryLevels = setOf(ChartLevel.SHOWS),
        countryCount = 26,
        cadence = "Dagelijks",
        summary = "Top 200 shows en afleveringen in 26 landen · categorieen in zeven daarvan"
    )

    override fun covers(countryCode: String) = countryCode.lowercase() in CHART_MARKETS

    override suspend fun load(query: ChartQuery): Chart {
        val region = query.country.code

        val category = when {
            // Spotify's categorielijsten gaan alleen over shows — in alle zeven
            // landen waar ze bestaan. Apple heeft die combinatie wel.
            query.level == ChartLevel.EPISODES && !query.category.isAll ->
                throw ChartUnavailable(
                    "Spotify publiceert afleveringen alleen als één lijst, niet per " +
                        "categorie. Apple doet dat wel.",
                    wayOut = WayOut.APPLE
                )

            query.level == ChartLevel.EPISODES -> "top-episodes"
            query.level == ChartLevel.TRENDING -> "trending"
            query.category.isAll -> "top-podcasts"

            else -> {
                val slug = query.category.spotifySlug
                    ?: throw ChartUnavailable("Spotify kent geen lijst voor ${query.category.label}.")
                if (region !in CATEGORY_MARKETS) {
                    throw ChartUnavailable(
                        "Spotify publiceert categorielijsten in maar zeven landen, en " +
                            "${query.country.label} hoort daar niet bij.",
                        wayOut = WayOut.APPLE
                    )
                }
                slug
            }
        }

        if (region !in CHART_MARKETS) {
            throw ChartUnavailable(
                "Spotify publiceert geen lijst voor ${query.country.label}.",
                wayOut = WayOut.APPLE
            )
        }

        val items = api.chart(category, region, query.limit)
        if (items.isEmpty()) {
            throw ChartUnavailable("Spotify heeft geen lijst voor ${query.country.label}.")
        }

        val episodes = query.level == ChartLevel.EPISODES
        val entries = items.mapIndexedNotNull { index, item ->
            val uri = (if (episodes) item.episodeUri else item.showUri) ?: return@mapIndexedNotNull null
            ChartEntry(
                rank = index + 1,
                id = uri,
                title = if (episodes) item.episodeName.orEmpty() else item.showName,
                publisher = if (episodes) item.showName else item.showPublisher.orEmpty(),
                artworkUrl = if (episodes) item.episodeImageUrl ?: item.showImageUrl else item.showImageUrl,
                genre = null,
                storeUrl = uri.toOpenUrl(),
                movement = item.chartRankMove.toMovement(),
                // Spotify geeft geen feed-URL; de podcastpagina zoekt die op naam op.
                showId = item.showUri,
                feedUrl = null,
                description = Html.toPlainText(if (episodes) item.episodeDescription else item.showDescription)
            )
        }

        val label = when {
            query.level == ChartLevel.TRENDING -> "Spotify's eigen trending-lijst · dagelijks"
            query.category.isAll -> "Spotify · dagelijks bijgewerkt"
            else -> "Spotify · top 50 in ${query.category.label}"
        }

        return Chart(query, entries, updatedLabel = label)
    }

    private companion object {
        /** De landen waar Spotify uberhaupt een lijst publiceert. */
        val CHART_MARKETS = setOf(
            "us", "ar", "au", "at", "br", "ca", "cl", "co", "dk", "fi", "fr", "de",
            "in", "id", "ie", "it", "jp", "mx", "nz", "no", "ph", "pl", "es", "se", "nl", "gb"
        )

        /** Categorielijsten bestaan maar in zeven daarvan, en dan top 50 in plaats van 200. */
        val CATEGORY_MARKETS = setOf("us", "au", "br", "de", "mx", "se", "gb")

        /**
         * Spotify zegt wél welke kant het op ging, maar niet hoeveel plaatsen.
         * Het aantal vult de repository aan uit de eigen momentopnames.
         */
        fun String?.toMovement(): Movement = when (this) {
            "UP" -> Movement.Up(null)
            "DOWN" -> Movement.Down(null)
            "NEW" -> Movement.New
            "UNCHANGED" -> Movement.Flat
            else -> Movement.Unknown
        }

        /** spotify:show:abc -> https://open.spotify.com/show/abc */
        fun String.toOpenUrl(): String? {
            val parts = split(':')
            return if (parts.size == 3) "https://open.spotify.com/${parts[1]}/${parts[2]}" else null
        }
    }
}
