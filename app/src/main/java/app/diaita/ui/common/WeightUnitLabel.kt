package app.diaita.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.diaita.R
import app.diaita.data.settings.WeightUnit

@Composable
fun WeightUnit.label(): String =
    stringResource(
        when (this) {
            WeightUnit.KG -> R.string.unit_kg
            WeightUnit.LB -> R.string.unit_lb
        },
    )
