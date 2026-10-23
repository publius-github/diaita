package app.diaita.data.remote

import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Retrofit/OkHttp wiring for OpenFoodFacts. A class (not an object) so the app version in
 * the User-Agent comes from the build ([app.diaita.BuildConfig.VERSION_NAME] injected by
 * [app.diaita.AppContainer]) instead of drifting in a hardcoded string.
 */
class OpenFoodFactsClient(
    appVersionName: String,
) {
    // OFF asks all API clients to identify themselves with a descriptive User-Agent.
    private val userAgent =
        "Diaita/$appVersionName (Android; non-commercial; +https://github.com/publius-github/diaita)"

    private val userAgentInterceptor =
        Interceptor { chain ->
            val request =
                chain
                    .request()
                    .newBuilder()
                    .header("User-Agent", userAgent)
                    .build()
            chain.proceed(request)
        }

    /**
     * One retry with a short backoff for transient I/O failures only. HTTP errors
     * (including 429) are never retried here — the caller surfaces them to the UI and
     * the 400 ms search debounce is the only client-side throttle OFF gets.
     */
    private val retryInterceptor =
        Interceptor { chain ->
            try {
                chain.proceed(chain.request())
            } catch (e: IOException) {
                Thread.sleep(RETRY_BACKOFF_MILLIS)
                chain.proceed(chain.request())
            }
        }

    private val okHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(userAgentInterceptor)
            .addInterceptor(retryInterceptor)
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

    private companion object {
        const val BASE_URL = "https://world.openfoodfacts.org/"
        const val CONNECT_TIMEOUT_SECONDS = 10L
        const val READ_TIMEOUT_SECONDS = 15L
        const val CALL_TIMEOUT_SECONDS = 30L
        const val RETRY_BACKOFF_MILLIS = 500L
    }
}
