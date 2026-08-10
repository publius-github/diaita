package app.diaita.data.remote.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonPrimitive

/**
 * Response shape of GET https://world.openfoodfacts.org/cgi/search.pl (classic search).
 * The newer Search-a-licious host (search.openfoodfacts.org) proved too unreliable
 * (frequent 502s), so both search and barcode lookups use the main API host.
 */
@Serializable
data class OffSearchResponse(
    val count: Int = 0,
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 0,
    val products: List<OffProduct> = emptyList(),
)

/** Only the per-100g fields we need; `ignoreUnknownKeys` on the Json instance drops the rest. */
@Serializable
data class OffNutriments(
    @SerialName("energy-kcal_100g") val energyKcal100g: Double? = null,
    /** kJ per 100 g — the fallback when the kcal field is missing (some products carry only this). */
    @SerialName("energy_100g") val energyKj100g: Double? = null,
    @SerialName("proteins_100g") val proteins100g: Double? = null,
    @SerialName("fat_100g") val fat100g: Double? = null,
    @SerialName("carbohydrates_100g") val carbohydrates100g: Double? = null,
    @SerialName("fiber_100g") val fiber100g: Double? = null,
    @SerialName("sugars_100g") val sugars100g: Double? = null,
    @SerialName("salt_100g") val salt100g: Double? = null,
)

/** Response shape of GET https://world.openfoodfacts.org/api/v2/product/{barcode}.json. */
@Serializable
data class OffProductResponse(
    val status: Int = 0,
    val product: OffProduct? = null,
)

/** OFF serves `serving_quantity` as either a number or a quoted string — accept both. */
private object LenientDoubleSerializer : KSerializer<Double?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LenientDouble", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): Double? {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeDouble()
        return jsonDecoder
            .decodeJsonElement()
            .jsonPrimitive.content
            .toDoubleOrNull()
    }

    override fun serialize(
        encoder: Encoder,
        value: Double?,
    ) {
        encoder.encodeDouble(value ?: 0.0)
    }
}

@Serializable
data class OffProduct(
    /** The product barcode — the stable OFF identifier. */
    val code: String? = null,
    @SerialName("product_name") val productName: String? = null,
    /** One comma-separated string on the main API (unlike Search-a-licious's list). */
    val brands: String? = null,
    /** Grams per declared serving; feeds [app.diaita.data.db.entities.FoodItem.defaultServingGrams]. */
    @SerialName("serving_quantity")
    @Serializable(with = LenientDoubleSerializer::class)
    val servingQuantity: Double? = null,
    val nutriments: OffNutriments? = null,
) {
    companion object {
        private const val KCAL_PER_KJ = 1 / 4.184
    }

    /** kcal per 100 g, falling back to the kJ field so kJ-only products aren't dropped. */
    val energyKcal: Double?
        get() =
            nutriments?.energyKcal100g
                ?: nutriments?.energyKj100g?.let { it * KCAL_PER_KJ }
}
