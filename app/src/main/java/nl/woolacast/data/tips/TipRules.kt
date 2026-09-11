package nl.woolacast.data.tips

/**
 * De regels waarmee uit een krantenkop een podcasttip valt te halen. Ze staan
 * los van Android en van het netwerk, zodat ze te testen zijn: dit is het deel
 * waar het misgaat als het misgaat.
 *
 * Dezelfde regels draaien in de verzamelaar (charts-service/collect.py). Daar
 * wordt ook het artikel achter de kop gelezen; hier hebben we alleen de feed.
 * Wat we zien is dus minder, en daarom zijn de regels hier strenger.
 */
object TipRules {

    /** Het woord zelf, in de talen die we raken. */
    private val PODCAST = Regex(
        "podcast|podkast|poddar|poddradio|\\bpodd\\b|luistertip|h\\u00f6rtipp",
        RegexOption.IGNORE_CASE
    )

    /** Woorden die een aanbeveling aankondigen, in dezelfde talen. */
    private val TIP_WORD = Regex(
        "\\b(tip|tips|tipp|tipps|recensie|recensies|review|reviews|rese\\u00f1a|" +
            "beste|besten|best|bedste|b\\u00e4sta|migliori|mejores|meilleurs|melhores|" +
            "top\\s*\\d+|gids|guide|gu\\u00eda|lijst|lijstje|list|picks|selectie|" +
            "s\\u00e9lection|selecci\\u00f3n|anbefalinger|anbefaling|empfehlung|" +
            "empfehlungen|consigli|aanrader|aanraders|luistertip|luistertips|" +
            "must-listen|van de week|of the week|der woche|de la semana|" +
            "della settimana|de la semaine)\\b",
        RegexOption.IGNORE_CASE
    )

    private const val OPEN = "‘“«„\"'"
    private const val CLOSE = "’”»\"'"

    /**
     * Een aangehaalde titel. Let op de afkappingsquote: in "'Afrika's grootste
     * beursgang'" is de eerste sluitquote een apostrof, geen einde. Een echt
     * sluitteken wordt niet door een letter gevolgd.
     */
    private val QUOTED = Regex("[$OPEN]([^$OPEN$CLOSE]{3,70})[$CLOSE](?![\\p{L}\\p{N}])")

    /** "Podcast 'X' onderzoekt ..." en "Podcasttip: X - ...". */
    private val NEAR_PODCAST = Regex(
        "podcast(?:serie|reeks|tip|recensie)?\\s*:?\\s*[$OPEN]([^$OPEN$CLOSE]{3,70})[$CLOSE]",
        RegexOption.IGNORE_CASE
    )
    private val TITLE_PREFIX = Regex(
        "^podcast\\s*(?:tip:?\\s+)?([^:–—-]{3,60})[:–—-]",
        RegexOption.IGNORE_CASE
    )

    /** Woorden die nooit een podcasttitel zijn, hoe vaak ze ook opduiken. */
    private val STOPWORDS = setOf(
        "podcast", "podcasts", "lees meer", "read more", "meest gelezen",
        "nieuws", "news", "home", "menu", "abonneren", "subscribe", "deel",
        "luister", "beluister", "listen", "share", "video", "audio"
    )

    fun mentionsPodcast(text: String): Boolean = PODCAST.containsMatchIn(text)

    fun mentionsTip(text: String): Boolean = TIP_WORD.containsMatchIn(text)

    /**
     * Kandidaat-titels uit een kop. Eerst de directe aanwijzingen (het woord
     * podcast met een titel erachter), dan elke aanhaling die op een titel
     * lijkt. Elke kandidaat moet verderop nog exact een show in Apple's
     * catalogus zijn; dit is het net, niet de zeef.
     */
    fun candidates(headline: String): List<String> {
        val found = LinkedHashSet<String>()
        fun add(raw: String?) {
            val name = raw?.trim(' ', '.', ',', ':', ';', '–', '—', '-') ?: return
            if (looksLikeTitle(name)) found.add(name)
        }
        TITLE_PREFIX.find(headline)?.let { add(it.groupValues[1]) }
        NEAR_PODCAST.findAll(headline).forEach { add(it.groupValues[1]) }
        QUOTED.findAll(headline).forEach { add(it.groupValues[1]) }
        return found.take(4)
    }

