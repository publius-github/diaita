package app.diaita.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import app.diaita.data.db.entities.MealSlot

/** Two rows of chips for picking a meal slot. Selection is sticky — re-tapping keeps it. */
@Composable
fun MealSlotPicker(
    selected: MealSlot,
    onSelected: (MealSlot) -> Unit,
) {
    val slots = listOf(MealSlot.BREAKFAST, MealSlot.LUNCH, MealSlot.DINNER, MealSlot.SNACK)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        slots.chunked(2).forEach { rowSlots ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowSlots.forEach { slot ->
                    FilterChip(
                        selected = selected == slot,
                        onClick = { onSelected(slot) },
                        label = { Text(slot.label()) },
                    )
                }
            }
        }
    }
}
