package nl.woolacast.player

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationManagerCompat
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.RemoteCastPlayer
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import com.google.common.util.concurrent.SettableFuture
import nl.woolacast.data.inbox.NewRules
import nl.woolacast.domain.Episode
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
class PlaybackService : MediaLibraryService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaSession: MediaLibrarySession? = null

    /** Maakt zachte opnames luider; hangt aan de audiosessie van de speler op de telefoon. */
    private var enhancer: android.media.audiofx.LoudnessEnhancer? = null
    private var boostOn = false

    override fun onCreate() {
        super.onCreate()
        val local = ExoPlayer.Builder(this)
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
            // Veel podcast-mp3's hebben geen kop die zegt waar wat staat. Zonder
            // dit geldt zo'n bestand als niet te doorzoeken, en verdwijnen
            // terug- en vooruitspoelen uit de bediening.
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(
                    this,
                    DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true)
                )
            )
            .build()

        // Casten: Media3 wikkelt de speler op de telefoon in en wisselt zelf naar
        // een speaker of tv en terug, ook via de uitvoerkiezer van Android. Zonder
        // Google Play-diensten lukt dat niet; dan blijft het de speler op de telefoon.
        val player: Player = runCatching {
            CastPlayer.Builder(this)
                .setLocalPlayer(local)
                .setRemotePlayer(
                    RemoteCastPlayer.Builder(this)
                        .setMediaItemConverter(FeedUrlConverter())
                        .setSeekBackIncrementMs(SKIP_BACK_MS)
                        .setSeekForwardIncrementMs(SKIP_FORWARD_MS)
                        .build()
                )
                .build()
        }.getOrElse { local }

        player.addListener(object : Player.Listener {
            // De snelheidsknop draagt zijn eigen waarde als icoon; Media3
            // ververst de melding daar niet vanzelf voor.
            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                publishButtons()
            }

            // Of er te spoelen valt, weet de speler pas als de aflevering
            // geladen is — ná het moment dat het systeem de knoppen ophaalt.
            override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {
                publishButtons()
            }
        })

        // Tikken op de melding opent de app weer.
        val openApp = PendingIntent.getActivity(
            this, 0,
            packageManager.getLaunchIntentForPackage(packageName) ?: Intent(),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        mediaSession = MediaLibrarySession.Builder(this, player, Bediening())
            .setSessionActivity(openApp)
            .setCustomLayout(extraButtons())
            .build()

        // Stiltes overslaan en zachte opnames luider: twee schakelaars uit de instellingen.
        local.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) = applyBoost(audioSessionId)
        })
        scope.launch {
            container.store.playback.collect { settings ->
                local.skipSilenceEnabled = settings.skipSilence
                boostOn = settings.volumeBoost
                applyBoost(local.audioSessionId)
            }
        }

        // De laag die voortgang, geschiedenis en de wachtrij bijhoudt, luistert mee
        // zolang deze dienst leeft, ook als de app zelf niet open is (Android Auto, de widget).
        scope.launch {
            container.store.ensureLoaded()
            container.player.connect()
        }

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

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaSession

    /** 6 dB erbij, of niets. Faalt het effect op dit toestel, dan speelt alles gewoon door. */
    private fun applyBoost(audioSessionId: Int) {
        runCatching {
            enhancer?.release()
            enhancer = null
            if (boostOn && audioSessionId != C.AUDIO_SESSION_ID_UNSET && audioSessionId != 0) {
                enhancer = android.media.audiofx.LoudnessEnhancer(audioSessionId).apply {
                    setTargetGain(BOOST_MILLIBEL)
                    enabled = true
                }
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            // De eigen verbinding met deze dienst houdt hem anders in leven.
            container.player.release()
            stopSelf()
        }
    }

    override fun onDestroy() {
        // Zonder speler kan het proces zo weg; de laatste voortgang moet dan op schijf staan.
        container.appScope.launch { container.store.flush() }
        runCatching { enhancer?.release() }
        enhancer = null
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
            .setCustomIconResId(if (showPause) R.drawable.ic_notif_pause else R.drawable.ic_notif_play)
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
     *
     * De volgorde is die van de systeembediening: de eerste twee nemen de vaste
     * plekken van 'vorige' en 'volgende' naast afspelen in, de volgende twee
     * komen daarbuiten. Zo staat er snelheid, terug, afspelen, vooruit, ster.
     */
    private fun extraButtons(): ImmutableList<CommandButton> {
        val row = mutableListOf<CommandButton>()
        if (canSeek(Player.COMMAND_SEEK_BACK)) row += skipButton(back = true)
        if (canSeek(Player.COMMAND_SEEK_FORWARD)) row += skipButton(back = false)
        row += speedButton()
        row += saveButton()
        return ImmutableList.copyOf(row)
    }

    /** Zolang er niets klaarstaat, staat ook nog niet vast of er te zoeken valt. */
    private fun canSeek(command: Int): Boolean =
        mediaSession?.player?.isCommandAvailable(command) != false

    private fun skipButton(back: Boolean, extras: Bundle = Bundle.EMPTY): CommandButton {
        val seconds = (if (back) SKIP_BACK_MS else SKIP_FORWARD_MS) / 1000
        return CommandButton.Builder(skipIcon(back))
            .setCustomIconResId(if (back) R.drawable.ic_notif_skip_back else R.drawable.ic_notif_skip_forward)
            .setSessionCommand(SessionCommand(if (back) ACTION_BACK else ACTION_FORWARD, Bundle.EMPTY))
            .setDisplayName("$seconds seconden ${if (back) "terug" else "vooruit"}")
            .setExtras(extras)
            .setEnabled(true)
            .build()
    }

    private fun speedButton(): CommandButton {
        val speed = mediaSession?.player?.playbackParameters?.speed ?: 1f
        val (icon, drawable) = speedIcon(speed)
        return CommandButton.Builder(icon)
            .setCustomIconResId(drawable)
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
            .setCustomIconResId(if (saved) R.drawable.ic_notif_star_filled else R.drawable.ic_notif_star)
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

    /* ---- bladeren: Android Auto en andere bedieningen met een lijst ---- */

    private fun folder(id: String, title: String): MediaItem = MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_PODCASTS)
                .build()
        )
        .build()

    /** Een aflevering uit de bibliotheek als speelbaar item, met het bestand op het toestel als dat er is. */
    private fun playable(episode: Episode): MediaItem? {
        val audio = episode.audioUrl ?: return null
        val local = container.downloads.localFile(episode.id)?.let { android.net.Uri.fromFile(it).toString() }
        val item = episode.toMediaItem(local ?: audio)
        return item.buildUpon()
            .setMediaMetadata(
                item.mediaMetadata.buildUpon()
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE)
                    .build()
            )
            .build()
    }

    /** Wat er onder een map staat. Alles komt uit de eigen bibliotheek; er gaat niets het net op. */
    private fun childrenOf(parentId: String): List<MediaItem> {
        val store = container.store
        return when (parentId) {
            ROOT -> listOf(
                folder(FOLDER_RESUME, "Verder luisteren"),
                folder(FOLDER_QUEUE, "Wachtrij"),
                folder(FOLDER_NEW, "Nieuw"),
                folder(FOLDER_DOWNLOADS, "Gedownload"),
                folder(FOLDER_SAVED, "Bewaard")
            )
            FOLDER_RESUME -> store.history.value.asReversed()
                .filter { !it.finished && it.episode.id in store.progress.value }
                .map { it.episode.toEpisode() }
            FOLDER_QUEUE -> store.queue.value.map { it.toEpisode() }
            FOLDER_NEW -> NewRules.inbox(
                store.follows.value, store.feedChecks.value, store.listened.value, store.hiddenNew.value,
                java.time.LocalDate.now()
            ).map { it.toEpisode() }
            FOLDER_DOWNLOADS -> store.downloads.value.values
                .filter { it.state == nl.woolacast.data.local.DownloadState.DONE }
                .sortedByDescending { it.addedAt }
                .map { it.episode.toEpisode() }
            FOLDER_SAVED -> store.saved.value.asReversed().map { it.toEpisode() }
            else -> emptyList<Episode>()
        }.mapNotNull { if (it is MediaItem) it else playable(it as Episode) }.take(MAX_CHILDREN)
    }

    /**
     * Van wat een bediening stuurt naar iets speelbaars. Een item met een
     * adres gaat ongemoeid door; een id zoeken we op in de bibliotheek; en een
     * gesproken zoekvraag ("speel … af") vergelijken we met de titels en shows
     * van wat klaarstaat. Zonder zoekwoorden: verder waar je was.
     */
    private fun resolve(item: MediaItem): MediaItem? {
        if (item.localConfiguration != null) return item
        container.player.known(item.mediaId)?.let { return playable(it) }
        val query = item.requestMetadata.searchQuery ?: return null
        val ready = (childrenOf(FOLDER_RESUME) + childrenOf(FOLDER_QUEUE) + childrenOf(FOLDER_NEW) +
            childrenOf(FOLDER_DOWNLOADS) + childrenOf(FOLDER_SAVED)).distinctBy { it.mediaId }
        val words = query.lowercase().split(Regex("\\s+")).filter { it.length > 2 }
        if (words.isEmpty()) return ready.firstOrNull()
        return ready.maxByOrNull { candidate ->
            val text = (candidate.mediaMetadata.title.toString() + " " + candidate.mediaMetadata.artist.toString()).lowercase()
            words.count { it in text }
        }?.takeIf { candidate ->
            val text = (candidate.mediaMetadata.title.toString() + " " + candidate.mediaMetadata.artist.toString()).lowercase()
            words.any { it in text }
        }
    }

    private fun <T> afterLoad(block: () -> T): ListenableFuture<T> {
        val future = SettableFuture.create<T>()
        scope.launch {
            runCatching {
                container.store.ensureLoaded()
                block()
            }.onSuccess(future::set).onFailure(future::setException)
        }
        return future
    }

    private inner class Bediening : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: MediaLibraryService.LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> =
            Futures.immediateFuture(LibraryResult.ofItem(folder(ROOT, "Toadcast"), params))

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: MediaLibraryService.LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = afterLoad {
            val all = childrenOf(parentId)
            val from = (page.toLong() * pageSize).coerceAtMost(all.size.toLong()).toInt()
            LibraryResult.ofItemList(all.drop(from).take(pageSize.coerceAtLeast(1)), params)
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> = afterLoad {
            val item = container.player.known(mediaId)?.let(::playable)
            if (item != null) LibraryResult.ofItem(item, null)
            else LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
        }

        /**
         * Wie door de bibliotheek bladert, stuurt alleen een id terug. Het
         * adres van de audio zoeken we er zelf weer bij, in de eigen
         * bibliotheek, en de aflevering begint waar je gebleven was. Een item
         * dat zijn adres al draagt (zo zet de app zelf een aflevering klaar)
         * gaat ongemoeid door.
         */
        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            if (mediaItems.all { it.localConfiguration != null }) {
                return Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(mediaItems, startIndex, startPositionMs)
                )
            }
            return afterLoad {
                val resolved = mediaItems.mapNotNull(::resolve)
                val index = if (startIndex == C.INDEX_UNSET) 0 else startIndex.coerceIn(0, (resolved.size - 1).coerceAtLeast(0))
                val position = if (startPositionMs != C.TIME_UNSET) startPositionMs
                else resolved.getOrNull(index)?.let { container.store.progress.value[it.mediaId] } ?: C.TIME_UNSET
                MediaSession.MediaItemsWithStartPosition(resolved, index, position)
            }
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>
        ): ListenableFuture<List<MediaItem>> = afterLoad {
            mediaItems.mapNotNull(::resolve)
        }

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

    private companion object {
        const val ROOT = "toadcast-root"
        const val FOLDER_RESUME = "toadcast-resume"
        const val FOLDER_QUEUE = "toadcast-queue"
        const val FOLDER_NEW = "toadcast-new"
        const val FOLDER_DOWNLOADS = "toadcast-downloads"
        const val FOLDER_SAVED = "toadcast-saved"
        const val MAX_CHILDREN = 100

        /** 6 dB. */
        const val BOOST_MILLIBEL = 600
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
 * Wat de knop betekent, voor bedieningen die zelf een icoon kiezen. Getekend
 * wordt overal ons eigen icoon — een open pijl zonder getal — maar wie op de
 * betekenis afgaat, krijgt het goede getal mee. Media3 kent sprongen van 5,
 * 10, 15 en 30 seconden; daarbuiten liever een kale pijl dan een verkeerd getal.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun skipIcon(back: Boolean): Int =
    when ((if (back) SKIP_BACK_MS else SKIP_FORWARD_MS) / 1000L) {
        5L -> if (back) CommandButton.ICON_SKIP_BACK_5 else CommandButton.ICON_SKIP_FORWARD_5
        10L -> if (back) CommandButton.ICON_SKIP_BACK_10 else CommandButton.ICON_SKIP_FORWARD_10
        15L -> if (back) CommandButton.ICON_SKIP_BACK_15 else CommandButton.ICON_SKIP_FORWARD_15
        30L -> if (back) CommandButton.ICON_SKIP_BACK_30 else CommandButton.ICON_SKIP_FORWARD_30
        else -> if (back) CommandButton.ICON_SKIP_BACK else CommandButton.ICON_SKIP_FORWARD
    }

/**
 * Het cijfer op de snelheidsknop: Media3's betekenis per stap, en ons eigen
 * icoon — de snelheid als kale tekst, zoals "1x" — om hem te tekenen.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun speedIcon(speed: Float): Pair<Int, Int> = when {
    speed < 0.9f -> CommandButton.ICON_PLAYBACK_SPEED_0_8 to R.drawable.ic_notif_speed_0_8
    speed < 1.1f -> CommandButton.ICON_PLAYBACK_SPEED_1_0 to R.drawable.ic_notif_speed_1_0
    speed < 1.35f -> CommandButton.ICON_PLAYBACK_SPEED_1_2 to R.drawable.ic_notif_speed_1_2
    speed < 1.65f -> CommandButton.ICON_PLAYBACK_SPEED_1_5 to R.drawable.ic_notif_speed_1_5
    speed < 1.9f -> CommandButton.ICON_PLAYBACK_SPEED_1_8 to R.drawable.ic_notif_speed_1_8
    else -> CommandButton.ICON_PLAYBACK_SPEED_2_0 to R.drawable.ic_notif_speed_2_0
}