    /**
     * De opsomming onder een tiplijst: "Met deze week: Luister Anita, Dan
     * Taberski's Manifesto, Proces X en De onderwereld." Dat is de enige plek
     * waar een feed de vijf besproken podcasts bij naam noemt, en het is de
     * reden dat de app zonder het artikel toch iets kan.
     */
    private val LIST_INTRO = Regex(
        "(?:met deze week|deze week|met onder meer|met o\\.a\\.|this week|" +
            "diese woche|esta semana|cette semaine|questa settimana)\\s*:\\s*(.+)",
        RegexOption.IGNORE_CASE
    )
    private val LIST_SPLIT = Regex("\\s*[,;]\\s*|\\s+(?:en|and|y|et|und|e)\\s+", RegexOption.IGNORE_CASE)

    /** "In de podcast Mijn taalmaatje wil naar huis volgt ..." — zonder aanhaling. */
    private val AFTER_WORD = Regex(
        "\\bpodcast(?:serie|reeks)?\\s+" +
            "([\\p{Lu}][\\p{L}'’\\-]*(?:\\s+[\\p{L}'’\\-]+){0,5})"
    )

    fun listCandidates(text: String): List<String> {
        val tail = LIST_INTRO.find(text)?.groupValues?.get(1) ?: return emptyList()
        return LIST_SPLIT.split(tail)
            .map { it.trim(' ', '.') }
            .filter { looksLikeTitle(it) }
            .take(8)
    }

    /**
     * De naam achter het woord podcast, woord voor woord langer geprobeerd:
     * waar de zin ophoudt en de titel begint is van buiten niet te zien, dus
     * laten we Apple's catalogus beslissen welke lengte bestaat.
     */
    fun afterWordCandidates(text: String): List<String> {
        val found = LinkedHashSet<String>()
        for (match in AFTER_WORD.findAll(text)) {
            val words = match.groupValues[1].split(' ').filter { it.isNotBlank() }
            for (length in 2..minOf(words.size, 6)) {
                val name = words.take(length).joinToString(" ").trim(' ', '.', ',')
                if (looksLikeTitle(name)) found.add(name)
            }
        }
        return found.take(8)
    }

    fun looksLikeTitle(name: String): Boolean {
        if (name.length < 4 || name.length > 60) return false
        if (name.lowercase() in STOPWORDS) return false
        if (name.count { it.isLetter() } < 4) return false
        if (name.split(' ').size > 8) return false
        if (name.endsWith("...") || name.endsWith("?") || name.endsWith("!")) return false
        // HELEMAAL IN HOOFDLETTERS is een rubriekskop, geen titel.
        if (name.length > 4 && name == name.uppercase() && !name.contains(' ')) return false
        return name.first().isUpperCase() || name.first().isDigit()
    }

    /** Staat de naam tussen aanhalingstekens? Uit een kop alleen is dat het bewijs. */
    fun quotedIn(headline: String, name: String): Boolean =
        QUOTED.findAll(headline).any { it.groupValues[1].equals(name, ignoreCase = true) } ||
            NEAR_PODCAST.findAll(headline).any { it.groupValues[1].equals(name, ignoreCase = true) }

