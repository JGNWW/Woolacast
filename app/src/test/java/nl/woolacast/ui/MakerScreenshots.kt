package nl.woolacast.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
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
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import nl.woolacast.data.ChartRepository
import nl.woolacast.data.PodcastRepository
import nl.woolacast.data.SearchRepository
import nl.woolacast.data.apple.AppleCatalogApi
import nl.woolacast.data.apple.LookupResponse
import nl.woolacast.data.apple.LookupResult
import nl.woolacast.data.dataset.ChartsDataset
import nl.woolacast.data.dataset.ChartsDatasetApi
import nl.woolacast.data.dataset.DatasetChannel
import nl.woolacast.data.dataset.DatasetChannelShow
import nl.woolacast.data.dataset.DatasetMakers
import nl.woolacast.data.feed.FeedClient
import nl.woolacast.data.local.LocalStore
import nl.woolacast.data.maker.MakerRepository
import nl.woolacast.domain.Chart
import nl.woolacast.domain.ChartEntry
import nl.woolacast.domain.ChartLevel
import nl.woolacast.domain.ChartQuery
import nl.woolacast.domain.ChartSource
import nl.woolacast.domain.SourceCapabilities
import nl.woolacast.domain.SourceId
import nl.woolacast.ui.charts.ChartsScreen
import nl.woolacast.ui.charts.ChartsViewModel
import nl.woolacast.ui.library.LibraryScreen
import nl.woolacast.ui.library.LibraryViewModel
import nl.woolacast.ui.maker.MakerScreen
import nl.woolacast.ui.maker.MakerViewModel
import nl.woolacast.ui.search.SearchScreen
import nl.woolacast.ui.search.SearchViewModel
import nl.woolacast.ui.theme.WoolacastTheme
import okhttp3.OkHttpClient
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Schermafbeeldingen van de makers-schermen (makerpagina, makers in de
 * hitlijst, makers volgen), naast de bestaande schermen waar ze op moeten
 * lijken. Draait alleen met -Pscreenshots; de PNG's komen in app/build/screenshots.
 * Alle makers en podcasts zijn verzonnen.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xxhdpi")
class MakerScreenshots {

    @get:Rule
    val compose = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val outDir by lazy { File(System.getProperty("woolacast.screenshotDir") ?: "build/screenshots").apply { mkdirs() } }

    private val today = LocalDate.now()
    private fun daysAgo(n: Long) = today.minusDays(n).toString()

    /** [id, titel, makersnaam, hoes, genre, dagen sinds nieuwste aflevering, afleveringen] */
    private data class Show(val id: Long, val title: String, val publisher: String, val cover: String, val genre: String, val age: Long, val episodes: Int = 40)

    private val shows = listOf(
        Show(1001, "Kade 12", "Kade Media", "rood", "True crime", 0),
        Show(1002, "Het Vijfde Kwartier", "Dagblad Noord", "blauwgroen", "Sport", 1),
        Show(1003, "Nachtdienst", "Kade Media / Nachtwerk", "amber", "Maatschappij", 1),
        Show(1004, "Ochtendspits", "Radio Oost", "blauwgroen", "Nieuws", 0),
        Show(1005, "Tafel voor Twee", "Kade Media", "amber", "Eten", 1),
        Show(1006, "Vandaag in Zeven", "Dagblad Noord", "rood", "Nieuws", 0),
        Show(1007, "Het Oosten Vertelt", "Radio Oost", "rood", "Geschiedenis", 1),
        Show(1008, "Koud Spoor", "Podium Audio", "blauwgroen", "True crime", 3),
        Show(1009, "Lange Adem", "Kade Media", "blauwgroen", "Sport", 3),
        Show(1010, "Zwart op Wit", "Studio Hemel", "amber", "Comedy", 2),
        Show(1011, "Derby", "Radio Oost", "amber", "Sport", 1),
        Show(1012, "Ondergronds", "Podium Audio", "rood", "True crime", 5),
        Show(1013, "Halve Zolen", "Kade Media", "rood", "Comedy", 2),
        Show(1014, "Kort Verhaal", "Studio Hemel", "blauwgroen", "Kunst", 4),
        Show(1015, "De Wandelgang", "Dagblad Noord", "amber", "Politiek", 6)
    )

    /** Shows die niet in de lijst staan, maar wel van deze makers zijn. */
    private val outside = listOf(
        Show(1101, "Oost aan Tafel", "Radio Oost", "amber", "Eten", 8),
        Show(1102, "Zaterdagavond Thuis", "Kade Media", "amber", "Comedy", 9),
        Show(1103, "Kort Lontje", "Dagblad Noord", "blauwgroen", "Comedy", 2, episodes = 3)
    )

