package nl.woolacast.domain

/** De drie hitlijsten die de app kent. */
enum class SourceId(val label: String, val initial: String) {
    APPLE("Apple Podcasts", "A"),
    SPOTIFY("Spotify", "S")
}

/** Podcast- of afleveringniveau. */
enum class ChartLevel(val label: String) {
    SHOWS("Podcasts"),
    EPISODES("Afleveringen")
}

data class Country(val code: String, val label: String, val flag: String)

/**
 * Categorie. [appleGenreId] is het genre-id van de iTunes-catalogus; null betekent
 * "alle categorieen".
 */
data class Category(
    val label: String,
    val appleGenreId: Int?,
    val spotifySlug: String? = null
) {
    val isAll: Boolean get() = appleGenreId == null
}

data class ChartQuery(
    val source: SourceId,
    val country: Country,
    val category: Category,
    val level: ChartLevel,
    val limit: Int = 200
) {
    /** Stabiele sleutel om momentopnames onder te bewaren. */
    val key: String
        get() = "${source.name}:${country.code}:${category.appleGenreId ?: "all"}:${level.name}"
}

/**
 * Wat een bron werkelijk publiceert. Niet elke bron kan elk niveau, en een
 * categoriefilter bestaat lang niet overal — de UI leunt hierop om nooit een
 * lege lijst te tonen waar simpelweg geen data voor is.
 */
data class SourceCapabilities(
    val levels: Set<ChartLevel>,
    val categoryLevels: Set<ChartLevel>,
    val countryCount: Int,
    val cadence: String,
    val summary: String
) {
    fun supports(level: ChartLevel) = level in levels
    fun supportsCategories(level: ChartLevel) = level in categoryLevels
}

object Catalog {

    val countries = listOf(
        Country("nl", "Nederland", "🇳🇱"),
        Country("be", "Belgie", "🇧🇪"),
        Country("de", "Duitsland", "🇩🇪"),
        Country("gb", "Verenigd Koninkrijk", "🇬🇧"),
        Country("us", "Verenigde Staten", "🇺🇸"),
        Country("fr", "Frankrijk", "🇫🇷"),
        Country("es", "Spanje", "🇪🇸"),
        Country("it", "Italie", "🇮🇹"),
        Country("se", "Zweden", "🇸🇪"),
        Country("dk", "Denemarken", "🇩🇰"),
        Country("no", "Noorwegen", "🇳🇴"),
        Country("ie", "Ierland", "🇮🇪"),
        Country("ca", "Canada", "🇨🇦"),
        Country("au", "Australie", "🇦🇺"),
        Country("br", "Brazilie", "🇧🇷"),
        Country("mx", "Mexico", "🇲🇽"),
        Country("jp", "Japan", "🇯🇵"),
        Country("in", "India", "🇮🇳")
    )

    /** Genre-ids zoals de iTunes-catalogus ze gebruikt; alle 19 nagelopen op de live feed. */
    val categories = listOf(
        Category("Alle categorieen", null),
        Category("Comedy", 1303, "comedy"),
        Category("Nieuws", 1489, "news"),
        Category("True crime", 1488, "true-crime"),
        Category("Maatschappij & cultuur", 1324, "society-culture"),
        Category("Sport", 1545, "sports"),
        Category("Zaken", 1321, "business"),
        Category("Wetenschap", 1533, "science"),
        Category("Geschiedenis", 1487, "history"),
        Category("Gezondheid & fitness", 1512, "health-fitness"),
        Category("Kunst", 1301, "arts"),
        Category("Educatie", 1304, "education"),
        Category("Technologie", 1318, "technology"),
        Category("Muziek", 1310, "music"),
        Category("Fictie", 1483, "fiction"),
        Category("Vrije tijd", 1502, "leisure"),
        Category("Kinderen & gezin", 1305),
        Category("Religie & spiritualiteit", 1314, "religion-spirituality"),
        Category("TV & film", 1309, "tv-film"),
        Category("Overheid", 1511)
    )

    val defaultCountry = countries.first()
    val defaultCategory = categories.first()

    fun country(code: String) = countries.firstOrNull { it.code == code } ?: defaultCountry
    fun category(genreId: Int?) = categories.firstOrNull { it.appleGenreId == genreId } ?: defaultCategory
}
