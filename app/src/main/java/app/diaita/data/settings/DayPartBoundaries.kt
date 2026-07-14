package app.diaita.data.settings

/**
 * User-configurable cut-off times (as minutes of day) that split the day into
 * morning / day / evening. They drive both the day-part balance bar on the Home
 * screen and the default meal slot suggested when logging food.
 */
data class DayPartBoundaries(
    val morningEndMinutes: Int = DEFAULT_MORNING_END_MINUTES,
    val dayEndMinutes: Int = DEFAULT_DAY_END_MINUTES,
) {
    companion object {
        /** 12:00 — historical hardcoded boundary between morning and day. */
        const val DEFAULT_MORNING_END_MINUTES = 12 * 60

        /** 17:00 — historical hardcoded boundary between day and evening. */
        const val DEFAULT_DAY_END_MINUTES = 17 * 60
    }
}
