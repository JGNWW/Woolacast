package nl.woolacast.data.download

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.File
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import nl.woolacast.data.local.DownloadRecord
import nl.woolacast.data.local.DownloadSettings
import nl.woolacast.data.local.DownloadState
import nl.woolacast.data.local.LocalStore
import nl.woolacast.data.local.toSaved
import nl.woolacast.domain.Episode

/** Hoe ver een lopende download is. [total] is 0 als de server het niet zegt. */
data class DownloadProgress(val bytes: Long, val total: Long) {
    val fraction: Float? get() = if (total > 0L) (bytes.toFloat() / total).coerceIn(0f, 1f) else null
}

/**
 * Afleveringen op het toestel. Het ophalen zelf doet [DownloadWorker] via
 * WorkManager, zodat het doorgaat als de app dicht is en wacht op wifi als dat
 * zo is ingesteld. Hier staat de administratie: wat er is, wat er mag, en wat
 * er weg kan.
 */
class Downloads(
    private val context: Context,
    private val store: LocalStore,
    /** De aflevering die nu speelt; die wissen we nooit onder je handen weg. */
    private val playingId: () -> String? = { null }
) {
    val dir: File = File(context.filesDir, "downloads")

    private val _progress = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    /** Lopende downloads; alleen wat nu echt binnenkomt staat erin. */
    val progress: StateFlow<Map<String, DownloadProgress>> = _progress.asStateFlow()

    internal fun report(episodeId: String, value: DownloadProgress?) {
        // Twee downloads tegelijk melden zich vanaf verschillende threads.
        _progress.update { if (value == null) it - episodeId else it + (episodeId to value) }
    }

    /**
     * Past deze aflevering nog binnen de grens? Voor automatisch downloaden: wat
     * niet past, haalt de app niet binnen, in plaats van het binnen te halen en
     * meteen weer op te ruimen. Zonder bekende duur rekenen we met een uur.
     */
    fun fits(episode: Episode): Boolean {
        val estimate = (episode.durationMillis ?: 3_600_000L) / 1000 * ESTIMATED_BYTES_PER_SECOND
        val pending = store.downloads.value.values.filter { it.state == DownloadState.QUEUED }.sumOf {
            (it.episode.durationMillis ?: 3_600_000L) / 1000 * ESTIMATED_BYTES_PER_SECOND
        }
        return usedBytes() + pending + estimate <= store.downloadSettings.value.limitMb.toLong() * 1024 * 1024
    }

    fun file(record: DownloadRecord) = File(dir, record.fileName)

    /** Het bestand van een klaar gedownloade aflevering, of null. */
    fun localFile(episodeId: String): File? {
        val record = store.download(episodeId)?.takeIf { it.state == DownloadState.DONE } ?: return null
        return file(record).takeIf { it.exists() }
    }

    /** Zet een aflevering in de rij. Een tweede keer doet niets, behalve bij een mislukte. */
    suspend fun enqueue(episode: Episode, auto: Boolean = false) {
        if (episode.audioUrl == null) return
        val existing = store.download(episode.id)
        if (existing != null && existing.state != DownloadState.FAILED) {
            // Zelf gekozen wint van automatisch: dan ruimt de app hem niet meer op.
            if (!auto && existing.auto) store.updateDownload(episode.id) { it.copy(auto = false) }
            return
        }
        store.putDownload(
            DownloadRecord(
                episode = episode.toSaved(),
                fileName = fileNameFor(episode),
                auto = auto && existing?.auto != false,
                addedAt = Instant.now().toString()
            )
        )
        schedule(episode.id)
    }

    fun schedule(episodeId: String) {
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(constraints(store.downloadSettings.value))
            .setInputData(workDataOf(DownloadWorker.KEY_EPISODE to episodeId))
            .addTag(TAG)
            .build()
        runCatching {
            WorkManager.getInstance(context).enqueueUniqueWork(workName(episodeId), ExistingWorkPolicy.KEEP, request)
        }
    }

    /** Opnieuw proberen na een mislukte download. */
    suspend fun retry(episodeId: String) {
        store.updateDownload(episodeId) { it.copy(state = DownloadState.QUEUED, error = null) }
        schedule(episodeId)
    }

    /**
     * Stopt een download of wist het bestand. Speelt de aflevering nu vanaf dat
     * bestand, dan blijft het staan tot hij klaar is; anders valt de bron onder de
     * speler weg. [cleanUp] ruimt het daarna op.
     */
    suspend fun remove(episodeId: String) {
        runCatching { WorkManager.getInstance(context).cancelUniqueWork(workName(episodeId)) }
        report(episodeId, null)
        store.download(episodeId)?.let { record ->
            File(dir, record.fileName + PART).delete()
            File(dir, record.fileName + PART + AudioFetcher.META).delete()
            if (episodeId == playingId()) store.addOrphan(record.fileName, episodeId) else file(record).delete()
            // Een automatische download die weg moest (of die je zelf weghaalde) komt niet vanzelf terug.
            if (record.auto && record.listenedAt == null) store.addEvicted(listOf(episodeId))
        }
        store.removeDownload(episodeId)
    }

    /** Wat er nu op het toestel staat, in bytes. */
    fun usedBytes(): Long = store.downloads.value.values.filter { it.state == DownloadState.DONE }.sumOf { it.bytes }

    /** Wacht een download op wifi? Dan zegt de rij dat, in plaats van "in de rij". */
    fun waitingForWifi(): Boolean {
        if (!store.downloadSettings.value.wifiOnly) return false
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val caps = manager.getNetworkCapabilities(manager.activeNetwork) ?: return true
        return !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    /**
     * Ruimt op: uitgeluisterde downloads een dag na het uitluisteren, en daarna
     * de oudste automatische tot alles onder de grens past. Bewaarde afleveringen,
     * de wachtrij en wat nu speelt blijven altijd staan.
     */
    suspend fun cleanUp(now: Instant = Instant.now()) {
        val playing = playingId()
        // Bestanden die bleven staan omdat ze speelden: nu weg, tenzij ze nog spelen.
        store.orphans.value.filterValues { it != playing }.forEach { (fileName, _) ->
            File(dir, fileName).delete()
            store.removeOrphan(fileName)
        }
        // Half beluisterd telt ook als beschermd: dat wil je nog afmaken.
        val protected = store.queue.value.map { it.id }.toSet() + store.saved.value.map { it.id } +
            store.progress.value.keys + listOfNotNull(playing)
        val doomed = DownloadPolicy.toDelete(store.downloads.value.values.toList(), store.downloadSettings.value, protected, now)
        doomed.forEach { remove(it) }
    }

    /** Zet de voorwaarden (wifi of niet) opnieuw op alles wat nog wacht. */
    fun reschedulePending() {
        store.downloads.value.values.filter { it.state == DownloadState.QUEUED }.forEach { record ->
            runCatching { WorkManager.getInstance(context).cancelUniqueWork(workName(record.episode.id)) }
            schedule(record.episode.id)
        }
    }

    companion object {
        const val TAG = "download"
        /** Een schatting voor podcastaudio: 128 kbit/s. */
        private const val ESTIMATED_BYTES_PER_SECOND = 16_000L
        internal const val PART = ".part"

        fun workName(episodeId: String) = "download-$episodeId"

        fun constraints(settings: DownloadSettings): Constraints = Constraints.Builder()
            .setRequiredNetworkType(if (settings.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresStorageNotLow(true)
            .build()

        /** Een bestandsnaam die bij de aflevering hoort en niets uit de titel lekt. */
        fun fileNameFor(episode: Episode): String {
            val digest = MessageDigest.getInstance("SHA-1").digest(episode.id.toByteArray())
                .joinToString("") { "%02x".format(it) }.take(20)
            val extension = episode.audioUrl.orEmpty().substringBefore('?').substringAfterLast('.', "")
                .lowercase().takeIf { it in setOf("mp3", "m4a", "aac", "ogg", "opus", "mp4", "wav") } ?: "mp3"
            return "$digest.$extension"
        }

        /** Kijkt vier keer per dag of er bij shows met automatisch downloaden iets nieuws is. */
        fun scheduleAuto(context: Context, settings: DownloadSettings) {
            val request = PeriodicWorkRequestBuilder<AutoDownloadWorker>(6, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(if (settings.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(AUTO_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        /** Meteen kijken voor één show, bijvoorbeeld net nadat je het aanzette. */
        fun checkNow(context: Context, showId: String? = null) {
            val request = OneTimeWorkRequestBuilder<AutoDownloadWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInputData(workDataOf(AutoDownloadWorker.KEY_SHOW to showId))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("$AUTO_WORK-now-${showId.orEmpty()}", ExistingWorkPolicy.REPLACE, request)
        }

        private const val AUTO_WORK = "auto-download"
    }
}

/** De regels voor opruimen en automatisch klaarzetten, los van Android zodat ze te testen zijn. */
object DownloadPolicy {

    /** Na zoveel tijd sinds het uitluisteren mag een download weg. */
    val LISTENED_GRACE: Duration = Duration.ofHours(24)

    fun toDelete(
        records: List<DownloadRecord>,
        settings: DownloadSettings,
        protected: Set<String>,
        now: Instant
    ): List<String> {
        val doomed = mutableSetOf<String>()
        if (settings.deleteListened) {
            records.filter { it.episode.id !in protected }.forEach { record ->
                val at = record.listenedAt?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return@forEach
                if (Duration.between(at, now) >= LISTENED_GRACE) doomed += record.episode.id
            }
        }
        // Boven de grens: eerst uitgeluisterd, dan de oudste automatische. Wat
        // je zelf downloadde, ruimt de app niet op; dat is jouw keuze.
        val limit = settings.limitMb.toLong() * 1024 * 1024
        var used = records.filter { it.state == DownloadState.DONE && it.episode.id !in doomed }.sumOf { it.bytes }
        if (used > limit) {
            records.filter { it.state == DownloadState.DONE && it.auto && it.episode.id !in protected && it.episode.id !in doomed }
                .sortedWith(compareBy({ it.listenedAt == null }, { it.addedAt }))
                .forEach { record ->
                    if (used <= limit) return@forEach
                    doomed += record.episode.id
                    used -= record.bytes
                }
        }
        return doomed.toList()
    }

    /**
     * Welke afleveringen van één show automatisch klaar moeten staan: de
     * nieuwste [count] die je nog niet uitluisterde. [newest] is de feed, de
     * nieuwste eerst. Geeft terug wat erbij moet en welke oudere automatische weg kunnen.
     */
    fun autoPlan(
        newest: List<Episode>,
        count: Int,
        existing: List<DownloadRecord>,
        listened: Set<String>,
        protected: Set<String>
    ): Pair<List<Episode>, List<String>> {
        val wanted = newest.filter { it.audioUrl != null && it.id !in listened }.take(count)
        val wantedIds = wanted.map { it.id }.toSet()
        val have = existing.map { it.episode.id }.toSet()
        val add = wanted.filter { it.id !in have }
        val drop = existing.filter { it.auto && it.episode.id !in wantedIds && it.episode.id !in protected && it.listenedAt == null }
            .map { it.episode.id }
        return add to drop
    }
}
