package nl.woolacast.data.spotify

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class SpotifyChartItem(
    val showUri: String? = null,
    val chartRankMove: String? = null,
    val showName: String = "",
    val showPublisher: String? = null,
    val showImageUrl: String? = null,
    val showDescription: String? = null,
    val episodeUri: String? = null,
    val episodeName: String? = null,
    val episodeImageUrl: String? = null,
    val episodeDescription: String? = null
)

/**
 * Het endpoint achter podcastcharts.byspotify.com. Geen sleutel, geen inlog —
 * dezelfde aanroep die hun eigen pagina doet. Wel ongedocumenteerd: Spotify kan
 * dit zonder aankondiging wijzigen, dus alles hieronder moet netjes stukgaan.
 */
interface SpotifyChartsApi {
    @Headers("Accept: application/json")
    @GET("api/charts/{category}")
    suspend fun chart(
        @Path("category") category: String,
        @Query("region") region: String,
        @Query("limit") limit: Int
    ): List<SpotifyChartItem>
}
