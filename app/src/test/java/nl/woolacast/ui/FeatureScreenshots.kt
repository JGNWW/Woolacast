package nl.woolacast.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.Coil
import coil.ImageLoader
import coil.decode.DataSource
import coil.intercept.Interceptor
import coil.request.ImageResult
import coil.request.SuccessResult
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import java.io.File
import java.io.IOException
import java.lang.reflect.Proxy
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import nl.woolacast.data.PodcastRepository
import nl.woolacast.data.apple.AppleCatalogApi
import nl.woolacast.data.apple.LookupResponse
import nl.woolacast.data.apple.LookupResult
import nl.woolacast.data.dataset.ChartsDataset
import nl.woolacast.data.dataset.ChartsDatasetApi
import nl.woolacast.data.download.DownloadProgress
import nl.woolacast.data.download.Downloads
import nl.woolacast.data.feed.FeedClient
import nl.woolacast.data.local.DownloadRecord
import nl.woolacast.data.local.DownloadState
import nl.woolacast.data.local.LocalStore
import nl.woolacast.data.local.SavedEpisode
import nl.woolacast.data.maker.MakerRepository
import nl.woolacast.data.opml.ShowImporter
import nl.woolacast.data.transcript.TranscriptParser
import nl.woolacast.domain.Chapter
import nl.woolacast.domain.Episode
import nl.woolacast.domain.TranscriptRef
import nl.woolacast.player.PlaybackState
import nl.woolacast.ui.common.DownloadUi
import nl.woolacast.ui.detail.DetailScreen
import nl.woolacast.ui.detail.DetailViewModel
import nl.woolacast.ui.library.DownloadsViewModel
import nl.woolacast.ui.library.ImportScreen
import nl.woolacast.ui.library.ImportViewModel
import nl.woolacast.ui.library.LibraryScreen
import nl.woolacast.ui.library.LibraryViewModel
import nl.woolacast.ui.player.PlayerScreen
import nl.woolacast.ui.player.TranscriptLoad
import nl.woolacast.ui.theme.WoolacastTheme
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Schermafbeeldingen van downloaden, hoofdstukken, meelezen en OPML, in de
 * echte schermen. Draait alleen met -Pscreenshots; de PNG's komen in
 * app/build/screenshots. Alle shows en afleveringen zijn verzonnen.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xxhdpi")
class FeatureScreenshots {

    @get:Rule
    val compose = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val outDir by lazy { File(System.getProperty("woolacast.screenshotDir") ?: "build/screenshots").apply { mkdirs() } }

    @Before
    fun setUp() {
        assumeTrue("alleen met -Pscreenshots", System.getProperty("woolacast.screenshots") == "true")
        Coil.setImageLoader(ImageLoader.Builder(context).components { add(Covers(context)) }.build())
    }

    /* ---- speler: hoofdstukken en tekst ---- */

    private val chapters = listOf(
        Chapter(0, "Opening en nieuws van de week"),
        Chapter(6 * 60_000L + 12_000L, "Gast: Sanne Kuipers over de nieuwe zaal"),
        Chapter(24 * 60_000L + 40_000L, "Live: Stadslicht met band"),
        Chapter(38 * 60_000L + 5_000L, "De vraag van de luisteraar"),
        Chapter(52 * 60_000L + 30_000L, "Wat we volgende week doen")
    )

    private val transcript = TranscriptParser.parse(
        """
        WEBVTT

        00:28:40.000 --> 00:28:46.000
        <v Sanne>De nieuwe zaal gaat pas in maart open, dus dit is echt de laatste keer hier.

        00:28:46.500 --> 00:28:49.000
        <v Joost>En dan de band. Zijn jullie er klaar voor?

        00:28:49.500 --> 00:28:58.000
        <v Joost>Dit nummer schreven we in de oude zaal, op de avond dat het dak lekte.

        00:28:58.500 --> 00:29:01.000
        <v Joost>Het heet Stadslicht.

        00:29:01.500 --> 00:29:08.000
        <v Sanne>Ik weet nog dat we emmers neerzetten tussen het publiek.

        00:29:08.500 --> 00:29:15.000
        <v Sanne>Er stond water tot aan de enkels bij de garderobe, en niemand ging naar huis.
        """.trimIndent(),
        "text/vtt"
    )

