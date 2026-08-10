package app.diaita.data.remote

import app.diaita.data.remote.dto.OffProductResponse
import app.diaita.data.remote.dto.OffSearchResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenFoodFactsApi {
    /**
     * Classic full-text search. Chosen over the newer Search-a-licious host
     * (search.openfoodfacts.org) because that one kept answering 502.
     */
    @GET("cgi/search.pl")
    suspend fun searchByName(
        @Query("search_terms") query: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        /** Interface/product-name language, e.g. "ru" — OFF prefers matching translations. */
        @Query("lc") languageCode: String? = null,
        @Query("search_simple") searchSimple: Int = 1,
        @Query("action") action: String = "process",
        @Query("json") json: Int = 1,
        @Query("fields") fields: String = FIELDS,
    ): OffSearchResponse

    @GET("api/v2/product/{barcode}.json")
    suspend fun productByBarcode(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String = FIELDS,
    ): OffProductResponse

    companion object {
        const val FIELDS = "code,product_name,brands,nutriments,serving_quantity"
    }
}
