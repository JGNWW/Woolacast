package nl.woolacast.player

import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import androidx.core.content.ContextCompat
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
import nl.woolacast.domain.Chapter
import nl.woolacast.domain.Episode

/**
 * De sprongen van de twee vaste knoppen. Ze staan hier omdat zowel het
 * spelerscherm als de melding ze gebruikt en ze hetzelfde moeten doen.
 */
const val SKIP_BACK_MS = 15_000L
const val SKIP_FORWARD_MS = 30_000L

/**
 * De snelheden die de app aanbiedt. De melding heeft geen ruimte voor een
 * keuzelijst en loopt ze rond; vandaar dat het scherm dezelfde rij gebruikt.
 */
val SPEEDS = listOf(0.8f, 1f, 1.2f, 1.5f, 1.8f, 2f)

/** De eerstvolgende snelheid, en na de laatste weer de eerste. */
fun nextSpeed(current: Float): Float {
    val index = SPEEDS.indexOfFirst { it > current + 0.01f }
    return if (index == -1) SPEEDS.first() else SPEEDS[index]
}

/**
 * Waar het hoofdstuk van [positionMs] ophoudt: het begin van het volgende, of
 * het einde van de aflevering. Een halve seconde speling, zodat een sprong naar
 * het begin van een hoofdstuk niet als het einde van het vorige telt.
 */
internal fun chapterStop(chapters: List<Chapter>, positionMs: Long, durationMs: Long): Long? =
    chapters.firstOrNull { it.startMs > positionMs + 500 }?.startMs ?: durationMs.takeIf { it > 0L }

/** Wanneer de speler zichzelf stilzet. */
sealed interface SleepTimer {
    data object Off : SleepTimer
    data class After(val minutes: Int) : SleepTimer
    data object EndOfEpisode : SleepTimer
    /**
     * Aan het eind van het hoofdstuk waar je bent. Spring je naar een ander
     * hoofdstuk, dan telt het einde van dat hoofdstuk.
     */
    data object EndOfChapter : SleepTimer
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
    /** Stopt op deze plek in de aflevering (einde van het huidige hoofdstuk); null als dat niet zo is. */
    val sleepAtMs: Long? = null,
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
    private val consumeQueued: suspend (episodeId: String) -> Unit = {},
    /** Het bestand op het toestel, als de aflevering gedownload is. */
    private val localUri: (episodeId: String) -> String? = { null }
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var controller: MediaController? = null
    private var pending: Pair<Episode, String?>? = null
    private var sleepEndsAt: Long? = null
    private var ticking = false

    private val _state = MutableStateFlow(PlaybackState())

    /** Hoofdstukken van wat nu speelt, voor de slaaptimer "einde hoofdstuk". */
    private var chapters: List<Chapter> = emptyList()

    fun setChapters(list: List<Chapter>) {
        chapters = list
        if (_state.value.sleepAtMs != null) aimAtChapterEnd()
    }

    /** Legt het stoppunt op het einde van het hoofdstuk waar de speler nu is. */
    private fun aimAtChapterEnd() {
        val position = controller?.currentPosition ?: _state.value.positionMs
        _state.value = _state.value.copy(sleepAtMs = chapterStop(chapters, position, _state.value.durationMs))
    }

    /** Na elke sprong: het stoppunt hoort bij het hoofdstuk waar je nu bent. */
    private fun afterSeek() {
        syncFromPlayer()
        if (_state.value.sleepAtMs != null) aimAtChapterEnd()
    }
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    /**
     * De aflevering die de app zélf heeft klaargezet. Een mediaitem kan ook van
     * buiten komen — elke app op het toestel mag deze sessie bedienen, dat hoort
     * bij een mediasessie — en zo'n item vertelt over zichzelf wat het wil.
     * [restoreFromPlayer] bouwt daaruit wel een aflevering om te tónen, maar
     * alleen wat hier staat is van ons en mag de bibliotheek in.
     */
    var ownEpisodeId: String? = null
        private set

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
                    restoreFromPlayer(it)
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
        ownEpisodeId = episode.id

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

        val item = episode.toMediaItem(localUri(episode.id) ?: audioUrl)

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
            // Het einde van een hoofdstuk hoort bij de vorige aflevering.
            sleepAtMs = null,
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
        afterSeek()
    }

    /** Naar een vaste plek, bijvoorbeeld het begin van een hoofdstuk of een zin. */
    fun seekToMs(positionMs: Long) {
        val player = controller ?: return
        player.seekTo(positionMs.coerceAtLeast(0L))
        if (!player.isPlaying) player.play()
        afterSeek()
    }

    fun seekBy(deltaMs: Long) {
        val player = controller ?: return
        player.seekTo((player.currentPosition + deltaMs).coerceAtLeast(0L))
        afterSeek()
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
        afterSeek()
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
                _state.value = _state.value.copy(sleepRemainingMs = null, sleepAtEnd = false, sleepAtMs = null)
            }
            is SleepTimer.After -> {
                sleepEndsAt = SystemClock.elapsedRealtime() + timer.minutes * 60_000L
                _state.value = _state.value.copy(
                    sleepRemainingMs = timer.minutes * 60_000L, sleepAtEnd = false, sleepAtMs = null
                )
            }
            SleepTimer.EndOfEpisode -> {
                sleepEndsAt = null
                _state.value = _state.value.copy(sleepRemainingMs = null, sleepAtEnd = true, sleepAtMs = null)
            }
            SleepTimer.EndOfChapter -> {
                sleepEndsAt = null
                _state.value = _state.value.copy(sleepRemainingMs = null, sleepAtEnd = false)
                aimAtChapterEnd()
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

    /**
     * De service kan doorspelen terwijl de app weg was; dan kent deze laag de
     * aflevering niet meer. Genoeg staat in de metadata om hem terug te bouwen.
     */
    private fun restoreFromPlayer(player: Player) {
        if (_state.value.episode != null) return
        val item = player.currentMediaItem ?: return
        _state.value = _state.value.copy(
            episode = item.toEpisode(player.duration.takeIf { it > 0L })
        )
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

                _state.value.sleepAtMs?.let { stopAt ->
                    if (_state.value.positionMs >= stopAt) {
                        controller?.pause()
                        rememberWhereWeAre()
                        setSleepTimer(SleepTimer.Off)
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
