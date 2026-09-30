package nl.woolacast.player

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import nl.woolacast.domain.Chapter
import nl.woolacast.domain.Episode
import nl.woolacast.domain.TranscriptRef

/** Wat Media3's metadata niet kent, maar de app wel nodig heeft. */
private const val EXTRA_SHOW_ID = "nl.woolacast.showId"
private const val EXTRA_RELEASE_DATE = "nl.woolacast.releaseDate"
private const val EXTRA_LINK = "nl.woolacast.link"
private const val EXTRA_REMOTE_URL = "nl.woolacast.remoteUrl"
private const val EXTRA_CHAPTERS_URL = "nl.woolacast.chaptersUrl"
private const val EXTRA_CHAPTERS = "nl.woolacast.chapters"
private const val EXTRA_TRANSCRIPT_URL = "nl.woolacast.transcriptUrl"
private const val EXTRA_TRANSCRIPT_TYPE = "nl.woolacast.transcriptType"

private val chapterList = ListSerializer(Chapter.serializer())

/**
 * Media3 kent titel, artiest en plaatje; de rest van een aflevering reist mee
 * in de extra's. Dat scheelt een tweede plek om te onthouden wat er speelt, en
 * het houdt "Ga naar podcast" en het bewaren heel wanneer de app opnieuw begint
 * terwijl de speler doorliep — dan is dit alles wat er nog van bekend is.
 */
/**
 * [playUri] is wat de speler leest: het bestand op het toestel als de aflevering
 * gedownload is, anders het adres uit de feed. Dat adres reist altijd mee, zodat
 * een aflevering die uit de speler wordt teruggebouwd nooit naar een bestand
 * wijst dat misschien al gewist is.
 */
internal fun Episode.toMediaItem(playUri: String): MediaItem =
    MediaItem.Builder()
        .setMediaId(id)
        .setUri(playUri)
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
                        audioUrl?.let { putString(EXTRA_REMOTE_URL, it) }
                        chaptersUrl?.let { putString(EXTRA_CHAPTERS_URL, it) }
                        if (inlineChapters.isNotEmpty()) {
                            putString(EXTRA_CHAPTERS, Json.encodeToString(chapterList, inlineChapters))
                        }
                        transcript?.let {
                            putString(EXTRA_TRANSCRIPT_URL, it.url)
                            putString(EXTRA_TRANSCRIPT_TYPE, it.type)
                        }
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
        audioUrl = extras?.getString(EXTRA_REMOTE_URL) ?: localConfiguration?.uri?.toString(),
        durationMillis = durationMillis,
        releaseDate = extras?.getString(EXTRA_RELEASE_DATE),
        link = extras?.getString(EXTRA_LINK),
        chaptersUrl = extras?.getString(EXTRA_CHAPTERS_URL),
        inlineChapters = extras?.getString(EXTRA_CHAPTERS)
            ?.let { runCatching { Json.decodeFromString(chapterList, it) }.getOrNull() }
            .orEmpty(),
        transcript = extras?.getString(EXTRA_TRANSCRIPT_URL)?.let { url ->
            TranscriptRef(url, extras.getString(EXTRA_TRANSCRIPT_TYPE).orEmpty())
        }
    )
}
