package app.diaita.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Track (unfilled) color shared by every progress bar/ring on the Diary card. The Material
 * default track (`surfaceVariant`) is nearly invisible on the light-theme card, so we use a
 * translucent tint of the fill color instead — visible on both themes and cohesive with the
 * filled portion.
 */
@Composable
fun progressTrackColor(): Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
