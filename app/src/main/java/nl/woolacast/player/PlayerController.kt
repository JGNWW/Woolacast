package nl.woolacast.player

import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
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

/** Wanneer de speler zichzelf stilzet. */
sealed interface SleepTimer {
    data object Off : SleepTimer
    data class After(val minutes: Int) : SleepTimer
    data object EndOfEpisode : SleepTimer
}

data class PlaybackState(
    val episode: Episode? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1f,
    /** Resterende tijd van de slaaptimer; null als er geen loopt. */
    val sleepRemainingMs: Long? = null,
    val sleepAtEnd: Boolean = false,
    /** Waar deze aflevering vandaan kwam, bijv. "#3 in Top afleveringen NL". */
    val chartLabel: String? = null,
    val error: String? = null
) {
    val episodeId: String? get() = episode?.id
    val title: String get() = episode?.title.orEmpty()
    val showTitle: String get() = episode?.showTitle.orEmpty()
    val artworkUrl: String? get() = episode?.artworkUrl
    val hasEpisode: Boolean get() = episode != null
    val progress: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val remainingMs: Long get() = (durationMs - positionMs).coerceAtLeast(0L)
}

/**
 * Dunne laag om de MediaController: de UI leest een StateFlow en hoeft niets
 * van Media3 te weten. Hier zitten ook de dingen die een podcastspeler van een
 * muziekspeler onderscheiden: hervatten waar je was, snelheid, slaaptimer en
 * doorspelen vanuit de wachtrij.
 */
