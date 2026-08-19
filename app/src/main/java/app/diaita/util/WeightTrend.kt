package app.diaita.util

import java.time.LocalDate

/** Weight-trend math for the History screen: rate of change and projection to target. */
object WeightTrend {
    /** Below this absolute weekly rate the trend counts as flat — no projection is shown. */
    const val FLAT_RATE_KG_PER_WEEK = 0.05

    /**
     * Minimum span between the first and last weigh-in for a fit. Was 7 days, which made
     * the rate/projection lines vanish on the 7-day History range (max span 6) — the user
     * wants them there too, accepting that a short-span fit is noisier (2026-08-19).
     */
    const val MIN_TREND_SPAN_DAYS = 3

    /** Projections further out than this are noise, not forecasts. */
    const val MAX_PROJECTION_DAYS = 365L * 2

    /**
     * Rate of weight change in kg/week via a least-squares fit over the weigh-ins.
     * Null when there are fewer than two points or they span less than
     * [MIN_TREND_SPAN_DAYS] — day-to-day water noise dominates anything shorter.
     */
    fun ratePerWeek(entries: List<Pair<LocalDate, Double>>): Double? {
        if (entries.size < 2) return null
        val first = entries.minOf { it.first }
        val spanDays = entries.maxOf { it.first }.toEpochDay() - first.toEpochDay()
        if (spanDays < MIN_TREND_SPAN_DAYS) return null

        val points = entries.map { (date, kg) -> (date.toEpochDay() - first.toEpochDay()).toDouble() to kg }
        val n = points.size.toDouble()
        val meanX = points.sumOf { it.first } / n
        val meanY = points.sumOf { it.second } / n
        val denominator = points.sumOf { (x, _) -> (x - meanX) * (x - meanX) }
        if (denominator == 0.0) return null
        val slopePerDay = points.sumOf { (x, y) -> (x - meanX) * (y - meanY) } / denominator
        return slopePerDay * 7
    }

    /**
     * The date the trend line crosses [targetKg], or null when the trend is flat, moving
     * away from the target, or the crossing is unreasonably far out ([MAX_PROJECTION_DAYS]).
     */
    fun projectedTargetDate(
        entries: List<Pair<LocalDate, Double>>,
        targetKg: Double,
    ): LocalDate? {
        val rate = ratePerWeek(entries) ?: return null
        if (kotlin.math.abs(rate) < FLAT_RATE_KG_PER_WEEK) return null
        val latest = entries.maxByOrNull { it.first } ?: return null
        val remainingKg = targetKg - latest.second
        if (remainingKg == 0.0) return latest.first
        // Moving away from the target: the signs of the remaining distance and rate differ.
        if (remainingKg * rate < 0) return null
        val daysToTarget = kotlin.math.round(remainingKg / (rate / 7)).toLong()
        if (daysToTarget > MAX_PROJECTION_DAYS) return null
        return latest.first.plusDays(daysToTarget)
    }
}
