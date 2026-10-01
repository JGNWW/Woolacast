package nl.woolacast.data.inbox

import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import nl.woolacast.data.feed.FeedClient
import nl.woolacast.data.feed.FeedFetch
import nl.woolacast.data.feed.ParsedFeed
import nl.woolacast.data.local.FeedCheck
import nl.woolacast.data.local.FollowedShow
import nl.woolacast.data.local.LocalStore
import nl.woolacast.data.local.SavedEpisode

/** De drie vaste chips boven Nieuw. Aan staan betekent: alleen wat hieraan voldoet. */
enum class NewFilter(val label: String) {
    UNSTARTED("Onbeluisterd"), SHORT("Kort < 30 min"), THIS_WEEK("Deze week")
}

/**
 * Wat "nieuw" is, op één plek. De lijst Nieuw, het getal "2 nieuw" onder een
 * show en de meldingen rekenen allemaal hiermee, zodat ze nooit iets anders
 * zeggen. Los van Android, zodat het te testen is.
 *
 * Nieuw is: verschenen ná de dag dat je de show ging volgen, in de laatste
 * [WINDOW_DAYS] dagen, niet uitgeluisterd en niet weggeveegd. Weggeveegd is
 * iets anders dan beluisterd: de aflevering blijft gewoon op de podcastpagina.
 */
object NewRules {

    /** Ouder dan dit is geen nieuws meer, ook als je hem nooit hoorde. */
    const val WINDOW_DAYS = 30L

    /** Zoveel van de nieuwste afleveringen per show onthoudt de ronde. */
    const val KEPT_PER_SHOW = 10

    /** "Alles in de wachtrij" zet er hooguit zoveel bij. */
    const val QUEUE_LIMIT = 20

    /** Een melding gaat alleen over wat zo recent verscheen; een feed die oude afleveringen terugzet, meldt niets. */
    const val NOTIFY_DAYS = 3L

    /** Vanaf welke dag een show nieuw telt. Wie volgde van vóór deze regel, telt vanaf zijn laatste bezoek. */
    fun since(show: FollowedShow, today: LocalDate): LocalDate =
        day(show.followedAt) ?: day(show.lastOpened) ?: today.minusDays(7)

    fun isNew(show: FollowedShow, episode: SavedEpisode, listened: Set<String>, hidden: Set<String>, today: LocalDate): Boolean {
        if (episode.id in listened || episode.id in hidden) return false
        val date = day(episode.releaseDate) ?: return false
        return date.isAfter(since(show, today)) && !date.isBefore(today.minusDays(WINDOW_DAYS))
    }

    /** Alles wat nieuw is, de nieuwste eerst. Binnen een dag houdt elke show de volgorde van zijn feed. */
    fun inbox(
        follows: List<FollowedShow>,
        checks: Map<String, FeedCheck>,
        listened: Set<String>,
        hidden: Set<String>,
        today: LocalDate
    ): List<SavedEpisode> =
        follows.flatMap { show ->
            checks[show.id]?.latest.orEmpty().filter { isNew(show, it, listened, hidden, today) }
        }.distinctBy { it.id }.sortedByDescending { it.releaseDate.orEmpty() }

    fun applyFilters(items: List<SavedEpisode>, filters: Set<NewFilter>, progress: Map<String, Long>, today: LocalDate) =
        items.filter { episode ->
            (NewFilter.UNSTARTED !in filters || (progress[episode.id] ?: 0L) == 0L) &&
                (NewFilter.SHORT !in filters || (episode.durationMillis ?: Long.MAX_VALUE) < 30 * 60_000L) &&
                (NewFilter.THIS_WEEK !in filters || day(episode.releaseDate)?.isBefore(today.minusDays(6)) == false)
        }

    /**
     * Wat er in deze ronde voor het eerst in de feed stond. De eerste keer dat
     * een show bekeken wordt, is er niets om mee te vergelijken: dan is niets
     * "binnengekomen", anders meldt het volgen van een show meteen zijn hele
     * achterstand.
     */
    fun arrivals(previous: FeedCheck?, latest: List<SavedEpisode>): List<SavedEpisode> {
        if (previous == null || previous.checkedAt.isEmpty()) return emptyList()
        val seen = previous.latest.map { it.id }.toSet()
        return latest.filter { it.id !in seen }
    }