    private val makers = DatasetMakers(
        country = "nl",
        channels = listOf(
            DatasetChannel(
                id = "c-kade", name = "Kade Media", color = "14504e", logo = "https://test.local/logo-kade.png",
                url = "https://podcasts.apple.com/nl/channel/id1", showCount = 7,
                shows = listOf("1001", "1003", "1005", "1009", "1013", "1102", "1201"),
                newShows = listOf(DatasetChannelShow("1201", "Nachtwerk Extra", "https://test.local/amber.png", null, daysAgo(3), 2))
            ),
            DatasetChannel(
                id = "c-noord", name = "Dagblad Noord", color = "2b4c7e", logo = "https://test.local/logo-noord.png",
                url = "https://podcasts.apple.com/nl/channel/id2", showCount = 4,
                shows = listOf("1002", "1006", "1015", "1103"),
                newShows = listOf(DatasetChannelShow("1103", "Kort Lontje", "https://test.local/blauwgroen.png", null, daysAgo(2), 3))
            ),
            DatasetChannel(
                id = "c-podium", name = "Podium Audio", color = "1e1b16", logo = "https://test.local/logo-podium.png",
                url = "https://podcasts.apple.com/nl/channel/id3", showCount = 2,
                shows = listOf("1008", "1012")
            )
        ),
        showChannel = mapOf(
            "1001" to "c-kade", "1003" to "c-kade", "1005" to "c-kade", "1009" to "c-kade", "1013" to "c-kade",
            "1002" to "c-noord", "1006" to "c-noord", "1015" to "c-noord",
            "1008" to "c-podium", "1012" to "c-podium"
        )
    )

    private val query = ChartQuery(SourceId.APPLE, nl.woolacast.domain.Catalog.defaultCountry, nl.woolacast.domain.Catalog.defaultCategory, ChartLevel.SHOWS)

    private val chart = Chart(query, shows.mapIndexed { index, show ->
        ChartEntry(
            rank = index + 1, id = show.id.toString(), title = show.title, publisher = show.publisher,
            artworkUrl = "https://test.local/${show.cover}.png", genre = show.genre, storeUrl = null,
            showId = show.id.toString()
        )
    }, updatedLabel = null)

    @Before
    fun setUp() {
        assumeTrue("alleen met -Pscreenshots", System.getProperty("woolacast.screenshots") == "true")
        Coil.setImageLoader(ImageLoader.Builder(context).components { add(ArtInterceptor(context)) }.build())
    }

    @Test fun hitlijstLicht() = hitlijst(dark = false)
    @Test fun hitlijstDonker() = hitlijst(dark = true)

