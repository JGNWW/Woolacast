package nl.woolacast.ui.settings

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.woolacast.data.inbox.NewEpisodeNotifier
import nl.woolacast.data.local.DownloadSettings
import nl.woolacast.data.local.LocalStore
import nl.woolacast.player.SPEEDS
import nl.woolacast.ui.common.FilterChipBox
import nl.woolacast.ui.common.TitleBar
import nl.woolacast.ui.common.WoolIcons
import nl.woolacast.ui.common.byteSize
import nl.woolacast.ui.common.speedLabel
import nl.woolacast.ui.library.LIMITS
import nl.woolacast.ui.library.SettingSwitch

class SettingsViewModel(
    private val store: LocalStore,
    /** De algemene snelheid veranderde: wat nu speelt gaat mee, tenzij de show een eigen snelheid heeft. */
    private val onSpeed: (Float) -> Unit = {},
    /** Downloadinstellingen gaan via het tabblad Gedownload, dat ook de taak opnieuw plant en opruimt. */
    private val onDownloadSettings: (DownloadSettings) -> Unit = {}
) : ViewModel() {

    val theme = store.theme
    val prefs = store.prefs
    val playback = store.playback
    val downloadSettings = store.downloadSettings

    fun setTheme(mode: String) = viewModelScope.launch { store.setTheme(mode) }
    fun setOpenPlayer(on: Boolean) = viewModelScope.launch { store.setPrefs { it.copy(openPlayerOnPlay = on) } }
    fun setChartAlerts(on: Boolean) = viewModelScope.launch { store.setPrefs { it.copy(chartAlertNotify = on) } }
    fun setSkipSilence(on: Boolean) = viewModelScope.launch { store.setPlayback { it.copy(skipSilence = on) } }
    fun setVolumeBoost(on: Boolean) = viewModelScope.launch { store.setPlayback { it.copy(volumeBoost = on) } }

    fun setSpeed(speed: Float) {
        viewModelScope.launch { store.setPlayback { it.copy(speed = speed) } }
        onSpeed(speed)
    }

    fun setDownloads(settings: DownloadSettings) = onDownloadSettings(settings)

    fun clearHistory(onDone: () -> Unit) = viewModelScope.launch {
        store.clearHistory()
        onDone()
    }

    /** Schrijft de reservekopie naar het gekozen bestand en meldt in een zin hoe het ging. */
    fun backup(uri: Uri, resolver: android.content.ContentResolver, onDone: (String) -> Unit) = viewModelScope.launch {
        val ok = runCatching {
            val text = store.exportBackup()
            withContext(Dispatchers.IO) {
                resolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) } ?: error("geen bestand")
            }
        }.isSuccess
        onDone(if (ok) "Reservekopie opgeslagen." else "Opslaan lukte niet. Kies een andere plek.")
    }

    fun restore(uri: Uri, resolver: android.content.ContentResolver, onDone: (String) -> Unit) = viewModelScope.launch {
        val text = runCatching {
            withContext(Dispatchers.IO) { resolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } }
        }.getOrNull()
        val ok = text != null && store.importBackup(text)
        if (ok) store.flush()
        onDone(
            if (ok) "Reservekopie teruggezet. Je shows, wachtrij en voortgang staan er weer."
            else "Dit is geen reservekopie van Toadcast. Er is niets veranderd."
        )
    }
}

