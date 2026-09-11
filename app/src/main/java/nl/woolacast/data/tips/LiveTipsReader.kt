package nl.woolacast.data.tips

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import nl.woolacast.data.apple.AppleCatalogApi
import nl.woolacast.data.dataset.MediaTip
import nl.woolacast.data.dataset.TipFeed
import nl.woolacast.data.dataset.TipFeeds
import nl.woolacast.data.feed.FeedClient
import nl.woolacast.data.feed.ParsedEpisode
import java.time.LocalDate

/**
 * Leest de podcastrubrieken van kranten, omroepen en gidsen rechtstreeks op het
 * toestel. Dat houdt de tips vers: de verzamelaar draait één keer per nacht,
 * dit draait wanneer je het scherm opent.
 *
 * Wat hier níet gebeurt is het artikel achter de kop lezen. Dat is het werk
 * waar de meeste haken en ogen zitten, het verschilt per site en het verandert
 * het vaakst; dat blijft in de verzamelaar, en de app erft het via de gegevens.
 * Hier zien we alleen wat de feed zelf geeft: kop, samenvatting, datum, link.
 *
 * Daarom is de regel hier streng. Een tip ontstaat pas als de kop of de
 * samenvatting het woord podcast draagt, er een titel in staat die exact een
 * show in Apple's catalogus is, en het medium niet zijn eigen aflevering
 * aankondigt. Liever een tip minder dan een kop boven de verkeerde podcast.
 */
