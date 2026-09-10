package nl.woolacast.data.apple

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/* ---- rss.marketingtools.apple.com : top shows en top afleveringen ---- */

@Serializable
data class MarketingFeedResponse(val feed: MarketingFeed)

@Serializable
data class MarketingFeed(
    val title: String? = null,
    val country: String? = null,
    val updated: String? = null,
    val results: List<MarketingResult> = emptyList()
)

@Serializable
data class MarketingResult(
    val id: String,
    val name: String,
    val artistName: String? = null,
    val artworkUrl100: String? = null,
    val url: String? = null,
    val genres: List<MarketingGenre> = emptyList()
)

@Serializable
data class MarketingGenre(val name: String? = null, val genreId: String? = null)

/* ---- itunes.apple.com/rss : top shows met genrefilter ----
   Andere, oudere vorm van dezelfde catalogus. Deze is de enige publieke weg
   naar een lijst per categorie, dus hij blijft nodig naast de feed hierboven. */

@Serializable
data class LegacyFeedResponse(val feed: LegacyFeed)

@Serializable
data class LegacyFeed(
    val title: LegacyLabel? = null,
    val updated: LegacyLabel? = null,
    val entry: List<LegacyEntry> = emptyList()
)

@Serializable
data class LegacyLabel(val label: String? = null)

@Serializable
data class LegacyEntry(
    @SerialName("im:name") val name: LegacyLabel? = null,
    @SerialName("im:artist") val artist: LegacyLabel? = null,
    @SerialName("im:image") val images: List<LegacyLabel> = emptyList(),
    val summary: LegacyLabel? = null,
    val id: LegacyId? = null,
    val category: LegacyCategory? = null,
    val link: LegacyLink? = null
)

@Serializable
data class LegacyId(val attributes: LegacyIdAttributes? = null)

@Serializable
data class LegacyIdAttributes(@SerialName("im:id") val id: String? = null)

@Serializable
data class LegacyCategory(val attributes: LegacyCategoryAttributes? = null)

@Serializable
data class LegacyCategoryAttributes(val label: String? = null, val term: String? = null)

@Serializable
data class LegacyLink(val attributes: LegacyLinkAttributes? = null)

@Serializable
data class LegacyLinkAttributes(val href: String? = null)

/* ---- MZStoreServices : de echte ranglijst per categorie, alleen ids ---- */

@kotlinx.serialization.Serializable
data class ChartIdsResponse(val resultIds: List<String> = emptyList())

/* ---- itunes.apple.com/lookup : podcast plus afleveringen ---- */

@Serializable
data class LookupResponse(
    val resultCount: Int = 0,
    val results: List<LookupResult> = emptyList()
)

@Serializable
data class LookupResult(
    val wrapperType: String? = null,
    val kind: String? = null,
    val collectionId: Long? = null,
    val trackId: Long? = null,
    val collectionName: String? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val description: String? = null,
    val artworkUrl600: String? = null,
    val artworkUrl100: String? = null,
    val episodeUrl: String? = null,
    val trackTimeMillis: Long? = null,
    val releaseDate: String? = null,
    val feedUrl: String? = null,
    val trackCount: Int? = null,
    val primaryGenreName: String? = null
)
