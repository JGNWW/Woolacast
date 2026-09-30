package nl.woolacast.ui.library

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nl.woolacast.data.download.DownloadProgress
import nl.woolacast.data.download.Downloads
import nl.woolacast.data.local.DownloadRecord
import nl.woolacast.data.local.DownloadSettings
import nl.woolacast.data.local.DownloadState
import nl.woolacast.data.local.LocalStore
import nl.woolacast.domain.Episode
import nl.woolacast.ui.common.Artwork
import nl.woolacast.ui.common.DownloadButton
import nl.woolacast.ui.common.DownloadUi
import nl.woolacast.ui.common.FilterChipBox
import nl.woolacast.ui.common.IconAction
import nl.woolacast.ui.common.NoticePanel
import nl.woolacast.ui.common.WoolIcons
import nl.woolacast.ui.common.byteSize
import nl.woolacast.ui.common.downloadUi
import nl.woolacast.ui.common.label
import nl.woolacast.ui.common.minutes
import nl.woolacast.ui.theme.LocalChartColors

/** De downloads zoals het tabblad ze toont. */
data class DownloadsUi(
    val records: List<DownloadRecord> = emptyList(),
    val progress: Map<String, DownloadProgress> = emptyMap(),
    val settings: DownloadSettings = DownloadSettings(),
    val waitingForWifi: Boolean = false,
    /** Hoeveel shows automatisch downloaden. */
    val autoShows: Int = 0
) {
    val usedBytes: Long get() = records.filter { it.state == DownloadState.DONE }.sumOf { it.bytes }
    val autoBytes: Long get() = records.filter { it.state == DownloadState.DONE && it.auto }.sumOf { it.bytes }
    val limitBytes: Long get() = settings.limitMb.toLong() * 1024 * 1024
    val pending: List<DownloadRecord> get() = records.filter { it.state != DownloadState.DONE }
    val done: List<DownloadRecord> get() = records.filter { it.state == DownloadState.DONE }
}

