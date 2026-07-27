package com.pamoron.electroperico.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pamoron.electroperico.R
import com.pamoron.electroperico.domain.model.EstimationMode
import com.pamoron.electroperico.ui.common.AppFooter
import com.pamoron.electroperico.ui.common.labelRes

/**
 * Pantalla de configuración.
 *
 * Permite editar por completo el perfil del vehículo, el grado de prudencia de
 * la estimación de tiempo y los umbrales de valoración del precio.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.titulo_ajustes)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.accion_volver),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle(R.string.titulo_perfil_vehiculo)

            val form = state.vehicleForm

            SettingsTextField(
                value = form.brand,
                onValueChange = { value -> viewModel.onVehicleFieldChange { it.copy(brand = value) } },
                labelRes = R.string.campo_marca,
                numeric = false,
            )
            SettingsTextField(
                value = form.model,
                onValueChange = { value -> viewModel.onVehicleFieldChange { it.copy(model = value) } },
                labelRes = R.string.campo_modelo,
                numeric = false,
            )
            SettingsTextField(
                value = form.grossCapacity,
                onValueChange = { value ->
                    viewModel.onVehicleFieldChange { it.copy(grossCapacity = value) }
                },
                labelRes = R.string.campo_capacidad_bruta,
                unit = stringResource(R.string.unidad_kwh),
                supportingRes = R.string.ayuda_capacidad_bruta,
            )
            SettingsTextField(
                value = form.usableCapacity,
                onValueChange = { value ->
                    viewModel.onVehicleFieldChange { it.copy(usableCapacity = value) }
                },
                labelRes = R.string.campo_capacidad_util,
                unit = stringResource(R.string.unidad_kwh),
                supportingRes = R.string.ayuda_capacidad_util,
            )
            SettingsTextField(
                value = form.maxDcPower,
                onValueChange = { value ->
                    viewModel.onVehicleFieldChange { it.copy(maxDcPower = value) }
                },
                labelRes = R.string.campo_potencia_maxima_dc,
                unit = stringResource(R.string.unidad_kw),
            )
            SettingsTextField(
                value = form.maxAcPower,
                onValueChange = { value ->
                    viewModel.onVehicleFieldChange { it.copy(maxAcPower = value) }
                },
                labelRes = R.string.campo_potencia_maxima_ac,
                unit = stringResource(R.string.unidad_kw),
            )
            SettingsTextField(
                value = form.consumption,
                onValueChange = { value ->
                    viewModel.onVehicleFieldChange { it.copy(consumption = value) }
                },
                labelRes = R.string.campo_consumo,
                unit = stringResource(R.string.unidad_kwh_100km),
            )
            SettingsTextField(
                value = form.acLoss,
                onValueChange = { value -> viewModel.onVehicleFieldChange { it.copy(acLoss = value) } },
                labelRes = R.string.campo_perdidas_ac,
                unit = stringResource(R.string.unidad_porcentaje),
            )
            SettingsTextField(
                value = form.dcLoss,
                onValueChange = { value -> viewModel.onVehicleFieldChange { it.copy(dcLoss = value) } },
                labelRes = R.string.campo_perdidas_dc,
                unit = stringResource(R.string.unidad_porcentaje),
                supportingRes = R.string.ayuda_perdidas,
            )

            Spacer(Modifier.height(4.dp))
            SectionTitle(R.string.titulo_estimacion_tiempo)
            Text(
                text = stringResource(R.string.ayuda_estimacion_tiempo),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            EstimationModeSelector(
                selected = state.estimationMode,
                onSelect = viewModel::onEstimationModeChange,
            )

            Spacer(Modifier.height(4.dp))
            SectionTitle(R.string.titulo_umbrales_precio)
            Text(
                text = stringResource(R.string.ayuda_umbrales_precio),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val thresholds = state.thresholdsForm
            SettingsTextField(
                value = thresholds.veryCheapMax,
                onValueChange = { value ->
                    viewModel.onThresholdFieldChange { it.copy(veryCheapMax = value) }
                },
                labelRes = R.string.campo_umbral_muy_barato,
                unit = stringResource(R.string.unidad_euro_kwh),
            )
            SettingsTextField(
                value = thresholds.goodMax,
                onValueChange = { value ->
                    viewModel.onThresholdFieldChange { it.copy(goodMax = value) }
                },
                labelRes = R.string.campo_umbral_buen_precio,
                unit = stringResource(R.string.unidad_euro_kwh),
            )
            SettingsTextField(
                value = thresholds.normalMax,
                onValueChange = { value ->
                    viewModel.onThresholdFieldChange { it.copy(normalMax = value) }
                },
                labelRes = R.string.campo_umbral_normal,
                unit = stringResource(R.string.unidad_euro_kwh),
            )
            SettingsTextField(
                value = thresholds.expensiveMax,
                onValueChange = { value ->
                    viewModel.onThresholdFieldChange { it.copy(expensiveMax = value) }
                },
                labelRes = R.string.campo_umbral_caro,
                unit = stringResource(R.string.unidad_euro_kwh),
            )

            if (!state.canSaveThresholds && state.loaded) {
                Text(
                    text = stringResource(R.string.error_umbrales_crecientes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (!state.canSaveVehicle && state.loaded) {
                Text(
                    text = stringResource(R.string.error_perfil_incompleto),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (state.savedMessage) {
                Text(
                    text = stringResource(R.string.mensaje_guardado),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = viewModel::onSave,
                    enabled = state.canSaveVehicle || state.canSaveThresholds,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                ) {
                    Text(stringResource(R.string.accion_guardar))
                }
                OutlinedButton(
                    onClick = viewModel::onRestoreDefaults,
                    modifier = Modifier.height(56.dp),
                ) {
                    Text(stringResource(R.string.accion_restaurar))
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.nota_fases_siguientes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            AppFooter()

            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Título de una sección de ajustes. */
@Composable
private fun SectionTitle(@StringRes titleRes: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 8.dp),
    )
}

/** Campo de texto de ajustes, numérico salvo que se indique lo contrario. */
@Composable
private fun SettingsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes labelRes: Int,
    modifier: Modifier = Modifier,
    unit: String? = null,
    numeric: Boolean = true,
    @StringRes supportingRes: Int? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
        trailingIcon = unit?.let { text ->
            { Text(text = text, style = MaterialTheme.typography.titleMedium) }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text,
        ),
        supportingText = supportingRes?.let { res -> { Text(stringResource(res)) } },
        modifier = modifier.fillMaxWidth(),
    )
}

/** Selector del grado de prudencia de la estimación de tiempo. */
@Composable
private fun EstimationModeSelector(
    selected: EstimationMode,
    onSelect: (EstimationMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.selectableGroup()) {
        EstimationMode.entries.forEach { mode ->
            val label = stringResource(mode.labelRes)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .selectable(
                        selected = mode == selected,
                        onClick = { onSelect(mode) },
                        role = Role.RadioButton,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = mode == selected, onClick = null)
                Spacer(Modifier.padding(horizontal = 6.dp))
                Text(text = label, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
