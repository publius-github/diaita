package app.diaita.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.diaita.R

@Composable
fun MacroProgressRing(
    consumedCalories: Double,
    goalCalories: Int?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(120.dp), contentAlignment = Alignment.Center) {
        if (goalCalories != null && goalCalories > 0) {
            val isOverGoal = consumedCalories > goalCalories
            val progress = (consumedCalories / goalCalories).toFloat().coerceIn(0f, 1f)
            val ringColor = if (isOverGoal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 10.dp,
                color = ringColor,
            )
            Text(
                stringResource(R.string.ring_progress, consumedCalories.toInt(), goalCalories),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = if (isOverGoal) MaterialTheme.colorScheme.error else Color.Unspecified,
            )
        } else {
            Text(
                stringResource(R.string.ring_no_goal, consumedCalories.toInt()),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