    /**
     * Draagt deze kop de titel van een show die we al kennen? Losser dan de
     * regel voor een onbekende titel: we hoeven niets te raden, dus een titel
     * die er letterlijk in staat is genoeg. Wel op een woordgrens, anders is
     * "Serial" ook een treffer in "Serialiseren".
     */
    fun titleIn(headline: String, showTitle: String): Boolean {
        val bare = showTitle.substringBefore(":").substringBefore(" - ").trim()
        if (bare.length < 4) return false
        val pattern = Regex("(?<![\\p{L}\\p{N}])" + Regex.escape(bare) + "(?![\\p{L}\\p{N}])",
            RegexOption.IGNORE_CASE)
        return pattern.containsMatchIn(headline)
    }

    /** Staat de naam in dezelfde adem als het woord podcast? */
    fun nearPodcast(headline: String, name: String, window: Int = 50): Boolean {
        var from = 0
        while (true) {
            val at = headline.indexOf(name, from, ignoreCase = true)
            if (at < 0) return false
            val start = maxOf(0, at - window)
            val end = minOf(headline.length, at + name.length + window)
            if (PODCAST.containsMatchIn(headline.substring(start, end))) return true
            from = at + 1
        }
    }

    /** Lidwoorden horen niet bij de naam van een medium. */
    private val HOUSE_SKIP = setOf(
        "de", "het", "een", "the", "la", "le", "el", "il", "los", "las",
        "les", "der", "die", "das", "van", "en", "and", "of", "nl", "be"
    )

    /**
     * Een medium dat zijn eigen aflevering aankondigt geeft geen tip. Dat is te
     * zien aan de kop ("SZ-Podcast: ...") en aan de uitgever van de show.
     */
    fun ownAnnouncement(headline: String, outlet: String, publisher: String): Boolean {
        // Ook een afkorting telt: "SZ-Podcast" is de Süddeutsche. Wel op een
        // woordgrens, anders zit "AD" in "advies" — en zonder de lidwoorden,
        // want wie "de" als naam telt schrapt de halve Nederlandse pers.
        val words = outlet.split(Regex("[\\W_]+"))
            .filter { it.length >= 2 && it.lowercase() !in HOUSE_SKIP }
        if (words.any {
                Regex("\\b${Regex.escape(it)}\\b", RegexOption.IGNORE_CASE)
                    .containsMatchIn(headline)
            }
        ) return true
        return sameHouse(outlet, publisher)
    }

    fun sameHouse(outlet: String, publisher: String): Boolean {
        val skip = setOf(
            "de", "het", "the", "la", "le", "el", "il", "podcast", "podcasts",
            "media", "nieuws", "news", "radio", "van", "and", "with"
        )
        fun tokens(text: String) = text.lowercase()
            .split(Regex("[\\W_]+"))
            .filter { it.length > 2 && it !in skip }
            .toSet()
        if (tokens(outlet).intersect(tokens(publisher)).isNotEmpty()) return true
        // "NPO Radio 1" en "nporadio1" zijn hetzelfde huis, maar delen geen
        // woord. Zonder spaties en leestekens vallen ze wel samen.
        val flatOutlet = normalise(outlet)
        val flatPublisher = normalise(publisher)
        if (flatOutlet.length < 4 || flatPublisher.length < 4) return false
        return flatOutlet.contains(flatPublisher) || flatPublisher.contains(flatOutlet)
    }

    /** Letters en cijfers, kleine letters, verder niets — om titels te vergelijken. */
    fun normalise(text: String): String =
        text.lowercase().filter { it.isLetterOrDigit() }

    /**
     * Heet de show precies zo? Een ondertitel achter een dubbele punt of een
     * streepje mag wegvallen, de rest niet: "Laika" mag geen show opleveren die
     * het woord toevallig in zijn titel heeft.
     */
    fun titleMatches(candidate: String, showTitle: String): Boolean {
        val wanted = normalise(candidate)
        if (wanted.length < 4) return false
        val variants = setOf(
            normalise(showTitle),
            normalise(showTitle.substringBefore(":")),
            normalise(showTitle.substringBefore(" - "))
        )
        return wanted in variants
    }
}
