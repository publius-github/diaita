package app.diaita.ui.theme

import androidx.compose.ui.unit.dp

/**
 * The app's vertical rhythm, in one place. Every screen is built from the same four steps, so
 * the same kind of gap measures the same everywhere and a change of mind lands app-wide:
 *
 * - [ScreenPadding] — the inset between screen content and the window edges, and the padding
 *   inside a Card.
 * - [SectionGap] — between two blocks that are about different things (a card and the buttons
 *   under it, a form section and the next one).
 * - [ItemGap] — between related controls, and between rows of a list.
 * - [LabelGap] — between a label and the control it names.
 */
val ScreenPadding = 16.dp
val SectionGap = 16.dp
val ItemGap = 8.dp
val LabelGap = 4.dp
