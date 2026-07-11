package app.diaita.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response shape of GET https://search.openfoodfacts.org/search (Search-a-licious). */
@Serializable
data class OffSearchResponse(
    val hits: List<OffSearchHit> = emptyList(),
)

@Serializable
data class OffSearchHit(
    val code: String? = null,
    @SerialName("product_name") val productName: String? = null,
    val brands: List<String>? = null,
    val nutriments: OffNutriments? = null,
)

/** Only the per-100g fields we need; `ignoreUnknownKeys` on the Json instance drops the rest. */
@Serializable
data class OffNutriments(
    @SerialName("energy-kcal_100g") val energyKcal100g: Double? = null,
    @SerialName("proteins_100g") val proteins100g: Double? = null,
    @SerialName("fat_100g") val fat100g: Double? = null,
    @SerialName("carbohydrates_100g") val carbohydrates100g: Double? = null,
)
