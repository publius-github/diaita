package app.diaita.util

/** WHO adult BMI bands, folded to the three colors the History tile can show. */
enum class BmiBand { UNDERWEIGHT, NORMAL, OVERWEIGHT, OBESE }

/** Body mass index (kg/m^2); null when either measurement is missing or non-positive. */
fun bmi(
    weightKg: Double?,
    heightCm: Double?,
): Double? {
    if (weightKg == null || heightCm == null || weightKg <= 0 || heightCm <= 0) return null
    val heightM = heightCm / 100.0
    return weightKg / (heightM * heightM)
}

/**
 * WHO adult cut-offs: 18.5 / 25 / 30. With [athleticBuild] the upper cut-offs shift to
 * 27.5 / 32.5 to allow for muscle mass. That shift is an informal heuristic, not a
 * published standard — no body publishes an athlete-corrected BMI; the rigorous fixes are
 * different metrics entirely (body fat %, FFMI, waist-to-height). The user chose this
 * knowingly ("на безрыбье и рак рыба", 2026-08-17). The underweight cut-off never moves:
 * muscle mass can't explain a BMI below 18.5.
 */
fun bmiBand(
    bmi: Double,
    athleticBuild: Boolean = false,
): BmiBand {
    val overweightFrom = if (athleticBuild) 27.5 else 25.0
    val obeseFrom = if (athleticBuild) 32.5 else 30.0
    return when {
        bmi < 18.5 -> BmiBand.UNDERWEIGHT
        bmi < overweightFrom -> BmiBand.NORMAL
        bmi < obeseFrom -> BmiBand.OVERWEIGHT
        else -> BmiBand.OBESE
    }
}
