package nl.woolacast.data.fyyd

import nl.woolacast.domain.Chart
import nl.woolacast.domain.ChartEntry
import nl.woolacast.domain.ChartLevel
import nl.woolacast.domain.ChartQuery
import nl.woolacast.domain.ChartSource
import nl.woolacast.domain.ChartUnavailable
import nl.woolacast.domain.SourceCapabilities
import nl.woolacast.domain.SourceId

/**
 * fyyd.de is een open podcastzoekmachine met een API zonder sleutel en zonder
 * account. Twee dingen om te weten: de ranglijst gaat per *taal*, niet per land,
 * en er is geen lijst op afleveringniveau. Buiten het Duitse en Nederlandse
 * taalgebied is de dekking dun.
 *
 * De winst zit in de feed-URL die meekomt: daarmee gaat de podcastpagina
 * rechtstreeks naar de RSS, zonder tussenkomst van een catalogus.
 */
class FyydChartSource(private val api: FyydApi) : ChartSource {

    override val id = SourceId.FYYD

    override val capabilities = SourceCapabilities(
        levels = setOf(ChartLevel.SHOWS),
        categoryLevels = emptySet(),
        countryCount = LANGUAGE_BY_COUNTRY.size,
        cadence = "Doorlopend",
        summary = "Open API zonder sleutel · alleen shows · per taal in plaats van per land"
    )

    override suspend fun load(query: ChartQuery): Chart {
        if (query.level != ChartLevel.SHOWS) {
            throw ChartUnavailable("fyyd publiceert geen lijst van losse afleveringen.")
        }
        if (!query.category.isAll) {
            throw ChartUnavailable("De ranglijst van fyyd kent geen categorieen.")
        }

        val language = LANGUAGE_BY_COUNTRY[query.country.code]
            ?: throw ChartUnavailable(
                "fyyd sorteert op taal. Voor ${query.country.label} is er geen taalgebied ingesteld."
            )

        val response = api.hot(count = query.limit, language = language)
        val entries = response.data.mapIndexed { index, podcast ->
            ChartEntry(
                rank = index + 1,
                id = "fyyd-${podcast.id}",
                title = podcast.title,
                publisher = podcast.author.orEmpty(),
                artworkUrl = podcast.layoutImageURL ?: podcast.imgURL,
                genre = null,
                storeUrl = podcast.htmlURL,
                showId = "fyyd-${podcast.id}",
                feedUrl = podcast.xmlURL
            )
        }

        return Chart(query, entries, updatedLabel = "fyyd · taalgebied ${language.uppercase()}")
    }

    private companion object {
        /** Land uit de app vertaald naar het taalgebied dat fyyd kent. */
        val LANGUAGE_BY_COUNTRY = mapOf(
            "nl" to "nl", "be" to "nl",
            "de" to "de",
            "fr" to "fr",
            "es" to "es", "mx" to "es",
            "it" to "it",
            "se" to "sv",
            "dk" to "da",
            "no" to "no",
            "br" to "pt",
            "jp" to "ja",
            "gb" to "en", "us" to "en", "ie" to "en", "ca" to "en", "au" to "en", "in" to "en"
        )
    }
}
