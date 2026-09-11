package nl.woolacast.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import nl.woolacast.AppContainer
import nl.woolacast.data.local.toSaved
import nl.woolacast.domain.Catalog
import nl.woolacast.domain.ChartLevel
import nl.woolacast.domain.ChartQuery
import nl.woolacast.domain.Episode
import nl.woolacast.domain.SourceId
import nl.woolacast.ui.charts.ChartListScreen
import nl.woolacast.ui.charts.ChartsScreen
import nl.woolacast.ui.charts.ChartsViewModel
import nl.woolacast.ui.common.MiniPlayer
import nl.woolacast.ui.common.WoolIcons
import nl.woolacast.ui.detail.DetailScreen
import nl.woolacast.ui.detail.DetailViewModel
import nl.woolacast.ui.discover.DiscoverScreen
import nl.woolacast.ui.discover.DiscoverViewModel
import nl.woolacast.ui.library.LibraryScreen
import nl.woolacast.ui.library.LibraryViewModel
import nl.woolacast.ui.player.PlayerScreen
import nl.woolacast.ui.search.SearchScreen
import nl.woolacast.ui.search.SearchViewModel
import nl.woolacast.ui.theme.LocalChartColors
import nl.woolacast.ui.tips.TipsScreen
import nl.woolacast.ui.tips.TipsViewModel
import nl.woolacast.ui.tracker.TrackerScreen
import nl.woolacast.ui.tracker.TrackerViewModel

private const val PLAYER_ROUTE = "player"

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    CHARTS("charts", "Hitlijsten", WoolIcons.Charts),
    DISCOVER("discover", "Ontdek", WoolIcons.Discover),
    LIBRARY("library", "Bibliotheek", WoolIcons.Library);

    /** Het startscherm van dit tabblad; de rest van de stapel staat eronder. */
    val home get() = "$route/home"

    companion object {
        /** Het tabblad waar een bestemming onder valt, of null voor de speler. */
        fun of(destination: NavDestination?): Tab? =
            entries.firstOrNull { tab -> destination?.hierarchy?.any { it.route == tab.route } == true }
    }
}

/**
 * Elk tabblad heeft zijn eigen stapel (podcastpagina, tracker, zoeken), zodat
 * wisselen en terugkomen je precies daar terugzet waar je was. Op het actieve
 * tabblad tikken brengt je naar zijn startscherm. Alleen de speler staat
 * erbuiten: die hoort bij geen enkel tabblad.
 */
