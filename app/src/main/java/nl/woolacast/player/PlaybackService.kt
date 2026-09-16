package nl.woolacast.player

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import nl.woolacast.R
import nl.woolacast.container
import nl.woolacast.data.local.SavedEpisode
import nl.woolacast.data.local.toSaved
import nl.woolacast.ui.common.speedLabel

/** Wat de melding kan wat Media3 niet uit zichzelf kent. */
private const val ACTION_SPEED = "nl.woolacast.SPEED"
private const val ACTION_SAVE = "nl.woolacast.SAVE"

/**
 * Draagt het afspelen buiten de activity, zodat een aflevering doorloopt als
 * de app naar de achtergrond gaat en op het vergrendelscherm bedienbaar is.
 *
 * De melding krijgt dezelfde bediening als het spelerscherm: snelheid, −15,
 * afspelen/pauzeren, +30 en bewaren. Media3's eigen rij is die van een
 * muziekspeler — vorige, afspelen, volgende — en dat is precies wat je bij een
 * gesprek van twee uur niet nodig hebt.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            // Anders valt het streamen stil zodra het scherm uitgaat.
            .setWakeMode(C.WAKE_MODE_NETWORK)
            // Dezelfde sprongen als de knoppen in de app, zodat de melding niet
            // iets anders doet dan het scherm.
            .setSeekBackIncrementMs(SKIP_BACK_MS)
            .setSeekForwardIncrementMs(SKIP_FORWARD_MS)
            .build()

        player.addListener(object : Player.Listener {
            // De snelheidsknop draagt zijn eigen waarde als icoon; Media3
            // ververst de melding daar niet vanzelf voor.
            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                publishButtons()
            }
        })

        // Tikken op de melding opent de app weer.
        val openApp = PendingIntent.getActivity(
            this, 0,
            packageManager.getLaunchIntentForPackage(packageName) ?: Intent(),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(openApp)
            .setCallback(Bediening())
            .setCustomLayout(extraButtons())
            .build()

        setMediaNotificationProvider(WoolNotification(this))

        // De ster hoort te kloppen, ook als er elders in de app bewaard wordt.
        scope.launch {
            container.store.saved.drop(1).collect { publishButtons() }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    /* ---- de knoppen ---- */

    /**
     * De rij zoals hij in de melding en op het vergrendelscherm staat. De
     * middelste drie — terug, afspelen, vooruit — staan ook in de ingeklapte
     * melding; snelheid en ster verschijnen zodra je hem uitklapt.
     */
    private fun notificationButtons(showPause: Boolean): ImmutableList<CommandButton> =
        ImmutableList.of(
            speedButton(),
            CommandButton.Builder(CommandButton.ICON_SKIP_BACK_15)
                .setPlayerCommand(Player.COMMAND_SEEK_BACK)
                .setDisplayName("15 seconden terug")
                .setExtras(compactAt(0))
                .setEnabled(true)
                .build(),
            CommandButton.Builder(if (showPause) CommandButton.ICON_PAUSE else CommandButton.ICON_PLAY)
                .setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
                .setDisplayName(if (showPause) "Pauzeren" else "Afspelen")
                .setExtras(compactAt(1))
                .setEnabled(true)
                .build(),
            CommandButton.Builder(CommandButton.ICON_SKIP_FORWARD_30)
                .setPlayerCommand(Player.COMMAND_SEEK_FORWARD)
                .setDisplayName("30 seconden vooruit")
                .setExtras(compactAt(2))
                .setEnabled(true)
                .build(),
            saveButton()
        )

    /** Wat een andere bediening — Android Auto, een horloge — erbij krijgt. */
    private fun extraButtons(): ImmutableList<CommandButton> =
        ImmutableList.of(speedButton(), saveButton())

    private fun speedButton(): CommandButton {
        val speed = mediaSession?.player?.playbackParameters?.speed ?: 1f
        return CommandButton.Builder(speedIcon(speed))
            .setSessionCommand(SessionCommand(ACTION_SPEED, Bundle.EMPTY))
            .setDisplayName("Snelheid ${speedLabel(speed)}")
            .setEnabled(true)
            .build()
    }

    private fun saveButton(): CommandButton {
        val saved = currentEpisode()?.let { container.store.isSaved(it.id) } == true
        return CommandButton.Builder(
            if (saved) CommandButton.ICON_STAR_FILLED else CommandButton.ICON_STAR_UNFILLED
        )
            .setSessionCommand(SessionCommand(ACTION_SAVE, Bundle.EMPTY))
            .setDisplayName(if (saved) "Niet meer bewaren" else "Bewaren")
            .setEnabled(true)
            .build()
    }

    /** Media3 leest de plek in de ingeklapte melding uit de extra's. */
    private fun compactAt(index: Int) = Bundle().apply {
        putInt(DefaultMediaNotificationProvider.COMMAND_KEY_COMPACT_VIEW_INDEX, index)
    }

    /**
     * De melding volgt de speler, maar niet de snelheid en niet wat er bewaard
     * is; die twee tekenen we hier zelf opnieuw.
     */
    private fun publishButtons() {
        val session = mediaSession ?: return
        session.setCustomLayout(extraButtons())
        if (session.player.currentMediaItem != null) onUpdateNotification(session, false)
    }

    /**
     * Wat er nu speelt, als bewaarbare aflevering. Meestal weet de app het
     * zelf; is de speler ouder dan het scherm, dan is de metadata genoeg.
     */
    private fun currentEpisode(): SavedEpisode? {
        val player = mediaSession?.player ?: return null
        val item = player.currentMediaItem ?: return null
        container.player.state.value.episode
            ?.takeIf { it.id == item.mediaId }
            ?.let { return it.toSaved() }
        val meta = item.mediaMetadata
        return SavedEpisode(
            id = item.mediaId,
            showId = "",
            showTitle = meta.artist?.toString().orEmpty(),
            title = meta.title?.toString().orEmpty(),
            artworkUrl = meta.artworkUri?.toString(),
            audioUrl = item.localConfiguration?.uri?.toString(),
            durationMillis = player.duration.takeIf { it > 0L }
        )
    }

    private fun toggleSaved() {
        val episode = currentEpisode() ?: return
        // De verzameling 'bewaard' wordt hierboven gevolgd; die tekent de ster.
        scope.launch { container.store.toggleSaved(episode) }
    }

    /* ---- bediening van buiten ---- */

    private inner class Bediening : MediaSession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult =
            MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(
                    MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                        .add(SessionCommand(ACTION_SPEED, Bundle.EMPTY))
                        .add(SessionCommand(ACTION_SAVE, Bundle.EMPTY))
                        .build()
                )
                .setCustomLayout(extraButtons())
                .build()

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                ACTION_SPEED -> session.player.setPlaybackSpeed(
                    nextSpeed(session.player.playbackParameters.speed)
                )
                ACTION_SAVE -> toggleSaved()
                else -> return Futures.immediateFuture(
                    SessionResult(SessionError.ERROR_NOT_SUPPORTED)
                )
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    /** Media3's melding, maar met onze rij knoppen erin. */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private class WoolNotification(private val service: PlaybackService) :
        DefaultMediaNotificationProvider(
            service,
            DefaultMediaNotificationProvider.NotificationIdProvider {
                DefaultMediaNotificationProvider.DEFAULT_NOTIFICATION_ID
            },
            DefaultMediaNotificationProvider.DEFAULT_CHANNEL_ID,
            R.string.playback_channel
        ) {

        init {
            setSmallIcon(R.drawable.ic_notification)
        }

        override fun getMediaButtons(
            session: MediaSession,
            playerCommands: Player.Commands,
            customLayout: ImmutableList<CommandButton>,
            showPauseButton: Boolean
        ): ImmutableList<CommandButton> = service.notificationButtons(showPauseButton)
    }
}

/** Het cijfer op de snelheidsknop; Media3 levert er een icoon per stap bij. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun speedIcon(speed: Float): Int = when {
    speed < 0.9f -> CommandButton.ICON_PLAYBACK_SPEED_0_8
    speed < 1.1f -> CommandButton.ICON_PLAYBACK_SPEED_1_0
    speed < 1.35f -> CommandButton.ICON_PLAYBACK_SPEED_1_2
    speed < 1.65f -> CommandButton.ICON_PLAYBACK_SPEED_1_5
    speed < 1.9f -> CommandButton.ICON_PLAYBACK_SPEED_1_8
    else -> CommandButton.ICON_PLAYBACK_SPEED_2_0
}
