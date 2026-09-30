package nl.woolacast.ui.library

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.woolacast.data.local.LocalStore
import nl.woolacast.data.opml.ImportOutcome
import nl.woolacast.data.opml.LinkProgress
import nl.woolacast.data.opml.Opml
import nl.woolacast.data.opml.OpmlFeed
import nl.woolacast.data.opml.ShowImporter
import nl.woolacast.ui.common.Artwork
import nl.woolacast.ui.common.ButtonKind
import nl.woolacast.ui.common.NoticePanel
import nl.woolacast.ui.common.TitleBar
import nl.woolacast.ui.common.WoolButton
import nl.woolacast.ui.common.WoolIcons
import nl.woolacast.ui.theme.DisplayFamily
import nl.woolacast.ui.theme.LocalChartColors

sealed interface ImportUi {
    data object Reading : ImportUi
    data class Checking(val done: Int, val total: Int) : ImportUi
    data class Done(val fileName: String?, val outcomes: List<ImportOutcome>) : ImportUi
    data class Error(val message: String) : ImportUi
}

/** Een geïmporteerde show zoals hij nu in de bibliotheek staat: gekoppeld of nog alleen op zijn feed. */
data class ImportedShow(val title: String, val artworkUrl: String?, val feedUrl: String, val linked: Boolean)

