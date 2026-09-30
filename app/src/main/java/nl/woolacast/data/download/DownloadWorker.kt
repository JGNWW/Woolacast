package nl.woolacast.data.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.woolacast.R
import nl.woolacast.container
import nl.woolacast.data.Network
import nl.woolacast.data.PodcastRepository
import nl.woolacast.data.local.DownloadState
import nl.woolacast.domain.Catalog

/**
 * Haalt één aflevering binnen. Gaat verder waar hij bleef als een eerdere poging
 * halverwege afbrak (een .part-bestand plus een Range-verzoek), en volgt de
 * doorverwijzingen van meetdiensten als OP3 en Podtrac.
 */
class DownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val episodeId = inputData.getString(KEY_EPISODE) ?: return Result.failure()
        val container = applicationContext.container
        val store = container.store
        store.ensureLoaded()
        val downloads = container.downloads
        val record = store.download(episodeId) ?: return Result.success() // intussen weggehaald
        if (record.state == DownloadState.DONE && downloads.file(record).exists()) return Result.success()
        val url = record.episode.audioUrl ?: return fail(episodeId, "Geen audio in de feed.")

        runCatching { setForeground(foregroundInfo(record.episode.title, null)) }

        val target = downloads.file(record)
        val part = File(target.path + Downloads.PART)
        return try {
            val bytes = coroutineScope {
                // De melding volgt de voortgang op zijn eigen tempo, los van het schrijven.
                val percent = AtomicInteger(-1)
                val ticker = launch {
                    while (isActive) {
                        delay(1_000)
                        percent.get().takeIf { it >= 0 }?.let { runCatching { setForeground(foregroundInfo(null, it)) } }
                    }
                }
                val result = withContext(Dispatchers.IO) {
                    AudioFetcher(Network.downloadClient).fetch(url, part, isStopped = { isStopped }) { done, total ->
                        downloads.report(episodeId, DownloadProgress(done, total))
                        if (total > 0L) percent.set((done * 100 / total).toInt())
                    }
                }
                ticker.cancel()
                result
            }
            if (bytes == null || isStopped) {
                downloads.report(episodeId, null)
                return Result.success()
            }
            if (!part.renameTo(target)) throw IOException("Kon het bestand niet opslaan.")
            downloads.report(episodeId, null)
            if (store.download(episodeId) == null) {
                target.delete() // weggehaald terwijl hij binnenkwam
            } else {
                store.updateDownload(episodeId) { it.copy(state = DownloadState.DONE, bytes = bytes, error = null) }
                downloads.cleanUp()
            }
            Result.success()
        } catch (error: IOException) {
            downloads.report(episodeId, null)
            if (isStopped) return Result.success()
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry()
            else fail(episodeId, reason(error))
        }
    }

    private suspend fun fail(episodeId: String, reason: String): Result {
        applicationContext.container.store.updateDownload(episodeId) {
            it.copy(state = DownloadState.FAILED, error = reason)
        }
        return Result.failure()
    }

    private var title: String? = null

    private fun foregroundInfo(newTitle: String?, percent: Int?): ForegroundInfo {
        if (newTitle != null) title = newTitle
        ensureChannel(applicationContext)
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL)
            .setSmallIcon(R.drawable.ic_notif_download)
            .setContentTitle("Downloaden")
            .setContentText(title)
            .setOngoing(true)
            .setSilent(true)
            .setProgress(100, percent ?: 0, percent == null)
            .build()
        val id = NOTIFICATION_BASE + (inputData.getString(KEY_EPISODE)?.hashCode() ?: 0) % 1000
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(id, notification)
        }
    }

    companion object {
        const val KEY_EPISODE = "episode"

        /** Wat er misging, in gewone taal; de melding van OkHttp is Engels en voor ontwikkelaars. */
        fun reason(error: Throwable): String {
            val message = error.message.orEmpty()
            val code = Regex("""\b(\d{3})\b""").find(message)?.groupValues?.get(1)
            return when {
                error is java.net.UnknownHostException -> "Geen verbinding, of de website bestaat niet meer"
                error is java.net.SocketTimeoutException -> "Geen antwoord van de server"
                code == "404" || code == "410" -> "Aflevering niet meer te vinden ($code)"
                code == "403" || code == "401" -> "De maker staat downloaden niet toe ($code)"
                code != null && code.startsWith("5") -> "De server van de maker heeft een storing ($code)"
                message.contains("te klein") -> "Geen audio gevonden op dit adres"
                message.contains("opslaan") -> "Het bestand kon niet worden bewaard"
                message.contains("viel weg") -> "De verbinding viel weg"
                else -> "Downloaden lukte niet"
            }
        }
        private const val CHANNEL = "downloads"
        private const val NOTIFICATION_BASE = 7000
        private const val MAX_ATTEMPTS = 3

        fun ensureChannel(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Downloads", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Voortgang van afleveringen die worden gedownload."
                }
            )
        }
    }
}

/**
 * Kijkt bij shows met automatisch downloaden of er een nieuwe aflevering is,
 * zet de nieuwste klaar en ruimt oudere automatische downloads op.
 */
class AutoDownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = applicationContext.container
        val store = container.store
        store.ensureLoaded()
        val only = inputData.getString(KEY_SHOW)
        val plans = store.autoDownload.value.filterKeys { only == null || it == only }
        val follows = store.follows.value.associateBy { it.id }
        val repository: PodcastRepository = container.podcastRepository

        for ((showId, count) in plans) {
            val show = follows[showId]
            val episodes = runCatching {
                val feed = show?.feedUrl
                if (feed != null) repository.latestEpisodes(feed, limit = count + 5).map { it.copy(showId = showId) }
                else repository.detail(showId, Catalog.defaultCountry.code, null, show?.title).episodes.take(count + 5)
            }.getOrNull() ?: continue
            if (episodes.isEmpty()) continue
            val artwork = show?.artworkUrl
            val existing = store.downloads.value.values.filter { it.episode.showId == showId }
            val protected = store.queue.value.map { it.id }.toSet() + store.saved.value.map { it.id } +
                store.progress.value.keys + listOfNotNull(container.player.state.value.episodeId)
            // Wat de app eerder wegens ruimtegebrek opruimde, haalt hij niet opnieuw binnen.
            val skip = store.listened.value + store.evicted.value
            val (add, drop) = DownloadPolicy.autoPlan(episodes, count, existing, skip, protected)
            drop.forEach { container.downloads.remove(it) }
            add.filter { container.downloads.fits(it) }.forEach { episode ->
                container.downloads.enqueue(
                    episode.copy(artworkUrl = episode.artworkUrl ?: artwork, showTitle = episode.showTitle.ifBlank { show?.title.orEmpty() }),
                    auto = true
                )
            }
        }
        container.downloads.cleanUp()
        return Result.success()
    }

    companion object {
        const val KEY_SHOW = "show"
    }
}
