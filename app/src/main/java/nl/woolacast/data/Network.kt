package nl.woolacast.data

import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import nl.woolacast.data.apple.AppleCatalogApi
import nl.woolacast.data.apple.AppleMarketingApi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object Network {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    fun marketingApi(): AppleMarketingApi =
        retrofit("https://rss.marketingtools.apple.com/").create(AppleMarketingApi::class.java)

    fun catalogApi(): AppleCatalogApi =
        retrofit("https://itunes.apple.com/").create(AppleCatalogApi::class.java)
}
