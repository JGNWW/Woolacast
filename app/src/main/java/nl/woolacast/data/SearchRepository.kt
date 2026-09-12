package nl.woolacast.data

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import nl.woolacast.data.apple.AppleCatalogApi
import nl.woolacast.domain.Episode
import nl.woolacast.domain.Podcast

data class SearchResults(
    val podcasts: List<Podcast> = emptyList(),
    val episodes: List<Episode> = emptyList()
) {
    val isEmpty: Boolean get() = podcasts.isEmpty() && episodes.isEmpty()
}

/** Zoekt in de publieke Apple-catalogus: podcasts en losse afleveringen naast elkaar. */
class SearchRepository(private val catalog: AppleCatalogApi) {

    /**
     * Alles van één maker. Apple kent geen lijst per uitgever, dus dit is een
     * zoekopdracht op zijn naam waar alleen de shows van blijven staan die
     * werkelijk van hem zijn. Een naam die de ander bevat telt mee: NPO Luister
     * geeft zijn shows nu eens uit als "BNNVARA" en dan als "NPO Luister /
     * BNNVARA", en dat is dezelfde maker.
     */
    suspend fun byMaker(publisher: String, countryCode: String, limit: Int = 60): List<Podcast> {
        val naam = publisher.trim()
        if (naam.length < 2) return emptyList()
        val found = runCatching { catalog.search(naam, countryCode, "podcast", limit) }
            .getOrNull()?.results.orEmpty()
        return found
            .filter { sameMaker(naam, it.artistName.orEmpty()) }
            .mapNotNull { result ->
                val id = result.collectionId?.toString() ?: return@mapNotNull null
                Podcast(
                    id = id,
                    title = result.collectionName ?: result.trackName.orEmpty(),
                    publisher = result.artistName.orEmpty(),
                    artworkUrl = result.artworkUrl600 ?: result.artworkUrl100,
                    description = Html.toPlainText(result.description),
                    genre = result.primaryGenreName,
                    episodeCount = result.trackCount,
                    feedUrl = result.feedUrl
                )
            }
            .distinctBy { it.id }
    }

    private fun sameMaker(wanted: String, found: String): Boolean {
        val a = wanted.lowercase().filter { it.isLetterOrDigit() }
        val b = found.lowercase().filter { it.isLetterOrDigit() }
        if (a.length < 3 || b.length < 3) return false
        // Korter dan vier tekens is te weinig om op te varen: "NPO" zit ook in
        // namen van makers die er niets mee te maken hebben.
        return a == b || (a.length >= 4 && b.contains(a)) || (b.length >= 4 && a.contains(b))
    }

    suspend fun search(term: String, countryCode: String): SearchResults = coroutineScope {
        val query = term.trim()
        if (query.length < 2) return@coroutineScope SearchResults()

        val shows = async {
            runCatching { catalog.search(query, countryCode, "podcast", 25) }.getOrNull()
        }
        val episodes = async {
            runCatching { catalog.search(query, countryCode, "podcastEpisode", 25) }.getOrNull()
        }

        SearchResults(
            podcasts = shows.await()?.results.orEmpty().mapNotNull { result ->
                val id = result.collectionId?.toString() ?: return@mapNotNull null
                Podcast(
                    id = id,
                    title = result.collectionName ?: result.trackName.orEmpty(),
                    publisher = result.artistName.orEmpty(),
                    artworkUrl = result.artworkUrl600 ?: result.artworkUrl100,
                    description = Html.toPlainText(result.description),
                    genre = result.primaryGenreName,
                    episodeCount = result.trackCount,
                    feedUrl = result.feedUrl
                )
            },
            episodes = episodes.await()?.results.orEmpty().mapNotNull { result ->
                val id = result.trackId?.toString() ?: return@mapNotNull null
                Episode(
                    id = id,
                    showId = result.collectionId?.toString().orEmpty(),
                    showTitle = result.collectionName.orEmpty(),
                    title = result.trackName ?: return@mapNotNull null,
                    description = Html.toPlainText(result.description),
                    artworkUrl = result.artworkUrl600 ?: result.artworkUrl100,
                    audioUrl = result.episodeUrl,
                    durationMillis = result.trackTimeMillis,
                    releaseDate = result.releaseDate?.take(10)
                )
            }
        )
    }
}
