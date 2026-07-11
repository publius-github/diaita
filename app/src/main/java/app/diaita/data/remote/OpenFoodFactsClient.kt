package app.diaita.data.remote

import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object OpenFoodFactsClient {
    private const val BASE_URL = "https://world.openfoodfacts.org/"

    // OFF asks all API clients to identify themselves with a descriptive User-Agent.
    private const val USER_AGENT = "Diaita/1.1 (Android; personal use, non-commercial)"

    private val userAgentInterceptor =
        Interceptor { chain ->
            val request =
                chain
                    .request()
                    .newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .build()
            chain.proceed(request)
        }

    private val okHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(userAgentInterceptor)
            .build()

    // Real OFF search responses carry dozens of fields we don't model (categories, states_tags, etc).
    private val json = Json { ignoreUnknownKeys = true }

    val api: OpenFoodFactsApi =
        Retrofit
            .Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenFoodFactsApi::class.java)
}
