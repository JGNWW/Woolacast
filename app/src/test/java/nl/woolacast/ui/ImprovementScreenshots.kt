package nl.woolacast.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.Coil
import coil.ImageLoader
import coil.decode.DataSource
import coil.intercept.Interceptor
import coil.request.ImageResult
import coil.request.SuccessResult
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import java.io.IOException
import java.lang.reflect.Proxy
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import nl.woolacast.data.PodcastRepository
import nl.woolacast.data.apple.AppleCatalogApi
import nl.woolacast.data.dataset.ChartsDataset
import nl.woolacast.data.dataset.ChartsDatasetApi
import nl.woolacast.data.feed.FeedClient
import nl.woolacast.data.local.FeedCheck
import nl.woolacast.data.local.FollowedShow
import nl.woolacast.data.local.LocalStore
import nl.woolacast.data.local.SavedEpisode
import nl.woolacast.data.maker.MakerRepository
import nl.woolacast.domain.Episode
import nl.woolacast.player.PlaybackState
import nl.woolacast.ui.detail.DetailScreen
import nl.woolacast.ui.detail.DetailViewModel
import nl.woolacast.ui.library.LibraryScreen
import nl.woolacast.ui.library.LibraryViewModel
import nl.woolacast.ui.player.CastRoutes
import nl.woolacast.ui.player.PlayerScreen
import nl.woolacast.ui.theme.WoolacastTheme
import nl.woolacast.ui.inbox.InboxScreen
import nl.woolacast.ui.settings.SettingsScreen
import nl.woolacast.ui.settings.SettingsViewModel
import nl.woolacast.ui.settings.StatsScreen
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
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
 * Schermen van de derde reeks: Verder luisteren, de herindeelde Bibliotheek,
 * instellingen, statistiek, het overzicht achter de bel, zoeken in afleveringen
 * en de vrije snelheid. Alleen op verzoek:
 * ./gradlew testDebugUnitTest -Pscreenshots --tests "*ImprovementScreenshots*"
 * Alle shows en afleveringen zijn verzonnen.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xxhdpi")
class ImprovementScreenshots {

    @get:Rule
    val compose = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val outDir by lazy { File(System.getProperty("woolacast.screenshotDir") ?: "build/screenshots").apply { mkdirs() } }
    private val today = LocalDate.now()

    @Before
    fun setUp() {
        assumeTrue("alleen met -Pscreenshots", System.getProperty("woolacast.screenshots") == "true")
        Coil.setImageLoader(ImageLoader.Builder(context).components { add(Covers(context)) }.build())
    }

    private fun ep(id: String, show: String, title: String, art: String, daysAgo: Long, minutes: Long) =
        SavedEpisode(id, show, showTitle(show), title, "https://test.local/$art.png", "https://test.local/$id.mp3",
            minutes * 60_000L, today.minusDays(daysAgo).toString())

    /** Een bibliotheek met van alles erin: gevolgd, nieuw, halverwege, wachtrij, geschiedenis en luistertijd. */
    private fun filled(): LocalStore {
        val store = store()
        val since = today.minusDays(20).toString()
        runBlocking {
            listOf(
                FollowedShow("deadline", "De Deadline", "Dagblad Noord", "https://test.local/rood.png", "https://test.local/deadline.xml", followedAt = since),
                FollowedShow("koud", "Koud Spoor", "Kelderwerk", "https://test.local/blauwgroen.png", "https://test.local/koud.xml", followedAt = since),
                FollowedShow("adem", "Lange Adem", "Studio Hemel", "https://test.local/amber.png", "https://test.local/adem.xml", followedAt = since)
            ).forEach { store.toggleFollow(it) }
            store.putFeedCheck("deadline", FeedCheck(Instant.now().toString(), latest = listOf(
                ep("d3", "deadline", "Wie betaalt de dijk?", "rood", 0, 38),
                ep("d2", "deadline", "Live vanuit Paradiso", "rood", 3, 71)
            )))
            store.putFeedCheck("koud", FeedCheck(Instant.now().toString(), latest = listOf(ep("k5", "koud", "Deel 5: het tweede alibi", "blauwgroen", 0, 52))))
            // Halverwege: drie afleveringen, de laatst gespeelde eerst.
            listOf(
                ep("a8", "adem", "De marathon die niemand liep", "amber", 2, 48) to 31L,
                ep("k4", "koud", "Deel 4: de buurvrouw", "blauwgroen", 7, 49) to 12L,
                ep("d2", "deadline", "Live vanuit Paradiso", "rood", 3, 71) to 29L
            ).forEach { (episode, minutes) ->
                store.touchHistory(episode)
                store.rememberProgress(episode.id, minutes * 60_000L, episode.durationMillis ?: 0L)
            }
            store.touchHistory(ep("d1", "deadline", "Het archief in Assen", "rood", 9, 52))
            store.rememberProgress("d1", 52 * 60_000L, 52 * 60_000L)
            store.touchHistory(ep("d2", "deadline", "Live vanuit Paradiso", "rood", 3, 71))
            store.enqueueAll(listOf(
                ep("k5", "koud", "Deel 5: het tweede alibi", "blauwgroen", 0, 52),
                ep("a9", "adem", "Rondje Amstel in 2:59", "amber", 1, 24),
                ep("d3", "deadline", "Wie betaalt de dijk?", "rood", 0, 38)
            ))
            store.addBookmark(ep("d2", "deadline", "Live vanuit Paradiso", "rood", 3, 71), 17 * 60_000L + 42_000L)
            store.addListening("deadline", "De Deadline", "https://test.local/rood.png", 14 * 3_600_000L + 20 * 60_000L)
            store.addListening("koud", "Koud Spoor", "https://test.local/blauwgroen.png", 6 * 3_600_000L)
            store.addListening("adem", "Lange Adem", "https://test.local/amber.png", 95 * 60_000L)
        }
        return store
    }

