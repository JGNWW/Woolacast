package nl.woolacast.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.Coil
import coil.ImageLoader
import coil.decode.DataSource
import coil.intercept.Interceptor
import coil.request.ImageResult
import coil.request.SuccessResult
import kotlinx.coroutines.runBlocking
import nl.woolacast.data.PodcastRepository
import nl.woolacast.data.apple.AppleCatalogApi
import nl.woolacast.data.feed.FeedClient
import nl.woolacast.data.local.LocalStore
import nl.woolacast.data.local.SavedEpisode
import nl.woolacast.domain.Episode
import nl.woolacast.player.PlaybackState
import nl.woolacast.ui.common.MiniPlayer
import nl.woolacast.ui.common.rememberCoverSeed
import nl.woolacast.ui.theme.CoverSeed
import nl.woolacast.ui.detail.DetailScreen
import nl.woolacast.ui.detail.DetailViewModel
import nl.woolacast.ui.player.PlayerScreen
import nl.woolacast.ui.theme.WoolacastTheme
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.IOException
import java.lang.reflect.Proxy

/**
 * Schermafbeeldingen van de Beeldgloed-schermen, op de JVM gerenderd met
 * Robolectric. Drie testhoezen (rood, blauwgroen, amber) laten zien hoe de
 * interface meekleurt. Draait alleen met -Pscreenshots; de PNG's komen in
 * app/build/screenshots.
 */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xxhdpi")
class BeeldgloedScreenshots {

    @get:Rule
    val compose = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val outDir by lazy { File(System.getProperty("woolacast.screenshotDir") ?: "build/screenshots").apply { mkdirs() } }

    private data class Show(val key: String, val title: String, val author: String, val episodes: List<String>)

    private val shows = listOf(
        Show("rood", "Het Verdwenen Dorp", "Studio Veenkoloniaal",
            listOf("De laatste bewoner", "Kaarten die niet kloppen", "Het archief in Assen", "Een brief uit 1975", "Wie tekende de grens?")),
        Show("blauwgroen", "Diepzee", "Getij Media",
            listOf("Licht op 4000 meter", "Het geluid van walvissen", "Leven zonder zon", "De kaart van de bodem", "Onder druk")),
        Show("amber", "Goud van Oud", "Radio Zolder",
            listOf("De eerste radio van Hilversum", "Tulpen en schulden", "Een koning op de fiets", "Het water komt", "De laatste trekschuit"))
    )

    @Before
    fun setUp() {
        assumeTrue("alleen met -Pscreenshots", System.getProperty("woolacast.screenshots") == "true")
        Coil.setImageLoader(
            ImageLoader.Builder(context)
                .components { add(CoverInterceptor(context)) }
                .build()
        )
    }

