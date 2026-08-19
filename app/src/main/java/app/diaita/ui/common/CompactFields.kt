package app.diaita.ui.common

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.diaita.R
import app.diaita.ui.theme.LabelGap

/** Search boxes: the standard Material button height, so a field and a button line up. */
private val SearchFieldHeight = 40.dp

/**
 * Form fields carry their label inside the box, which needs a little more room than a search
 * box — a floor rather than a fixed height so the label can never be clipped. Still well under
 * Material's own 56dp.
 */
private val FormFieldMinHeight = 48.dp

/** Trailing buttons are sized down to fit inside [SearchFieldHeight]; the field itself stays tappable. */
private val TrailingButtonSize = 32.dp
private val IconSize = 18.dp

/**
 * The app's text field, used by [CompactSearchField] and [CompactTextField] alike so search
 * boxes and form inputs are the same object with different trimmings. Material's own
 * `OutlinedTextField` is 56dp tall with padding sized for a floating label — next to a 40dp
 * button, and stacked ten deep in a form, it reads as a wall of boxes. This wraps
 * [BasicTextField] in the same outlined decoration with most of that padding taken out.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DecoratedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String?,
    placeholder: String?,
    modifier: Modifier,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    isError: Boolean,
    leadingIcon: (@Composable () -> Unit)?,
    trailingIcon: (@Composable () -> Unit)?,
    focusRequester: FocusRequester?,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colors = OutlinedTextFieldDefaults.colors()

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier =
            modifier
                // A labeled field only gets a floor: the floating label decides the rest, and a
                // fixed height would crop it. A search box has no label, so it can be exact.
                .then(
                    if (label != null) {
                        Modifier.heightIn(min = FormFieldMinHeight)
                    } else {
                        Modifier.height(SearchFieldHeight)
                    },
                ).let { if (focusRequester != null) it.focusRequester(focusRequester) else it },
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
    ) { innerTextField ->
        OutlinedTextFieldDefaults.DecorationBox(
            value = value,
            innerTextField = innerTextField,
            enabled = true,
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource,
            isError = isError,
            label =
                label?.let {
                    { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                },
            placeholder =
                placeholder?.let {
                    { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            colors = colors,
            // Trimmed padding is what makes the box compact; with a label the decoration adds
            // back only what the label itself needs.
            contentPadding =
                if (label != null) {
                    PaddingValues(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
                } else {
                    PaddingValues(start = 8.dp, end = 8.dp)
                },
            container = {
                OutlinedTextFieldDefaults.Container(
                    enabled = true,
                    isError = isError,
                    interactionSource = interactionSource,
                    colors = colors,
                    shape = MaterialTheme.shapes.small,
                )
            },
        )
    }
}

/**
 * The one search/filter field in the app — the presets list, the food search, anywhere else a
 * query narrows a list — so they all look and measure the same. A non-empty query always offers
 * the clear (x); [trailing] (a progress spinner, say) takes the slot over while it is showing.
 */
@Composable
fun CompactSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailing: (@Composable () -> Unit)? = null,
) {
    DecoratedField(
        value = value,
        onValueChange = onValueChange,
        label = null,
        placeholder = placeholder,
        modifier = modifier,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        isError = false,
        focusRequester = null,
        leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(IconSize))
        },
        trailingIcon =
            when {
                trailing != null -> trailing
                value.isNotEmpty() -> {
                    {
                        IconButton(
                            onClick = { onValueChange("") },
                            modifier = Modifier.size(TrailingButtonSize),
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.clear_search),
                                modifier = Modifier.size(IconSize),
                            )
                        }
                    }
                }
                else -> null
            },
    )
}

/**
 * The form input: the same box as [CompactSearchField], a touch taller so the [label] floats
 * inside it the way Material does it — outside the box the labels read as loose text between
 * the fields, and a placeholder would vanish the moment the field is filled (in a form of ten
 * numbers, "12.5" without its name is useless).
 */
@Composable
fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isError: Boolean = false,
    /** Helper or error line under the box, in place of Material's supporting-text slot. */
    supportingText: String? = null,
    /** Applied to the input itself — the [modifier] lands on the box + helper-line column. */
    focusRequester: FocusRequester? = null,
) {
    Column(modifier = modifier) {
        DecoratedField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            placeholder = null,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            isError = isError,
            leadingIcon = null,
            trailingIcon = null,
            focusRequester = focusRequester,
        )
        supportingText?.let { supporting ->
            Spacer(modifier = Modifier.height(LabelGap))
            Text(
                supporting,
                style = MaterialTheme.typography.bodySmall,
                color =
                    if (isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
    }
}
