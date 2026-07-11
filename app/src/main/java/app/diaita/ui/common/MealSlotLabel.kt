package app.diaita.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.diaita.R
import app.diaita.data.db.entities.MealSlot

@Composable
fun MealSlot.label(): String = stringResource(
    when (this) {
        MealSlot.BREAKFAST -> R.string.meal_breakfast
        MealSlot.LUNCH -> R.string.meal_lunch
        MealSlot.DINNER -> R.string.meal_dinner
        MealSlot.SNACK -> R.string.meal_snack
        MealSlot.UNSPECIFIED -> R.string.meal_other
    },
)