    @Test fun bibliotheekLicht() = bibliotheek(dark = false)
    @Test fun bibliotheekDonker() = bibliotheek(dark = true)

    private fun bibliotheek(dark: Boolean) {
        val store = filled()
        val library = LibraryViewModel(store, dataset(), makers(store))
        compose.setContent {
            Screen(dark) {
                LibraryScreen(viewModel = library, countryCode = "nl", playingId = "k5",
                    onOpenPodcast = { _, _, _ -> }, onSearch = {}, onPlay = {})
            }
        }
        settle()
        val suffix = if (dark) "-donker" else "-licht"
        save("verder-luisteren$suffix")
        compose.onAllNodesWithText("Afleveringen")[0].performClick()
        settle()
        save("wachtrij-slepen$suffix")
        if (dark) return
        compose.onAllNodesWithText("Geschiedenis")[0].performClick()
        settle()
        save("geschiedenis$suffix")
        compose.onAllNodesWithText("Bladwijzers")[0].performClick()
        settle()
        save("bladwijzers$suffix")
    }

    @Test fun instellingenLicht() = instellingen(dark = false)
    @Test fun instellingenDonker() = instellingen(dark = true)

    private fun instellingen(dark: Boolean) {
        val store = filled()
        runBlocking { store.setPlayback { it.copy(skipSilence = true, speed = 1.2f) } }
        val model = SettingsViewModel(store)
        compose.setContent { Screen(dark) { SettingsScreen(viewModel = model, onBack = {}, onOpenStats = {}, versionName = "0.1.0") } }
        settle()
        save("instellingen" + if (dark) "-donker" else "-licht")
    }

    @Test fun statistiekLicht() = statistiek(dark = false)
    @Test fun statistiekDonker() = statistiek(dark = true)

    private fun statistiek(dark: Boolean) {
        val store = filled()
        compose.setContent { Screen(dark) { StatsScreen(store = store, onBack = {}) } }
        settle()
        save("statistiek" + if (dark) "-donker" else "-licht")
    }

    @Test fun overzichtLicht() {
        val store = filled()
        val library = LibraryViewModel(store, dataset(), makers(store))
        compose.setContent {
            Screen(false) {
                InboxScreen(viewModel = library, countryCode = "nl", playingId = null, onBack = {}, onOpenPodcast = { _, _, _ -> }, onPlay = {})
            }
        }
        settle()
        save("wat-is-er-nieuw-licht")
    }

    @Test fun afleveringenZoeken() {
        val store = filled()
        runBlocking { store.setListened(listOf("d-2"), true) }
        val model = DetailViewModel(
            showId = "deadline", countryCode = "nl", feedUrl = "https://test.local/deadline.xml", title = null,
            repository = PodcastRepository(noCatalog(), FeedClient(deadlineFeed())), store = store
        )
        compose.setContent {
            Screen(false) {
                DetailScreen(viewModel = model, playingId = null, onBack = {}, onPlay = { _, _ -> }, onOpenTracker = {}, onOpenMaker = {})
            }
        }
        settle { !model.state.value.loading }
        save("afleveringen-gereedschap-licht")
    }

    @Test fun snelheidLicht() = snelheid(dark = false)
    @Test fun snelheidDonker() = snelheid(dark = true)

