package nl.woolacast.data.dataset

import kotlinx.serialization.Serializable

/**
 * Een podcasttip uit een krant, omroep of podcastgids. Wat de app toont komt
 * uit het artikel; de show erachter is opgezocht in Apple's catalogus, zodat
 * je er meteen naartoe kunt.
 */
@Serializable
data class MediaTip(
    val outlet: String = "",
    val headline: String = "",
    val summary: String = "",
    val url: String = "",
    val date: String? = null,
    val showId: String? = null,
    val showTitle: String? = null,
    val publisher: String = "",
    val artworkUrl: String? = null,
    val feedUrl: String? = null,
    val genre: String? = null,
    /** Bestandsnaam van het beeldmerk; de dataset maakt er een adres van. */
    val logo: String? = null
)

@Serializable
data class MediaTips(
    val country: String = "",
    val updated: String? = null,
    val count: Int = 0,
    val outlets: List<String> = emptyList(),
    val entries: List<MediaTip> = emptyList()
)

/**
 * Waar de app zelf kan kijken. De verzamelaar schrijft per land op welke feeds
 * er zijn en wat voor soort ze zijn, zodat een nieuwe bron geen nieuwe versie
 * van de app vraagt.
 *
 *   guide   de feed van een podcastrubriek: alles erin gaat al over podcasts
 *   news    een gewone nieuwsfeed: daar moet een tipwoord bij staan
 *   google  een zoekopdracht bij Google Nieuws, met de strengste regels
 */
@Serializable
data class TipFeed(
    val outlet: String = "",
    val url: String = "",
    val kind: String = "news",
    val logo: String? = null
)

@Serializable
data class TipFeeds(
    val country: String = "",
    val updated: String? = null,
    val count: Int = 0,
    /** De media die in dit land meetellen, voor wat uit een zoekmachine komt. */
    val hosts: List<String> = emptyList(),
    val entries: List<TipFeed> = emptyList()
)
