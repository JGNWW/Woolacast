package nl.woolacast.domain

/** Beweging ten opzichte van de vorige bewaarde momentopname. */
sealed interface Movement {
    /** Geen eerdere momentopname om mee te vergelijken. */
    data object Unknown : Movement
    data object New : Movement
    data object Flat : Movement
    data class Up(val places: Int) : Movement
    data class Down(val places: Int) : Movement
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
    val showId: String? = null
)

data class Chart(
    val query: ChartQuery,
    val entries: List<ChartEntry>,
    val updatedLabel: String?
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
    val releaseDate: String?
)