    private fun playing(position: Long) = PlaybackState(
        episode = Episode(
            id = "deadline-1", showId = "deadline", showTitle = "De Deadline", title = "Live vanuit Paradiso",
            description = null, artworkUrl = "https://test.local/rood.png", audioUrl = "https://test.local/a.mp3",
            durationMillis = 71 * 60_000L, releaseDate = "2026-09-08",
            transcript = TranscriptRef("https://test.local/a.vtt", "text/vtt")
        ),
        isPlaying = true,
        positionMs = position,
        durationMs = 71 * 60_000L,
        speed = 1.2f
    )

    @Test fun spelerDonker() = speler(dark = true)
    @Test fun spelerLicht() = speler(dark = false)

    private fun speler(dark: Boolean) {
        val suffix = if (dark) "-donker" else "-licht"
        var state by mutableStateOf(playing(29 * 60_000L + 8_000L))
        compose.setContent {
            WoolacastTheme(darkTheme = dark) {
                PlayerScreen(
                    state = state, queue = emptyList(), isSaved = false,
                    onCollapse = {}, onTogglePlay = {}, onSeekTo = {}, onSeekBy = {}, onPrevious = {}, onNext = {},
                    onSpeed = {}, onSleep = {}, onToggleSave = {}, onPlayQueued = {}, onRemoveQueued = {},
                    onOpenPodcast = {}, onDismissError = {},
                    chapters = chapters, hasTranscript = true,
                    transcript = TranscriptLoad.Ready(transcript),
                    download = DownloadUi.Done
                )
            }
        }
        settle()
        save("speler-hoofdstukken$suffix")
        compose.onAllNodesWithText("3/5")[0].performClick()
        settle()
        shot("hoofdstukken-lijst$suffix")
        compose.onAllNodesWithText("Live: Stadslicht met band")[1].performClick()
        settle()
        compose.onAllNodesWithText("Timer")[0].performClick()
        settle()
        shot("slaaptimer-hoofdstuk$suffix")
        compose.onAllNodesWithText("Einde hoofdstuk")[0].performClick()
        settle()
        compose.onAllNodesWithText("Tekst")[0].performClick()
        settle()
        shot("tekst-meelezen$suffix")
        if (!dark) {
            compose.onAllNodesWithContentDescription("Zoek in de tekst")[0].performTextInput("zaal")
            settle()
            shot("tekst-zoeken$suffix")
        }
    }

