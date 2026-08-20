package app.diaita.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import app.diaita.data.db.entities.MealSlot
import app.diaita.ui.theme.ItemGap
import app.diaita.ui.theme.LabelGap

/**
 * Chips for picking a meal slot. Selection is sticky — re-tapping keeps it. One row where the
 * four labels fit and a wrapped second row where they don't; they used to be split two-and-two
 * whatever the screen width was.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MealSlotPicker(
    selected: MealSlot,
    onSelected: (MealSlot) -> Unit,
) {
    val slots = listOf(MealSlot.BREAKFAST, MealSlot.LUNCH, MealSlot.DINNER, MealSlot.SNACK)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(ItemGap),
        verticalArrangement = Arrangement.spacedBy(LabelGap),
    ) {
        slots.forEach { slot ->
            FilterChip(
                selected = selected == slot,
                onClick = { onSelected(slot) },
                label = { Text(slot.label()) },
            )
        }
    }
}
