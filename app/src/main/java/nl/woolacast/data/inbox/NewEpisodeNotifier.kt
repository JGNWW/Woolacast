package nl.woolacast.data.inbox

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import nl.woolacast.MainActivity
import nl.woolacast.R
import nl.woolacast.container
import nl.woolacast.data.local.SavedEpisode

/**
 * Eén melding per feedronde over nieuwe afleveringen, in een eigen kanaal
 * "Nieuwe afleveringen" dat je los kunt uitzetten. Met twee knoppen: de nieuwste
 * afspelen, of alles in de wachtrij.
 */
object NewEpisodeNotifier {

    const val CHANNEL = "new-episodes"
    const val EXTRA_PLAY = "nl.woolacast.extra.PLAY_EPISODE"
    const val EXTRA_OPEN_NEW = "nl.woolacast.extra.OPEN_NEW"
    private const val NOTIFICATION_ID = 7400

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** De zinnen in de melding, los van Android zodat ze te testen zijn. */
    fun title(episodes: List<SavedEpisode>): String =
        if (episodes.size == 1) episodes.first().showTitle.ifBlank { "Nieuwe aflevering" }
        else "${episodes.size} nieuwe afleveringen"

    fun line(episode: SavedEpisode): String =
        listOf(episode.title, episode.showTitle).filter { it.isNotBlank() }.joinToString(" · ")

    /** Geeft terug of de melding geplaatst kon worden; zo niet, dan telt hij niet als gemeld. */
    fun post(context: Context, episodes: List<SavedEpisode>): Boolean {
        if (episodes.isEmpty()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        ensureChannel(context)
        val first = episodes.first()

        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_OPEN_NEW, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val play = PendingIntent.getActivity(
            context, 1,
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_PLAY, first.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val queue = PendingIntent.getBroadcast(
            context, 2,
            Intent(context, QueueNewReceiver::class.java)
                .putExtra(QueueNewReceiver.EXTRA_IDS, episodes.map { it.id }.toTypedArray()),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val style = NotificationCompat.InboxStyle().setBigContentTitle(title(episodes))
        episodes.take(5).forEach { style.addLine(line(it)) }
        if (episodes.size > 5) style.setSummaryText("en nog ${episodes.size - 5}")

        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title(episodes))
            .setContentText(if (episodes.size == 1) first.title else episodes.joinToString(", ") { it.showTitle })
            .setStyle(style)
            .setContentIntent(open)
            .addAction(R.drawable.ic_notif_play, "Afspelen", play)
            .addAction(0, if (episodes.size == 1) "In de wachtrij" else "Alles in de wachtrij", queue)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        return runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }.isSuccess
    }

    fun cancel(context: Context) {
        runCatching { NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID) }
    }

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Nieuwe afleveringen", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Een nieuwe aflevering van een show waarvoor je de melding aanzette."
            }
        )
    }
}

/** "In de wachtrij" uit de melding: zet de afleveringen erachter zonder de app te openen. */
class QueueNewReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val ids = intent.getStringArrayExtra(EXTRA_IDS)?.toList().orEmpty()
        if (ids.isEmpty()) return
        val pending = goAsync()
        val container = context.applicationContext.container
        container.appScope.launch {
            try {
                val store = container.store
                store.ensureLoaded()
                val known = store.feedChecks.value.values.flatMap { it.latest }.associateBy { it.id }
                store.enqueueAll(NewRules.forQueue(ids.mapNotNull { known[it] }, store.queue.value.map { it.id }.toSet()))
                NewEpisodeNotifier.cancel(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_IDS = "ids"
    }
}
