package nl.woolacast.domain

/**
 * Wie een podcast maakt. Apple kent voor een derde van de lijst een kanaal
 * (met logo en kleur); de rest heeft alleen een makersnaam, en die is rommelig:
 * "NPO Luister / BNNVARA", "Dag en Nacht | Podimo", "Podimo & Alexander
 * Klöpping". Eén regel maakt daar één maker van, zodat een show nooit bij twee
 * makers tegelijk telt.
 */
object Makers {

    private val separators = Regex("""\s+[/|&]\s+|\s+[/|]|[/|]\s+""")

    /** Het eerste deel van een makersnaam: "NPO Luister / BNNVARA" wordt "NPO Luister". */
    fun name(publisher: String): String =
        // Spotify geeft soms nog HTML-tekens mee: "Sam &amp; Rijk".
        publisher.replace("&amp;", "&").split(separators).firstOrNull { it.isNotBlank() }?.trim() ?: publisher.trim()

    /** Sleutel om op te vergelijken: kleine letters, alleen letters en cijfers. */
    fun key(name: String): String = name.lowercase().filter { it.isLetterOrDigit() }

    /** De sleutel van de maker achter een ruwe makersnaam. */
    fun keyOf(publisher: String): String = key(name(publisher))
}

/** Een Apple-kanaal zoals de verzamelaar het vastlegt. */
data class Channel(
    val id: String,
    val name: String,
    /** "ff6e00", zonder hekje; null als Apple er geen geeft. */
    val color: String?,
    val logoUrl: String?,
    val url: String?,
    val showCount: Int,
    val showIds: List<String>,
    val newShows: List<ChannelShow>,
    /** De hoezen van zijn eerste vier shows, zoals de verzamelaar ze vastlegde. */
    val covers: List<String> = emptyList()
)

/** Een nieuwe show van een kanaal, uit Apple's lijst "nieuwe programma's" van dat kanaal. */
data class ChannelShow(
    val id: String,
    val title: String,
    val artworkUrl: String?,
    val feedUrl: String?,
    /** Wanneer Apple de show voor het eerst kende, "2026-09-28". */
    val createdDate: String?,
    val trackCount: Int?
)

/**
 * Een maker zoals de app hem toont: met kanaal als Apple er een kent, anders
 * alleen een naam. [key] is wat volgen en tellen gebruiken.
 */
data class Maker(
    val key: String,
    val name: String,
    val channel: Channel? = null
)

/** Een show van een maker, met wanneer zijn nieuwste aflevering verscheen. */
data class MakerShow(
    val podcast: Podcast,
    /** Datum van de nieuwste aflevering, zoals de catalogus hem geeft. */
    val latestRelease: String?
)
