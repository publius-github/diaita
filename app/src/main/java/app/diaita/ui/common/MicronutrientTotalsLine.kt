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
 * the History day detail. Renders nothing when no logged food carried any of the three, so days
 * of hand-entered foods without label data stay as clean as before.
 */
@Composable
fun MicronutrientTotalsLine(
    fiberGrams: Double?,
    sugarGrams: Double?,
    saltGrams: Double?,
    modifier: Modifier = Modifier,
) {
    val totals = micronutrientTotals(fiberGrams, sugarGrams, saltGrams)
    if (totals.isEmpty()) return
    val parts =
        totals.map { total ->
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