class ImportViewModel(
    private val uri: Uri,
    private val resolver: ContentResolver,
    private val importer: ShowImporter,
    private val store: LocalStore,
    private val countryCode: String,
    /** Leeft langer dan dit scherm, zodat het koppelen doorloopt als je weggaat. */
    private val appScope: CoroutineScope
) : ViewModel() {

    private val _ui = MutableStateFlow<ImportUi>(ImportUi.Reading)
    val ui: StateFlow<ImportUi> = _ui.asStateFlow()

    val linking: StateFlow<LinkProgress> = importer.linking

    /** De gevolgde shows uit dit bestand, zoals ze nu zijn: het koppelen verandert ze onder je ogen. */
    val imported: StateFlow<List<ImportedShow>> = combine(_ui, store.follows) { ui, follows ->
        val done = ui as? ImportUi.Done ?: return@combine emptyList()
        val byFeed = follows.filter { it.feedUrl != null }.associateBy { Opml.key(it.feedUrl!!) }
        done.outcomes.filterIsInstance<ImportOutcome.Followed>().mapNotNull { outcome ->
            val show = byFeed[Opml.key(outcome.feed.url)] ?: return@mapNotNull null
            ImportedShow(show.title, show.artworkUrl, outcome.feed.url, linked = !show.id.startsWith(Opml.FEED_PREFIX))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        run()
    }

    private fun run() = viewModelScope.launch {
        _ui.value = ImportUi.Reading
        val name = runCatching { displayName() }.getOrNull()
        val feeds = runCatching {
            withContext(Dispatchers.IO) {
                resolver.openInputStream(uri)?.use { Opml.parse(it) } ?: error("Kon het bestand niet openen.")
            }
        }.getOrElse {
            _ui.value = ImportUi.Error("Dit is geen OPML-bestand, of het is niet te lezen. Exporteer je shows opnieuw uit de andere app.")
            return@launch
        }
        if (feeds.isEmpty()) {
            _ui.value = ImportUi.Error("Er staan geen podcastfeeds in dit bestand.")
            return@launch
        }
        _ui.value = ImportUi.Checking(0, feeds.size)
        val outcomes = importer.import(feeds) { done, total -> _ui.value = ImportUi.Checking(done, total) }
        _ui.value = ImportUi.Done(name, outcomes)
        importer.startLinking(appScope, countryCode)
    }

    /** Eén feed nog eens proberen, bijvoorbeeld na een time-out. */
    fun retry(feed: OpmlFeed) = viewModelScope.launch {
        val current = _ui.value as? ImportUi.Done ?: return@launch
        val again = importer.import(listOf(feed)) { _, _ -> }.first()
        _ui.value = current.copy(outcomes = current.outcomes.map { if (it.feed == feed) again else it })
        if (again is ImportOutcome.Followed) importer.startLinking(appScope, countryCode)
    }

    private suspend fun displayName(): String? = withContext(Dispatchers.IO) {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: uri.lastPathSegment?.substringAfterLast('/')
    }
}

/**
 * Wat een import opleverde. Bovenaan het getal dat telt (hoeveel shows je nu
 * volgt), daaronder hoe ze erbij staan, en wat er misging met een knop om het
 * opnieuw te proberen.
 */
@Composable
fun ImportScreen(viewModel: ImportViewModel, onBack: () -> Unit, onDone: () -> Unit) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val linking by viewModel.linking.collectAsStateWithLifecycle()
    val imported by viewModel.imported.collectAsStateWithLifecycle()
    val colors = LocalChartColors.current

    Column(Modifier.fillMaxSize()) {
        TitleBar("Importeren", onBack)
        when (val state = ui) {
            ImportUi.Reading -> Busy("Bestand lezen…", null)
            is ImportUi.Checking -> Busy("Feeds controleren: ${state.done} van ${state.total}", state.done.toFloat() / state.total)
            is ImportUi.Error -> NoticePanel(
                title = "Importeren lukte niet",
                message = state.message,
                actionLabel = "Terug",
                onAction = onBack
            )
            is ImportUi.Done -> {
                val followed = state.outcomes.filterIsInstance<ImportOutcome.Followed>()
                val known = state.outcomes.filterIsInstance<ImportOutcome.Known>()
                val failed = state.outcomes.filterIsInstance<ImportOutcome.Failed>()
                val linked = imported.count { it.linked }
                val feedOnly = imported.filterNot { it.linked }
                LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp)) {
                    item {
                        Hero(followed.size, state.fileName, state.outcomes.size)
                        Spacer(Modifier.height(10.dp))
                    }
                    item {
                        SummaryRow(colors.rise, "Gekoppeld aan de catalogus", linked)
                        SummaryRow(colors.muted, if (linking.running) "Nog alleen op de feed" else "Alleen de feed, geen hitlijstplek", feedOnly.size)
                        if (known.isNotEmpty()) SummaryRow(MaterialTheme.colorScheme.outline, "Volgde je al", known.size)
                        SummaryRow(colors.fall, "Feed niet bereikbaar", failed.size)
                    }
                    if (linking.running) {
                        item {
                            Column(Modifier.padding(top = 14.dp)) {
                                Text(
                                    "Koppelen aan Apple's catalogus: ${linking.done} van ${linking.total}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { if (linking.total > 0) linking.done.toFloat() / linking.total else 0f },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    "Dat gaat rustig, één zoekopdracht per paar seconden. Je kunt de app gewoon gebruiken.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }
                    if (failed.isNotEmpty()) {
                        item { Label("Niet bereikbaar") }
                        items(failed, key = { "f-" + it.feed.url }) { outcome ->
                            FailedRow(outcome, onRetry = { viewModel.retry(outcome.feed) })
                        }
                    }
                    if (!linking.running && feedOnly.isNotEmpty()) {
                        item {
                            Label("Alleen de feed")
                            Text(
                                "Deze shows spelen en krijgen nieuwe afleveringen, maar Apple kent ze niet onder dezelfde feed. " +
                                    "Ze staan dus niet in de hitlijsten.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        items(feedOnly, key = { "o-" + it.feedUrl }) { show ->
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Artwork(show.artworkUrl, 44.dp, corner = 9.dp)
                                Text(show.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                    item {
                        WoolButton(
                            "Naar de bibliotheek",
                            onClick = onDone,
                            modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Busy(text: String, fraction: Float?) {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
        if (fraction != null) LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
        else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Hero(followed: Int, fileName: String?, feeds: Int) {
    val colors = LocalChartColors.current
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(if (followed > 0) colors.riseContainer else colors.fallContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (followed > 0) WoolIcons.Check else WoolIcons.Warning, null,
                tint = if (followed > 0) colors.onRiseContainer else colors.onFallContainer,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                when (followed) { 0 -> "Geen nieuwe shows"; 1 -> "1 show gevolgd"; else -> "$followed shows gevolgd" },
                fontFamily = DisplayFamily,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                listOfNotNull(fileName?.let { "uit $it" }, "$feeds ${if (feeds == 1) "feed" else "feeds"}").joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SummaryRow(dot: Color, label: String, count: Int) {
    Row(
        Modifier.fillMaxWidth().height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(dot))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text("$count", style = MaterialTheme.typography.titleMedium)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun Label(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = LocalChartColors.current.muted,
        modifier = Modifier.padding(top = 22.dp, bottom = 6.dp)
    )
}

@Composable
private fun FailedRow(outcome: ImportOutcome.Failed, onRetry: () -> Unit) {
    val colors = LocalChartColors.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(WoolIcons.Warning, null, tint = colors.fall, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(
                outcome.feed.title ?: outcome.feed.url.substringAfter("://").substringBefore('/'),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${outcome.feed.url.substringAfter("://")} · ${outcome.reason}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        WoolButton("Opnieuw", onClick = onRetry, kind = ButtonKind.OUTLINE)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}
