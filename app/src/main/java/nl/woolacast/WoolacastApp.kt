package nl.woolacast

import android.app.Application
import android.content.Context
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import nl.woolacast.data.ChartRepository
import nl.woolacast.data.Network
import nl.woolacast.data.PodcastRepository
import nl.woolacast.data.SearchRepository
import nl.woolacast.data.apple.AppleChartSource
import nl.woolacast.data.apple.AppleGenreTree
import nl.woolacast.data.dataset.ChartsDataset
import nl.woolacast.data.feed.FeedClient
import nl.woolacast.data.local.LocalStore
import nl.woolacast.data.spotify.SpotifyChartSource
import nl.woolacast.player.PlayerController

/**
 * Handmatige bedrading in plaats van een DI-raamwerk: bij deze omvang is het
 * korter en scheelt het een annotatieprocessor in de build.
 */
class AppContainer(context: Context) {

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

    val podcastRepository = PodcastRepository(catalogApi, feedClient)

    val searchRepository = SearchRepository(catalogApi)

    val dataset: ChartsDataset get() = chartsDataset

    val player = PlayerController(context) { episodeId, positionMs, durationMs ->
        store.rememberProgress(episodeId, positionMs, durationMs)
    }
}

class WoolacastApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            container.store.load()
        }
    }
}

val Context.container: AppContainer
    get() = (applicationContext as WoolacastApp).container
