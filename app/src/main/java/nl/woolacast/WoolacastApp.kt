package nl.woolacast

import android.app.Application
import android.content.Context
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import nl.woolacast.data.ChartRepository
import nl.woolacast.data.Network
import nl.woolacast.data.PodcastRepository
import nl.woolacast.data.SearchRepository
import nl.woolacast.data.apple.AppleChartSource
import nl.woolacast.data.apple.AppleGenreTree
import nl.woolacast.data.chapters.ChapterRepository
import nl.woolacast.data.download.Downloads
import nl.woolacast.data.opml.ShowImporter
import nl.woolacast.data.transcript.TranscriptRepository
import nl.woolacast.data.dataset.ChartsDataset
import nl.woolacast.data.feed.FeedCache
import nl.woolacast.data.feed.FeedClient
import nl.woolacast.data.local.toSaved
import nl.woolacast.domain.Episode
import nl.woolacast.data.inbox.NewEpisodes
import nl.woolacast.data.local.LocalStore
import nl.woolacast.data.maker.MakerCheckWorker
import nl.woolacast.data.maker.MakerRepository
import nl.woolacast.data.reco.RecoRepository
import nl.woolacast.data.tips.LiveTipsReader
import nl.woolacast.data.spotify.SpotifyChartSource
import nl.woolacast.player.PlayerController

/**
 * Handmatige bedrading in plaats van een DI-raamwerk: bij deze omvang is het
 * korter en scheelt het een annotatieprocessor in de build.
 */
class AppContainer(context: Context) {

    init {
        // Vóór het eerste verzoek: waar antwoorden bewaard mogen worden.
        if (Network.cacheDir == null) Network.cacheDir = context.cacheDir
    }

    private val marketingApi = Network.marketingApi()
    private val catalogApi = Network.catalogApi()
    private val spotifyApi = Network.spotifyChartsApi()
    private val feedClient = FeedClient(Network.client)

    val store = LocalStore(File(context.filesDir, "woolacast-store.json"))

    private val chartsDataset = ChartsDataset(Network.chartsDatasetApi())

    val chartRepository = ChartRepository(
        dataset = chartsDataset,
        sources = listOf(
            AppleChartSource(
                marketing = marketingApi,
                catalog = catalogApi,
                genreTree = AppleGenreTree(catalogApi),
                dataset = chartsDataset
            ),
            SpotifyChartSource(spotifyApi)
        ),
        store = store
    )

    /** De laatst gelezen feed per show, zodat een podcastpagina er meteen staat. */
    private val feedCache = FeedCache(File(context.cacheDir, "feeds"))

    val podcastRepository = PodcastRepository(catalogApi, feedClient, feedCache)

    val searchRepository = SearchRepository(catalogApi)

    /** Makers: kanalen uit de verzamelaar, shows uit de catalogus, plekken uit de lijsten. */
    val makerRepository = MakerRepository(chartsDataset, catalogApi, searchRepository, chartRepository, store)

    /** Leest de podcastrubrieken van de media zelf, om de tips vers te houden. */
    val liveTips = LiveTipsReader(feedClient, catalogApi)

    /** Voorstellen op grond van je eigen bibliotheek, gerekend op het toestel. */
    val reco = RecoRepository(chartRepository, catalogApi, store)

    val dataset: ChartsDataset get() = chartsDataset

    /** Afleveringen op het toestel. */
    val downloads: Downloads = Downloads(context, store, playingId = { player.state.value.episodeId })

    /** Hoofdstukken uit de feed, een JSON-bestand of de kop van het mp3-bestand. */
    val chapters = ChapterRepository(Network.client)

    /** Transcripties die de maker in de feed aanwijst. */
    val transcripts = TranscriptRepository(Network.client)

    /** Voor werk dat langer duurt dan een scherm, zoals het koppelen na een import. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Een OPML-bestand dat van buiten binnenkwam (delen of openen met Toadcast). */
    val incomingOpml = kotlinx.coroutines.flow.MutableStateFlow<android.net.Uri?>(null)

