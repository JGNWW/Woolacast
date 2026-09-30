package nl.woolacast.data.maker

import nl.woolacast.data.ChartRepository
import nl.woolacast.data.SearchRepository
import nl.woolacast.data.apple.AppleCatalogApi
import nl.woolacast.data.dataset.ChartsDataset
import nl.woolacast.data.dataset.DatasetChannel
import nl.woolacast.data.dataset.DatasetMakers
import nl.woolacast.data.local.LocalStore
import nl.woolacast.data.toMakerShow
import nl.woolacast.domain.Catalog
import nl.woolacast.domain.Channel
import nl.woolacast.domain.Chart
import nl.woolacast.domain.ChannelShow
import nl.woolacast.domain.ChartLevel
import nl.woolacast.domain.ChartQuery
import nl.woolacast.domain.Country
import nl.woolacast.domain.Maker
import nl.woolacast.domain.MakerShow
import nl.woolacast.domain.Makers
import nl.woolacast.domain.SourceId

/** De Apple-kanalen van één land, op id, op makerssleutel en per show. */
class MakerDirectory(data: DatasetMakers?) {
    private val channels: Map<String, Channel> =
        data?.channels.orEmpty().associate { it.id to it.toChannel() }
    private val byKey: Map<String, Channel> =
        channels.values.associateBy { Makers.key(it.name) }
    private val showChannel: Map<String, String> = data?.showChannel.orEmpty()

    fun channel(id: String): Channel? = channels[id]

    /** Het kanaal van een Apple-show, als Apple hem aan een kanaal hangt. */
    fun channelOfShow(showId: String?): Channel? = showId?.let { showChannel[it] }?.let { channels[it] }

    /** Een kanaal op naam, alleen bij een exacte overeenkomst van de sleutel. */
    fun channelNamed(key: String): Channel? = byKey[key]

    /** De maker achter een vermelding: eerst het kanaal van de show, dan de naam. */
    fun makerOf(showId: String?, publisher: String): Maker {
        val key = Makers.keyOf(publisher)
        val channel = channelOfShow(showId) ?: channelNamed(key)
        return if (channel != null) Maker(Makers.key(channel.name), channel.name, channel)
        else Maker(key, Makers.name(publisher))
    }
}

/**
 * Waar een show van een maker staat in de gekozen lijst. [unmatched] telt
 * Spotify-shows van deze maker die niet aan een Apple-show te koppelen waren:
 * die tellen niet mee, en dat zegt de pagina erbij.
 */
data class Placement(
    val source: SourceId,
    val country: Country,
    val listSize: Int,
    val ranks: Map<String, Int>,
    val unmatched: Int
)

/**
 * Het gezicht van een maker: de hoezen van zijn eerste vier shows, in de
 * volgorde van de catalogus. Bij een kanaal is dat Apple's volgorde, van
 * populair naar minder. Elk scherm toont dit, zodat een maker er overal
 * hetzelfde uitziet.
 */
object MakerFace {
    const val SIZE = 4

    fun of(shows: List<MakerShow>): List<String> =
        shows.mapNotNull { it.podcast.artworkUrl }.distinct().take(SIZE)
}

/** De shows van een maker; [complete] is false als het een zoekopdracht op naam was. */
data class MakerShows(val shows: List<MakerShow>, val complete: Boolean)

/**
 * Alles over makers. Kanalen komen uit de verzamelaar (die heeft het webtoken
 * van Apple); de rest is live: de catalogus voor de shows, de hitlijsten voor
 * waar ze staan.
 */
