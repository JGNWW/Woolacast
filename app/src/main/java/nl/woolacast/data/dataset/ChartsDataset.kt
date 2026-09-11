package nl.woolacast.data.dataset

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import nl.woolacast.domain.Chart
import nl.woolacast.domain.ChartEntry
import nl.woolacast.domain.ChartLevel
import nl.woolacast.domain.ChartQuery
import nl.woolacast.domain.SourceId
import retrofit2.http.GET
import retrofit2.http.Url

@Serializable
data class DatasetEntry(
    val rank: Int = 0,
    val id: String = "",
    val title: String = "",
    val publisher: String = "",
    val artworkUrl: String? = null,
    val feedUrl: String? = null,
    val showId: String? = null,
    val durationMs: Long? = null,
    val releaseDate: String? = null,
    val previousRank: Int? = null,
    val move: Int? = null
)

@Serializable
data class DatasetChart(
    val source: String = "",
    val level: String = "",
    val country: String = "",
    val genreId: Int = 0,
    val genreLabel: String = "",
    val updated: String? = null,
    val count: Int = 0,
    val entries: List<DatasetEntry> = emptyList()
)

@Serializable
data class DatasetMover(
    val id: String = "",
    val title: String = "",
    val publisher: String = "",
    val artworkUrl: String? = null,
    val feedUrl: String? = null,
    val showId: String? = null,
    val rank: Int = 0,
    val previousRank: Int? = null,
    val move: Int = 0,
    val source: String = "",
    val level: String = "",
    val genreLabel: String = ""
)

@Serializable
data class DatasetMovers(
    val country: String = "",
    val updated: String? = null,
    val entries: List<DatasetMover> = emptyList()
)

/** Rang per dag per id: {"2026-09-10": {"1513807137": 3}}. */
@Serializable
data class DatasetHistory(
    val source: String = "",
    val country: String = "",
    val genreId: Int = 0,
    val level: String = "",
    val days: Map<String, Map<String, Int>> = emptyMap()
)

@Serializable
data class ShowRecord(
    /** [land, bron ("a"/"s"), rang] */
    val p: List<List<kotlinx.serialization.json.JsonPrimitive>> = emptyList(),
    /** De Spotify-uri, als die op naam gekoppeld kon worden. */
    val u: String? = null
)

data class ShowPosition(val country: String, val source: SourceId, val rank: Int)

data class ShowTracking(
    val positions: List<ShowPosition> = emptyList(),
    val spotifyUri: String? = null
)

interface ChartsDatasetApi {
    @GET
    suspend fun chart(@Url url: String): DatasetChart

    @GET
    suspend fun movers(@Url url: String): DatasetMovers

    @GET
    suspend fun history(@Url url: String): DatasetHistory

    @GET
    suspend fun shows(@Url url: String): Map<String, ShowRecord>

    @GET
    suspend fun tips(@Url url: String): MediaTips

    @GET
    suspend fun feeds(@Url url: String): TipFeeds
}

/**
 * Leest wat een klusje heeft verzameld: Apple's afleveringenlijst per categorie
 * (die de app zelf niet kan ophalen — elk id kost een aanroep) en de historie
 * die stijgers en de tracker mogelijk maakt. Historie kun je niet met
 * terugwerkende kracht bepalen; wie hem niet elke dag vastlegt, heeft hem niet.
 *
 * Alles is platte JSON in een publieke repo. Geen server, geen sleutel, geen
 * account. Staat er niets, dan werkt de app gewoon zonder.
 *
 * De verzamelaar staat in charts-service/.
 */
