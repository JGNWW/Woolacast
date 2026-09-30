package nl.woolacast.data.opml

import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import nl.woolacast.data.apple.AppleCatalogApi
import nl.woolacast.data.feed.FeedClient
import nl.woolacast.data.local.FollowedShow
import nl.woolacast.data.local.LocalStore

/** Hoe een feed uit het bestand het haalde. */
sealed interface ImportOutcome {
    val feed: OpmlFeed
    /** Nu gevolgd. [linked] als hij aan Apple's catalogus hangt, en dus in de lijsten kan staan. */
    data class Followed(override val feed: OpmlFeed, val show: FollowedShow, val linked: Boolean) : ImportOutcome
    /** Volgde je al. */
    data class Known(override val feed: OpmlFeed, val title: String) : ImportOutcome
    /** Niet te lezen: weg, verhuisd of geen podcastfeed. */
    data class Failed(override val feed: OpmlFeed, val reason: String) : ImportOutcome
}

/** Hoe ver het koppelen aan de catalogus is. */
data class LinkProgress(val done: Int = 0, val total: Int = 0, val running: Boolean = false)

/**
 * Brengt shows binnen uit een andere app, of één feed die je zelf plakt, en
 * schrijft ze weer uit als OPML.
 *
 * Een OPML-bestand kent alleen feed-adressen; Toadcast kent een show het liefst
 * aan zijn Apple-id, want daarmee staat hij in de hitlijsten. Volgen wacht daar
 * niet op: een show volgt eerst op zijn feed, en [link] koppelt hem daarna aan
 * de catalogus, rustig, één zoekopdracht per keer.
 */
