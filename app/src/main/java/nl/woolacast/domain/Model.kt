package nl.woolacast.domain

import kotlinx.serialization.Serializable

/** Beweging ten opzichte van de vorige bewaarde momentopname. */
sealed interface Movement {
    /** Geen eerdere momentopname om mee te vergelijken. */
    data object Unknown : Movement
    data object New : Movement
    data object Flat : Movement
    /** [places] is null als de bron wel de richting geeft maar niet hoeveel. */
    data class Up(val places: Int?) : Movement
    data class Down(val places: Int?) : Movement
}

data class ChartEntry(
    val rank: Int,
    val id: String,
    val title: String,
    val publisher: String,
    val artworkUrl: String?,
    val genre: String?,
    val storeUrl: String?,
    val movement: Movement = Movement.Unknown,
    /** Alleen gevuld op afleveringniveau. */
    val showId: String? = null,
    /** Bekend bij open bronnen; bij Apple pas na een lookup. */
    val feedUrl: String? = null,
    val description: String? = null,
    /** Alleen op afleveringniveau, en alleen als de bron ze meegeeft. */
    val durationMillis: Long? = null,
    val releaseDate: String? = null,
    /** Alleen op het tabblad Nieuw: de dag waarop de show de lijst binnenkwam. */
    val enteredOn: String? = null
)

data class Chart(
    val query: ChartQuery,
    val entries: List<ChartEntry>,
    val updatedLabel: String?,
    /** Gezet als deze lijst uit de lokale cache komt in plaats van van het net. */
    val cachedAt: String? = null
)

data class Podcast(
    val id: String,
    val title: String,
    val publisher: String,
    val artworkUrl: String?,
    val description: String?,
    val genre: String?,
    val episodeCount: Int?,
    val feedUrl: String?
)

data class Episode(
    val id: String,
    val showId: String,
    val showTitle: String,
    val title: String,
    val description: String?,
    val artworkUrl: String?,
    /** Directe audio-URL uit de catalogus; null betekent niet afspeelbaar. */
    val audioUrl: String?,
    val durationMillis: Long?,
    val releaseDate: String?,
    /** Webpagina van de aflevering uit de feed, om te delen; anders de audio-URL. */
    val link: String? = null,
    /** Hoofdstukken als los JSON-bestand (podcast:chapters). */
    val chaptersUrl: String? = null,
    /** Hoofdstukken die in de feed zelf staan (Podlove Simple Chapters). */
    val inlineChapters: List<Chapter> = emptyList(),
    /** De beste transcriptie die de maker meelevert, als die er is. */
    val transcript: TranscriptRef? = null
)

/** Een hoofdstuk: waar het begint en hoe het heet. */
@Serializable
data class Chapter(val startMs: Long, val title: String)

/**
 * Een transcriptie zoals de feed hem aanwijst (podcast:transcript). [type] is
 * het mediatype: text/vtt, application/x-subrip, application/json, text/html…
 */
@Serializable
data class TranscriptRef(val url: String, val type: String, val language: String? = null) {
    /** Heeft deze vorm tijden, zodat de tekst kan meelopen? */
    val timed: Boolean get() = TranscriptFormats.timed(type)
}

object TranscriptFormats {
    /**
     * Welke vorm we het liefst lezen. JSON en VTT dragen sprekers en tijden,
     * SRT alleen tijden; HTML en platte tekst hebben geen van beide.
     */
    fun rank(type: String): Int = when (normalise(type)) {
        "application/json" -> 0
        "text/vtt" -> 1
        "application/x-subrip", "application/srt", "text/srt" -> 2
        "text/html" -> 3
        "text/plain" -> 4
        else -> 5
    }

    fun timed(type: String): Boolean = rank(type) <= 2

    fun normalise(type: String) = type.substringBefore(';').trim().lowercase()
}
