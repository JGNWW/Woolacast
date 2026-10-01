package nl.woolacast.player

import android.net.Uri
import androidx.media3.cast.DefaultMediaItemConverter
import androidx.media3.cast.MediaItemConverter
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import com.google.android.gms.cast.MediaQueueItem

/**
 * Wat er naar de speaker gaat. Een speaker kan niet bij een bestand op de
 * telefoon, dus een gedownloade aflevering gaat naar de speaker met het adres
 * uit de feed; dat reist altijd mee in de extra's. En de ontvanger wil weten
 * wat voor geluid het is, wat een feed niet altijd zegt: dan raden we het aan de
 * extensie, en anders is het mp3, zoals bij vrijwel elke podcast.
 */
internal fun MediaItem.forRemote(): MediaItem {
    val config = localConfiguration ?: return this
    val uri = config.uri
    val remote = if (uri.scheme == "http" || uri.scheme == "https") uri
        else remoteUrl()?.let(Uri::parse) ?: return this
    val mime = config.mimeType ?: audioMimeFor(remote.toString())
    return buildUpon().setUri(remote).setMimeType(mime).build()
}

/** Raadt het soort geluid aan het adres, zonder de vraagtekens erachter. */
internal fun audioMimeFor(url: String): String =
    when (url.substringBefore('?').substringAfterLast('.', "").lowercase()) {
        "m4a", "mp4", "aac" -> MimeTypes.AUDIO_MP4
        "ogg", "oga" -> MimeTypes.AUDIO_OGG
        "opus" -> MimeTypes.AUDIO_OPUS
        "wav" -> MimeTypes.AUDIO_WAV
        else -> MimeTypes.AUDIO_MPEG
    }

/** Media3's omzetting naar de speaker, met het feedadres in plaats van het bestand. */
@UnstableApi
internal class FeedUrlConverter(
    private val base: MediaItemConverter = DefaultMediaItemConverter()
) : MediaItemConverter {
    override fun toMediaQueueItem(mediaItem: MediaItem): MediaQueueItem = base.toMediaQueueItem(mediaItem.forRemote())
    override fun toMediaItem(mediaQueueItem: MediaQueueItem): MediaItem = base.toMediaItem(mediaQueueItem)
}
