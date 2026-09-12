package nl.woolacast.data.reco

import nl.woolacast.domain.ChartEntry

/**
 * Wat je smaak verraadt, uitgedrukt in drie dingen: in welke genres je luistert,
 * van welke makers, en welke woorden er in de titels van jouw podcasts staan.
 *
 * Er komt geen server aan te pas en er gaat niets de deur uit. Alles wat hier
 * gebeurt, gebeurt met wat al op het toestel staat: je bibliotheek, en de
 * hitlijsten die de app toch al ophaalt. AntennaPod laat aanbevelingen bewust
 * weg omdat een algoritme meestal een server met een profiel betekent; die
 * afweging vervalt als het profiel je toestel niet verlaat.
 */
data class TasteProfile(
    /** Genre → gewicht, opgeteld over je bibliotheek. */
    val genres: Map<String, Int> = emptyMap(),
    /** De makers waar je al iets van hebt. */
    val publishers: Set<String> = emptySet(),
    /** Woorden uit de titels, om verwantschap aan af te meten. */
    val words: Set<String> = emptySet(),
    /** Wat je al volgt of bewaard hebt; dat hoef je niet aanbevolen te krijgen. */
    val known: Set<String> = emptySet()
) {
    val isEmpty: Boolean get() = known.isEmpty()

    /** De genres waar je het meest in luistert, zwaarste eerst. */
    fun topGenres(limit: Int): List<String> =
        genres.entries.sortedByDescending { it.value }.take(limit).map { it.key }
}

/** Een aanbeveling met de reden erbij; zonder reden is het een gok. */
data class Suggestion(
    val entry: ChartEntry,
    val score: Double,
    val reason: String
)

object Recommender {

    /** Woorden die in elke podcasttitel staan en dus niets zeggen. */
    private val NOISE = setOf(
        "podcast", "podcasts", "de", "het", "een", "the", "and", "van", "met",
        "show", "radio", "der", "die", "das", "und", "les", "des", "los",
        "een", "over", "voor", "met", "aflevering", "episode", "season"
    )

    fun words(text: String?): Set<String> =
        text.orEmpty().lowercase()
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.length > 3 && it !in NOISE }
            .toSet()

    /**
     * Bouwt het profiel uit je bibliotheek. [genreOf] levert het genre van een
     * show; dat weet de app pas na een opzoeking, dus dat komt van buiten.
     */
    fun profile(
        shows: List<LibraryShow>,
        genreOf: (String) -> String? = { null }
    ): TasteProfile {
        val genres = mutableMapOf<String, Int>()
        val publishers = mutableSetOf<String>()
        val bag = mutableSetOf<String>()
        val known = mutableSetOf<String>()
        for (show in shows) {
            known += show.id
            val genre = show.genre ?: genreOf(show.id)
            if (!genre.isNullOrBlank()) {
                // Wat je volgt weegt zwaarder dan wat je een keer bewaarde.
                genres[genre] = (genres[genre] ?: 0) + show.weight
            }
            if (show.publisher.isNotBlank()) publishers += show.publisher.lowercase()
            bag += words(show.title)
        }
        return TasteProfile(genres, publishers, bag, known)
    }

    /**
     * Rangschikt kandidaten tegen een profiel. De volgorde komt tot stand uit
     * drie dingen: hoe zwaar het genre in jouw bibliotheek weegt, of je de maker
     * al kent, en hoeveel woorden de titel deelt met wat je al luistert. De
     * plek in de hitlijst telt licht mee, als scheidsrechter bij gelijke stand.
     */
    fun rank(
        profile: TasteProfile,
        candidates: List<ChartEntry>,
        limit: Int = 12
    ): List<Suggestion> {
        if (profile.isEmpty) return emptyList()
        val zwaarste = profile.genres.values.maxOrNull()?.toDouble() ?: 1.0
        val gezien = mutableSetOf<String>()
        return candidates
            .asSequence()
            .filter { it.id.isNotBlank() && it.id !in profile.known && gezien.add(it.id) }
            .map { entry ->
                val genreScore = (profile.genres[entry.genre] ?: 0) / zwaarste
                val zelfdeMaker = entry.publisher.lowercase() in profile.publishers
                val overlap = words(entry.title).intersect(profile.words).size
                val lijst = (100 - entry.rank).coerceAtLeast(0) / 400.0
                val score = genreScore * 2 + (if (zelfdeMaker) 1.5 else 0.0) +
                    overlap * 0.8 + lijst
                Suggestion(entry, score, reason(entry, genreScore > 0, zelfdeMaker, overlap))
            }
            .filter { it.score > 0.3 }
            .sortedByDescending { it.score }
            .take(limit)
            .toList()
    }

    private fun reason(
        entry: ChartEntry,
        genre: Boolean,
        zelfdeMaker: Boolean,
        overlap: Int
    ): String = when {
        zelfdeMaker -> "Ook van ${entry.publisher}"
        overlap > 0 -> "Lijkt op wat je luistert"
        genre -> "Veel geluisterd in ${entry.genre}"
        else -> "Populair in jouw land"
    }

    /**
     * Verwant aan één podcast. Hier is geen bibliotheek in het spel: alleen deze
     * show, zijn genre, zijn maker en de woorden uit zijn titel en omschrijving.
     */
    fun similarTo(
        title: String,
        publisher: String,
        genre: String?,
        description: String?,
        showId: String,
        candidates: List<ChartEntry>,
        limit: Int = 8
    ): List<Suggestion> {
        val bag = words(title) + words(description).take(40)
        val gezien = mutableSetOf<String>()
        return candidates
            .asSequence()
            .filter { it.id != showId && it.id.isNotBlank() && gezien.add(it.id) }
            .map { entry ->
                val zelfdeMaker = entry.publisher.isNotBlank() &&
                    entry.publisher.equals(publisher, ignoreCase = true)
                val zelfdeGenre = genre != null && entry.genre == genre
                val overlap = words(entry.title).intersect(bag).size
                val lijst = (100 - entry.rank).coerceAtLeast(0) / 300.0
                val score = (if (zelfdeMaker) 3.0 else 0.0) +
                    (if (zelfdeGenre) 1.0 else 0.0) + overlap * 0.9 + lijst
                val reden = when {
                    zelfdeMaker -> "Van dezelfde maker"
                    overlap > 0 -> "Zelfde onderwerpen"
                    zelfdeGenre -> "Zelfde categorie"
                    else -> "Ook populair"
                }
                Suggestion(entry, score, reden)
            }
            .filter { it.score > 0.4 }
            .sortedByDescending { it.score }
            .take(limit)
            .toList()
    }
}

/** Een show uit je bibliotheek, met hoe zwaar hij meetelt. */
data class LibraryShow(
    val id: String,
    val title: String,
    val publisher: String,
    val genre: String? = null,
    /** Volgen weegt zwaarder dan een losse bewaarde aflevering. */
    val weight: Int = 1
)