    /** Waar een melding over moet: binnengekomen bij een show met de melding aan, nieuw, recent en nog niet gemeld. */
    fun toNotify(
        arrivals: Map<String, List<SavedEpisode>>,
        follows: List<FollowedShow>,
        notified: Set<String>,
        listened: Set<String>,
        hidden: Set<String>,
        today: LocalDate
    ): List<SavedEpisode> =
        follows.filter { it.notifyNew }.flatMap { show ->
            arrivals[show.id].orEmpty().filter { episode ->
                episode.id !in notified && isNew(show, episode, listened, hidden, today) &&
                    day(episode.releaseDate)?.isBefore(today.minusDays(NOTIFY_DAYS)) == false
            }
        }.distinctBy { it.id }.sortedByDescending { it.releaseDate.orEmpty() }

    /** Wat "Alles in de wachtrij" toevoegt: wat er nog niet in staat, hooguit [QUEUE_LIMIT]. */
    fun forQueue(items: List<SavedEpisode>, queued: Set<String>): List<SavedEpisode> =
        items.filter { it.id !in queued }.take(QUEUE_LIMIT)

    private fun day(iso: String?): LocalDate? =
        iso?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
}

/**
 * De feedronde: leest de feeds van alle gevolgde shows, hooguit vier tegelijk,
 * en onthoudt per show de nieuwste afleveringen. Voorwaardelijk: een feed die
 * niet veranderde is een 304 en kost niets. De achtergrondtaak (elke 6 uur) en
 * het verversen van de Bibliotheek gebruiken allebei deze ronde.
 */
class NewEpisodes(private val feeds: FeedClient, private val store: LocalStore) {

    /** Per show wat er deze ronde voor het eerst in de feed stond. */
    suspend fun refresh(shows: List<FollowedShow> = store.follows.value): Map<String, List<SavedEpisode>> = coroutineScope {
        val gate = Semaphore(PARALLEL)
        shows.filter { it.feedUrl != null }.map { show ->
            async { gate.withPermit { refreshOne(show) } }
        }.awaitAll().filterNotNull().toMap()
    }

    private suspend fun refreshOne(show: FollowedShow): Pair<String, List<SavedEpisode>>? {
        val feedUrl = show.feedUrl ?: return null
        val previous = store.feedCheck(show.id)
        val fetch = try {
            feeds.fetchIfChanged(feedUrl, previous?.etag, previous?.lastModified)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            return null // geen verbinding of een kapotte feed: de vorige stand blijft staan
        } catch (_: Exception) {
            return null // een feed die de parser niet snapt
        }
        val now = Instant.now().toString()
        return when (fetch) {
            FeedFetch.NotModified -> {
                store.putFeedCheck(show.id, (previous ?: FeedCheck()).copy(checkedAt = now))
                show.id to emptyList()
            }
            is FeedFetch.Fetched -> {
                val latest = latestOf(fetch.feed, show)
                store.putFeedCheck(show.id, FeedCheck(now, fetch.etag, fetch.lastModified, latest))
                show.id to NewRules.arrivals(previous, latest)
            }
        }
    }

    companion object {
        private const val PARALLEL = 4

        /** De nieuwste afleveringen met audio, zoals de wachtrij en de speler ze kennen. */
        fun latestOf(feed: ParsedFeed, show: FollowedShow): List<SavedEpisode> =
            feed.episodes.filter { it.audioUrl != null }
                .sortedByDescending { it.releaseDate.orEmpty() }
                .take(NewRules.KEPT_PER_SHOW)
                .map { parsed ->
                    SavedEpisode(
                        id = parsed.guid,
                        showId = show.id,
                        showTitle = feed.title?.takeIf { it.isNotBlank() } ?: show.title,
                        title = parsed.title,
                        artworkUrl = parsed.imageUrl ?: feed.imageUrl ?: show.artworkUrl,
                        audioUrl = parsed.audioUrl,
                        durationMillis = parsed.durationMillis,
                        releaseDate = parsed.releaseDate,
                        link = parsed.link,
                        chaptersUrl = parsed.chaptersUrl,
                        chapters = parsed.inlineChapters,
                        transcript = parsed.transcript
                    )
                }
    }
}
