package app.diaita.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.diaita.R
import app.diaita.util.Micronutrient
import app.diaita.util.micronutrientTotals

/**
 * Fiber/sugar/salt totals under the day's calorie/macro figures, shared by the Diary card and
 * the History day detail.
 *
 * Always renders while the setting is on, exactly like the macro line above it — a line that
 * disappears on its own reads as a broken feature, which is precisely how this looked before.
 * Three cases: values when the day's foods carry them; a plain zero line on a day with nothing
 * logged (eating nothing really is 0 g); and an explicit "no data" when the day has entries but
 * none of them carries these values — quick-add entries never do, nor do foods cached from
 * OpenFoodFacts before the columns existed.
 */
@Composable
fun MicronutrientTotalsLine(
    fiberGrams: Double?,
    sugarGrams: Double?,
    saltGrams: Double?,
    hasEntries: Boolean,
    modifier: Modifier = Modifier,
) {
    val totals = micronutrientTotals(fiberGrams, sugarGrams, saltGrams)
    if (totals.isEmpty() && hasEntries) {
        Text(
            stringResource(R.string.micro_no_data),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }
    val parts =
        totals.ifEmpty { micronutrientTotals(0.0, 0.0, 0.0) }.map { total ->
            stringResource(
                when (total.nutrient) {
                    Micronutrient.FIBER -> R.string.micro_fiber_value
                    Micronutrient.SUGAR -> R.string.micro_sugar_value
                    Micronutrient.SALT -> R.string.micro_salt_value
                },
                total.displayGrams,
            )
        }
    Text(
        parts.joinToString("   "),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
