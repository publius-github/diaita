package app.diaita.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response shape of GET https://world.openfoodfacts.org/cgi/search.pl (classic search).
 * The newer Search-a-licious host (search.openfoodfacts.org) proved too unreliable
 * (frequent 502s), so both search and barcode lookups use the main API host.
 */
@Serializable
data class OffSearchResponse(
    val count: Int = 0,
    val products: List<OffProduct> = emptyList(),
)

/** Only the per-100g fields we need; `ignoreUnknownKeys` on the Json instance drops the rest. */
@Serializable
data class OffNutriments(
    @SerialName("energy-kcal_100g") val energyKcal100g: Double? = null,
    @SerialName("proteins_100g") val proteins100g: Double? = null,
    @SerialName("fat_100g") val fat100g: Double? = null,
    @SerialName("carbohydrates_100g") val carbohydrates100g: Double? = null,
)

/** Response shape of GET https://world.openfoodfacts.org/api/v2/product/{barcode}.json. */
@Serializable
data class OffProductResponse(
    val status: Int = 0,
    val product: OffProduct? = null,
)

@Serializable
data class OffProduct(
    /** The product barcode — the stable OFF identifier. */
    val code: String? = null,
    @SerialName("product_name") val productName: String? = null,
    /** One comma-separated string on the main API (unlike Search-a-licious's list). */
    val brands: String? = null,
    val nutriments: OffNutriments? = null,
)
