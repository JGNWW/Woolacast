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
import nl.woolacast.ui.library.LibraryScreen

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

    Scaffold(
        bottomBar = {
            Column {
                if (playback.hasEpisode) {
                    MiniPlayer(
                        state = playback,
                        onTogglePlay = container.player::togglePlayPause,
                        onSkipForward = { container.player.seekBy(30_000L) }
                    )
                    Spacer(Modifier.height(6.dp))
                }
                NavigationBar {
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
                    onOpenPodcast = { showId, feedUrl, countryCode ->
                        navController.navigate(
                            "podcast/$showId?feed=${Uri.encode(feedUrl.orEmpty())}&country=$countryCode"
                        )
                    }
                )
            }

            composable(Tab.DISCOVER.route) {
                DiscoverScreen(
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
                LibraryScreen(
                    store = container.store,
                    onOpenPodcast = { showId, feedUrl ->
                        navController.navigate(
                            "podcast/$showId?feed=${Uri.encode(feedUrl.orEmpty())}" +
                                "&country=${Catalog.defaultCountry.code}"
                        )
                    }
                )
            }

            composable("podcast/{showId}?feed={feed}&country={country}") { entry ->
                val showId = entry.arguments?.getString("showId").orEmpty()
                val feedUrl = entry.arguments?.getString("feed")?.takeIf { it.isNotBlank() }
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
                                repository = container.podcastRepository,
                                store = container.store
                            )
                        }
                    }
                )
                DetailScreen(
                    viewModel = detailViewModel,
                    onBack = { navController.popBackStack() },
                    onPlay = container.player::play
                )
            }
        }
    }
}
