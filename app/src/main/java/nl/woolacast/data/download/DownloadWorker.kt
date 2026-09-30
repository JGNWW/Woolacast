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
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.woolacast.R
import nl.woolacast.container
import nl.woolacast.data.Network
import nl.woolacast.data.PodcastRepository
import nl.woolacast.data.local.DownloadState
import nl.woolacast.domain.Catalog
import okhttp3.Request

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
            val bytes = fetch(url, part) { done, total ->
                downloads.report(episodeId, DownloadProgress(done, total))
            }
            if (isStopped) return Result.success()
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
            else fail(episodeId, error.message ?: "De download is mislukt.")
        }
    }

    private suspend fun fail(episodeId: String, reason: String): Result {
        applicationContext.container.store.updateDownload(episodeId) {
            it.copy(state = DownloadState.FAILED, error = reason)
        }
        return Result.failure()
    }

    /** Schrijft naar [part] en geeft het totaal aantal bytes terug. */
    private suspend fun fetch(url: String, part: File, onProgress: (Long, Long) -> Unit): Long = withContext(Dispatchers.IO) {
        part.parentFile?.mkdirs()
        val already = if (part.exists()) part.length() else 0L
        val request = Request.Builder().url(url)
            .apply { if (already > 0L) header("Range", "bytes=$already-") }
            .build()
        Network.downloadClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("De server gaf ${response.code}.")
            val body = response.body ?: throw IOException("Leeg antwoord.")
            val resumed = response.code == 206 && already > 0L
            val start = if (resumed) already else 0L
            val total = body.contentLength().takeIf { it > 0L }?.plus(start) ?: 0L
            var done = start
            var lastReport = 0L
            FileOutputStream(part, resumed).use { out ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        if (isStopped) return@withContext done
                        val read = input.read(buffer)
                        if (read == -1) break
                        out.write(buffer, 0, read)
                        done += read
                        if (done - lastReport > 256 * 1024) {
                            lastReport = done
                            onProgress(done, total)
                            if (total > 0L) runCatching { setForeground(foregroundInfo(null, (done * 100 / total).toInt())) }
                        }
                    }
                }
            }
            if (total > 0L && done < total) throw IOException("De verbinding viel weg.")
            if (done < MIN_BYTES) throw IOException("Het bestand is te klein om audio te zijn.")
            done
        }
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
        private const val CHANNEL = "downloads"
        private const val NOTIFICATION_BASE = 7000
        private const val MAX_ATTEMPTS = 3
        /** Minder dan dit is een foutpagina, geen aflevering. */
        private const val MIN_BYTES = 16 * 1024L

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
            val (add, drop) = DownloadPolicy.autoPlan(episodes, count, existing, store.listened.value, protected)
            drop.forEach { container.downloads.remove(it) }
            add.forEach { episode ->
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
