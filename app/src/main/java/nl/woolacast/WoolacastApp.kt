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
import nl.woolacast.data.UnsupportedChartSource
import nl.woolacast.data.apple.AppleChartSource
import nl.woolacast.data.feed.FeedClient
import nl.woolacast.data.fyyd.FyydChartSource
import nl.woolacast.data.local.LocalStore
import nl.woolacast.player.PlayerController

/**
 * Handmatige bedrading in plaats van een DI-raamwerk: bij deze omvang is het
 * korter en scheelt het een annotatieprocessor in de build.
 */
class AppContainer(context: Context) {

    private val marketingApi = Network.marketingApi()
    private val catalogApi = Network.catalogApi()
    private val fyydApi = Network.fyydApi()
    private val feedClient = FeedClient(Network.client)

    val store = LocalStore(File(context.filesDir, "woolacast-store.json"))

    val chartRepository = ChartRepository(
        sources = listOf(
            AppleChartSource(marketingApi, catalogApi),
            FyydChartSource(fyydApi),
            UnsupportedChartSource.spotify(),
            UnsupportedChartSource.youtube()
        ),
        store = store
    )

    val podcastRepository = PodcastRepository(catalogApi, feedClient)

    val player = PlayerController(context)
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
