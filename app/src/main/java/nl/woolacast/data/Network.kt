package nl.woolacast.data

import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import nl.woolacast.data.apple.AppleCatalogApi
import nl.woolacast.data.apple.AppleMarketingApi
import nl.woolacast.data.fyyd.FyydApi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Interceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

object Network {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    /**
     * Sommige open API's (fyyd, Podcast Index) weigeren een verzoek zonder
     * herkenbare User-Agent, dus die zetten we overal.
     */
    private val userAgent = Interceptor { chain ->
        chain.proceed(
            chain.request().newBuilder()
                .header("User-Agent", "Woolacast/0.1 (+https://github.com/jgnww/Woolacast)")
                .build()
        )
    }

    val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(userAgent)
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

    fun fyydApi(): FyydApi =
        retrofit("https://api.fyyd.de/").create(FyydApi::class.java)
}
