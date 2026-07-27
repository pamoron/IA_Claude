package com.pamoron.cargaev.ui.calculator.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pamoron.cargaev.R
import com.pamoron.cargaev.domain.model.CurrentType
import com.pamoron.cargaev.ui.common.labelRes
import kotlin.math.roundToInt

/**
 * Campo numérico con teclado decimal.
 *
 * El teclado se abre directamente en modo decimal para poder escribir "0,45"
 * sin cambiar de teclado, que es lo que se quiere estando de pie junto al poste.
 */
@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes labelRes: Int,
    unit: String,
    modifier: Modifier = Modifier,
    isLast: Boolean = false,
    @StringRes supportingRes: Int? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
        trailingIcon = {
            Text(text = unit, style = MaterialTheme.typography.titleMedium)
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.headlineSmall,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = if (isLast) ImeAction.Done else ImeAction.Next,
        ),
        supportingText = supportingRes?.let { res -> { Text(stringResource(res)) } },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
    )
}

/** Selector de corriente alterna o continua. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentTypeSelector(
    selected: CurrentType,
    onSelect: (CurrentType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = CurrentType.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, type ->
            SegmentedButton(
                selected = type == selected,
                onClick = { onSelect(type) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                modifier = Modifier.height(56.dp),
            ) {
                Text(
                    text = stringResource(type.labelRes),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

/**
 * Selector de batería actual y objetivo.
 *
 * Combina deslizadores para ajustar con el pulgar, atajos con los objetivos
 * habituales y un botón para intercambiar ambos valores.
 */
@Composable
fun SocSection(
    startSoc: Int,
    targetSoc: Int,
    onStartChange: (Int) -> Unit,
    onTargetChange: (Int) -> Unit,
    onSwap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SocSlider(
            labelRes = R.string.campo_bateria_actual,
            value = startSoc,
            onValueChange = onStartChange,
            valueRange = 0f..99f,
        )

        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            SocSlider(
                labelRes = R.string.campo_bateria_objetivo,
                value = targetSoc,
                onValueChange = onTargetChange,
                valueRange = 1f..100f,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            FilledTonalIconButton(
                onClick = onSwap,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.SwapVert,
                    contentDescription = stringResource(R.string.accion_intercambiar),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        QuickTargetChips(targetSoc = targetSoc, onTargetChange = onTargetChange)
    }
}

/** Deslizador de porcentaje con su etiqueta y su valor. */
@Composable
private fun SocSlider(
    @StringRes labelRes: Int,
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(labelRes)
    val percent = stringResource(R.string.formato_porcentaje, value)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, style = MaterialTheme.typography.titleMedium)
            Text(
                text = percent,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = valueRange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .semantics { contentDescription = "$label: $percent" },
        )
    }
}

/** Atajos con los objetivos de carga más habituales. */
@Composable
private fun QuickTargetChips(
    targetSoc: Int,
    onTargetChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QUICK_TARGETS.forEach { target ->
            FilterChip(
                selected = targetSoc == target,
                onClick = { onTargetChange(target) },
                label = {
                    Text(
                        text = stringResource(R.string.formato_porcentaje, target),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
            )
        }
    }
}

/** Objetivos de carga ofrecidos como atajo. */
private val QUICK_TARGETS = listOf(50, 80, 90, 100)
