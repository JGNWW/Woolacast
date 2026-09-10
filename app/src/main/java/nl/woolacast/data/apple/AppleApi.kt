package nl.woolacast.data.apple

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface AppleMarketingApi {
    /** [feed] is "podcasts" of "podcast-episodes". */
    @GET("api/v2/{country}/podcasts/top/{limit}/{feed}.json")
    suspend fun top(
        @Path("country") country: String,
        @Path("limit") limit: Int,
        @Path("feed") feed: String
    ): MarketingFeedResponse
}

interface AppleCatalogApi {
    /**
     * De oude rss-generator. Als volledige URL opgevraagd omdat de padvorm
     * (limit=50/genre=1303) geen gewone padsegmenten zijn.
     */
    @GET
    suspend fun legacyTop(@Url url: String): LegacyFeedResponse

    /** Zoekt een podcast op naam; de enige weg naar een feed voor een Spotify-vermelding. */
    @GET("search")
    suspend fun search(
        @Query("term") term: String,
        @Query("country") country: String,
        @Query("entity") entity: String = "podcast",
        @Query("limit") limit: Int = 1
    ): LookupResponse

    @GET("lookup")
    suspend fun lookup(
        @Query("id") id: String,
        @Query("country") country: String,
        @Query("entity") entity: String? = null,
        @Query("limit") limit: Int? = null
    ): LookupResponse
}