class ShowImporter(
    private val feedClient: FeedClient,
    private val catalog: AppleCatalogApi,
    private val store: LocalStore
) {

    private val _linking = MutableStateFlow(LinkProgress())
    val linking: StateFlow<LinkProgress> = _linking.asStateFlow()
    private var linkJob: Job? = null

    /**
     * Koppelt op de achtergrond, in [scope] die langer leeft dan een scherm:
     * bij veertig shows duurt het een paar minuten. Loopt er al een ronde, dan
     * pakt die ook de nieuwe shows mee bij de volgende start.
     */
    fun startLinking(scope: CoroutineScope, countryCode: String) {
        if (linkJob?.isActive == true) return
        linkJob = scope.launch {
            val total = store.follows.value.count { it.id.startsWith(Opml.FEED_PREFIX) && it.feedUrl != null }
            _linking.value = LinkProgress(0, total, running = total > 0)
            var done = 0
            link(countryCode) { _, _ -> _linking.value = LinkProgress(++done, total, running = true) }
            _linking.value = LinkProgress(done, total, running = false)
        }
    }

    /** Leest elke feed (een paar tegelijk) en volgt wat werkt. */
    suspend fun import(feeds: List<OpmlFeed>, onChecked: (done: Int, total: Int) -> Unit): List<ImportOutcome> = coroutineScope {
        val known = store.follows.value.mapNotNull { show -> show.feedUrl?.let { Opml.key(it) to show } }.toMap()
        val gate = Semaphore(CONCURRENT)
        var done = 0
        val outcomes = feeds.map { feed ->
            async {
                gate.withPermit {
                    val outcome = known[Opml.key(feed.url)]?.let { ImportOutcome.Known(feed, it.title) } ?: check(feed)
                    synchronized(this@ShowImporter) { done++ }
                    onChecked(done, feeds.size)
                    outcome
                }
            }
        }.awaitAll()
        store.followAll(outcomes.filterIsInstance<ImportOutcome.Followed>().map { it.show })
        outcomes
    }

    /** Eén feed die je zelf invoert. Gooit een fout met een leesbare uitleg als het geen podcastfeed is. */
    suspend fun addFeed(url: String): FollowedShow {
        val clean = normaliseInput(url) ?: throw IOException("Dat is geen webadres. Een feed begint met https://.")
        store.follows.value.firstOrNull { it.feedUrl != null && Opml.key(it.feedUrl) == Opml.key(clean) }?.let { return it }
        return when (val outcome = check(OpmlFeed(clean, null))) {
            is ImportOutcome.Followed -> outcome.show.also { store.followAll(listOf(it)) }
            is ImportOutcome.Failed -> throw IOException(outcome.reason)
            is ImportOutcome.Known -> throw IOException("Die volg je al.")
        }
    }

    private suspend fun check(feed: OpmlFeed): ImportOutcome = withContext(Dispatchers.IO) {
        val parsed = runCatching { feedClient.fetch(feed.url) }
            .getOrElse { error ->
                return@withContext ImportOutcome.Failed(feed, reason(error))
            }
        if (parsed.episodes.none { it.audioUrl != null }) {
            return@withContext ImportOutcome.Failed(feed, "Geen afleveringen met audio")
        }
        val title = parsed.title ?: feed.title ?: feed.url
        ImportOutcome.Followed(
            feed,
            FollowedShow(
                id = Opml.feedId(feed.url),
                title = title,
                publisher = parsed.author.orEmpty(),
                artworkUrl = parsed.imageUrl,
                feedUrl = feed.url,
                lastOpened = LocalDate.now().toString()
            ),
            linked = false
        )
    }

    /**
     * Zoekt voor shows die alleen op hun feed gevolgd worden de Apple-id op:
     * een zoekopdracht op de naam, en alleen een treffer met precies dezelfde
     * feed telt. Apple staat ongeveer twintig zoekopdrachten per minuut toe,
     * vandaar de pauze. Geeft per show door of het lukte.
     */
    suspend fun link(countryCode: String, onLinked: (oldId: String, newId: String?) -> Unit = { _, _ -> }) {
        val pending = store.follows.value.filter { it.id.startsWith(Opml.FEED_PREFIX) && it.feedUrl != null }
        pending.forEachIndexed { index, show ->
            if (index > 0) delay(SEARCH_PAUSE_MS)
            val want = Opml.key(show.feedUrl!!)
            val hit = runCatching {
                catalog.search(term = show.title, country = countryCode, limit = 10).results
            }.getOrNull()?.firstOrNull { result ->
                result.feedUrl?.let(Opml::key) == want && result.collectionId != null
            }
            val newId = hit?.collectionId?.toString()
            if (newId != null) {
                store.relinkFollow(show.id, show.copy(id = newId, artworkUrl = hit.artworkUrl600 ?: show.artworkUrl))
            }
            onLinked(show.id, newId)
        }
    }

    /** Alle gevolgde shows met een feed, en apart wat niet mee kan. */
    fun exportable(): Pair<List<OpmlFeed>, List<FollowedShow>> {
        val (with, without) = store.follows.value.partition { !it.feedUrl.isNullOrBlank() }
        return with.map { OpmlFeed(it.feedUrl!!, it.title) } to without
    }

    private fun reason(error: Throwable): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("404") -> "Feed bestaat niet meer (404)"
            message.contains("410") -> "Feed is opgeheven (410)"
            message.contains("403") -> "Feed weigert ons (403)"
            error is java.net.UnknownHostException -> "Website niet gevonden"
            error is java.net.SocketTimeoutException -> "Geen antwoord (time-out)"
            error is org.xmlpull.v1.XmlPullParserException -> "Geen podcastfeed"
            message.startsWith("Feed gaf") -> message.replace("Feed gaf", "Server gaf")
            else -> "Niet te lezen"
        }
    }

    companion object {
        private const val CONCURRENT = 4
        private const val SEARCH_PAUSE_MS = 3_000L

        /** Wat iemand plakt: soms zonder https://, soms met spaties of als podcast://. */
        fun normaliseInput(raw: String): String? {
            var value = raw.trim()
            if (value.isEmpty() || value.contains(' ')) return null
            value = value.replaceFirst(Regex("^(itpc|pcast|podcast|feed)://", RegexOption.IGNORE_CASE), "https://")
            if (!value.startsWith("http://", true) && !value.startsWith("https://", true)) value = "https://$value"
            return value.takeIf { it.substringAfter("://").contains('.') }
        }
    }
}