    private fun snelheid(dark: Boolean) {
        val state = PlaybackState(
            episode = Episode(
                id = "deadline-1", showId = "deadline", showTitle = "De Deadline", title = "Live vanuit Paradiso",
                description = null, artworkUrl = "https://test.local/rood.png", audioUrl = "https://test.local/a.mp3",
                durationMillis = 71 * 60_000L, releaseDate = "2026-09-08"
            ),
            isPlaying = false, positionMs = 29 * 60_000L, durationMs = 71 * 60_000L, speed = 1.35f
        )
        val store = filled()
        val queue = store.queue.value
        val bookmarks = store.bookmarks.value
        compose.setContent {
            Screen(dark) {
                PlayerScreen(
                    state = state, queue = queue, isSaved = false,
                    onCollapse = {}, onTogglePlay = {}, onSeekTo = {}, onSeekBy = {}, onPrevious = {}, onNext = {},
                    onSpeed = {}, onSleep = {}, onToggleSave = {}, onPlayQueued = {}, onRemoveQueued = {},
                    onOpenPodcast = {}, onDismissError = {},
                    cast = CastRoutes(available = false, playingOn = null, preview = true),
                    showHasOwnSpeed = true,
                    bookmarks = bookmarks
                )
            }
        }
        settle()
        val suffix = if (dark) "-donker" else "-licht"
        save("speler-gereedschap$suffix")
        compose.onAllNodesWithContentDescription("Snelheid 1,35×")[0].performClick()
        settle()
        shot("vrije-snelheid$suffix")
    }

    /** Een schermafdruk met wat er bovenop ligt: een blad of een venster. */
    private fun shot(name: String) =
        com.github.takahirom.roborazzi.captureScreenRoboImage(File(outDir, "$name.png").path)

    /* ---- hulpjes ---- */

    private fun showTitle(id: String) = when (id) { "deadline" -> "De Deadline"; "koud" -> "Koud Spoor"; else -> "Lange Adem" }

    private fun store() = LocalStore(File.createTempFile("store", ".json").apply { delete() }).also { runBlocking { it.load() } }

    private fun noCatalog(): AppleCatalogApi = Proxy.newProxyInstance(
        AppleCatalogApi::class.java.classLoader, arrayOf(AppleCatalogApi::class.java)
    ) { _, _, _ -> throw IOException("geen netwerk in de test") } as AppleCatalogApi

    private fun dataset() = ChartsDataset(Proxy.newProxyInstance(
        ChartsDatasetApi::class.java.classLoader, arrayOf(ChartsDatasetApi::class.java)
    ) { _, _, _ -> throw IOException("geen netwerk in de test") } as ChartsDatasetApi)

    private fun makers(store: LocalStore): MakerRepository {
        val catalog = noCatalog()
        return MakerRepository(dataset(), catalog, nl.woolacast.data.SearchRepository(catalog),
            nl.woolacast.data.ChartRepository(sources = emptyList(), store = store), store)
    }


    private fun deadlineFeed() = OkHttpClient.Builder().addInterceptor { chain ->
        val body = buildString {
            append("""<rss xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd"><channel><title>De Deadline</title>""")
            append("<itunes:author>Dagblad Noord</itunes:author><description>Een verzonnen testpodcast.</description>")
            append("""<itunes:image href="https://test.local/rood.png"/>""")
            listOf("Wie betaalt de dijk?", "Live vanuit Paradiso", "Het archief in Assen").forEachIndexed { i, title ->
                append("<item><title>$title</title><guid>d-$i</guid><itunes:duration>${40 + i}:00</itunes:duration>")
                append("<pubDate>${java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.format(today.minusDays(i * 3L).atTime(6, 0).atOffset(java.time.ZoneOffset.UTC))}</pubDate>")
                append("""<enclosure url="https://test.local/d$i.mp3" type="audio/mpeg"/></item>""")
            }
            append("</channel></rss>")
        }
        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
            .body(body.toResponseBody("application/rss+xml".toMediaType())).build()
    }.build()

    @androidx.compose.runtime.Composable
    private fun Screen(dark: Boolean, content: @androidx.compose.runtime.Composable () -> Unit) {
        WoolacastTheme(darkTheme = dark) {
            androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background, content = content)
        }
    }

    private fun settle(ready: () -> Boolean = { true }) {
        repeat(80) {
            compose.waitForIdle()
            if (it > 20 && ready()) return@repeat
            Thread.sleep(50)
        }
        repeat(20) { compose.mainClock.advanceTimeBy(100); Thread.sleep(30); compose.waitForIdle() }
    }

    private fun save(name: String) = compose.onRoot().captureRoboImage(File(outDir, "$name.png").path)

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