class LiveTipsReader(
    private val feedClient: FeedClient,
    private val catalog: AppleCatalogApi
) {

    /** Titels die al opgezocht zijn, om niet twee keer hetzelfde te vragen. */
    private val resolved = mutableMapOf<String, MediaTip?>()

    suspend fun read(
        countryCode: String,
        catalogue: TipFeeds,
        withinDays: Long = 60
    ): List<MediaTip> = coroutineScope {
        val cutoff = LocalDate.now().minusDays(withinDays).toString()
        val hosts = catalogue.hosts.toSet()
        val perFeed = catalogue.entries.take(MAX_FEEDS).map { feed ->
            async(Dispatchers.IO) {
                runCatching { readFeed(countryCode, feed, cutoff, hosts) }
                    .getOrDefault(emptyList())
            }
        }.awaitAll()

        // Twee feeds van hetzelfde medium kunnen dezelfde tip geven, en een
        // medium kan een show twee keer noemen.
        val seen = mutableSetOf<Pair<String, String>>()
        perFeed.flatten()
            .sortedByDescending { it.date ?: "" }
            .filter { tip -> seen.add(tip.outlet to (tip.showId ?: tip.url)) }
    }

    private suspend fun readFeed(
        countryCode: String,
        feed: TipFeed,
        cutoff: String,
        hosts: Set<String>
    ): List<MediaTip> {
        val parsed = feedClient.fetch(feed.url)
        val tips = mutableListOf<MediaTip>()
        for (item in parsed.episodes.take(MAX_ITEMS_PER_FEED)) {
            if (tips.size >= MAX_TIPS_PER_FEED) break
            val date = item.releaseDate
            if (date != null && date < cutoff) continue
            val headline = headlineOf(item, feed)
            val blob = headline + " " + (item.description ?: "")
            if (!TipRules.mentionsPodcast(blob)) continue
            if (feed.kind == "news" && !TipRules.mentionsTip(blob)) continue
            // Uit een zoekmachine komt alleen de kop mee, zonder tekst eronder;
            // daar moet de kop het dus helemaal zelf zeggen.
            if (feed.kind == "google" && !TipRules.mentionsPodcast(headline)) continue
            // Een zoekmachine levert ook blogs en persberichtensites op. Alleen
            // de media die in dit land meetellen doen mee.
            if (feed.kind == "google" && !allowed(item.sourceUrl, hosts)) continue

            val outlet = outletOf(item, feed)
            var kept = 0
            for (name in namesIn(headline, item.description, feed.kind)) {
                if (kept >= MAX_SHOWS_PER_ITEM) break
                if (feed.kind == "google" &&
                    !(TipRules.quotedIn(headline, name) && TipRules.nearPodcast(headline, name))
                ) continue
                val match = lookup(name, countryCode) ?: continue
                if (TipRules.ownAnnouncement(headline, outlet, match.publisher)) continue
                if (tips.any { it.showId == match.showId }) continue
                tips += match.copy(
                    outlet = outlet,
                    headline = headline,
                    summary = item.description?.take(300).orEmpty(),
                    url = item.link.orEmpty(),
                    date = date,
                    logo = feed.logo
                )
                kept++
            }
        }
        return tips
    }

    /**
     * Waar de namen vandaan komen. Uit een zoekmachine alleen de kop, want daar
     * staat verder niets bij. Uit de feed van een medium ook de samenvatting:
     * juist daar somt een tiplijst zijn vijf podcasts op.
     */
    private fun namesIn(headline: String, description: String?, kind: String): List<String> {
        if (kind == "google") return TipRules.candidates(headline)
        val text = headline + " " + description.orEmpty()
        return (TipRules.listCandidates(text) +
            TipRules.candidates(headline) +
            TipRules.candidates(description.orEmpty()) +
            TipRules.afterWordCandidates(text)).distinct().take(MAX_NAMES_PER_ITEM)
    }

    /** Google zet de naam van het medium achter de kop: "Kop - De Standaard". */
    private fun headlineOf(item: ParsedEpisode, feed: TipFeed): String {
        if (feed.kind != "google") return item.title
        val cut = item.title.lastIndexOf(" - ")
        return if (cut > 20) item.title.substring(0, cut) else item.title
    }

    private fun outletOf(item: ParsedEpisode, feed: TipFeed): String {
        if (feed.kind != "google") return feed.outlet
        item.sourceName?.takeIf { it.isNotBlank() }?.let { return tidyOutlet(it) }
        val cut = item.title.lastIndexOf(" - ")
        return if (cut > 20) item.title.substring(cut + 3).trim() else feed.outlet
    }

    /** "NRC - Nieuws, achtergronden en onderzoeksjournalistiek" wordt "NRC". */
    private fun tidyOutlet(name: String): String =
        name.split(Regex("\\s+[-–|]\\s+")).first().trim()

    private fun allowed(sourceUrl: String?, hosts: Set<String>): Boolean {
        if (hosts.isEmpty()) return true
        val host = (sourceUrl ?: return false)
            .substringAfter("//").substringBefore("/").removePrefix("www.")
        return hosts.any { host == it || host.endsWith(".$it") }
    }

    /**
     * Zoekt de genoemde titel op bij Apple. Alleen een show die exact zo heet
     * telt; de rest verdwijnt. Het antwoord blijft hangen, want dezelfde titel
     * komt in meerdere feeds voorbij.
     */
    private suspend fun lookup(name: String, countryCode: String): MediaTip? {
        val key = "$countryCode:${TipRules.normalise(name)}"
        if (resolved.containsKey(key)) return resolved[key]
        val found = withContext(Dispatchers.IO) {
            runCatching { catalog.search(term = name, country = countryCode, limit = 5) }
                .getOrNull()
                ?.results
                ?.firstOrNull { TipRules.titleMatches(name, it.collectionName.orEmpty()) }
                ?.let { hit ->
                    MediaTip(
                        showId = hit.collectionId?.toString(),
                        showTitle = hit.collectionName,
                        publisher = hit.artistName.orEmpty(),
                        artworkUrl = hit.artworkUrl600 ?: hit.artworkUrl100,
                        feedUrl = hit.feedUrl
                    )
                }
        }
        resolved[key] = found
        return found
    }

    private companion object {
        /** Zoveel verzoeken doet één verversing hooguit, plus de opzoekingen. */
        const val MAX_FEEDS = 16
        const val MAX_ITEMS_PER_FEED = 40
        const val MAX_TIPS_PER_FEED = 8

        /** Een tiplijst noemt er vijf; meer is bijvangst. */
        const val MAX_SHOWS_PER_ITEM = 6

        /** Zoveel titels proberen we hooguit op te zoeken per artikel. */
        const val MAX_NAMES_PER_ITEM = 12
    }
}