    @Test
    fun tekstZonderTijden() {
        val html = TranscriptParser.parse("<p>Welkom bij De Deadline.</p><p>Vandaag spelen we live vanuit Paradiso.</p>", "text/html")
        compose.setContent {
            WoolacastTheme(darkTheme = false) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLowest)) {
                    nl.woolacast.ui.player.TranscriptView(
                        state = playing(60_000L), load = TranscriptLoad.Ready(html),
                        onSeekToMs = {}, onTogglePlay = {}, onSeekBy = {}, onRetry = {}, onClose = {}
                    )
                }
            }
        }
        settle()
        save("tekst-zonder-tijden")
    }

    /* ---- podcastpagina met downloads en tekst ---- */

    @Test fun podcastpaginaLicht() = podcastpagina(dark = false)
    @Test fun podcastpaginaDonker() = podcastpagina(dark = true)

    private fun podcastpagina(dark: Boolean) {
        val suffix = if (dark) "-donker" else "-licht"
        val store = store()
        runBlocking {
            store.putDownload(record("deadline-1", DownloadState.DONE, bytes = 58L * 1024 * 1024))
            store.putDownload(record("deadline-2", DownloadState.QUEUED))
            store.putDownload(record("deadline-4", DownloadState.FAILED).copy(error = "De server gaf 404."))
            store.toggleFollow(nl.woolacast.data.local.FollowedShow("deadline", "De Deadline", "Dagblad Noord", "https://test.local/rood.png", "https://test.local/deadline.xml"))
            store.setAutoDownload("deadline", 2)
        }
        val downloads = Downloads(context, store)
        downloads.report("deadline-2", DownloadProgress(38L * 1024 * 1024, 58L * 1024 * 1024))
        val model = DetailViewModel(
            showId = "deadline", countryCode = "nl", feedUrl = "https://test.local/deadline.xml", title = null,
            repository = PodcastRepository(noCatalog(), FeedClient(feeds())), store = store, downloads = downloads
        )
        compose.setContent {
            WoolacastTheme(darkTheme = dark) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    DetailScreen(viewModel = model, playingId = null, onBack = {}, onPlay = { _, _ -> }, onOpenTracker = {}, onOpenMaker = {})
                }
            }
        }
        settle { !model.state.value.loading }
        compose.onAllNodesWithText("Live vanuit Paradiso")[0].performScrollTo()
        settle()
        save("podcast-afleveringen$suffix")
        compose.onAllNodesWithText("Live vanuit Paradiso")[0].performClick()
        settle()
        shot("aflevering-blad$suffix")
        if (!dark) {
            compose.onAllNodesWithText("Het archief in Assen")[0].performClick()
            settle()
            shot("aflevering-blad-downloaden")
        }
    }

    @Test
    fun automatischDownloaden() {
        val store = store()
        runBlocking { store.setAutoDownload("deadline", 2) }
        val model = DetailViewModel(
            showId = "deadline", countryCode = "nl", feedUrl = "https://test.local/deadline.xml", title = null,
            repository = PodcastRepository(noCatalog(), FeedClient(feeds())), store = store
        )
        compose.setContent {
            WoolacastTheme(darkTheme = false) {
                DetailScreen(viewModel = model, playingId = null, onBack = {}, onPlay = { _, _ -> }, onOpenTracker = {}, onOpenMaker = {})
            }
        }
        settle { !model.state.value.loading }
        compose.onAllNodesWithContentDescription("Meer")[0].performClick()
        settle()
        compose.onAllNodesWithText("Automatisch downloaden: nieuwste 2")[0].performClick()
        settle()
        shot("automatisch-downloaden")
    }

    /* ---- bibliotheek: gedownload, je shows, importeren ---- */

    @Test fun bibliotheekLicht() = bibliotheek(dark = false, shows = false)
    @Test fun bibliotheekDonker() = bibliotheek(dark = true, shows = false)
    @Test fun jeShows() = bibliotheek(dark = false, shows = true)

    private fun bibliotheek(dark: Boolean, shows: Boolean) {
        val suffix = if (dark) "-donker" else "-licht"
        val store = store()
        runBlocking {
            store.putDownload(record("koud-1", DownloadState.DONE, bytes = 61L * 1024 * 1024, show = "Koud Spoor", art = "blauwgroen", auto = true, title = "Het mes in de polder"))
            store.putDownload(record("nacht-1", DownloadState.DONE, bytes = 42L * 1024 * 1024, show = "Nachtdienst", art = "amber", auto = true, title = "De nachtbus naar huis"))
            store.putDownload(record("deadline-1", DownloadState.DONE, bytes = 58L * 1024 * 1024))
            store.putDownload(record("deadline-2", DownloadState.QUEUED, title = "Kaarten die niet kloppen"))
            store.putDownload(record("deadline-3", DownloadState.QUEUED, title = "Het archief in Assen"))
            store.putDownload(record("deadline-4", DownloadState.FAILED, title = "Een brief uit 1975").copy(error = "De server gaf 404."))
            store.setAutoDownload("koud", 1)
            store.setAutoDownload("nacht", 1)
            store.setDownloadSettings(store.downloadSettings.value.copy(limitMb = 1024))
            store.toggleFollow(nl.woolacast.data.local.FollowedShow("deadline", "De Deadline", "Dagblad Noord", "https://test.local/rood.png", "https://test.local/deadline.xml"))
            store.toggleFollow(nl.woolacast.data.local.FollowedShow("spot", "Alleen Spotify", "Iemand", null, null))
        }
        val downloads = Downloads(context, store)
        downloads.report("deadline-2", DownloadProgress(38L * 1024 * 1024, 58L * 1024 * 1024))
        val library = LibraryViewModel(store, dataset(), PodcastRepository(noCatalog(), FeedClient(feeds())), makers(store),
            ShowImporter(FeedClient(feeds()), noCatalog(), store))
        val downloadsModel = DownloadsViewModel(store, downloads)
        compose.setContent {
            WoolacastTheme(darkTheme = dark) {
                LibraryScreen(
                    viewModel = library, countryCode = "nl", playingId = null,
                    onOpenPodcast = { _, _, _ -> }, onSearch = {}, onPlay = {},
                    downloadsViewModel = downloadsModel
                )
            }
        }
        settle()
        if (shows) {
            compose.onAllNodesWithContentDescription("Shows importeren of exporteren")[0].performClick()
            settle()
            shot("je-shows")
            compose.onAllNodesWithText("Voeg een feed toe")[0].performClick()
            // De cursor knippert; wachten tot alles stilstaat lukt dan nooit.
            compose.mainClock.autoAdvance = false
            repeat(10) { compose.mainClock.advanceTimeBy(100); Thread.sleep(50) }
            shot("feed-toevoegen")
            return
        }
        compose.onAllNodesWithText("Gedownload")[0].performScrollTo().performClick()
        settle()
        save("bibliotheek-gedownload$suffix")
        compose.onAllNodesWithText("Instellen")[0].performClick()
        settle()
        shot("download-instellingen$suffix")
    }

    @Test
    fun importeren() {
        val store = store()
        val file = File.createTempFile("pocketcasts-export", ".opml").apply {
            writeText(
                """<?xml version="1.0"?><opml version="1.0"><head><title>Pocket Casts Feeds</title></head><body><outline text="feeds">
                <outline type="rss" text="De Deadline" xmlUrl="https://test.local/deadline.xml" />
                <outline type="rss" text="Diepzee" xmlUrl="https://test.local/diepzee.xml" />
                <outline type="rss" text="Goud van Oud" xmlUrl="https://test.local/goud.xml" />
                <outline type="rss" text="Radio Oost Nazit" xmlUrl="https://radiooost.test/nazit.xml" />
                <outline type="rss" text="Lichtkogel Live" xmlUrl="https://lichtkogel.test/rss" />
                </outline></body></opml>"""
            )
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val importer = ShowImporter(FeedClient(feeds()), catalog(), store)
        val model = ImportViewModel(Uri.fromFile(file), context.contentResolver, importer, store, "nl", scope)
        compose.setContent {
            WoolacastTheme(darkTheme = false) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    ImportScreen(viewModel = model, onBack = {}, onDone = {})
                }
            }
        }
        settle { model.ui.value is nl.woolacast.ui.library.ImportUi.Done }
        save("import-bezig")
        // Koppelen gaat met een pauze tussen de zoekopdrachten; wacht tot het klaar is.
        repeat(200) {
            if (!importer.linking.value.running && importer.linking.value.total > 0) return@repeat
            Thread.sleep(100)
        }
        settle()
        save("import-klaar")
    }

    /* ---- nepdata ---- */

    private fun store() = LocalStore(File.createTempFile("store", ".json").apply { delete() }).also { runBlocking { it.load() } }

    private fun record(
        id: String, state: DownloadState, bytes: Long = 0L, show: String = "De Deadline", art: String = "rood",
        auto: Boolean = false, title: String = "Live vanuit Paradiso"
    ) = DownloadRecord(
        episode = SavedEpisode(id, id.substringBefore('-'), show, title, "https://test.local/$art.png", "https://test.local/$id.mp3", 62 * 60_000L, "2026-09-08"),
        fileName = "$id.mp3", state = state, bytes = bytes, auto = auto, addedAt = Instant.now().toString()
    )

    private fun noCatalog(): AppleCatalogApi = Proxy.newProxyInstance(
        AppleCatalogApi::class.java.classLoader, arrayOf(AppleCatalogApi::class.java)
    ) { _, _, _ -> throw IOException("geen netwerk in de test") } as AppleCatalogApi

    /** Kent twee van de drie geïmporteerde shows onder dezelfde feed. */
    private fun catalog(): AppleCatalogApi = Proxy.newProxyInstance(
        AppleCatalogApi::class.java.classLoader, arrayOf(AppleCatalogApi::class.java)
    ) { _, method, args ->
        if (method.name != "search") throw IOException("geen netwerk in de test")
        val term = (args[0] as String)
        val hits = mapOf("De Deadline" to 3001L, "Diepzee" to 3002L)
        LookupResponse(results = hits.filterKeys { term.contains(it) }.map { (title, id) ->
            LookupResult(wrapperType = "track", kind = "podcast", collectionId = id, collectionName = title,
                feedUrl = "https://test.local/${if (id == 3001L) "deadline" else "diepzee"}.xml")
        })
    } as AppleCatalogApi

    private fun dataset() = ChartsDataset(Proxy.newProxyInstance(
        ChartsDatasetApi::class.java.classLoader, arrayOf(ChartsDatasetApi::class.java)
    ) { _, _, _ -> throw IOException("geen netwerk in de test") } as ChartsDatasetApi)

    private fun makers(store: LocalStore): MakerRepository {
        val catalog = noCatalog()
        val search = nl.woolacast.data.SearchRepository(catalog)
        val charts = nl.woolacast.data.ChartRepository(sources = emptyList(), store = store)
        return MakerRepository(dataset(), catalog, search, charts, store)
    }

    private val episodes = listOf("Live vanuit Paradiso", "Kaarten die niet kloppen", "Het archief in Assen", "Een brief uit 1975", "Wie tekende de grens?")

    /** Een feed per testshow; twee adressen geven een fout, zoals in het echt. */
    private fun feeds() = OkHttpClient.Builder().addInterceptor { chain ->
        val url = chain.request().url.toString()
        val body = when {
            "radiooost" in url -> return@addInterceptor response(chain, 404, "")
            "lichtkogel" in url -> throw java.net.SocketTimeoutException("time-out")
            "diepzee" in url -> rss("Diepzee", "Getij Media", "blauwgroen", "diepzee")
            "goud" in url -> rss("Goud van Oud", "Radio Zolder", "amber", "goud")
            else -> rss("De Deadline", "Dagblad Noord", "rood", "deadline")
        }
        response(chain, 200, body)
    }.build()

    private fun response(chain: okhttp3.Interceptor.Chain, code: Int, body: String) = Response.Builder()
        .request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message(if (code == 200) "OK" else "Not Found")
        .body(body.toResponseBody("application/rss+xml".toMediaType())).build()

    private fun rss(title: String, author: String, art: String, key: String) = buildString {
        append("""<?xml version="1.0" encoding="UTF-8"?><rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd" xmlns:podcast="https://podcastindex.org/namespace/1.0"><channel>""")
        append("<title>$title</title><itunes:author>$author</itunes:author>")
        append("<description>Een verzonnen testpodcast.</description>")
        append("""<itunes:image href="https://test.local/$art.png"/>""")
        episodes.forEachIndexed { i, episode ->
            append("<item><title>$episode</title><guid>$key-${i + 1}</guid>")
            val day = java.time.LocalDate.of(2026, 9, 28 - i * 5)
            append("<pubDate>${java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.format(day.atTime(6, 0).atOffset(java.time.ZoneOffset.UTC))}</pubDate>")
            append("<itunes:duration>${60 + i}:00</itunes:duration>")
            if (i % 2 == 0) append("""<podcast:transcript url="https://test.local/$key-${i + 1}.vtt" type="text/vtt"/>""")
            append("""<enclosure url="https://test.local/$key-${i + 1}.mp3" type="audio/mpeg" length="1"/></item>""")
        }
        append("</channel></rss>")
    }

    /* ---- wachten en vastleggen ---- */

    private fun settle(ready: () -> Boolean = { true }) {
        repeat(80) {
            compose.waitForIdle()
            if (it > 20 && ready()) return@repeat
            Thread.sleep(50)
        }
        repeat(20) { compose.mainClock.advanceTimeBy(100); Thread.sleep(30); compose.waitForIdle() }
    }

    private fun save(name: String) = compose.onRoot().captureRoboImage(File(outDir, "$name.png").path)

    /** Met bladen en vensters erbij: die staan in een eigen venster. */
    private fun shot(name: String) = captureScreenRoboImage(File(outDir, "$name.png").path)

    private class Covers(private val context: Context) : Interceptor {
        override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
            val url = chain.request.data.toString()
            val (bg, fg) = when {
                "blauwgroen" in url -> 0xFF14504E.toInt() to 0xFFE9D9B8.toInt()
                "amber" in url -> 0xFFDFA83A.toInt() to 0xFF25201A.toInt()
                else -> 0xFFB5482A.toInt() to 0xFFF4E3CE.toInt()
            }
            val bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888).also {
                Canvas(it).apply {
                    drawColor(bg)
                    drawCircle(210f, 100f, 70f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fg })
                }
            }
            return SuccessResult(BitmapDrawable(context.resources, bitmap), chain.request, DataSource.MEMORY)
        }
    }
}