    /** OPML in en uit, en zelf een feed toevoegen. */
    val importer = ShowImporter(feedClient, catalogApi, store)

    /** De feedronde langs alle gevolgde shows, voor Nieuw, meldingen en automatisch downloaden. */
    val newEpisodes = NewEpisodes(feedClient, store)

    /** Uit een melding: een aflevering om meteen af te spelen, of de lijst Nieuw openen. */
    val incomingPlay = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val incomingOpenNew = kotlinx.coroutines.flow.MutableStateFlow(false)

    val player: PlayerController = PlayerController(
        context = context,
        resumePosition = { episodeId -> store.progress.value[episodeId] ?: 0L },
        onProgress = { episodeId, positionMs, durationMs, periodic ->
            store.rememberProgress(episodeId, positionMs, durationMs, whilePlaying = periodic)
        },
        nextInQueue = { store.queue.value.firstOrNull()?.toEpisode() },
        consumeQueued = { episodeId -> store.removeFromQueue(episodeId) },
        localUri = { episodeId -> downloads.localFile(episodeId)?.let { android.net.Uri.fromFile(it).toString() } },
        speedFor = { episode -> store.showPrefs.value[episode.showId]?.speed ?: store.playback.value.speed },
        onSpeedChanged = { episode, speed ->
            // Heeft de show een eigen snelheid, dan verandert die; anders de algemene.
            if (store.showPrefs.value[episode.showId]?.speed != null) store.setShowPrefs(episode.showId) { it.copy(speed = speed) }
            else store.setPlayback { it.copy(speed = speed) }
        },
        onStarted = { episode -> store.touchHistory(episode.toSaved()) },
        onListened = { episode, ms -> store.addListening(episode.showId, episode.showTitle, episode.artworkUrl, ms) },
        knownEpisode = ::knownEpisode
    )

    /** Een aflevering die ergens in de bibliotheek staat, op zijn id. */
    private fun knownEpisode(id: String): Episode? =
        (store.queue.value.firstOrNull { it.id == id }
            ?: store.saved.value.firstOrNull { it.id == id }
            ?: store.downloads.value[id]?.episode
            ?: store.history.value.lastOrNull { it.episode.id == id }?.episode
            ?: store.feedChecks.value.values.firstNotNullOfOrNull { check -> check.latest.firstOrNull { it.id == id } }
            ?: store.bookmarks.value.firstOrNull { it.episode.id == id }?.episode)?.toEpisode()
}

class WoolacastApp : Application() {

    lateinit var container: AppContainer
        private set

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            container.store.load()
            // Uitgeluisterde downloads van gisteren mogen weg; en de feedronde
            // (Nieuw, meldingen, automatisch downloaden) krijgt zijn vaste tijden.
            container.downloads.cleanUp()
            runCatching { Downloads.scheduleAuto(this@WoolacastApp, container.store.downloadSettings.value) }
        }
        // Een download die gewist werd terwijl hij speelde, gaat weg zodra er iets anders speelt.
        container.appScope.launch {
            container.player.state.map { it.episodeId }.distinctUntilChanged().drop(1)
                .collect { container.downloads.cleanUp() }
        }
        // De widget volgt wat er speelt; zonder widget op het startscherm doet dit niets.
        container.appScope.launch {
            container.player.state.map { Triple(it.episodeId, it.isPlaying, it.title) }.distinctUntilChanged()
                .collect { runCatching { nl.woolacast.widget.PlayerWidget.refresh(this@WoolacastApp) } }
        }
        // Casten: de Cast-omgeving laadt op de achtergrond. Zonder Google
        // Play-diensten lukt dat niet, en dan is er gewoon geen cast-knop.
        runCatching { androidx.media3.cast.Cast.getSingletonInstance(this).initialize() }
        // Meldingen over nieuwe podcasts van gevolgde makers. In een testomgeving
        // zonder WorkManager slaat dit stil over.
        runCatching { MakerCheckWorker.schedule(this) }
    }
}

val Context.container: AppContainer
    get() = (applicationContext as WoolacastApp).container
