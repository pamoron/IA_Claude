package com.pamoron.electroperico.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pamoron.electroperico.R
import com.pamoron.electroperico.ui.calculator.components.CurrentTypeSelector
import com.pamoron.electroperico.ui.calculator.components.NumberField
import com.pamoron.electroperico.ui.calculator.components.SocSection
import com.pamoron.electroperico.ui.comparator.OptionForm

/**
 * Formulario de un cargador, en forma de diálogo.
 *
 * Lo comparten el comparador y el historial: los campos son exactamente los
 * mismos, así que tener un único formulario evita que se desincronicen.
 */
@Composable
fun ChargerFormDialog(
    form: OptionForm,
    @StringRes titleRes: Int,
    onChange: ((OptionForm) -> OptionForm) -> Unit,
    onStartSocChange: (Int) -> Unit,
    onTargetSocChange: (Int) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                NumberField(
                    value = form.name,
                    onValueChange = { value -> onChange { it.copy(name = value) } },
                    labelRes = R.string.campo_nombre_operador,
                    unit = "",
                    numeric = false,
                )
                NumberField(
                    value = form.priceText,
                    onValueChange = { value -> onChange { it.copy(priceText = value) } },
                    labelRes = R.string.campo_precio,
                    unit = stringResource(R.string.unidad_euro_kwh),
                )
                NumberField(
                    value = form.powerText,
                    onValueChange = { value -> onChange { it.copy(powerText = value) } },
                    labelRes = R.string.campo_potencia,
                    unit = stringResource(R.string.unidad_kw),
                )
                CurrentTypeSelector(
                    selected = form.currentType,
                    onSelect = { type -> onChange { it.copy(currentType = type) } },
                )
                SocSection(
                    startSoc = form.startSoc,
                    targetSoc = form.targetSoc,
                    onStartChange = onStartSocChange,
                    onTargetChange = onTargetSocChange,
                    onSwap = {
                        onChange { it.copy(startSoc = it.targetSoc, targetSoc = it.startSoc) }
                    },
                )
                NumberField(
                    value = form.startFeeText,
                    onValueChange = { value -> onChange { it.copy(startFeeText = value) } },
                    labelRes = R.string.campo_coste_inicio,
                    unit = stringResource(R.string.unidad_euro),
                )
                NumberField(
                    value = form.pricePerMinuteText,
                    onValueChange = { value -> onChange { it.copy(pricePerMinuteText = value) } },
                    labelRes = R.string.campo_coste_minuto,
                    unit = stringResource(R.string.unidad_euro_min),
                )
                NumberField(
                    value = form.parkingFeeText,
                    onValueChange = { value -> onChange { it.copy(parkingFeeText = value) } },
                    labelRes = R.string.campo_coste_estacionamiento,
                    unit = stringResource(R.string.unidad_euro),
                    isLast = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = form.toOption() != null) {
                Text(stringResource(R.string.accion_guardar))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.accion_cancelar))
            }
        },
    )
}
