package nl.woolacast.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.widget.RemoteViews
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import nl.woolacast.R
import nl.woolacast.container
import nl.woolacast.domain.Episode
import nl.woolacast.player.SKIP_FORWARD_MS

/**
 * De widget op het startscherm: wat er speelt, met afspelen en vooruit. Speelt
 * er niets, dan staat de aflevering klaar waar je het laatst in bezig was, en
 * begint afspelen daar weer.
 */
class PlayerWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        refresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        if (action != ACTION_TOGGLE && action != ACTION_FORWARD) return
        val container = context.applicationContext.container
        val pending = goAsync()
        container.appScope.launch {
            try {
                container.store.ensureLoaded()
                val player = container.player
                player.whenConnected {
                    when {
                        action == ACTION_FORWARD -> player.seekBy(SKIP_FORWARD_MS)
                        player.hasMedia -> player.togglePlayPause()
                        else -> resumable(context)?.let { player.play(it) }
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val ACTION_TOGGLE = "nl.woolacast.widget.TOGGLE"
        private const val ACTION_FORWARD = "nl.woolacast.widget.FORWARD"

        /** De hoes die er nu in staat, zodat een tik op pauze hem niet opnieuw ophaalt. */
        private var art: Pair<String, Bitmap>? = null

        /** De aflevering waar je het laatst in bezig was. */
        private fun resumable(context: Context): Episode? {
            val store = context.applicationContext.container.store
            return store.history.value.lastOrNull { !it.finished && it.episode.audioUrl != null }?.episode?.toEpisode()
        }

        /** Tekent alle widgets opnieuw. Zonder widget op het startscherm gebeurt er niets. */
        fun refresh(context: Context) {
            val app = context.applicationContext
            val manager = runCatching { AppWidgetManager.getInstance(app) }.getOrNull() ?: return
            val ids = runCatching { manager.getAppWidgetIds(ComponentName(app, PlayerWidget::class.java)) }.getOrNull()
            if (ids == null || ids.isEmpty()) return

            val container = app.container
            val state = container.player.state.value
            val shown = state.episode ?: resumable(app)
            val artUrl = shown?.artworkUrl

            fun draw(bitmap: Bitmap?) {
                val views = RemoteViews(app.packageName, R.layout.widget_player)
                views.setTextViewText(R.id.widget_title, shown?.title ?: app.getString(R.string.widget_empty_title))
                views.setTextViewText(
                    R.id.widget_subtitle,
                    when {
                        shown == null -> app.getString(R.string.widget_empty_subtitle)
                        state.episode == null -> "Verder luisteren · ${shown.showTitle}"
                        else -> shown.showTitle
                    }
                )
                views.setImageViewResource(
                    R.id.widget_play,
                    if (state.isPlaying) R.drawable.ic_notif_pause else R.drawable.ic_notif_play
                )
                if (bitmap != null) views.setImageViewBitmap(R.id.widget_art, bitmap)
                else views.setImageViewResource(R.id.widget_art, R.drawable.widget_art_placeholder)

                val open = PendingIntent.getActivity(
                    app, 0,
                    app.packageManager.getLaunchIntentForPackage(app.packageName) ?: Intent(),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                views.setOnClickPendingIntent(R.id.widget_root, open)
                views.setOnClickPendingIntent(R.id.widget_play, broadcast(app, ACTION_TOGGLE, 1))
                views.setOnClickPendingIntent(R.id.widget_forward, broadcast(app, ACTION_FORWARD, 2))
                runCatching { manager.updateAppWidget(ids, views) }
            }

            val kept = art?.takeIf { it.first == artUrl }?.second
            draw(kept)
            if (artUrl != null && kept == null) {
                container.appScope.launch {
                    val request = ImageRequest.Builder(app).data(artUrl).size(192).allowHardware(false).build()
                    val bitmap = (runCatching { app.imageLoader.execute(request) }.getOrNull()?.drawable as? BitmapDrawable)?.bitmap
                    if (bitmap != null) {
                        art = artUrl to bitmap
                        // Intussen kan er iets anders spelen; dan tekent die verversing zijn eigen hoes.
                        val now = container.player.state.value.episode ?: resumable(app)
                        if (now?.artworkUrl == artUrl) refresh(app)
                    }
                }
            }
        }

        private fun broadcast(context: Context, action: String, code: Int): PendingIntent =
            PendingIntent.getBroadcast(
                context, code,
                Intent(context, PlayerWidget::class.java).setAction(action),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
    }
}
