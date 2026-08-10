package app.diaita.util

/**
 * Parses user-typed decimal input, accepting both '.' and ',' as the separator
 * (numeric keyboards on many locales only offer the comma).
 */
fun String.normalizedDecimalOrNull(): Double? = replace(',', '.').trim().toDoubleOrNull()

/** Formats grams for an editable field: whole numbers without the trailing ".0". */
fun formatGramsForInput(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

/**
 * One-decimal weight for display, in the user's locale (comma-decimal where customary).
 * Never use for CSV/machine formats — those stay on Locale.US.
 */
fun formatWeightForDisplay(value: Double): String = String.format(java.util.Locale.getDefault(), "%.1f", value)
