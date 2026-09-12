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
        "podcast|podkast|poddar|poddradio|\\bpodd\\b|luistertip|h\\u00f6rtipp|" +
            // Japans: het woord zelf en de afkorting die koppen ervan maken.
            "ポッドキャスト|ポッド",
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

    /**
     * Elke taal zet zijn eigen tekens om een titel: ‘zo’ en “zo” bij ons, „zo“
     * in het Duits, ”zo” en »zo» in het Zweeds, «zo» in het Frans. Wie die niet
     * allemaal kent, mist in zo'n taal alles — Zweden stond daardoor op nul.
     */
    private const val OPEN = "‘“”«»„\"'"
    private const val CLOSE = "’”“»«\"'"

    /**
     * Japans zet een titel tussen 「 en 」. Daar hoort geen regel bij over wat
     * erachter komt: in "「…」がおすすめ" is dat gewoon het volgende woord, terwijl
     * een aanhalingsteken in ons schrift ook een afkappingsquote kan zijn.
     */
    private val CJK_QUOTED = Regex("[「『]([^「」『』]{2,70})[」』]")
    private val CJK = Regex("[\\u4e00-\\u9fff\\u3040-\\u30ff]")

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
        CJK_QUOTED.findAll(headline).forEach { add(it.groupValues[1]) }
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
        if (name.lowercase() in STOPWORDS) return false
        // Japans en Chinees kennen geen hoofdletters, schrijven zonder spaties
        // en zijn korter. Dit moet vóór de rest: zonder hoofdletters is
        // "HELEMAAL IN HOOFDLETTERS" altijd waar.
        if (CJK.containsMatchIn(name)) return name.length in 2..40
        if (name.length < 4 || name.length > 60) return false
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
            CJK_QUOTED.findAll(headline).any { it.groupValues[1] == name } ||
            NEAR_PODCAST.findAll(headline).any { it.groupValues[1].equals(name, ignoreCase = true) }

    /**
     * De kern van een showtitel: wat er overblijft als je de ondertitel en het
     * woord podcast eraf haalt. Apple noemt de show "Spuiten en Slikken De
     * Podcast", de krant schrijft "Spuiten en Slikken"; gemeten over tien
     * Nederlandse shows vindt de volledige titel nul koppen en de kern achttien.
     */
    fun coreTitle(showTitle: String): String {
        val bare = showTitle.substringBefore(":").substringBefore(" - ").trim()
        return PODCAST_SUFFIX.replace(bare, "").trim().trim('-', '–', '|', ',', ' ')
            .ifBlank { bare }
    }

    /**
     * Staat de titel in de kop als naam geschreven, dus met de hoofdletters die
     * de show zelf voert? Dat scheelt: "Nieuwe feiten in onderzoek Dascha
     * Graafsma" gaat niet over de podcast Nieuwe Feiten, en "de zwarte doos van
     * Trumps deportatieregime" niet over Zwarte Doos. Over tien shows gemeten
     * zakte Zwarte Doos van 82 treffers naar 8 en Nieuwe Feiten van 43 naar 1,
     * zonder dat er een echte tip sneuvelde.
     */
    fun titleAsName(headline: String, title: String): Boolean {
        if (title.length < 4) return false
        val pattern = Regex("(?<![\\p{L}\\p{N}])" + Regex.escape(title) + "(?![\\p{L}\\p{N}])",
            RegexOption.IGNORE_CASE)
        val found = pattern.find(headline)?.value ?: return false
        // EEN KOP IN KAPITALEN zegt niets over hoofdletters.
        if (found == found.uppercase() && found != found.lowercase()) return true
        return title.split(" ").zip(found.split(" ")).none { (want, got) ->
            want.firstOrNull()?.isUpperCase() == true && got.firstOrNull()?.isUpperCase() == false
        }
    }

    /**
     * Is deze titel eigen genoeg om op zichzelf te staan? Een kop die "Het Uur"
     * of "Echt Gebeurd" bevat kan over van alles gaan; bij zo'n titel eisen we
     * dat het woord podcast erbij staat. Drie woorden, achttien tekens of het
     * woord cast erin maakt een toevallige treffer onwaarschijnlijk.
     */
    fun strongTitle(title: String): Boolean =
        title.split(Regex("\\s+")).size >= 3 || title.length >= 18 ||
            title.contains("cast", ignoreCase = true)

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

    /**
     * Een krant die een persbericht overneemt tipt niets; die drukt af wat een
     * uitgever zelf rondstuurde. In elke taal die we raken herkenbaar aan een
     * woord vooraan de kop.
     */
    private val PRESS_RELEASE = Regex(
        "プレスリリース|press release|pressemitteilung|persbericht|" +
            "comunicado de prensa|communiqué de presse|comunicato stampa|" +
            "pressmeddelande|pressemeddelelse",
        RegexOption.IGNORE_CASE
    )

    fun isPressRelease(headline: String): Boolean = PRESS_RELEASE.containsMatchIn(headline)

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
        // Alleen de héle naam telt, niet een los woord eruit. "The Irish Times"
        // deelt "Irish" met half Ierland en "Guardian Australia" deelt
        // "Australia" met half Australië; op losse woorden filterden we daar
        // de echte tips weg.
        val losse = outlet.split(Regex("[\\W_]+"))
            .filter { it.isNotEmpty() && it.lowercase() !in HOUSE_SKIP }
        val words = if (losse.size == 1 && losse[0].length >= 2) losse else emptyList()
        // "F.A.Z. Bücher-Podcast" draagt de naam van de FAZ, maar met puntjes
        // ertussen herkent geen woordgrens hem. Plak zo'n reeks aan elkaar.
        val plat = Regex("\\b(?:\\p{L}\\.){2,}").replace(headline) { it.value.replace(".", "") }
        if (words.any {
                Regex("\\b${Regex.escape(it)}\\b", RegexOption.IGNORE_CASE)
                    .containsMatchIn(plat)
            }
        ) return true
        // En de volledige naam achter elkaar: "Guardian Australia" in de kop.
        val heel = normalise(outlet)
        if (heel.length >= 6 && normalise(headline).contains(heel)) return true
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
        // Een afkorting hoort bij de naam die hij afkort: BR is de Bayerischer
        // Rundfunk, FAZ de Frankfurter Allgemeine Zeitung. Dat delen ze niet
        // als woord, wel als beginletters.
        val kort = outlet.replace(Regex("[\\W_]+"), "").lowercase()
        if (kort.length in 2..4 && outlet == outlet.uppercase()) {
            val letters = publisher.split(Regex("[\\W_]+"))
                .filter { it.isNotEmpty() }
                .map { it.first().lowercaseChar() }
                .joinToString("")
            if (letters.startsWith(kort) || letters.contains(kort)) return true
        }
        // "NPO Radio 1" en "nporadio1" zijn hetzelfde huis, maar delen geen
        // woord. Zonder spaties en leestekens vallen ze wel samen.
        val flatOutlet = normalise(outlet)
        val flatPublisher = normalise(publisher)
        if (flatOutlet.length < 4 || flatPublisher.length < 4) return false
        return flatOutlet.contains(flatPublisher) || flatPublisher.contains(flatOutlet)
    }

    /** Letters en cijfers, kleine letters, verder niets — om titels te vergelijken. */
    /** "De Podcast", "the podcast", "der Podcast" achteraan een titel. */
    private val PODCAST_SUFFIX = Regex(
        "[\\s\\-\u2013|:,]*\\b(?:de|het|the|der|die|das|el|la|il|le|les|lo|o|a|en|ein)?" +
            "\\s*podcasts?\\s*$",
        RegexOption.IGNORE_CASE
    )

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
