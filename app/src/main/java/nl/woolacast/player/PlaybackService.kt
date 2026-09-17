package nl.woolacast.player

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationManagerCompat
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import nl.woolacast.R
import nl.woolacast.container
import nl.woolacast.data.local.SavedEpisode
import nl.woolacast.data.local.toSaved
import nl.woolacast.ui.common.speedLabel

/**
 * De knoppen die Media3 niet uit zichzelf kent. Ook de sprongen staan hier als
 * eigen opdracht: vanaf Android 13 bouwt het systeem zijn mediabediening uit de
 * sessie en niet uit onze melding, en daar tellen alleen eigen acties mee —
 * terugspoelen en vooruitspoelen krijgen daar geen eigen plek.
 */
private const val ACTION_SPEED = "nl.woolacast.SPEED"
private const val ACTION_BACK = "nl.woolacast.BACK"
private const val ACTION_FORWARD = "nl.woolacast.FORWARD"
private const val ACTION_SAVE = "nl.woolacast.SAVE"

/**
 * Draagt het afspelen buiten de activity, zodat een aflevering doorloopt als
 * de app naar de achtergrond gaat en op het vergrendelscherm bedienbaar is.
 *
 * De melding krijgt dezelfde bediening als het spelerscherm: snelheid, −15,
 * afspelen/pauzeren, +30 en bewaren. Media3's eigen rij is die van een
 * muziekspeler — vorige, afspelen, volgende — en dat is precies wat je bij een
 * gesprek van twee uur niet nodig hebt.
 *
 * Die rij moet op twee plekken landen, en dat gaat op twee manieren: de melding
 * tekenen we zelf, maar vanaf Android 13 bouwt het systeem zijn eigen
 * mediabediening uit de sessie. Daar tellen alleen eigen opdrachten mee, dus
 * staan de vier knoppen naast afspelen ook alle vier in de indeling die de
 * sessie uitdeelt.
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

        // De ster hoort te kloppen, ook als er elders in de app bewaard wordt —
        // maar alleen de aflevering die speelt zegt hier iets.
        scope.launch {
            container.store.saved
                .map { isCurrentEpisodeSaved() }
                .distinctUntilChanged()
                .drop(1)
                .collect { publishButtons() }
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
     * De rij zoals hij in de melding staat. De middelste drie — terug,
     * afspelen, vooruit — staan ook in de ingeklapte melding; snelheid en ster
     * verschijnen zodra je hem uitklapt. Een sprong die de speler nu niet aankan
     * laten we weg in plaats van hem dood te tonen.
     */
    private fun notificationButtons(showPause: Boolean): ImmutableList<CommandButton> {
        // Media3 leest de plek in de ingeklapte melding uit de extra's, en er
        // passen er drie: wat hier als eerste om een plek vraagt, krijgt hem.
        var taken = 0
        fun inCompactView() = Bundle().apply {
            putInt(DefaultMediaNotificationProvider.COMMAND_KEY_COMPACT_VIEW_INDEX, taken++)
        }

        val row = mutableListOf(speedButton())
        if (canSeek(Player.COMMAND_SEEK_BACK)) row += skipButton(back = true, extras = inCompactView())
        row += CommandButton.Builder(if (showPause) CommandButton.ICON_PAUSE else CommandButton.ICON_PLAY)
            .setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
            .setDisplayName(if (showPause) "Pauzeren" else "Afspelen")
            .setExtras(inCompactView())
            .setEnabled(true)
            .build()
        if (canSeek(Player.COMMAND_SEEK_FORWARD)) row += skipButton(back = false, extras = inCompactView())
        row += saveButton()
        return ImmutableList.copyOf(row)
    }

    /**
     * Wat het systeem en andere bedieningen — het vergrendelscherm, Android
     * Auto, een horloge — naast afspelen en pauzeren te zien krijgen. Alleen
     * eigen opdrachten halen die lijst; afspelen zit er al in.
     */
    private fun extraButtons(): ImmutableList<CommandButton> {
        val row = mutableListOf(speedButton())
        if (canSeek(Player.COMMAND_SEEK_BACK)) row += skipButton(back = true)
        if (canSeek(Player.COMMAND_SEEK_FORWARD)) row += skipButton(back = false)
        row += saveButton()
        return ImmutableList.copyOf(row)
    }

    /** Zolang er niets klaarstaat, staat ook nog niet vast of er te zoeken valt. */
    private fun canSeek(command: Int): Boolean =
        mediaSession?.player?.isCommandAvailable(command) != false

    private fun skipButton(back: Boolean, extras: Bundle = Bundle.EMPTY): CommandButton {
        val seconds = (if (back) SKIP_BACK_MS else SKIP_FORWARD_MS) / 1000
        return CommandButton.Builder(
            if (back) CommandButton.ICON_SKIP_BACK else CommandButton.ICON_SKIP_FORWARD
        )
            .setIconResId(if (back) R.drawable.ic_media_back else R.drawable.ic_media_forward)
            .setSessionCommand(SessionCommand(if (back) ACTION_BACK else ACTION_FORWARD, Bundle.EMPTY))
            .setDisplayName("$seconds seconden ${if (back) "terug" else "vooruit"}")
            .setExtras(extras)
            .setEnabled(true)
            .build()
    }

    private fun speedButton(): CommandButton {
        val speed = mediaSession?.player?.playbackParameters?.speed ?: 1f
        val (icon, drawing) = speedIcons(speed)
        return CommandButton.Builder(icon)
            .setIconResId(drawing)
            .setSessionCommand(SessionCommand(ACTION_SPEED, Bundle.EMPTY))
            .setDisplayName("Snelheid ${speedLabel(speed)}")
            .setEnabled(true)
            .build()
    }

    private fun saveButton(): CommandButton {
        val saved = isCurrentEpisodeSaved()
        return CommandButton.Builder(
            if (saved) CommandButton.ICON_STAR_FILLED else CommandButton.ICON_STAR_UNFILLED
        )
            .setIconResId(if (saved) R.drawable.ic_media_star_filled else R.drawable.ic_media_star)
            .setSessionCommand(SessionCommand(ACTION_SAVE, Bundle.EMPTY))
            .setDisplayName(if (saved) "Niet meer bewaren" else "Bewaren")
            .setEnabled(true)
            .build()
    }

    /**
     * De melding volgt de speler, maar niet de snelheid en niet wat er bewaard
     * is; die twee tekenen we hier zelf opnieuw. Alleen zolang de melding er
     * staat: wie hem heeft weggeveegd, wil hem niet terug omdat er elders in de
     * app op een ster is getikt.
     */
    private fun publishButtons() {
        val session = mediaSession ?: return
        session.setCustomLayout(extraButtons())
        if (notificationIsShowing()) onUpdateNotification(session, false)
    }

    private fun notificationIsShowing(): Boolean =
        NotificationManagerCompat.from(this).activeNotifications.any {
            it.id == DefaultMediaNotificationProvider.DEFAULT_NOTIFICATION_ID
        }

    /**
     * Wat er nu speelt, als bewaarbare aflevering — en alleen als de app hem
     * zelf heeft klaargezet. De metadata van een mediaitem komt over dezelfde
     * lijn binnen als de knoppen, en een vreemde app mag er een neerleggen:
     * titel, plaatje, audio-URL en link zouden dan verzonnen zijn, en via
     * Bewaard belanden ze in een lijst die eruitziet alsof de app hem vulde.
     * Dan liever niets bewaren.
     */
    private fun currentEpisode(): SavedEpisode? {
        val id = mediaSession?.player?.currentMediaItem?.mediaId ?: return null
        if (container.player.ownEpisodeId != id) return null
        return container.player.state.value.episode?.takeIf { it.id == id }?.toSaved()
    }

    /**
     * Bewaren is de enige knop die iets wégschrijft. Afspelen en spoelen raken
     * alleen wat er nu klinkt, maar bewaren raakt de bibliotheek, en deze
     * sessie staat open voor elke app op het toestel. Die knop is er dus voor
     * onszelf en voor wat Android vertrouwt met mediabediening — het systeem,
     * het vergrendelscherm, Android Auto — en niet voor de rest.
     */
    private fun maySave(controller: MediaSession.ControllerInfo): Boolean =
        controller.packageName == packageName || controller.isTrusted

    private fun isCurrentEpisodeSaved(): Boolean {
        val id = mediaSession?.player?.currentMediaItem?.mediaId ?: return false
        return container.store.isSaved(id)
    }

    private fun toggleSaved() {
        val episode = currentEpisode() ?: return
        // De verzameling 'bewaard' wordt gevolgd in onCreate; die tekent de ster.
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
                        .add(SessionCommand(ACTION_BACK, Bundle.EMPTY))
                        .add(SessionCommand(ACTION_FORWARD, Bundle.EMPTY))
                        .apply {
                            if (maySave(controller)) add(SessionCommand(ACTION_SAVE, Bundle.EMPTY))
                        }
                        .build()
                )
                // 'Vorige' en 'volgende' bezetten in de systeembediening een
                // vaste plek, en bij een wachtrij van één aflevering doen ze
                // daar niets. Weg ermee, dan is er ruimte voor de sprongen.
                .setAvailablePlayerCommands(
                    MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                        .removeAll(
                            Player.COMMAND_SEEK_TO_PREVIOUS,
                            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                            Player.COMMAND_SEEK_TO_NEXT,
                            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
                        )
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
                ACTION_BACK -> session.player.seekBack()
                ACTION_FORWARD -> session.player.seekForward()
                ACTION_SAVE -> {
                    if (!maySave(controller)) return Futures.immediateFuture(
                        SessionResult(SessionError.ERROR_PERMISSION_DENIED)
                    )
                    toggleSaved()
                }
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

/**
 * Het merk van de knop en de tekening erop. Het merk is van Media3 en gaat mee
 * naar buiten: een bediening in een andere app kan onze tekening niet opzoeken
 * en valt daarop terug. De tekening is van onszelf, want Media3's snelheidscijfer
 * staat er versmald in en zijn sprongpijlen dragen het aantal seconden — allebei
 * een vlek op een knop van 24 dp.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun speedIcons(speed: Float): Pair<Int, Int> = when {
    speed < 0.9f -> CommandButton.ICON_PLAYBACK_SPEED_0_8 to R.drawable.ic_media_speed_0_8
    speed < 1.1f -> CommandButton.ICON_PLAYBACK_SPEED_1_0 to R.drawable.ic_media_speed_1
    speed < 1.35f -> CommandButton.ICON_PLAYBACK_SPEED_1_2 to R.drawable.ic_media_speed_1_2
    speed < 1.65f -> CommandButton.ICON_PLAYBACK_SPEED_1_5 to R.drawable.ic_media_speed_1_5
    speed < 1.9f -> CommandButton.ICON_PLAYBACK_SPEED_1_8 to R.drawable.ic_media_speed_1_8
    else -> CommandButton.ICON_PLAYBACK_SPEED_2_0 to R.drawable.ic_media_speed_2
}