class DownloadsViewModel(
    private val context: Context,
    private val store: LocalStore,
    private val downloads: Downloads
) : ViewModel() {

    private val wifiWait = MutableStateFlow(downloads.waitingForWifi())

    val ui: StateFlow<DownloadsUi> = combine(
        store.downloads, downloads.progress, store.downloadSettings, wifiWait, store.autoDownload
    ) { records, progress, settings, waiting, auto ->
        DownloadsUi(
            records = records.values.sortedByDescending { it.addedAt },
            progress = progress,
            settings = settings,
            waitingForWifi = waiting,
            autoShows = auto.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadsUi())

    init {
        // Of er wifi is verandert buiten de app om; zo klopt "wacht op wifi" binnen een paar tellen.
        viewModelScope.launch {
            while (isActive) {
                wifiWait.value = downloads.waitingForWifi()
                delay(5_000)
            }
        }
    }

    fun remove(episodeId: String) = viewModelScope.launch { downloads.remove(episodeId) }
    fun retry(episodeId: String) = viewModelScope.launch { downloads.retry(episodeId) }
    fun download(episode: Episode) = viewModelScope.launch { downloads.enqueue(episode) }

    fun setSettings(settings: DownloadSettings) = viewModelScope.launch {
        val before = store.downloadSettings.value
        store.setDownloadSettings(settings)
        if (before.wifiOnly != settings.wifiOnly) {
            downloads.reschedulePending()
            runCatching { Downloads.scheduleAuto(context, settings) }
            wifiWait.value = downloads.waitingForWifi()
        }
        if (settings.limitMb < before.limitMb || (settings.deleteListened && !before.deleteListened)) downloads.cleanUp()
    }
}

/**
 * Bibliotheek → Gedownload. Bovenaan hoeveel ruimte het kost en hoe het
 * automatisch downloaden staat; daaronder wat nog bezig is en wat klaar is.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsTab(
    viewModel: DownloadsViewModel,
    playingId: String?,
    onPlay: (Episode) -> Unit
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var settingsOpen by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(bottom = 20.dp)) {
        item { StorageHeader(ui, onSettings = { settingsOpen = true }) }

        if (ui.records.isEmpty()) {
            item {
                NoticePanel(
                    title = "Nog niets gedownload",
                    message = "Tik op een aflevering en kies Downloaden, dan kun je hem ook zonder verbinding " +
                        "luisteren. Op de pagina van een podcast zet je automatisch downloaden aan.",
                    outerPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        }

        if (ui.pending.isNotEmpty()) {
            item { GroupLabel("Bezig") }
            items(ui.pending, key = { "p-" + it.episode.id }) { record ->
                DownloadRow(
                    record = record,
                    state = downloadUi(record, ui.progress[record.episode.id], ui.waitingForWifi),
                    playing = false,
                    onPlay = null,
                    onRemove = { viewModel.remove(record.episode.id) },
                    onRetry = { viewModel.retry(record.episode.id) }
                )
            }
        }
        if (ui.done.isNotEmpty()) {
            item { GroupLabel("Klaar om te luisteren") }
            items(ui.done, key = { "d-" + it.episode.id }) { record ->
                DownloadRow(
                    record = record,
                    state = DownloadUi.Done,
                    playing = record.episode.id == playingId,
                    onPlay = { onPlay(record.episode.toEpisode()) },
                    onRemove = { viewModel.remove(record.episode.id) },
                    onRetry = {}
                )
            }
        }
    }

    if (settingsOpen) {
        ModalBottomSheet(
            onDismissRequest = { settingsOpen = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            DownloadSettingsSheet(ui, viewModel::setSettings)
        }
    }
}

@Composable
private fun StorageHeader(ui: DownloadsUi, onSettings: () -> Unit) {
    val colors = LocalChartColors.current
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(byteSize(ui.usedBytes), style = MaterialTheme.typography.titleMedium)
            Text(
                " van ${byteSize(ui.limitBytes)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${ui.done.size} ${if (ui.done.size == 1) "aflevering" else "afleveringen"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
        val autoShare = (ui.autoBytes.toFloat() / ui.limitBytes).coerceIn(0f, 1f)
        val ownShare = ((ui.usedBytes - ui.autoBytes).toFloat() / ui.limitBytes).coerceIn(0f, 1f - autoShare)
        Row(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .semantics {
                    contentDescription = "${byteSize(ui.usedBytes)} van ${byteSize(ui.limitBytes)} gebruikt, " +
                        "waarvan ${byteSize(ui.autoBytes)} automatisch"
                }
        ) {
            if (autoShare > 0f) Box(Modifier.fillMaxHeight().weight(autoShare).background(MaterialTheme.colorScheme.primary))
            if (ownShare > 0f) Box(Modifier.fillMaxHeight().weight(ownShare).background(colors.muted))
            val rest = 1f - autoShare - ownShare
            if (rest > 0f) Spacer(Modifier.weight(rest))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Legend(MaterialTheme.colorScheme.primary, "Automatisch")
            Legend(colors.muted, "Zelf gedownload")
        }
        Spacer(Modifier.height(14.dp))
        val shape = RoundedCornerShape(16.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                .clickable(onClick = onSettings)
                .padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(WoolIcons.Wifi, null, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (ui.settings.wifiOnly) "Alleen op wifi" else "Ook via mobiele data",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    when (ui.autoShows) {
                        0 -> "Automatisch downloaden staat bij geen enkele show aan"
                        1 -> "Automatisch downloaden bij 1 show"
                        else -> "Automatisch downloaden bij ${ui.autoShows} shows"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("Instellen", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun Legend(color: androidx.compose.ui.graphics.Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(9.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.96.sp,
        color = LocalChartColors.current.muted,
        modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 4.dp)
    )
}

@Composable
private fun DownloadRow(
    record: DownloadRecord,
    state: DownloadUi,
    playing: Boolean,
    onPlay: (() -> Unit)?,
    onRemove: () -> Unit,
    onRetry: () -> Unit
) {
    val episode = record.episode
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onPlay != null) { onPlay?.invoke() }
            .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Artwork(episode.artworkUrl, 48.dp, corner = 10.dp)
        Column(Modifier.weight(1f)) {
            Text(
                episode.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (playing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                listOfNotNull(
                    episode.showTitle.takeIf { it.isNotBlank() },
                    when (state) {
                        DownloadUi.Done -> byteSize(record.bytes)
                        is DownloadUi.Failed -> state.reason
                        else -> state.label()
                    },
                    if (state == DownloadUi.Done) minutes(episode.durationMillis) else null
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = if (state is DownloadUi.Failed) LocalChartColors.current.fall else muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (record.auto && state == DownloadUi.Done) {
                Spacer(Modifier.height(5.dp))
                Tag("Automatisch")
            }
        }
        if (state == DownloadUi.Done) {
            IconAction(WoolIcons.Close, "Download verwijderen", onRemove, tint = muted, iconSize = 18.dp)
        } else {
            DownloadButton(state, onDownload = onRetry, onCancel = onRemove, onRetry = onRetry, onDone = {})
            if (state is DownloadUi.Failed) {
                IconAction(WoolIcons.Close, "Weghalen", onRemove, tint = muted, iconSize = 18.dp)
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(start = 80.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun Tag(text: String) {
    Box(
        Modifier
            .heightIn(min = 20.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 7.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DownloadSettingsSheet(ui: DownloadsUi, onChange: (DownloadSettings) -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
        Text("Downloads", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 10.dp))
        SettingSwitch(
            title = "Alleen op wifi",
            detail = "Downloads wachten tot er wifi is. Geldt voor alle shows.",
            checked = ui.settings.wifiOnly,
            onChange = { onChange(ui.settings.copy(wifiOnly = it)) }
        )
        SettingSwitch(
            title = "Uitgeluisterd wissen",
            detail = "Een dag na het uitluisteren. Bewaard en in de wachtrij blijft altijd staan.",
            checked = ui.settings.deleteListened,
            onChange = { onChange(ui.settings.copy(deleteListened = it)) }
        )
        Text(
            "Ruimte voor downloads",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LIMITS.forEach { mb ->
                FilterChipBox(byteSize(mb * 1024L * 1024L), selected = ui.settings.limitMb == mb, onClick = {
                    onChange(ui.settings.copy(limitMb = mb))
                })
            }
        }
        Text(
            "Is het vol, dan ruimt de app eerst oudere automatische downloads op. Wat je zelf downloadde blijft staan.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

@Composable
fun SettingSwitch(title: String, detail: String, checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
        )
    }
}

private val LIMITS = listOf(512, 1024, 2048, 5120, 10240)