class PlayerController(
    private val context: Context,
    /** Waar je gebleven was, per aflevering. */
    private val resumePosition: (episodeId: String) -> Long = { 0L },
    /** Waar je gebleven bent, zodat de podcastpagina dat kan tonen. */
    private val onProgress: suspend (episodeId: String, positionMs: Long, durationMs: Long) -> Unit =
        { _, _, _ -> },
    /** De eerstvolgende aflevering uit de wachtrij, of null als die leeg is. */
    private val nextInQueue: () -> Episode? = { null },
    /** Wordt aangeroepen zodra een wachtrij-aflevering begint te spelen. */
    private val consumeQueued: suspend (episodeId: String) -> Unit = {}
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var controller: MediaController? = null
    private var pending: Pair<Episode, String?>? = null
    private var sleepEndsAt: Long? = null
    private var ticking = false

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = syncFromPlayer()

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) onEnded()
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.value = _state.value.copy(
                isPlaying = false,
                isBuffering = false,
                error = "Kan deze aflevering niet afspelen (${error.errorCodeName.lowercase().replace('_', ' ')})."
            )
        }
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
                // Wat er getikt werd voordat de verbinding er was, alsnog starten.
                pending?.let { (episode, label) -> play(episode, label) }
                pending = null
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    fun release() {
        controller?.removeListener(listener)
        controller?.release()
        controller = null
    }

    /**
     * Start een aflevering, of hervat hem als hij al klaarstaat. Bewust geen
     * pauze bij een tweede tik op dezelfde rij — daar is de knop voor.
     */
    fun play(episode: Episode, chartLabel: String? = null) {
        val audioUrl = episode.audioUrl
        if (audioUrl == null) {
            _state.value = _state.value.copy(error = "Deze aflevering heeft geen audiobestand in de feed.")
            return
        }
        val player = controller
        if (player == null) {
            pending = episode to chartLabel
            _state.value = PlaybackState(episode = episode, isBuffering = true, chartLabel = chartLabel,
                durationMs = episode.durationMillis ?: 0L, speed = _state.value.speed)
            return
        }

        if (_state.value.episodeId == episode.id) {
            if (!player.isPlaying) player.play()
            if (chartLabel != null) _state.value = _state.value.copy(chartLabel = chartLabel)
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

        val resumeAt = resumePosition(episode.id)
        _state.value = PlaybackState(
            episode = episode,
            isPlaying = true,
            isBuffering = true,
            positionMs = resumeAt,
            durationMs = episode.durationMillis ?: 0L,
            speed = _state.value.speed,
            sleepRemainingMs = _state.value.sleepRemainingMs,
            sleepAtEnd = _state.value.sleepAtEnd,
            chartLabel = chartLabel
        )

        player.setMediaItem(item, if (resumeAt > 0L) resumeAt else 0L)
        player.prepare()
        player.play()
    }

    fun togglePlayPause() {
        val player = controller ?: return
        if (player.isPlaying) {
            player.pause()
            rememberWhereWeAre()
        } else {
            if (player.playbackState == Player.STATE_ENDED) player.seekTo(0L)
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            player.play()
        }
    }

    fun seekTo(fraction: Float) {
        val player = controller ?: return
        val duration = player.duration.takeIf { it > 0L } ?: return
        player.seekTo((duration * fraction.coerceIn(0f, 1f)).toLong())
        syncFromPlayer()
    }

    fun seekBy(deltaMs: Long) {
        val player = controller ?: return
        player.seekTo((player.currentPosition + deltaMs).coerceAtLeast(0L))
        syncFromPlayer()
    }

    /** Volgende uit de wachtrij; niets als die leeg is. */
    fun next(): Boolean {
        val episode = nextInQueue() ?: return false
        scope.launch { consumeQueued(episode.id) }
        play(episode)
        return true
    }

    /** Terug naar het begin — bij een podcast is 'vorige' zelden iets anders. */
    fun previous() {
        val player = controller ?: return
        player.seekTo(0L)
        syncFromPlayer()
    }

    fun setSpeed(speed: Float) {
        val player = controller ?: return
        player.setPlaybackSpeed(speed)
        _state.value = _state.value.copy(speed = speed)
    }

    fun setSleepTimer(timer: SleepTimer) {
        when (timer) {
            SleepTimer.Off -> {
                sleepEndsAt = null
                _state.value = _state.value.copy(sleepRemainingMs = null, sleepAtEnd = false)
            }
            is SleepTimer.After -> {
                sleepEndsAt = SystemClock.elapsedRealtime() + timer.minutes * 60_000L
                _state.value = _state.value.copy(
                    sleepRemainingMs = timer.minutes * 60_000L, sleepAtEnd = false
                )
            }
            SleepTimer.EndOfEpisode -> {
                sleepEndsAt = null
                _state.value = _state.value.copy(sleepRemainingMs = null, sleepAtEnd = true)
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun onEnded() {
        val snapshot = _state.value
        snapshot.episodeId?.let { id ->
            // Uitgeluisterd: de bewaarde positie mag weg.
            scope.launch { onProgress(id, snapshot.durationMs, snapshot.durationMs) }
        }
        if (snapshot.sleepAtEnd) {
            controller?.pause()
            setSleepTimer(SleepTimer.Off)
            return
        }
        if (!next()) {
            _state.value = _state.value.copy(isPlaying = false, isBuffering = false)
        }
    }

    private fun rememberWhereWeAre() {
        val snapshot = _state.value
        snapshot.episodeId?.let { id ->
            scope.launch { onProgress(id, snapshot.positionMs, snapshot.durationMs) }
        }
    }

    private fun startTicking() {
        if (ticking) return
        ticking = true
        scope.launch {
            var sinceSave = 0
            while (true) {
                delay(500)
                if (!_state.value.isPlaying) continue
                syncFromPlayer()

                sleepEndsAt?.let { endsAt ->
                    val remaining = endsAt - SystemClock.elapsedRealtime()
                    if (remaining <= 0L) {
                        controller?.pause()
                        rememberWhereWeAre()
                        setSleepTimer(SleepTimer.Off)
                    } else {
                        _state.value = _state.value.copy(sleepRemainingMs = remaining)
                    }
                }

                // Elke tien seconden vastleggen; vaker heeft geen zin en
                // schrijft alleen maar naar schijf.
                if (++sinceSave >= 20) {
                    sinceSave = 0
                    rememberWhereWeAre()
                }
            }
        }
    }

    private fun syncFromPlayer() {
        val player = controller ?: return
        val duration = player.duration.takeIf { it > 0L } ?: _state.value.durationMs
        val ended = player.playbackState == Player.STATE_ENDED
        _state.value = _state.value.copy(
            isPlaying = player.playWhenReady && !ended && player.playbackState != Player.STATE_IDLE,
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = duration,
            speed = player.playbackParameters.speed
        )
    }
}
