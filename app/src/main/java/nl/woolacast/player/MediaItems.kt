package nl.woolacast.player

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import nl.woolacast.domain.Episode

/** Wat Media3's metadata niet kent, maar de app wel nodig heeft. */
private const val EXTRA_SHOW_ID = "nl.woolacast.showId"
private const val EXTRA_RELEASE_DATE = "nl.woolacast.releaseDate"
private const val EXTRA_LINK = "nl.woolacast.link"

/**
 * Media3 kent titel, artiest en plaatje; de rest van een aflevering reist mee
 * in de extra's. Dat scheelt een tweede plek om te onthouden wat er speelt, en
 * het houdt "Ga naar podcast" en het bewaren heel wanneer de app opnieuw begint
 * terwijl de speler doorliep — dan is dit alles wat er nog van bekend is.
 */
internal fun Episode.toMediaItem(audioUrl: String): MediaItem =
    MediaItem.Builder()
        .setMediaId(id)
        .setUri(audioUrl)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(showTitle)
                .setArtworkUri(artworkUrl?.let(Uri::parse))
                .setExtras(
                    Bundle().apply {
                        putString(EXTRA_SHOW_ID, showId)
                        releaseDate?.let { putString(EXTRA_RELEASE_DATE, it) }
                        link?.let { putString(EXTRA_LINK, it) }
                    }
                )
                .build()
        )
        .build()

/** De weg terug, voor als de speler ouder is dan het scherm. */
internal fun MediaItem.toEpisode(durationMillis: Long?): Episode {
    val meta = mediaMetadata
    val extras = meta.extras
    return Episode(
        id = mediaId,
        showId = extras?.getString(EXTRA_SHOW_ID).orEmpty(),
        showTitle = meta.artist?.toString().orEmpty(),
        title = meta.title?.toString().orEmpty(),
        description = null,
        artworkUrl = meta.artworkUri?.toString(),
        audioUrl = localConfiguration?.uri?.toString(),
        durationMillis = durationMillis,
        releaseDate = extras?.getString(EXTRA_RELEASE_DATE),
        link = extras?.getString(EXTRA_LINK)
    )
}