    private fun hitlijst(dark: Boolean) {
        val store = seededStore()
        val model = ChartsViewModel(chartRepository(store), podcasts(), repository(store))
        compose.setContent {
            WoolacastTheme(darkTheme = dark) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ChartsScreen(viewModel = model, repository = chartRepository(store), playingId = null,
                        onOpenPodcast = { _, _, _, _ -> }, onSearch = {}, onAlerts = {})
                }
            }
        }
        val suffix = if (dark) "-donker" else ""
        settle { model.state.value.makers != null }
        save("hitlijst-per-show$suffix")
        model.setByMaker(true)
        settle()
        save("hitlijst-per-maker$suffix")
        if (!dark) {
            compose.onAllNodesWithContentDescription("Hoe tellen we?")[0].performClick()
            settle()
            captureScreenRoboImage(File(outDir, "hitlijst-hoe-tellen-we.png").path)
        }
    }

    @Test fun makerKanaal() = makerpagina("maker-kanaal", "Kade Media", "1001", dark = false, follow = false)
    @Test fun makerKanaalDonker() = makerpagina("maker-kanaal-donker", "Podium Audio", "1008", dark = true, follow = true)
    @Test fun makerZonderKanaal() = makerpagina("maker-zonder-kanaal", "Radio Oost", "1004", dark = false, follow = false)
    @Test fun makerZonderKanaalDonker() = makerpagina("maker-zonder-kanaal-donker", "Radio Oost", "1004", dark = true, follow = true)

    private fun makerpagina(name: String, publisher: String, from: String?, dark: Boolean, follow: Boolean) {
        val store = seededStore()
        val model = MakerViewModel(repository(store), store, publisher, "nl", from, SourceId.APPLE)
        compose.setContent {
            WoolacastTheme(darkTheme = dark) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MakerScreen(viewModel = model, onBack = {}, onOpenPodcast = { _, _, _ -> })
                }
            }
        }
        settle { !model.state.value.loading && model.state.value.placement != null }
        if (follow) {
            model.toggleFollow()
            settle { model.following.value }
        }
        settle()
        save(name)
    }

    @Test fun bibliotheekLicht() = bibliotheek(dark = false, followMakers = true, name = "bibliotheek-makers")
    @Test fun bibliotheekDonker() = bibliotheek(dark = true, followMakers = true, name = "bibliotheek-makers-donker")
    @Test fun bibliotheekLeeg() = bibliotheek(dark = false, followMakers = false, name = "bibliotheek-makers-leeg")

    private fun bibliotheek(dark: Boolean, followMakers: Boolean, name: String) {
        val store = seededStore(followMakers = followMakers)
        val model = LibraryViewModel(store, dataset(), podcasts(), repository(store))
        compose.setContent {
            WoolacastTheme(darkTheme = dark) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    LibraryScreen(viewModel = model, countryCode = "nl", playingId = null,
                        onOpenPodcast = { _, _, _ -> }, onSearch = {}, onPlay = {})
                }
            }
        }
        settle()
        if (!dark && followMakers) save("bibliotheek-gevolgd")
        compose.onAllNodesWithText("Makers")[0].performClick()
        settle { if (followMakers) model.makerStatus.value.isNotEmpty() else model.suggestions.value.isNotEmpty() }
        settle()
        save(name)
    }

    @Test
    fun zoeken() {
        val store = seededStore()
        val catalog = catalog()
        val model = SearchViewModel(SearchRepository(catalog), "nl", repository(store))
        compose.setContent {
            WoolacastTheme(darkTheme = false) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    SearchScreen(viewModel = model, onBack = {}, onOpenPodcast = { _, _, _ -> }, onPlay = {})
                }
            }
        }
        model.onTermChanged("Kade")
        // Het zoeken wacht tot het typen stilvalt; in de test laten we die tijd verstrijken.
        repeat(10) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper(100, java.util.concurrent.TimeUnit.MILLISECONDS)
            Thread.sleep(50)
        }
        settle { model.state.value.searched }
        settle()
        save("zoeken-maker")
    }

    /* ---- nepdata ---- */

    /** Een opslag met een momentopname van gisteren, zodat er pijlen staan, en een paar gevolgde shows. */
    private fun seededStore(followMakers: Boolean = false): LocalStore {
        val file = File.createTempFile("store", ".json")
        val yesterday = daysAgo(1)
        val showRanks = chart.entries.associate { it.id to it.rank + if (it.rank % 3 == 0) 2 else 0 }
        val makerRanks = mapOf("kademedia" to 2, "dagbladnoord" to 1, "radiooost" to 3, "podiumaudio" to 4)
        fun ranks(map: Map<String, Int>) = map.entries.joinToString(",") { "\"${it.key}\":${it.value}" }
        val follows = """[
            {"id":"1002","title":"Het Vijfde Kwartier","publisher":"Dagblad Noord","artworkUrl":"https://test.local/blauwgroen.png"},
            {"id":"1006","title":"Vandaag in Zeven","publisher":"Dagblad Noord","artworkUrl":"https://test.local/rood.png"},
            {"id":"1010","title":"Zwart op Wit","publisher":"Studio Hemel","artworkUrl":"https://test.local/amber.png"},
            {"id":"1008","title":"Koud Spoor","publisher":"Podium Audio","artworkUrl":"https://test.local/blauwgroen.png"}
        ]"""
        val makers = if (followMakers) """[
            {"key":"kademedia","name":"Kade Media","channelId":"c-kade","logoUrl":"https://test.local/logo-kade.png","color":"14504e","followedOn":"${daysAgo(20)}","knownShowIds":["1001","1003","1005","1009","1013","1102"]},
            {"key":"podiumaudio","name":"Podium Audio","channelId":"c-podium","logoUrl":"https://test.local/logo-podium.png","color":"1e1b16","followedOn":"${daysAgo(20)}","knownShowIds":["1008","1012"]},
            {"key":"radiooost","name":"Radio Oost","followedOn":"${daysAgo(20)}","knownShowIds":["1004","1007","1011","1101"]}
        ]""" else "[]"
        file.writeText("""{"follows":$follows,"makers":$makers,"snapshots":{
            "${query.key}":[{"date":"$yesterday","ranks":{${ranks(showRanks)}}}],
            "MAKERS|${query.key}":[{"date":"$yesterday","ranks":{${ranks(makerRanks)}}}]
        }}""")
        return LocalStore(file).also { runBlocking { it.load() } }
    }

    private fun dataset() = ChartsDataset(Proxy.newProxyInstance(
        ChartsDatasetApi::class.java.classLoader, arrayOf(ChartsDatasetApi::class.java)
    ) { _, method, _ ->
        if (method.name == "makers") makers else throw IOException("geen netwerk in de test")
    } as ChartsDatasetApi)

    private fun lookup(show: Show) = LookupResult(
        wrapperType = "track", kind = "podcast", collectionId = show.id, collectionName = show.title,
        artistName = show.publisher, artworkUrl600 = "https://test.local/${show.cover}.png",
        releaseDate = "${daysAgo(show.age)}T06:00:00Z", trackCount = show.episodes, primaryGenreName = show.genre,
        feedUrl = "https://test.local/${show.id}.xml"
    )

    private fun catalog(): AppleCatalogApi = Proxy.newProxyInstance(
        AppleCatalogApi::class.java.classLoader, arrayOf(AppleCatalogApi::class.java)
    ) { _, method, args ->
        val all = shows + outside + Show(1201, "Nachtwerk Extra", "Kade Media / Nachtwerk", "amber", "Maatschappij", 1, episodes = 2)
        when (method.name) {
            "lookupMany" -> {
                val ids = (args[0] as String).split(',').toSet()
                LookupResponse(results = all.filter { it.id.toString() in ids }.map(::lookup))
            }
            "search" -> {
                val term = (args[0] as String).lowercase()
                LookupResponse(results = all.filter { it.publisher.lowercase().contains(term) }.map(::lookup))
            }
            else -> throw IOException("geen netwerk in de test")
        }
    } as AppleCatalogApi

    private fun chartRepository(store: LocalStore) = ChartRepository(
        sources = listOf(FixedSource(SourceId.APPLE, chart), FixedSource(SourceId.SPOTIFY, chart)),
        store = store
    )

    private fun repository(store: LocalStore): MakerRepository {
        val catalog = catalog()
        return MakerRepository(dataset(), catalog, SearchRepository(catalog), chartRepository(store), store)
    }

    private fun podcasts() = PodcastRepository(catalog(), FeedClient(OkHttpClient.Builder().addInterceptor { throw IOException("geen feed") }.build()))

    private class FixedSource(override val id: SourceId, private val chart: Chart) : ChartSource {
        override val capabilities = SourceCapabilities(
            levels = setOf(ChartLevel.SHOWS, ChartLevel.EPISODES), categoryLevels = setOf(ChartLevel.SHOWS),
            countryCount = 1, cadence = "dagelijks", summary = "test"
        )
        override suspend fun load(query: ChartQuery): Chart = chart.copy(query = query)
    }

    /* ---- wachten en vastleggen ---- */

    private fun settleModel(ready: () -> Boolean) {
        repeat(100) {
            if (ready()) return
            Thread.sleep(50)
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

    /** Tekent hoezen en logo's: drie testhoezen, en per kanaal een vierkant logo in zijn kleur. */
    private class ArtInterceptor(private val context: Context) : Interceptor {
        override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
            val url = chain.request.data.toString()
            val bitmap = when {
                "logo-kade" in url -> logo(0xFF14504E.toInt(), "KADE")
                "logo-noord" in url -> logo(0xFF2B4C7E.toInt(), "DN")
                "logo-podium" in url -> logo(0xFF1E1B16.toInt(), "PA")
                "rood" in url -> cover(0xFFB5482A.toInt(), 0xFFF4E3CE.toInt())
                "blauwgroen" in url -> cover(0xFF14504E.toInt(), 0xFFE9D9B8.toInt())
                else -> cover(0xFFDFA83A.toInt(), 0xFF25201A.toInt())
            }
            return SuccessResult(BitmapDrawable(context.resources, bitmap), chain.request, DataSource.MEMORY)
        }

        private fun logo(color: Int, text: String) = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888).also {
            Canvas(it).apply {
                drawColor(color)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = Color.WHITE; textAlign = Paint.Align.CENTER
                    typeface = Typeface.DEFAULT_BOLD; textSize = if (text.length > 2) 70f else 110f
                }
                drawText(text, 150f, 150f - (paint.descent() + paint.ascent()) / 2, paint)
            }
        }

        private fun cover(background: Int, accent: Int) = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888).also {
            Canvas(it).apply {
                drawColor(background)
                drawCircle(210f, 100f, 70f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent })
            }
        }
    }
}
