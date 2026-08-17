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

/** WHO adult cut-offs: 18.5 / 25 / 30. */
fun bmiBand(bmi: Double): BmiBand =
    when {
        bmi < 18.5 -> BmiBand.UNDERWEIGHT
        bmi < 25.0 -> BmiBand.NORMAL
        bmi < 30.0 -> BmiBand.OVERWEIGHT
        else -> BmiBand.OBESE
    }