class ChartsDataset(
    private val api: ChartsDatasetApi,
    private val baseUrl: String = DEFAULT_BASE_URL
) {

    private val mutex = Mutex()
    private val charts = mutableMapOf<String, DatasetChart?>()
    private val histories = mutableMapOf<String, DatasetHistory?>()
    private val showShards = mutableMapOf<String, Map<String, ShowRecord>?>()
    private val movers = mutableMapOf<String, DatasetMovers?>()
    private val tips = mutableMapOf<String, MediaTips?>()
    private val feeds = mutableMapOf<String, TipFeeds?>()

    private fun ChartQuery.datasetPath(): String {
        val source = if (this.source == SourceId.SPOTIFY) "spotify" else "apple"
        val genre = category.appleGenreId ?: 26
        // Trending en Nieuw worden afgeleid uit de historie van de showlijst.
        val level = if (this.level == ChartLevel.EPISODES) "episodes" else "shows"
        return "$source/${country.code}/$genre/$level"
    }

    private suspend fun chartFor(query: ChartQuery): DatasetChart? {
        val path = query.datasetPath()
        return mutex.withLock {
            if (charts.containsKey(path)) charts[path]
            else runCatching { api.chart("$baseUrl/$path.json") }
                .getOrNull()
                .takeIf { it != null && it.entries.isNotEmpty() }
                .also { charts[path] = it }
        }
    }

    /**
     * Apple's echte afleveringenlijst per categorie, als die verzameld is.
     * Null betekent simpelweg: nog niet beschikbaar.
     */
    suspend fun episodesByCategory(query: ChartQuery): Chart? {
        val dataset = chartFor(query) ?: return null
        val entries = dataset.entries.map { entry ->
            ChartEntry(
                rank = entry.rank,
                id = entry.id,
                title = entry.title,
                publisher = entry.publisher,
                artworkUrl = entry.artworkUrl,
                genre = dataset.genreLabel,
                storeUrl = null,
                showId = entry.showId,
                feedUrl = entry.feedUrl,
                durationMillis = entry.durationMs,
                releaseDate = entry.releaseDate
            )
        }
        return Chart(
            query = query,
            entries = entries,
            updatedLabel = "Apple's eigen categorielijst · ${dataset.updated?.take(10).orEmpty()}".trim()
        )
    }

    /**
     * Apple's redactionele lijst "Nieuwe programma's" van het land, als het
     * klusje hem heeft opgehaald. Null: nog niet verzameld.
     */
    suspend fun newShows(countryCode: String): List<ChartEntry>? {
        val path = "apple/$countryCode/26/new"
        val dataset = mutex.withLock {
            if (charts.containsKey(path)) charts[path]
            else runCatching { api.chart("$baseUrl/$path.json") }
                .getOrNull()
                .takeIf { it != null && it.entries.isNotEmpty() }
                .also { charts[path] = it }
        } ?: return null
        return dataset.entries.map { entry ->
            ChartEntry(
                rank = entry.rank, id = entry.id, title = entry.title, publisher = entry.publisher,
                artworkUrl = entry.artworkUrl, genre = null, storeUrl = null,
                showId = entry.id, feedUrl = entry.feedUrl, releaseDate = entry.releaseDate
            )
        }
    }

    /**
     * Per id: de dag waarop hij de lijst binnenkwam, afgeleid uit de bewaarde
     * dagen — de dag na de laatste dag waarop hij ontbrak. Wie er alle bewaarde
     * dagen al stond, staat er niet in. Alleen mogelijk vanaf twee dagen.
     */
    suspend fun enteredOn(query: ChartQuery): Map<String, String> {
        val days = history(query)?.days?.toSortedMap() ?: return emptyMap()
        if (days.size < 2) return emptyMap()
        val ordered = days.keys.toList()
        val latest = days.getValue(ordered.last())
        return latest.keys.mapNotNull { id ->
            val lastAbsent = ordered.dropLast(1).lastOrNull { day -> !days.getValue(day).containsKey(id) }
                ?: return@mapNotNull null
            val entered = ordered[ordered.indexOf(lastAbsent) + 1]
            id to entered
        }.toMap()
    }

    /**
     * Beweging per id, om de eigen metingen aan te vullen zolang die er nog niet
     * zijn. Komt uit de twee laatste vastgelegde dagen; met één dag valt er nog
     * niets te zeggen.
     */
    suspend fun moves(query: ChartQuery): Map<String, Int> {
        val days = history(query)?.days ?: return emptyMap()
        if (days.size < 2) return emptyMap()

        val ordered = days.keys.sorted()
        val today = days.getValue(ordered.last())
        val before = days.getValue(ordered[ordered.size - 2])

        return today.mapNotNull { (id, rank) ->
            before[id]?.let { previous -> id to (previous - rank) }
        }.toMap()
    }

    /**
     * Podcasttips uit de media van dit land. Leeg betekent: nog niet
     * verzameld, of geen medium in dit land met een leesbare rubriek.
     */
    suspend fun tips(countryCode: String): MediaTips? = mutex.withLock {
        if (tips.containsKey(countryCode)) return@withLock tips[countryCode]
        runCatching { api.tips("$baseUrl/tips/$countryCode.json") }
            .getOrNull()
            ?.let { loaded ->
                // Het logo staat als bestandsnaam in de gegevens; hier wordt
                // het een adres, zodat de app niet bij de uitgever hoeft aan
                // te kloppen om te weten hoe een medium eruitziet.
                loaded.copy(entries = loaded.entries.map { tip ->
                    if (tip.logo == null) tip else tip.copy(logo = "$baseUrl/logos/${tip.logo}")
                })
            }
            .takeIf { it != null && it.entries.isNotEmpty() }
            .also { tips[countryCode] = it }
    }

    /**
     * De feeds die de app zelf mag lezen, per land. Hiermee blijven de tips
     * vers tussen twee ronden van de verzamelaar door.
     */
    suspend fun feeds(countryCode: String): TipFeeds = mutex.withLock {
        feeds[countryCode]?.let { return@withLock it }
        val loaded = runCatching { api.feeds("$baseUrl/feeds/$countryCode.json") }
            .getOrNull() ?: TipFeeds(country = countryCode)
        val resolved = loaded.copy(entries = loaded.entries.map { feed ->
            if (feed.logo == null) feed else feed.copy(logo = "$baseUrl/logos/${feed.logo}")
        })
        feeds[countryCode] = resolved
        resolved
    }

    /** De tips over één show, om ze op de podcastpagina te tonen. */
    suspend fun tipsFor(showId: String, countryCode: String): List<MediaTip> =
        tips(countryCode)?.entries?.filter { it.showId == showId }.orEmpty()

    suspend fun movers(countryCode: String): List<DatasetMover> = mutex.withLock {
        val cached = movers[countryCode]
        if (movers.containsKey(countryCode)) return@withLock cached?.entries.orEmpty()
        runCatching { api.movers("$baseUrl/movers/$countryCode.json") }
            .getOrNull()
            .also { movers[countryCode] = it }
            ?.entries.orEmpty()
    }

    suspend fun history(query: ChartQuery): DatasetHistory? {
        val path = query.datasetPath()
        return mutex.withLock {
            if (histories.containsKey(path)) histories[path]
            else runCatching { api.history("$baseUrl/$path.history.json") }
                .getOrNull()
                .also { histories[path] = it }
        }
    }

    /**
     * Waar een show noteert, over landen en bronnen heen. Het register is
     * verdeeld over honderd scherven op de laatste twee cijfers van het id, dus
     * dit haalt een paar kilobyte op in plaats van alles.
     */
    suspend fun tracking(appleShowId: String): ShowTracking? {
        if (appleShowId.length < 2 || !appleShowId.all { it.isDigit() }) return null
        val shard = appleShowId.takeLast(2)

        val records = mutex.withLock {
            if (showShards.containsKey(shard)) showShards[shard]
            else runCatching { api.shows("$baseUrl/shows/$shard.json") }
                .getOrNull()
                .also { showShards[shard] = it }
        } ?: return null

        val record = records[appleShowId] ?: return null
        val positions = record.p.mapNotNull { row ->
            if (row.size < 3) return@mapNotNull null
            val rank = row[2].content.toIntOrNull() ?: return@mapNotNull null
            ShowPosition(
                country = row[0].content,
                source = if (row[1].content == "s") SourceId.SPOTIFY else SourceId.APPLE,
                rank = rank
            )
        }
        return ShowTracking(positions, record.u)
    }

    /** Rang per dag voor één show in één land, om een lijn van te tekenen. */
    suspend fun rankHistory(
        source: SourceId,
        countryCode: String,
        showId: String
    ): List<Pair<String, Int>> {
        val folder = if (source == SourceId.SPOTIFY) "spotify" else "apple"
        val path = "$folder/$countryCode/26/shows"
        val history = mutex.withLock {
            if (histories.containsKey(path)) histories[path]
            else runCatching { api.history("$baseUrl/$path.history.json") }
                .getOrNull()
                .also { histories[path] = it }
        } ?: return emptyList()

        return history.days.toSortedMap().mapNotNull { (day, ranks) ->
            ranks[showId]?.let { day to it }
        }
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