    @Test
    fun podcastPages() {
        val models = shows.map { show ->
            val store = LocalStore(File.createTempFile("store", ".json").apply { delete() })
            runBlocking {
                store.load()
                store.rememberProgress("${show.key}-2", 24 * 60_000L + 37_000L, 42 * 60_000L)
            }
            DetailViewModel(
                showId = show.key,
                countryCode = "nl",
                feedUrl = "https://test.local/${show.key}.xml",
                title = null,
                repository = PodcastRepository(noCatalog(), FeedClient(feedClient())),
                store = store
            )
        }
        var current by mutableStateOf(0)
        compose.setContent {
            val show = shows[current]
            val playing = PlaybackState(
                episode = episode(show, 2),
                isPlaying = true,
                positionMs = 24 * 60_000L + 37_000L,
                durationMs = 42 * 60_000L
            )
            WoolacastTheme(darkTheme = true) {
                key(current) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        DetailScreen(
                            viewModel = models[current],
                            playingId = playing.episodeId,
                            onBack = {}, onPlay = { _, _ -> }, onOpenTracker = {}, onOpenMaker = {}
                        )
                        MiniPlayer(
                            state = playing, onExpand = {}, onTogglePlay = {}, onSkipForward = {},
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
                        )
                    }
                }
            }
        }
        shows.forEachIndexed { index, show ->
            current = index
            settle { !models[index].state.value.loading }
            save("podcast-${show.key}")
            // Gescrold: de gloed moet vloeiend tot in de afleveringen doorlopen,
            // en de bovenbalk wordt dicht zodra de hoes weg is.
            compose.onNode(hasScrollAction()).performTouchInput { swipeUp(startY = bottom * 0.8f, endY = top + bottom * 0.2f) }
            settle()
            save("podcast-${show.key}-gescrold")
        }
    }

    @Test
    fun player() {
        var current by mutableStateOf(0)
        var justStarted by mutableStateOf(false)
        compose.setContent {
            val index = current
            val show = shows[index]
            val state = PlaybackState(
                episode = episode(show, 2),
                isPlaying = true,
                positionMs = if (justStarted) 4_000L else 24 * 60_000L + 37_000L,
                durationMs = 42 * 60_000L + 39_000L,
                speed = 1.2f,
                chartLabel = "#3 in Top afleveringen NL"
            )
            val queue = listOf(saved(shows[(index + 1) % shows.size], 1), saved(shows[(index + 2) % shows.size], 1))
            WoolacastTheme(darkTheme = true) {
                key(index) {
                    PlayerScreen(
                        state = state, queue = queue, isSaved = false,
                        onCollapse = {}, onTogglePlay = {}, onSeekTo = {}, onSeekBy = {}, onPrevious = {}, onNext = {},
                        onSpeed = {}, onSleep = {}, onToggleSave = {}, onPlayQueued = {}, onRemoveQueued = {},
                        onOpenPodcast = {}, onDismissError = {}
                    )
                }
            }
        }
        shows.forEachIndexed { index, show ->
            current = index
            settle()
            save("speler-${show.key}")
        }
        // Net begonnen: het handvat moet een hele stip zijn, links op de balk.
        current = 0
        justStarted = true
        settle()
        save("speler-${shows[0].key}-begin")
        justStarted = false
        // De wachtrij open, over de speler met de blauwgroene hoes.
        current = 1
        settle()
        compose.onAllNodesWithText("Wachtrij · 2")[0].performClick()
        settle()
        // Het blad staat in een eigen venster; dit legt alle vensters vast.
        captureScreenRoboImage(File(outDir, "wachtrij-${shows[1].key}.png").path)
    }

    /**
     * De podcastpagina opende met het oude merkaccent als de mini-speler dezelfde
     * hoes al had ingeladen: een hoes uit de cache zette de kleur niet.
     */
    @Test
    fun cachedCoverStillColoursASecondScreen() {
        val url = "https://test.local/rood.png"
        var second by mutableStateOf<String?>(null)
        var first: CoverSeed? = null
        var seen: CoverSeed? = null
        compose.setContent {
            first = rememberCoverSeed(url)
            seen = rememberCoverSeed(second)
        }
        settle { first != null }
        assertNotNull("eerste hoes geladen", first)
        second = url
        settle()
        assertEquals(first, seen)
        second = null
        settle()
        assertNull(seen)
    }

    private fun episode(show: Show, number: Int) = Episode(
        id = "${show.key}-$number",
        showId = show.key,
        showTitle = show.title,
        title = show.episodes[number - 1],
        description = null,
        artworkUrl = "https://test.local/${show.key}.png",
        audioUrl = "https://test.local/${show.key}-$number.mp3",
        durationMillis = 42 * 60_000L,
        releaseDate = "2026-09-${String.format("%02d", 28 - number * 7)}"
    )

    private fun saved(show: Show, number: Int) = episode(show, number).let {
        SavedEpisode(it.id, it.showId, it.showTitle, it.title, it.artworkUrl, it.audioUrl, it.durationMillis, it.releaseDate)
    }

    /** Wacht tot de ViewModel, Coil en de kleurextractie (op andere threads) klaar zijn. */
    private fun settle(ready: () -> Boolean = { true }) {
        repeat(80) {
            compose.waitForIdle()
            if (it > 20 && ready()) return@repeat
            Thread.sleep(50)
        }
        repeat(20) { compose.mainClock.advanceTimeBy(100); Thread.sleep(30); compose.waitForIdle() }
    }

    private fun save(name: String) = compose.onRoot().captureRoboImage(File(outDir, "$name.png").path)

    private fun Bitmap.writeTo(name: String) {
        File(outDir, "$name.png").outputStream().use { compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun noCatalog(): AppleCatalogApi = Proxy.newProxyInstance(
        AppleCatalogApi::class.java.classLoader, arrayOf(AppleCatalogApi::class.java)
    ) { _, _, _ -> throw IOException("geen netwerk in de test") } as AppleCatalogApi

    private fun feedClient() = OkHttpClient.Builder().addInterceptor { chain ->
        val key = chain.request().url.encodedPath.trim('/').removeSuffix(".xml")
        val show = shows.first { it.key == key }
        Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(200).message("OK")
            .body(rss(show).toResponseBody("application/rss+xml".toMediaType()))
            .build()
    }.build()

    private fun rss(show: Show): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8"?><rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd"><channel>""")
        append("<title>${show.title}</title><itunes:author>${show.author}</itunes:author>")
        append("<description>Een verzonnen testpodcast om te zien hoe de pagina meekleurt met de hoes. Elke week een nieuwe aflevering.</description>")
        append("""<itunes:image href="https://test.local/${show.key}.png"/>""")
        show.episodes.forEachIndexed { i, title ->
            val day = 28 - (i + 1) * 7
            append("<item><title>$title</title><guid>${show.key}-${i + 1}</guid>")
            append("<pubDate>${if (day > 0) "Mon, ${String.format("%02d", day)} Sep 2026" else "Mon, ${String.format("%02d", day + 31)} Aug 2026"} 06:00:00 +0000</pubDate>")
            append("<itunes:duration>${40 + i}:00</itunes:duration>")
            append("""<enclosure url="https://test.local/${show.key}-${i + 1}.mp3" type="audio/mpeg" length="1"/></item>""")
        }
        append("</channel></rss>")
    }

    /** Tekent de testhoezen, zoals in de ontwerpmockups. */
    private class CoverInterceptor(private val context: Context) : Interceptor {
        override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
            val url = chain.request.data.toString()
            val bitmap = when {
                "rood" in url -> red()
                "blauwgroen" in url -> teal()
                else -> amber()
            }
            return SuccessResult(BitmapDrawable(context.resources, bitmap), chain.request, DataSource.MEMORY)
        }

        private val size = 600f
        private fun canvas(block: Canvas.() -> Unit): Bitmap =
            Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_8888).also { Canvas(it).block() }

        private fun red() = canvas {
            drawRect(0f, 0f, size, size, Paint().apply {
                shader = LinearGradient(0f, 0f, size * 0.2f, size, intArrayOf(0xFFD8452F.toInt(), 0xFFA42A1F.toInt(), 0xFF5E130E.toInt()), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
            })
            drawCircle(size * 0.74f, size * 0.6f, size * 0.1f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF6B39E.toInt() })
            val hills = Path().apply {
                moveTo(0f, size * 0.79f); lineTo(size * 0.16f, size * 0.75f); lineTo(size * 0.29f, size * 0.81f)
                lineTo(size * 0.47f, size * 0.70f); lineTo(size * 0.63f, size * 0.78f); lineTo(size * 0.81f, size * 0.73f)
                lineTo(size, size * 0.80f); lineTo(size, size); lineTo(0f, size); close()
            }
            drawPath(hills, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2A0907.toInt() })
        }

        private fun teal() = canvas {
            drawRect(0f, 0f, size, size, Paint().apply {
                shader = RadialGradient(size / 2, -size * 0.1f, size * 1.1f, intArrayOf(0xFF8FE6DB.toInt(), 0xFF2A9C98.toInt(), 0xFF0E4F5A.toInt(), 0xFF06222B.toInt()), floatArrayOf(0f, 0.34f, 0.68f, 1f), Shader.TileMode.CLAMP)
            })
            val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 8f; color = 0x21FFFFFF }
            for (r in 1..14) drawCircle(size / 2, size * 1.25f, r * 48f, ring)
        }

        private fun amber() = canvas {
            drawRect(0f, 0f, size, size, Paint().apply {
                shader = LinearGradient(0f, 0f, size * 0.3f, size, intArrayOf(0xFFF9D05C.toInt(), 0xFFE39E1E.toInt(), 0xFFAC650F.toInt()), floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
            })
            drawCircle(size * 0.74f, size * 0.78f, size * 0.33f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3B2208.toInt() })
            drawCircle(size * 0.74f, size * 0.78f, size * 0.28f, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 6f; color = 0xFF6B3F0E.toInt() })
        }
    }
}