/**
 * Alle instellingen op één plek. Wat per show geldt (melding, automatisch
 * downloaden, een eigen snelheid) staat op de pagina van die show.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenStats: () -> Unit,
    versionName: String = ""
) {
    val context = LocalContext.current
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    val downloads by viewModel.downloadSettings.collectAsStateWithLifecycle()
    var restoreFrom by remember { mutableStateOf<Uri?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_LONG).show()

    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.setChartAlerts(true)
        else toast("Meldingen staan uit voor Toadcast. Zet ze aan in de instellingen van Android.")
    }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.backup(uri, context.contentResolver, ::toast)
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) restoreFrom = uri
    }

    Column(Modifier.fillMaxSize()) {
        TitleBar("Instellingen", onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp)) {

            Group("Weergave")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("system" to "Zoals het toestel", "light" to "Licht", "dark" to "Donker").forEach { (mode, label) ->
                    FilterChipBox(label, selected = theme == mode, onClick = { viewModel.setTheme(mode) }, modifier = Modifier.heightIn(min = 44.dp))
                }
            }

            Group("Afspelen")
            SettingSwitch(
                title = "Speler openen bij afspelen",
                detail = "Uit: een aflevering begint in de balk onderin en je blijft waar je was.",
                checked = prefs.openPlayerOnPlay,
                onChange = viewModel::setOpenPlayer
            )
            SettingSwitch(
                title = "Stiltes overslaan",
                detail = "Pauzes in het gesprek worden ingekort. Werkt op de telefoon, niet bij casten.",
                checked = playback.skipSilence,
                onChange = viewModel::setSkipSilence
            )
            SettingSwitch(
                title = "Zachte opnames luider",
                detail = "Zes decibel erbij, voor podcasts die te zacht zijn opgenomen.",
                checked = playback.volumeBoost,
                onChange = viewModel::setVolumeBoost
            )
            Text(
                "Snelheid",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SPEEDS.forEach { speed ->
                    FilterChipBox(
                        speedLabel(speed),
                        selected = kotlin.math.abs(playback.speed - speed) < 0.01f,
                        onClick = { viewModel.setSpeed(speed) },
                        modifier = Modifier.heightIn(min = 44.dp)
                    )
                }
            }
            Note(
                (if (SPEEDS.none { kotlin.math.abs(playback.speed - it) < 0.01f }) "Nu ${speedLabel(playback.speed)}. " else "") +
                    "Geldt voor alles zonder eigen snelheid. In de speler kies je elke snelheid tussen 0,5× en 3×, " +
                    "ook voor één show apart."
            )

            Group("Downloads")
            SettingSwitch(
                title = "Alleen op wifi",
                detail = "Downloads wachten tot er wifi is. Geldt voor alle shows.",
                checked = downloads.wifiOnly,
                onChange = { viewModel.setDownloads(downloads.copy(wifiOnly = it)) }
            )
            SettingSwitch(
                title = "Uitgeluisterd wissen",
                detail = "Een dag na het uitluisteren. Bewaard en in de wachtrij blijft altijd staan.",
                checked = downloads.deleteListened,
                onChange = { viewModel.setDownloads(downloads.copy(deleteListened = it)) }
            )
            Text(
                "Ruimte voor downloads",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LIMITS.forEach { mb ->
                    FilterChipBox(
                        byteSize(mb * 1024L * 1024L),
                        selected = downloads.limitMb == mb,
                        onClick = { viewModel.setDownloads(downloads.copy(limitMb = mb)) },
                        modifier = Modifier.heightIn(min = 44.dp)
                    )
                }
            }

            Group("Meldingen")
            SettingSwitch(
                title = "Chart-alerts",
                detail = "Een melding als een show die je volgt op 1 komt, de top tien in gaat, nieuw binnenkomt " +
                    "of twintig plekken stijgt. Twee keer per dag gekeken.",
                checked = prefs.chartAlertNotify,
                onChange = { on ->
                    if (on && !NewEpisodeNotifier.canPost(context)) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else viewModel.setChartAlerts(on)
                }
            )
            Note("Een melding bij een nieuwe aflevering zet je per show aan, met de bel op de pagina van de podcast.")

            Group("Je gegevens")
            ActionLine(WoolIcons.Bars, "Luisterstatistiek", "Hoeveel je luisterde, en naar wat", onOpenStats)
            ActionLine(
                WoolIcons.FileOut, "Reservekopie maken",
                "Shows, makers, wachtrij, voortgang, geschiedenis en instellingen in één bestand"
            ) { backupLauncher.launch("toadcast-reservekopie-${LocalDate.now()}.json") }
            ActionLine(
                WoolIcons.FileIn, "Reservekopie terugzetten",
                "Vervangt wat er nu staat. Downloads op dit toestel blijven."
            ) { restoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
            ActionLine(WoolIcons.Trash, "Geschiedenis wissen", "Ook de luistertijd. Waar je in een aflevering gebleven bent, blijft staan.") {
                confirmClear = true
            }
            Note("Alles staat alleen op dit toestel. Je shows uitwisselen met een andere app kan met OPML, via Bibliotheek → ⋮.")

            if (versionName.isNotBlank()) {
                Text(
                    "Toadcast $versionName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 28.dp)
                )
            }
        }
    }

    restoreFrom?.let { uri ->
        AlertDialog(
            onDismissRequest = { restoreFrom = null },
            title = { Text("Reservekopie terugzetten?") },
            text = { Text("Je shows, wachtrij, voortgang en instellingen worden vervangen door wat er in het bestand staat.") },
            confirmButton = {
                TextButton(onClick = {
                    restoreFrom = null
                    viewModel.restore(uri, context.contentResolver, ::toast)
                }) { Text("Terugzetten") }
            },
            dismissButton = { TextButton(onClick = { restoreFrom = null }) { Text("Annuleren") } }
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Geschiedenis wissen?") },
            text = { Text("De lijst met wat je afspeelde en je luistertijd gaan weg. Dit kan niet ongedaan gemaakt worden.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    viewModel.clearHistory { toast("Geschiedenis gewist.") }
                }) { Text("Wissen") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Annuleren") } }
        )
    }
}

@Composable
private fun Group(title: String) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(top = 20.dp))
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 18.dp, bottom = 10.dp)
    )
}

@Composable
private fun Note(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 10.dp)
    )
}

@Composable
private fun ActionLine(icon: ImageVector, title: String, detail: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(WoolIcons.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
    }
}
