package nl.woolacast.data.reco

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import nl.woolacast.data.ChartRepository
import nl.woolacast.data.apple.AppleCatalogApi
import nl.woolacast.data.local.LocalStore
import nl.woolacast.domain.Catalog
import nl.woolacast.domain.Category
import nl.woolacast.domain.ChartEntry
import nl.woolacast.domain.ChartLevel
import nl.woolacast.domain.ChartQuery
import nl.woolacast.domain.SourceId

/**
 * Zoekt podcasts die bij je passen. Alles gebeurt op het toestel: het profiel
 * komt uit je eigen bibliotheek en de kandidaten komen uit de hitlijsten die de
 * app toch al ophaalt, dus er hoeft niets extra's opgevraagd te worden behalve
 * één opzoeking om te weten in welk genre jouw shows vallen.
 */
class RecoRepository(
    private val charts: ChartRepository,
    private val catalog: AppleCatalogApi,
    private val store: LocalStore
) {

    private val genreCache = mutableMapOf<String, String>()

    /** "Misschien vind je dit leuk", op basis van je bibliotheek. */
    suspend fun forLibrary(countryCode: String, limit: Int = 12): List<Suggestion> {
        val shows = library()
        if (shows.isEmpty()) return emptyList()
        val genres = genresOf(shows.map { it.id }, countryCode)
        val profile = Recommender.profile(shows) { genres[it] }
        if (profile.isEmpty) return emptyList()

        val categories = profile.topGenres(3).mapNotNull { categoryFor(it) }
            .ifEmpty { listOf(Catalog.defaultCategory) }
        val candidates = chartsFor(countryCode, categories)
        return Recommender.rank(profile, candidates, limit)
    }

    /** "Lijkt hierop", op de pagina van één podcast. */
    suspend fun similarTo(
        showId: String,
        title: String,
        publisher: String,
        genre: String?,
        description: String?,
        countryCode: String,
        limit: Int = 8
    ): List<Suggestion> {
        val category = genre?.let { categoryFor(it) } ?: Catalog.defaultCategory
        val uitLijst = chartsFor(countryCode, listOf(category, Catalog.defaultCategory))
        // Shows van dezelfde maker staan zelden samen in één lijst; die haalt
        // een zoekopdracht op naam er wel bij.
        val vanMaker = byPublisher(publisher, countryCode)
        return Recommender.similarTo(
            title = title,
            publisher = publisher,
            genre = genre,
            description = description,
            showId = showId,
            candidates = vanMaker + uitLijst,
            limit = limit
        )
    }

    /** Wat er in je bibliotheek staat: gevolgd weegt zwaarder dan bewaard. */
    private fun library(): List<LibraryShow> {
        val shows = mutableMapOf<String, LibraryShow>()
        for (follow in store.follows.value) {
            shows[follow.id] = LibraryShow(follow.id, follow.title, follow.publisher, weight = 3)
        }
        for (episode in store.saved.value + store.queue.value) {
            if (episode.showId.isBlank() || shows.containsKey(episode.showId)) continue
            shows[episode.showId] = LibraryShow(episode.showId, episode.showTitle, "", weight = 1)
        }
        return shows.values.toList()
    }

    /** Eén opzoeking voor de hele bibliotheek; Apple neemt tot vijftig ids tegelijk. */
    private suspend fun genresOf(ids: List<String>, countryCode: String): Map<String, String> {
        val missing = ids.filter { it.isNotBlank() && it !in genreCache }
        if (missing.isEmpty()) return genreCache
        withContext(Dispatchers.IO) {
            missing.chunked(40).forEach { groep ->
                runCatching { catalog.lookupMany(groep.joinToString(","), countryCode) }
                    .getOrNull()?.results.orEmpty()
                    .forEach { hit ->
                        val id = hit.collectionId?.toString() ?: return@forEach
                        hit.primaryGenreName?.let { genreCache[id] = it }
                    }
            }
        }
        return genreCache
    }

    private suspend fun chartsFor(
        countryCode: String,
        categories: List<Category>
    ): List<ChartEntry> = coroutineScope {
        val country = Catalog.country(countryCode)
        categories.distinct().map { category ->
            async {
                runCatching {
                    charts.chart(
                        ChartQuery(SourceId.APPLE, country, category, ChartLevel.SHOWS)
                    ).entries
                }.getOrDefault(emptyList())
            }
        }.awaitAll().flatten()
    }

    private suspend fun byPublisher(publisher: String, countryCode: String): List<ChartEntry> {
        if (publisher.isBlank()) return emptyList()
        return withContext(Dispatchers.IO) {
            runCatching {
                catalog.search(term = publisher, country = countryCode, limit = 12).results
            }.getOrDefault(emptyList()).mapIndexedNotNull { index, hit ->
                val id = hit.collectionId?.toString() ?: return@mapIndexedNotNull null
                ChartEntry(
                    rank = index + 1,
                    id = id,
                    title = hit.collectionName.orEmpty(),
                    publisher = hit.artistName.orEmpty(),
                    artworkUrl = hit.artworkUrl600 ?: hit.artworkUrl100,
                    genre = hit.primaryGenreName,
                    storeUrl = null,
                    feedUrl = hit.feedUrl
                )
            }
        }
    }

    /** Van Apple's genrenaam naar de categorie die wij kennen. */
    private fun categoryFor(genre: String): Category? {
        val plat = genre.lowercase()
        return Catalog.categories.firstOrNull { category ->
            val label = category.label.lowercase()
            label == plat || plat.startsWith(label.substringBefore(" ")) ||
                APPLE_NAMES[plat] == category.appleGenreId
        }
    }

    private companion object {
        /** Apple noemt zijn genres in het Engels; onze categorieën heten anders. */
        val APPLE_NAMES = mapOf(
            "comedy" to 1303, "news" to 1489, "true crime" to 1488,
            "society & culture" to 1324, "sports" to 1545, "business" to 1321,
            "science" to 1533, "history" to 1487, "health & fitness" to 1512,
            "arts" to 1301, "education" to 1304, "technology" to 1318,
            "music" to 1310, "fiction" to 1483, "leisure" to 1502,
            "kids & family" to 1305, "religion & spirituality" to 1314,
            "tv & film" to 1309, "government" to 1511
        )
    }
}
