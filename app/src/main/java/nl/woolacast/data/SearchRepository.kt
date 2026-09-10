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
