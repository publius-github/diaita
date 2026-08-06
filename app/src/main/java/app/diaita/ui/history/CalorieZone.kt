package app.diaita.ui.history

/** How a day's intake relates to the goal and maintenance reference lines. */
enum class CalorieZone {
    /** On the goal side of both lines: the day works toward the plan. */
    ON_TRACK,

    /** Between goal and maintenance: missed the goal, but not moving in the wrong direction. */
    BETWEEN,

    /** Past maintenance on the wrong side: the day actively moves weight away from the plan. */
    OFF_TRACK,
}

/**
 * Classifies a day's calories against the goal and maintenance lines. The comparison is
 * direction-aware: on a deficit plan (goal below maintenance) overeating is off-track, on a
 * surplus plan (goal above maintenance) undereating is — the zones mirror.
 *
 * Without a usable maintenance value (wizard never completed, or plan is MAINTAIN so the two
 * lines coincide) this degrades to the historical two-state coloring: over goal = off-track.
 */
fun calorieZone(
    calories: Double,
    goalCalories: Int?,
    maintenanceCalories: Int?,
): CalorieZone {
    val goal = goalCalories?.takeIf { it > 0 } ?: return CalorieZone.ON_TRACK
    val maintenance = maintenanceCalories?.takeIf { it > 0 && it != goal }
    return when {
        maintenance == null -> if (calories > goal) CalorieZone.OFF_TRACK else CalorieZone.ON_TRACK
        goal < maintenance ->
            when {
                calories <= goal -> CalorieZone.ON_TRACK
                calories <= maintenance -> CalorieZone.BETWEEN
                else -> CalorieZone.OFF_TRACK
            }
        else ->
            when {
                calories >= goal -> CalorieZone.ON_TRACK
                calories >= maintenance -> CalorieZone.BETWEEN
                else -> CalorieZone.OFF_TRACK
            }
    }
}
