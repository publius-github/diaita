package app.diaita.data.remote

import app.diaita.data.remote.dto.OffSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenFoodFactsApi {
    @GET("search")
    suspend fun searchByName(
        @Query("q") query: String,
        @Query("page_size") pageSize: Int = 20,
        @Query("langs") langs: String = "en",
    ): OffSearchResponse
}
