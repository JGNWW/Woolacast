package nl.woolacast.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import android.net.Uri
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import nl.woolacast.AppContainer
import nl.woolacast.domain.Catalog
import nl.woolacast.ui.charts.ChartsScreen
import nl.woolacast.ui.charts.ChartsViewModel
import nl.woolacast.ui.common.MiniPlayer
import nl.woolacast.ui.detail.DetailScreen
import nl.woolacast.ui.detail.DetailViewModel
import nl.woolacast.ui.discover.DiscoverScreen
import nl.woolacast.ui.discover.DiscoverViewModel
import nl.woolacast.ui.library.LibraryScreen
import nl.woolacast.ui.library.LibraryViewModel
import nl.woolacast.ui.search.SearchScreen
import nl.woolacast.ui.search.SearchViewModel
import nl.woolacast.ui.player.PlayerScreen
import nl.woolacast.ui.tracker.TrackerScreen
import nl.woolacast.ui.tracker.TrackerViewModel

private const val PLAYER_ROUTE = "player"
private const val SEARCH_ROUTE = "search"

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    CHARTS("charts", "Hitlijsten", Icons.Filled.BarChart),
    DISCOVER("discover", "Ontdek", Icons.Filled.Explore),
    LIBRARY("library", "Bibliotheek", Icons.Filled.LibraryMusic)
}

@Composable
fun WoolacastNav(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination

    val playback by container.player.state.collectAsStateWithLifecycle()

    // Eén instantie voor de hele app, zodat Ontdek de lijst kan instellen die
    // het hitlijstenscherm daarna toont.
    val chartsViewModel: ChartsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ChartsViewModel(container.chartRepository) }
        }
    )
    val chartsState by chartsViewModel.state.collectAsStateWithLifecycle()
    val chartsCountry = chartsState.query.country.code

    val openPodcast: (String, String?, String) -> Unit = { showId, feedUrl, title ->
        navController.navigate(
            "podcast/${Uri.encode(showId)}" +
                "?feed=${Uri.encode(feedUrl.orEmpty())}" +
                "&country=$chartsCountry&title=${Uri.encode(title)}"
        )
    }
    val listened by container.store.progress.collectAsStateWithLifecycle()
    val playAndOpen: (nl.woolacast.domain.Episode) -> Unit = { episode ->
        // Verder waar je gebleven was.
        container.player.play(episode, listened[episode.id] ?: 0L)
        navController.navigate(PLAYER_ROUTE)
    }

    Scaffold(
        bottomBar = {
            Column {
                // Op het spelerscherm zelf hoeft de mini-speler er niet ook te staan.
                val onPlayerRoute = currentRoute?.route == PLAYER_ROUTE
                if (playback.hasEpisode && !onPlayerRoute) {
                    MiniPlayer(
                        state = playback,
                        onExpand = { navController.navigate(PLAYER_ROUTE) },
                        onTogglePlay = container.player::togglePlayPause,
                        onSkipForward = { container.player.seekBy(30_000L) }
                    )
                    Spacer(Modifier.height(6.dp))
                }
                if (!onPlayerRoute) NavigationBar {
                    Tab.entries.forEach { tab ->
                        val selected = currentRoute?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.CHARTS.route,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            composable(Tab.CHARTS.route) {
                ChartsScreen(
                    viewModel = chartsViewModel,
                    repository = container.chartRepository,
                    onSearch = { navController.navigate(SEARCH_ROUTE) },
                    onOpenPodcast = { showId, feedUrl, _, title ->
                        openPodcast(showId, feedUrl, title)
                    }
                )
            }

            composable(Tab.DISCOVER.route) {
                val discoverViewModel: DiscoverViewModel = viewModel(
                    factory = viewModelFactory { initializer { DiscoverViewModel(container.dataset) } }
                )
                DiscoverScreen(
                    viewModel = discoverViewModel,
                    countryCode = chartsCountry,
                    onOpenPodcast = openPodcast,
                    onSearch = { navController.navigate(SEARCH_ROUTE) },
                    onPick = { country, category ->
                        chartsViewModel.pick(country, category)
                        navController.navigate(Tab.CHARTS.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Tab.LIBRARY.route) {
                val libraryViewModel: LibraryViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { LibraryViewModel(container.store, container.dataset) }
                    }
                )
                LibraryScreen(
                    viewModel = libraryViewModel,
                    countryCode = chartsCountry,
                    onOpenPodcast = openPodcast,
                    onPlay = playAndOpen
                )
            }

            composable(SEARCH_ROUTE) {
                val searchViewModel: SearchViewModel = viewModel(
                    key = "search-$chartsCountry",
                    factory = viewModelFactory {
                        initializer { SearchViewModel(container.searchRepository, chartsCountry) }
                    }
                )
                SearchScreen(
                    viewModel = searchViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenPodcast = openPodcast,
                    onPlay = playAndOpen
                )
            }

            composable("tracker/{showId}?country={country}&title={title}&publisher={publisher}&art={art}") { entry ->
                val arguments = entry.arguments
                val showId = arguments?.getString("showId").orEmpty()
                val trackerViewModel: TrackerViewModel = viewModel(
                    key = "tracker-$showId",
                    factory = viewModelFactory {
                        initializer {
                            TrackerViewModel(
                                dataset = container.dataset,
                                showId = showId,
                                countryCode = arguments?.getString("country")
                                    ?: Catalog.defaultCountry.code,
                                title = arguments?.getString("title").orEmpty(),
                                publisher = arguments?.getString("publisher").orEmpty(),
                                artworkUrl = arguments?.getString("art")?.takeIf { it.isNotBlank() }
                            )
                        }
                    }
                )
                TrackerScreen(
                    viewModel = trackerViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(PLAYER_ROUTE) {
                PlayerScreen(
                    state = playback,
                    onCollapse = { navController.popBackStack() },
                    onTogglePlay = container.player::togglePlayPause,
                    onSeekTo = container.player::seekTo,
                    onSeekBy = container.player::seekBy
                )
            }

            composable("podcast/{showId}?feed={feed}&country={country}&title={title}") { entry ->
                val showId = entry.arguments?.getString("showId").orEmpty()
                val feedUrl = entry.arguments?.getString("feed")?.takeIf { it.isNotBlank() }
                val showTitle = entry.arguments?.getString("title")?.takeIf { it.isNotBlank() }
                val countryCode = entry.arguments?.getString("country")
                    ?: Catalog.defaultCountry.code
                val detailViewModel: DetailViewModel = viewModel(
                    key = showId,
                    factory = viewModelFactory {
                        initializer {
                            DetailViewModel(
                                showId = showId,
                                countryCode = countryCode,
                                feedUrl = feedUrl,
                                title = showTitle,
                                repository = container.podcastRepository,
                                store = container.store,
                                dataset = container.dataset
                            )
                        }
                    }
                )
                val podcast = detailViewModel.state.collectAsStateWithLifecycle().value.podcast
                DetailScreen(
                    viewModel = detailViewModel,
                    onOpenTracker = {
                        navController.navigate(
                            "tracker/${Uri.encode(showId)}?country=$countryCode" +
                                "&title=${Uri.encode(podcast?.title ?: showTitle.orEmpty())}" +
                                "&publisher=${Uri.encode(podcast?.publisher.orEmpty())}" +
                                "&art=${Uri.encode(podcast?.artworkUrl.orEmpty())}"
                        )
                    },
                    onBack = { navController.popBackStack() },
                    onPlay = playAndOpen
                )
            }
        }
    }
}