class MakerRepository(
    private val dataset: ChartsDataset,
    private val catalog: AppleCatalogApi,
    private val search: SearchRepository,
    private val charts: ChartRepository,
    private val store: LocalStore
) {

    suspend fun directory(countryCode: String): MakerDirectory =
        MakerDirectory(runCatching { dataset.makers(countryCode) }.getOrNull())

    /** Wie er achter een makersregel zit, met kanaal als dat te vinden is. */
    suspend fun identify(publisher: String, countryCode: String, fromShowId: String?): Maker =
        directory(countryCode).makerOf(fromShowId, publisher)

    /**
     * De shows van een maker. Met een kanaal is dat de volledige lijst van
     * Apple, opgehaald in één of twee aanroepen; zonder kanaal een zoekopdracht
     * op naam, en dan is de lijst wat de catalogus erbij vindt.
     */
    suspend fun shows(maker: Maker, countryCode: String): MakerShows =
        findShows(maker, countryCode).also { store.rememberMakerFace(maker.key, MakerFace.of(it.shows)) }

    /**
     * Het gezicht van een maker: wat de app zag toen hij zijn shows laadde,
     * anders wat de verzamelaar van zijn kanaal vastlegde. Null als geen van
     * beide er is; dan toont een scherm de hoezen die het zelf kent.
     */
    fun face(maker: Maker): List<String>? =
        store.makerFace(maker.key) ?: maker.channel?.covers?.takeIf { it.isNotEmpty() }

    private suspend fun findShows(maker: Maker, countryCode: String): MakerShows {
        val channel = maker.channel
        if (channel != null && channel.showIds.isNotEmpty()) {
            val found = channel.showIds.chunked(LOOKUP_BATCH).flatMap { ids ->
                runCatching { catalog.lookupMany(ids.joinToString(","), countryCode) }
                    .getOrNull()?.results.orEmpty()
                    .mapNotNull { it.toMakerShow() }
            }.distinctBy { it.podcast.id }
                // Op Apple's volgorde, net als de hoezen van de verzamelaar.
                .sortedBy { show -> channel.showIds.indexOf(show.podcast.id).takeIf { it >= 0 } ?: Int.MAX_VALUE }
            if (found.isNotEmpty()) return MakerShows(found, complete = true)
        }
        return MakerShows(search.byMaker(maker.name, countryCode), complete = false)
    }

    /**
     * Waar de shows van deze maker staan in de lijst over alle categorieën van
     * [source] en [country]. Bij Apple op id; bij Spotify op titel, want Spotify
     * geeft geen Apple-id.
     */
    suspend fun placement(source: SourceId, country: Country, maker: Maker, shows: List<MakerShow>): Placement? {
        val query = ChartQuery(source, country, Catalog.defaultCategory, ChartLevel.SHOWS)
        val chart = runCatching { charts.chart(query) }.getOrNull() ?: charts.cached(query) ?: return null

        if (source == SourceId.APPLE) {
            val ids = shows.map { it.podcast.id }.toSet()
            val ranks = chart.entries.filter { it.id in ids }.associate { it.id to it.rank }
            return Placement(source, country, chart.entries.size, ranks, unmatched = 0)
        }

        val byTitle = shows.associateBy { titleKey(it.podcast.title) }
        val ranks = mutableMapOf<String, Int>()
        var unmatched = 0
        chart.entries.forEach { entry ->
            val show = byTitle[titleKey(entry.title)]
            when {
                show != null -> ranks.putIfAbsent(show.podcast.id, entry.rank)
                Makers.keyOf(entry.publisher) == maker.key -> unmatched++
            }
        }
        return Placement(source, country, chart.entries.size, ranks, unmatched)
    }

    /**
     * De lijst geteld per maker, met beweging ten opzichte van de makersranglijst
     * van een eerdere dag. Die ranglijst legt de app zelf vast, net als de
     * showlijsten: zonder gisteren is er geen pijl.
     */
    suspend fun rankMakers(chart: Chart): List<MakerRank> {
        val key = MAKERS_PREFIX + chart.query.key
        val rows = MakerRanking.rank(chart, directory(chart.query.country.code), store.baseline(key))
        // Een lijst uit de cache is niet van vandaag; die leggen we niet opnieuw vast.
        if (chart.cachedAt == null && rows.isNotEmpty()) store.record(key, MakerRanking.snapshot(rows))
        return rows.map { row -> face(row.maker)?.let { row.copy(artworks = it) } ?: row }
    }

    private fun titleKey(title: String) = title.lowercase().filter { it.isLetterOrDigit() }

    private companion object {
        /** De lookup van Apple lost er zoveel tegelijk op. */
        const val LOOKUP_BATCH = 150

        /** Voor de sleutel van de momentopnames: dezelfde lijst, maar dan per maker. */
        const val MAKERS_PREFIX = "MAKERS|"
    }
}

internal fun DatasetChannel.toChannel() = Channel(
    id = id,
    name = name,
    color = color?.removePrefix("#")?.takeIf { it.length == 6 },
    logoUrl = logo,
    url = url,
    showCount = showCount,
    showIds = shows,
    newShows = newShows.map {
        ChannelShow(it.id, it.title, it.artworkUrl, it.feedUrl, it.createdDate, it.trackCount)
    },
    covers = covers
)
