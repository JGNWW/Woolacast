package nl.woolacast.data.fyyd

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class FyydResponse(
    val status: Int = 0,
    val msg: String? = null,
    val data: List<FyydPodcast> = emptyList()
)

@Serializable
data class FyydPodcast(
    val id: Long = 0L,
    val title: String = "",
    val author: String? = null,
    val xmlURL: String? = null,
    val htmlURL: String? = null,
    val imgURL: String? = null,
    val layoutImageURL: String? = null,
    val smallImageURL: String? = null
)

interface FyydApi {
    /** "hot" is fyyds doorlopende ranglijst; hij filtert op taal, niet op land. */
    @GET("0.2/feature/podcast/hot")
    suspend fun hot(
        @Query("count") count: Int,
        @Query("language") language: String?
    ): FyydResponse
}
