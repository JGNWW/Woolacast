package nl.woolacast.data

import nl.woolacast.data.Html
import nl.woolacast.data.apple.AppleCatalogApi
import nl.woolacast.data.feed.CachedFeed
import nl.woolacast.data.feed.FeedCache
import nl.woolacast.data.feed.FeedClient
import nl.woolacast.data.feed.FeedFetch
import nl.woolacast.data.feed.ParsedFeed
import nl.woolacast.data.opml.Opml
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
    private val feedClient: FeedClient,
    /** De laatst gelezen feed per show; zonder is elke opening een volledige download. */
    private val cache: FeedCache? = null
) {

    /** Wat er van deze show bewaard is, zonder het net op te gaan. */
    suspend fun cached(showId: String): PodcastDetail? =
        cache?.get(showId)?.let { PodcastDetail(it.podcast, it.episodes) }

    /**
     * [maxAgeMs]: is de bewaarde feed zo kort geleden nog nagekeken, dan is hij
     * goed genoeg en gaat er niets het net op. Nul betekent: altijd navragen,
     * en dan nog alleen óf er iets veranderd is.
     */
    suspend fun detail(
        showId: String,
        countryCode: String,
        feedUrl: String? = null,
        title: String? = null,
        maxAgeMs: Long = 0L
    ): PodcastDetail {
        val kept = cache?.get(showId)
        if (kept != null && maxAgeMs > 0L && System.currentTimeMillis() - kept.checkedAtMs in 0 until maxAgeMs) {
            return PodcastDetail(kept.podcast, kept.episodes)
        }
        // Een show die alleen op zijn feed bestaat, zoeken we niet op naam op:
        // dat geeft een andere podcast met dezelfde titel.
        if (feedUrl.isNullOrBlank() && kept == null && showId.startsWith(Opml.FEED_PREFIX)) {
            throw IllegalStateException("Het feed-adres van deze show is niet meer bekend. Volg hem opnieuw via Bibliotheek → Je shows.")
        }
        // Het adres dat we al kennen scheelt een opzoeking bij Apple.
        val resolvedFeed = feedUrl?.takeIf { it.isNotBlank() }
            ?: kept?.feedUrl
            ?: lookupFeedUrl(showId, countryCode)
            ?: searchFeedUrl(title, countryCode)
            ?: return lookupDetail(showId, countryCode)

        val sameFeed = kept?.takeIf { it.feedUrl == resolvedFeed }
        val fetch = try {
            feedClient.fetchIfChanged(resolvedFeed, sameFeed?.etag, sameFeed?.lastModified)
        } catch (cancelled: kotlin.coroutines.cancellation.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            // Geen verbinding of een kapotte feed: liever de vorige stand dan niets.
            if (sameFeed != null) return PodcastDetail(sameFeed.podcast, sameFeed.episodes)
            return lookupDetail(showId, countryCode)
        }
        return when (fetch) {
            FeedFetch.NotModified -> {
                val same = sameFeed ?: return lookupDetail(showId, countryCode)
                cache?.put(showId, same.copy(checkedAtMs = System.currentTimeMillis()))
                PodcastDetail(same.podcast, same.episodes)
            }
            is FeedFetch.Fetched -> {
                val read = fromFeed(showId, resolvedFeed, fetch.feed)
                val detail = when {
                    !read.podcast.artworkUrl.isNullOrBlank() -> read
                    // De hoes die we eerder bij Apple haalden, hoeft niet opnieuw.
                    sameFeed?.podcast?.artworkUrl != null -> read.withArt(sameFeed.podcast.artworkUrl)
                    else -> withArtwork(read, showId, countryCode)
                }
                cache?.put(
                    showId,
                    CachedFeed(resolvedFeed, fetch.etag, fetch.lastModified, System.currentTimeMillis(), detail.podcast, detail.episodes)
                )
                detail
            }
        }
    }

    private fun PodcastDetail.withArt(art: String) = copy(
        podcast = podcast.copy(artworkUrl = art),
        episodes = episodes.map { if (it.artworkUrl.isNullOrBlank()) it.copy(artworkUrl = art) else it }
    )

    /**
     * Sommige feeds dragen geen plaatje. In de hitlijst staat er dan wel een,
     * want die komt van Apple; op de podcastpagina bleef een leeg vlak achter.
     * Eén opzoeking haalt het alsnog, en alleen in dat geval.
     */
    private suspend fun withArtwork(
        detail: PodcastDetail,
        showId: String,
        countryCode: String
    ): PodcastDetail {
        if (!showId.all { it.isDigit() }) return detail
        val hit = runCatching { catalog.lookup(id = showId, country = countryCode, limit = 1) }
            .getOrNull()?.results?.firstOrNull() ?: return detail
        val art = hit.artworkUrl600 ?: hit.artworkUrl100 ?: return detail
        return detail.withArt(art)
    }

    private suspend fun fromFeed(showId: String, feedUrl: String): PodcastDetail =
        fromFeed(showId, feedUrl, feedClient.fetch(feedUrl))

    private fun fromFeed(showId: String, feedUrl: String, feed: ParsedFeed): PodcastDetail {
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
                link = parsed.link,
                chaptersUrl = parsed.chaptersUrl,
                inlineChapters = parsed.inlineChapters,
                transcript = parsed.transcript
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
        val wanted = normalise(episodeTitle)
        fun find(detail: PodcastDetail?) = detail?.episodes?.let { episodes ->
            episodes.firstOrNull { normalise(it.title) == wanted }
                ?: episodes.firstOrNull { normalise(it.title).contains(wanted) || wanted.contains(normalise(it.title)) }
        }
        // Eerst wat er net nog gelezen is; pas als de aflevering daar niet in staat, de maker opnieuw vragen.
        val recent = runCatching { detail(showId, countryCode, feedUrl, showTitle, maxAgeMs = RECENT_MS) }.getOrNull()
        return find(recent) ?: find(runCatching { detail(showId, countryCode, feedUrl, showTitle) }.getOrNull())
    }

    /** Haalt de feed alvast binnen, zodat afspelen uit een lijst niet op de maker hoeft te wachten. */
    suspend fun prefetch(showId: String, countryCode: String, feedUrl: String?, showTitle: String?) {
        runCatching { detail(showId, countryCode, feedUrl, showTitle, maxAgeMs = RECENT_MS) }
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

    private companion object {
        /** Zo lang is een gelezen feed goed genoeg om een aflevering in te zoeken. */
        const val RECENT_MS = 30 * 60_000L
    }
}