@Composable
fun WoolacastNav(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination
    val scope = rememberCoroutineScope()

    val playback by container.player.state.collectAsStateWithLifecycle()
    val queue by container.store.queue.collectAsStateWithLifecycle()
    val saved by container.store.saved.collectAsStateWithLifecycle()

    // Het tabblad waar we (het laatst) op stonden; op het spelerscherm is er geen.
    var currentTab by remember { mutableStateOf(Tab.CHARTS) }
    LaunchedEffect(currentRoute) { Tab.of(currentRoute)?.let { currentTab = it } }

    // Eén instantie voor de hele app, zodat Ontdek de lijst kan instellen die
    // het hitlijstenscherm daarna toont.
    val chartsViewModel: ChartsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ChartsViewModel(container.chartRepository, container.podcastRepository) }
        }
    )
    val chartsState by chartsViewModel.state.collectAsStateWithLifecycle()
    val chartsCountry = chartsState.query.country.code

    val selectTab: (Tab) -> Unit = { tab ->
        if (Tab.of(currentRoute) == tab) {
            // Al op dit tabblad: terug naar zijn startscherm.
            navController.popBackStack(tab.home, inclusive = false)
        } else {
            navController.navigate(tab.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val openPodcast: (String, String?, String) -> Unit = { showId, feedUrl, title ->
        navController.navigate(
            "${currentTab.route}/podcast/${Uri.encode(showId)}" +
                "?feed=${Uri.encode(feedUrl.orEmpty())}" +
                "&country=$chartsCountry&title=${Uri.encode(title)}"
        )
    }
    val openSearch = { navController.navigate("${currentTab.route}/search") }
    val openPlayer = { navController.navigate(PLAYER_ROUTE) { launchSingleTop = true } }
    val playAndOpen: (Episode, String?) -> Unit = { episode, label ->
        container.player.play(episode, label)
        openPlayer()
    }

    // Afleveringen uit een hitlijst hebben geen audio-URL; de ViewModel zoekt
    // die op in de feed en meldt zich hier zodra hij speelbaar is.
    LaunchedEffect(chartsViewModel) {
        chartsViewModel.playRequests.collect { request ->
            playAndOpen(request.episode, request.label)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            val onPlayerRoute = currentRoute?.route == PLAYER_ROUTE
            if (!onPlayerRoute) Column(Modifier.background(MaterialTheme.colorScheme.background)) {
                // Op het spelerscherm zelf hoeft de mini-speler er niet ook te staan.
                if (playback.hasEpisode) {
                    MiniPlayer(
                        state = playback,
                        onExpand = openPlayer,
                        onTogglePlay = container.player::togglePlayPause,
                        onSkipForward = { container.player.seekBy(30_000L) }
                    )
                    Spacer(Modifier.height(6.dp))
                }
                BottomNav(selected = Tab.of(currentRoute), onSelect = selectTab)
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.CHARTS.route,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            navigation(startDestination = Tab.CHARTS.home, route = Tab.CHARTS.route) {
                composable(Tab.CHARTS.home) {
                    ChartsScreen(
                        viewModel = chartsViewModel,
                        repository = container.chartRepository,
                        playingId = playback.episodeId,
                        onSearch = openSearch,
                        onAlerts = { selectTab(Tab.LIBRARY) },
                        onOpenPodcast = { showId, feedUrl, _, title -> openPodcast(showId, feedUrl, title) }
                    )
                }
                tabScreens(Tab.CHARTS, navController, container, chartsCountry, playback.episodeId, openPodcast, playAndOpen)
            }

            navigation(startDestination = Tab.DISCOVER.home, route = Tab.DISCOVER.route) {
                composable(Tab.DISCOVER.home) {
                    val discoverViewModel: DiscoverViewModel = viewModel(
                        factory = viewModelFactory { initializer { DiscoverViewModel(container.dataset) } }
                    )
                    DiscoverScreen(
                        viewModel = discoverViewModel,
                        countryCode = chartsCountry,
                        sourceCount = { country ->
                            container.chartRepository.allSources().count { it.covers(country.code) }
                        },
                        onOpenPodcast = openPodcast,
                        onSearch = openSearch,
                        onAlerts = { selectTab(Tab.LIBRARY) },
                        onTips = { navController.navigate("${Tab.DISCOVER.route}/tips") },
                        onPick = { country, category ->
                            // Een land of categorie vanaf Ontdek opent zijn eigen lijst.
                            val query = chartsState.query.copy(
                                country = country ?: chartsState.query.country,
                                category = category ?: chartsState.query.category
                            )
                            navController.navigate(
                                "${Tab.DISCOVER.route}/list?source=${query.source.name}" +
                                    "&country=${query.country.code}&genre=${query.category.appleGenreId ?: 0}" +
                                    "&level=${query.level.name}"
                            )
                        }
                    )
                }
                tabScreens(Tab.DISCOVER, navController, container, chartsCountry, playback.episodeId, openPodcast, playAndOpen)
            }

            navigation(startDestination = Tab.LIBRARY.home, route = Tab.LIBRARY.route) {
                composable(Tab.LIBRARY.home) {
                    val libraryViewModel: LibraryViewModel = viewModel(
                        factory = viewModelFactory {
                            initializer {
                                LibraryViewModel(container.store, container.dataset, container.podcastRepository)
                            }
                        }
                    )
                    LibraryScreen(
                        viewModel = libraryViewModel,
                        countryCode = chartsCountry,
                        playingId = playback.episodeId,
                        onOpenPodcast = openPodcast,
                        onSearch = openSearch,
                        onPlay = { episode -> playAndOpen(episode, null) }
                    )
                }
                tabScreens(Tab.LIBRARY, navController, container, chartsCountry, playback.episodeId, openPodcast, playAndOpen)
            }

            composable(PLAYER_ROUTE) {
                val episode = playback.episode
                PlayerScreen(
                    state = playback,
                    queue = queue,
                    isSaved = episode != null && saved.any { it.id == episode.id },
                    onCollapse = { navController.popBackStack() },
                    onTogglePlay = container.player::togglePlayPause,
                    onSeekTo = container.player::seekTo,
                    onSeekBy = container.player::seekBy,
                    onPrevious = container.player::previous,
                    onNext = { container.player.next() },
                    onSpeed = container.player::setSpeed,
                    onSleep = container.player::setSleepTimer,
                    onToggleSave = {
                        episode?.let { scope.launch { container.store.toggleSaved(it.toSaved()) } }
                    },
                    onPlayQueued = { item ->
                        scope.launch { container.store.removeFromQueue(item.id) }
                        container.player.play(item.toEpisode())
                    },
                    onRemoveQueued = { id -> scope.launch { container.store.removeFromQueue(id) } },
                    onOpenPodcast = {
                        episode?.let {
                            navController.popBackStack()
                            openPodcast(it.showId, null, it.showTitle)
                        }
                    },
                    onDismissError = container.player::clearError
                )
            }
        }
    }
}

/** De schermen die elk tabblad onder zich kan hebben: zoeken, podcastpagina, tracker. */
private fun NavGraphBuilder.tabScreens(
    tab: Tab,
    navController: NavHostController,
    container: AppContainer,
    chartsCountry: String,
    playingId: String?,
    openPodcast: (String, String?, String) -> Unit,
    playAndOpen: (Episode, String?) -> Unit
) {
    val prefix = tab.route

    composable("$prefix/list?source={source}&country={country}&genre={genre}&level={level}") { entry ->
        val arguments = entry.arguments
        val query = ChartQuery(
            source = runCatching { SourceId.valueOf(arguments?.getString("source").orEmpty()) }.getOrDefault(SourceId.APPLE),
            country = Catalog.country(arguments?.getString("country") ?: chartsCountry),
            category = Catalog.category(arguments?.getString("genre")?.toIntOrNull()?.takeIf { it != 0 }),
            level = runCatching { ChartLevel.valueOf(arguments?.getString("level").orEmpty()) }.getOrDefault(ChartLevel.SHOWS)
        )
        val listViewModel: ChartsViewModel = viewModel(
            key = "list-${query.key}",
            factory = viewModelFactory {
                initializer { ChartsViewModel(container.chartRepository, container.podcastRepository, query) }
            }
        )
        LaunchedEffect(listViewModel) {
            listViewModel.playRequests.collect { request -> playAndOpen(request.episode, request.label) }
        }
        ChartListScreen(
            viewModel = listViewModel,
            repository = container.chartRepository,
            onBack = { navController.popBackStack() },
            onOpenPodcast = { showId, feedUrl, _, title -> openPodcast(showId, feedUrl, title) }
        )
    }

    composable("$prefix/tips") {
        val tipsViewModel: TipsViewModel = viewModel(
            factory = viewModelFactory {
                initializer {
                    TipsViewModel(container.dataset, container.liveTips, container.store)
                }
            }
        )
        TipsScreen(
            viewModel = tipsViewModel,
            countryCode = chartsCountry,
            onBack = { navController.popBackStack() },
            // Het land van de tips volgt de hitlijsten; daar kies je het.
            onPickCountry = { navController.popBackStack() },
            onOpenPodcast = openPodcast
        )
    }

    composable("$prefix/search") {
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
            onPlay = { episode -> playAndOpen(episode, null) }
        )
    }

    composable("$prefix/podcast/{showId}?feed={feed}&country={country}&title={title}") { entry ->
        val showId = entry.arguments?.getString("showId").orEmpty()
        val feedUrl = entry.arguments?.getString("feed")?.takeIf { it.isNotBlank() }
        val showTitle = entry.arguments?.getString("title")?.takeIf { it.isNotBlank() }
        val countryCode = entry.arguments?.getString("country") ?: Catalog.defaultCountry.code
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
                        dataset = container.dataset,
                        charts = container.chartRepository,
                        liveTips = container.liveTips
                    )
                }
            }
        )
        val podcast = detailViewModel.state.collectAsStateWithLifecycle().value.podcast
        DetailScreen(
            viewModel = detailViewModel,
            playingId = playingId,
            onOpenTracker = {
                navController.navigate(
                    "$prefix/tracker/${Uri.encode(showId)}?country=$countryCode" +
                        "&title=${Uri.encode(podcast?.title ?: showTitle.orEmpty())}" +
                        "&publisher=${Uri.encode(podcast?.publisher.orEmpty())}" +
                        "&art=${Uri.encode(podcast?.artworkUrl.orEmpty())}" +
                        "&genre=${Uri.encode(podcast?.genre.orEmpty())}" +
                        "&feed=${Uri.encode(podcast?.feedUrl.orEmpty())}"
                )
            },
            onBack = { navController.popBackStack() },
            onPlay = playAndOpen
        )
    }

    composable("$prefix/tracker/{showId}?country={country}&title={title}&publisher={publisher}&art={art}&genre={genre}&feed={feed}") { entry ->
        val arguments = entry.arguments
        val showId = arguments?.getString("showId").orEmpty()
        val trackerViewModel: TrackerViewModel = viewModel(
            key = "tracker-$showId",
            factory = viewModelFactory {
                initializer {
                    TrackerViewModel(
                        dataset = container.dataset,
                        store = container.store,
                        showId = showId,
                        countryCode = arguments?.getString("country") ?: Catalog.defaultCountry.code,
                        title = arguments?.getString("title").orEmpty(),
                        publisher = arguments?.getString("publisher").orEmpty(),
                        artworkUrl = arguments?.getString("art")?.takeIf { it.isNotBlank() },
                        genre = arguments?.getString("genre")?.takeIf { it.isNotBlank() },
                        feedUrl = arguments?.getString("feed")?.takeIf { it.isNotBlank() }
                    )
                }
            }
        )
        TrackerScreen(viewModel = trackerViewModel, onBack = { navController.popBackStack() })
    }
}

/** Navigatie zoals in de mockup: drie items, icoon met label, actief in accentkleur. */
@Composable
private fun BottomNav(selected: Tab?, onSelect: (Tab) -> Unit) {
    val muted = LocalChartColors.current.muted
    Column(Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Tab.entries.forEach { tab ->
                val on = tab == selected
                val tint = if (on) MaterialTheme.colorScheme.primary else muted
                Column(
                    modifier = Modifier
                        .width(96.dp)
                        .height(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelect(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
                ) {
                    Icon(tab.icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                    Text(tab.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = tint)
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}
