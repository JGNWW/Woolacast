package nl.woolacast.data

import nl.woolacast.data.Html
import nl.woolacast.data.apple.AppleCatalogApi
import nl.woolacast.data.feed.FeedClient
import nl.woolacast.domain.Episode
import nl.woolacast.domain.Podcast

data class PodcastDetail(val podcast: Podcast, val episodes: List<Episode>)

/**
 * De podcastpagina komt uit de RSS van de maker zelf — daar staat alles in en
 * er zit niemand tussen. Alleen als we nog geen feed-URL hebben (Apple geeft
 * een catalogus-id, geen feed) is er één lookup nodig om die op te zoeken.
 */
class PodcastRepository(
    private val catalog: AppleCatalogApi,
    private val feedClient: FeedClient
) {

    suspend fun detail(
        showId: String,
        countryCode: String,
        feedUrl: String? = null,
        title: String? = null
    ): PodcastDetail {
        val resolvedFeed = feedUrl?.takeIf { it.isNotBlank() }
            ?: lookupFeedUrl(showId, countryCode)
            ?: searchFeedUrl(title, countryCode)
            ?: return lookupDetail(showId, countryCode)

        return runCatching { fromFeed(showId, resolvedFeed) }
            .getOrElse { lookupDetail(showId, countryCode) }
    }

    private suspend fun fromFeed(showId: String, feedUrl: String): PodcastDetail {
        val feed = feedClient.fetch(feedUrl)
        val title = feed.title.orEmpty()

        val podcast = Podcast(
            id = showId,
            title = title,
            publisher = feed.author.orEmpty(),
            artworkUrl = feed.imageUrl,
            description = feed.description,
            genre = null,
            episodeCount = feed.episodes.size,
            feedUrl = feedUrl
        )

        val episodes = feed.episodes.map { parsed ->
            Episode(
                id = parsed.guid,
                showId = showId,
                showTitle = title,
                title = parsed.title,
                description = parsed.description,
                artworkUrl = parsed.imageUrl ?: feed.imageUrl,
                audioUrl = parsed.audioUrl,
                durationMillis = parsed.durationMillis,
                releaseDate = parsed.releaseDate,
                link = parsed.link
            )
        }

        return PodcastDetail(podcast, episodes)
    }

    /**
     * Zoekt een aflevering uit een hitlijst op in de feed van de show, zodat hij
     * direct vanuit de lijst kan spelen. Lijsten geven geen audio-URL; de feed wel.
     * Vergelijkt op titel, want Apple's aflevering-id's staan niet in RSS.
     */
    suspend fun resolveEpisode(
        showId: String,
        countryCode: String,
        feedUrl: String?,
        showTitle: String?,
        episodeTitle: String
    ): Episode? {
        val detail = runCatching { detail(showId, countryCode, feedUrl, showTitle) }.getOrNull()
            ?: return null
        val wanted = normalise(episodeTitle)
        return detail.episodes.firstOrNull { normalise(it.title) == wanted }
            ?: detail.episodes.firstOrNull { normalise(it.title).contains(wanted) || wanted.contains(normalise(it.title)) }
    }

    /** Alleen de kop van een feed: wat is de nieuwste aflevering, en hoeveel zijn er. */
    suspend fun latestEpisodes(feedUrl: String, limit: Int = 10): List<Episode> =
        runCatching { fromFeed("", feedUrl).episodes.take(limit) }.getOrDefault(emptyList())

    private fun normalise(text: String) =
        text.lowercase().replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()

    /** Alleen voor Apple-ids: zoekt de feed-URL op in de publieke catalogus. */
    private suspend fun lookupFeedUrl(showId: String, countryCode: String): String? {
        if (!showId.all { it.isDigit() }) return null
        return runCatching {
            catalog.lookup(id = showId, country = countryCode)
                .results.firstOrNull()?.feedUrl
        }.getOrNull()
    }

    /**
     * Spotify geeft een eigen uri, geen feed. De naam is wat we wel hebben, dus
     * die zoeken we op in de publieke catalogus om alsnog bij de RSS te komen.
     */
    private suspend fun searchFeedUrl(title: String?, countryCode: String): String? {
        val term = title?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return runCatching {
            catalog.search(term = term, country = countryCode)
                .results.firstOrNull()?.feedUrl
        }.getOrNull()
    }

    /** Terugval als de feed niet te lezen is: dan maar de catalogusgegevens. */
    private suspend fun lookupDetail(showId: String, countryCode: String): PodcastDetail {
        require(showId.all { it.isDigit() }) { "Geen feed gevonden voor deze podcast." }
        val response = catalog.lookup(
            id = showId,
            country = countryCode,
            entity = "podcastEpisode",
            limit = 50
        )

        val show = response.results.firstOrNull { it.wrapperType == "track" }
            ?: error("Podcast $showId niet gevonden")

        val podcast = Podcast(
            id = showId,
            title = show.collectionName ?: show.trackName.orEmpty(),
            publisher = show.artistName.orEmpty(),
            artworkUrl = show.artworkUrl600 ?: show.artworkUrl100,
            description = Html.toPlainText(show.description),
            genre = show.primaryGenreName,
            episodeCount = show.trackCount,
            feedUrl = show.feedUrl
        )

        val episodes = response.results
            .filter { it.wrapperType == "podcastEpisode" }
            .mapNotNull { result ->
                val episodeId = result.trackId?.toString() ?: return@mapNotNull null
                Episode(
                    id = episodeId,
                    showId = showId,
                    showTitle = result.collectionName ?: podcast.title,
                    title = result.trackName ?: return@mapNotNull null,
                    description = Html.toPlainText(result.description),
                    artworkUrl = result.artworkUrl600 ?: result.artworkUrl100,
                    audioUrl = result.episodeUrl,
                    durationMillis = result.trackTimeMillis,
                    releaseDate = result.releaseDate?.take(10),
                    link = "https://podcasts.apple.com/$countryCode/podcast/id$showId?i=$episodeId"
                )
            }

        return PodcastDetail(podcast, episodes)
    }
}
