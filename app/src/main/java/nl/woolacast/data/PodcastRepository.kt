package nl.woolacast.data

import nl.woolacast.data.apple.AppleCatalogApi
import nl.woolacast.data.apple.LookupResult
import nl.woolacast.domain.Episode
import nl.woolacast.domain.Podcast

data class PodcastDetail(val podcast: Podcast, val episodes: List<Episode>)

/**
 * Een enkele lookup levert de show plus zijn recente afleveringen, inclusief
 * de directe audio-URL. Die URL is wat de speler afspeelt.
 */
class PodcastRepository(private val catalog: AppleCatalogApi) {

    suspend fun detail(showId: String, countryCode: String, episodeLimit: Int = 50): PodcastDetail {
        val response = catalog.lookup(
            id = showId,
            country = countryCode,
            entity = "podcastEpisode",
            limit = episodeLimit
        )

        val show = response.results.firstOrNull { it.wrapperType == "track" }
            ?: error("Podcast $showId niet gevonden")

        val podcast = Podcast(
            id = showId,
            title = show.collectionName ?: show.trackName.orEmpty(),
            publisher = show.artistName.orEmpty(),
            artworkUrl = show.artworkUrl600 ?: show.artworkUrl100,
            description = show.description,
            genre = show.primaryGenreName,
            episodeCount = show.trackCount,
            feedUrl = show.feedUrl
        )

        val episodes = response.results
            .filter { it.wrapperType == "podcastEpisode" }
            .mapNotNull { it.toEpisode(showId, podcast.title) }

        return PodcastDetail(podcast, episodes)
    }

    private fun LookupResult.toEpisode(showId: String, showTitle: String): Episode? {
        val episodeId = trackId?.toString() ?: return null
        return Episode(
            id = episodeId,
            showId = showId,
            showTitle = collectionName ?: showTitle,
            title = trackName ?: return null,
            description = description,
            artworkUrl = artworkUrl600 ?: artworkUrl100,
            audioUrl = episodeUrl,
            durationMillis = trackTimeMillis,
            releaseDate = releaseDate
        )
    }
}
