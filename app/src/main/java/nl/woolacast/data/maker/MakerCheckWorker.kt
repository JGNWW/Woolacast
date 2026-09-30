package nl.woolacast.data.maker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import nl.woolacast.MainActivity
import nl.woolacast.R
import nl.woolacast.container

/**
 * Kijkt twee keer per dag of een kanaal dat je volgt een nieuwe podcast begon,
 * en meldt dat. Leest alleen makers.json van de verzamelaar: één bestand per
 * land, geen feeds.
 */
class MakerCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = applicationContext.container
        val store = container.store
        store.ensureLoaded()
        val followed = store.makers.value
        if (followed.isEmpty()) return Result.success()

        followed.groupBy { it.country }.forEach { (country, makers) ->
            val directory = container.makerRepository.directory(country)
            makers.forEach { maker ->
                val channel = maker.channelId?.let(directory::channel) ?: directory.channelNamed(maker.key)
                val fresh = MakerNews.toNotify(maker, channel)
                if (fresh.isNotEmpty()) {
                    fresh.forEach { show -> notify(maker.name, show.title, show.id) }
                    store.markMakerNotified(maker.key, fresh.map { it.id })
                }
            }
        }
        return Result.success()
    }

    private fun notify(makerName: String, title: String, showId: String) {
        val context = applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context, showId.hashCode(),
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Nieuwe podcast van $makerName")
            .setContentText(title)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(showId.hashCode(), notification) }
    }

    companion object {
        private const val CHANNEL = "makers"
        private const val WORK = "makers-check"

        private fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Nieuwe podcasts van makers", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Als een maker die je volgt een nieuwe podcast begint."
                }
            )
        }

        /** Twee keer per dag, alleen met netwerk. Staat er al een, dan blijft die. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<MakerCheckWorker>(12, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
