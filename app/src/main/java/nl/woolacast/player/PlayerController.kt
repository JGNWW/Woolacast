package nl.woolacast.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.woolacast.domain.Episode

data class PlaybackState(
    val episodeId: String? = null,
    val title: String = "",
    val showTitle: String = "",
    val artworkUrl: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
) {
    val hasEpisode: Boolean get() = episodeId != null
    val progress: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

/**
 * Dunne laag om de MediaController: de UI leest een StateFlow en hoeft niets
 * van Media3 te weten.
 */
class PlayerController(
    private val context: Context,
    /** Waar je gebleven bent, zodat de podcastpagina dat kan tonen. */
    private val onProgress: suspend (episodeId: String, positionMs: Long, durationMs: Long) -> Unit =
        { _, _, _ -> }
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var controller: MediaController? = null
    private var currentEpisodeId: String? = null

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = syncFromPlayer()
    }

    fun connect() {
        if (controller != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                controller = runCatching { future.get() }.getOrNull()?.also {
                    it.addListener(listener)
                    syncFromPlayer()
                }
                startTicking()
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    fun release() {
        controller?.removeListener(listener)
        controller?.release()
        controller = null
    }

    fun play(episode: Episode, resumeAtMs: Long = 0L) {
        val audioUrl = episode.audioUrl ?: return
        val player = controller ?: return

        if (currentEpisodeId == episode.id) {
            togglePlayPause()
            return
        }

        val item = MediaItem.Builder()
            .setMediaId(episode.id)
            .setUri(audioUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(episode.title)
                    .setArtist(episode.showTitle)
                    .setArtworkUri(episode.artworkUrl?.let(android.net.Uri::parse))
                    .build()
            )
            .build()

        currentEpisodeId = episode.id
        _state.value = PlaybackState(
            episodeId = episode.id,
            title = episode.title,
            showTitle = episode.showTitle,
            artworkUrl = episode.artworkUrl,
            isPlaying = true,
            durationMs = episode.durationMillis ?: 0L
        )

        player.setMediaItem(item)
        player.prepare()
        if (resumeAtMs > 0L) player.seekTo(resumeAtMs)
        player.play()
    }

    fun togglePlayPause() {
        val player = controller ?: return
        if (player.isPlaying) {
            player.pause()
            // Bij pauzeren is de plek het interessantst om te onthouden.
            val snapshot = _state.value
            snapshot.episodeId?.let { id ->
                scope.launch { onProgress(id, snapshot.positionMs, snapshot.durationMs) }
            }
        } else {
            player.play()
        }
    }

    fun seekTo(fraction: Float) {
        val player = controller ?: return
        val duration = player.duration.takeIf { it > 0L } ?: return
        player.seekTo((duration * fraction.coerceIn(0f, 1f)).toLong())
    }

    fun seekBy(deltaMs: Long) {
        val player = controller ?: return
        player.seekTo((player.currentPosition + deltaMs).coerceAtLeast(0L))
    }

    private fun startTicking() {
        scope.launch {
            var sinceSave = 0
            while (true) {
                delay(500)
                if (!_state.value.isPlaying) continue
                syncFromPlayer()

                // Elke tien seconden vastleggen; vaker heeft geen zin en
                // schrijft alleen maar naar schijf.
                if (++sinceSave >= 20) {
                    sinceSave = 0
                    val snapshot = _state.value
                    snapshot.episodeId?.let { id ->
                        onProgress(id, snapshot.positionMs, snapshot.durationMs)
                    }
                }
            }
        }
    }

    private fun syncFromPlayer() {
        val player = controller ?: return
        val duration = player.duration.takeIf { it > 0L } ?: _state.value.durationMs
        _state.value = _state.value.copy(
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = duration
        )
    }
}
