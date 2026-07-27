package com.pamoron.cargaev.ui.calculator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pamoron.cargaev.R
import com.pamoron.cargaev.domain.model.ChargeResult
import com.pamoron.cargaev.domain.model.ChargeWarning
import com.pamoron.cargaev.ui.calculator.components.ContextualWarning
import com.pamoron.cargaev.ui.calculator.components.CurrentTypeSelector
import com.pamoron.cargaev.ui.calculator.components.DetailCard
import com.pamoron.cargaev.ui.calculator.components.DetailDivider
import com.pamoron.cargaev.ui.calculator.components.DetailRow
import com.pamoron.cargaev.ui.calculator.components.GeneralWarningsCard
import com.pamoron.cargaev.ui.calculator.components.HeadlineRow
import com.pamoron.cargaev.ui.calculator.components.NumberField
import com.pamoron.cargaev.ui.calculator.components.RatingBanner
import com.pamoron.cargaev.ui.calculator.components.SegmentsCard
import com.pamoron.cargaev.ui.calculator.components.SocSection
import com.pamoron.cargaev.ui.common.icon
import com.pamoron.cargaev.ui.common.labelRes
import com.pamoron.cargaev.ui.common.messageRes
import com.pamoron.cargaev.ui.common.shortLabelRes
import com.pamoron.cargaev.ui.common.textRes
import com.pamoron.cargaev.ui.common.tone
import com.pamoron.cargaev.ui.format.Formatters

/**
 * Pantalla principal.
 *
 * El orden de los campos es el que se pidió: precio, potencia, tipo de
 * corriente, batería actual, batería objetivo y botón de calcular. Todo lo
 * demás (costes adicionales, desgloses y advertencias) queda por debajo o
 * plegado para no estorbar al uso rápido.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    onOpenSettings: () -> Unit,
    viewModel: CalculatorViewModel = viewModel(factory = CalculatorViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) { viewModel.start() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.accion_ajustes),
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            VehicleHeader(
                vehicleName = state.vehicle.displayName,
                usableCapacity = Formatters.energy(state.vehicle.usableCapacityKWh),
                onClick = onOpenSettings,
            )

            NumberField(
                value = state.inputs.priceText,
                onValueChange = viewModel::onPriceChange,
                labelRes = R.string.campo_precio,
                unit = stringResource(R.string.unidad_euro_kwh),
            )

            NumberField(
                value = state.inputs.powerText,
                onValueChange = viewModel::onPowerChange,
                labelRes = R.string.campo_potencia,
                unit = stringResource(R.string.unidad_kw),
            )

            CurrentTypeSelector(
                selected = state.inputs.currentType,
                onSelect = viewModel::onCurrentTypeChange,
            )

            SocSection(
                startSoc = state.inputs.startSoc,
                targetSoc = state.inputs.targetSoc,
                onStartChange = viewModel::onStartSocChange,
                onTargetChange = viewModel::onTargetSocChange,
                onSwap = viewModel::onSwapSoc,
            )

            ExtraCostsSection(
                inputs = state.inputs,
                onToggle = viewModel::onToggleExtras,
                onStartFeeChange = viewModel::onStartFeeChange,
                onPricePerMinuteChange = viewModel::onPricePerMinuteChange,
                onParkingFeeChange = viewModel::onParkingFeeChange,
            )

            ActionButtons(
                canCalculate = state.canCalculate,
                onCalculate = {
                    focusManager.clearFocus()
                    viewModel.onCalculate()
                },
                onClear = {
                    focusManager.clearFocus()
                    viewModel.onClear()
                },
            )

            ValidationErrors(state = state)

            val result = state.visibleResult
            if (result != null) {
                ResultsSection(
                    result = result,
                    estimationLabel = stringResource(state.estimationMode.shortLabelRes),
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Cabecera con el vehículo activo; toda ella lleva a los ajustes. */
@Composable
private fun VehicleHeader(
    vehicleName: String,
    usableCapacity: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = Icons.Filled.DirectionsCar,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.cabecera_vehiculo, vehicleName, usableCapacity),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Start,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Sección plegable con los costes adicionales, opcionales y a cero por defecto. */
@Composable
private fun ExtraCostsSection(
    inputs: CalculatorInputs,
    onToggle: () -> Unit,
    onStartFeeChange: (String) -> Unit,
    onPricePerMinuteChange: (String) -> Unit,
    onParkingFeeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        TextButton(
            onClick = onToggle,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = if (inputs.extrasCount > 0) {
                    stringResource(R.string.seccion_costes_adicionales_con_datos, inputs.extrasCount)
                } else {
                    stringResource(R.string.seccion_costes_adicionales)
                },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start,
            )
            Icon(
                imageVector = if (inputs.extrasExpanded) {
                    Icons.Filled.ExpandLess
                } else {
                    Icons.Filled.ExpandMore
                },
                contentDescription = null,
            )
        }
        AnimatedVisibility(visible = inputs.extrasExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField(
                    value = inputs.startFeeText,
                    onValueChange = onStartFeeChange,
                    labelRes = R.string.campo_coste_inicio,
                    unit = stringResource(R.string.unidad_euro),
                )
                NumberField(
                    value = inputs.pricePerMinuteText,
                    onValueChange = onPricePerMinuteChange,
                    labelRes = R.string.campo_coste_minuto,
                    unit = stringResource(R.string.unidad_euro_min),
                )
                NumberField(
                    value = inputs.parkingFeeText,
                    onValueChange = onParkingFeeChange,
                    labelRes = R.string.campo_coste_estacionamiento,
                    unit = stringResource(R.string.unidad_euro),
                    isLast = true,
                )
            }
        }
    }
}

/** Botón grande de calcular y botón secundario para borrar. */
@Composable
private fun ActionButtons(
    canCalculate: Boolean,
    onCalculate: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onCalculate,
            enabled = canCalculate,
            modifier = Modifier
                .weight(1f)
                .height(64.dp),
        ) {
            Icon(imageVector = Icons.Filled.Calculate, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.accion_calcular),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        OutlinedButton(
            onClick = onClear,
            modifier = Modifier.height(64.dp),
        ) {
            Text(stringResource(R.string.accion_borrar))
        }
    }
}

/** Mensajes de validación, solo tras haber pulsado "Calcular". */
@Composable
private fun ValidationErrors(state: CalculatorUiState, modifier: Modifier = Modifier) {
    val errors = state.visibleErrors
    if (errors.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        errors.forEach { error ->
            Text(
                text = stringResource(error.messageRes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** Bloque completo de resultados. */
@Composable
private fun ResultsSection(
    result: ChargeResult,
    estimationLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HeadlineRow(
            cost = Formatters.money(result.totalCostEur),
            time = Formatters.duration(result.minutes),
            costCaption = stringResource(
                R.string.resultado_coste_por_100,
                Formatters.money(result.costPer100KmEur),
            ),
            timeCaption = stringResource(R.string.resultado_estimacion, estimationLabel),
        )

        RatingBanner(
            title = stringResource(result.priceRating.labelRes),
            detail = stringResource(
                R.string.detalle_precio_efectivo_y_anunciado,
                Formatters.pricePerKWh(result.effectivePricePerKWh),
                Formatters.pricePerKWh(result.announcedPricePerKWh),
            ),
            icon = result.priceRating.icon,
            tone = result.priceRating.tone,
        )

        RatingBanner(
            title = stringResource(result.chargerRating.labelRes),
            detail = stringResource(
                R.string.detalle_potencia_aprovechada,
                Formatters.power(result.effectivePowerKw),
                Formatters.power(result.chargerPowerKw),
            ),
            icon = result.chargerRating.icon,
            tone = result.chargerRating.tone,
        )

        // Avisos que dependen de los datos: van destacados y arriba del detalle.
        result.warnings.filter { it.isContextual }.forEach { warning ->
            val text = if (warning == ChargeWarning.CARGADOR_SOBREDIMENSIONADO) {
                stringResource(
                    warning.textRes,
                    Formatters.power(result.chargerPowerKw),
                    Formatters.power(result.vehicleMaxPowerKw),
                )
            } else {
                stringResource(warning.textRes)
            }
            ContextualWarning(text = text)
        }

        DetailCard(title = stringResource(R.string.titulo_energia_y_autonomia)) {
            DetailRow(
                label = stringResource(R.string.detalle_energia_bateria),
                value = Formatters.energy(result.energyNeededKWh),
                emphasized = true,
            )
            DetailRow(
                label = stringResource(R.string.detalle_energia_facturada),
                value = Formatters.energy(result.energyBilledKWh),
                emphasized = true,
            )
            DetailRow(
                label = stringResource(R.string.detalle_perdidas),
                value = Formatters.percent(result.lossFraction * 100.0),
            )
            DetailDivider()
            DetailRow(
                label = stringResource(R.string.detalle_bateria_anadida),
                value = Formatters.percent(result.socAddedPercent),
            )
            DetailRow(
                label = stringResource(R.string.detalle_autonomia_anadida),
                value = Formatters.distance(result.rangeAddedKm),
                emphasized = true,
            )
            Text(
                text = stringResource(R.string.nota_autonomia_aproximada),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        DetailCard(title = stringResource(R.string.titulo_desglose_coste)) {
            DetailRow(
                label = stringResource(R.string.detalle_coste_energia),
                value = Formatters.money(result.energyCostEur),
            )
            DetailRow(
                label = stringResource(R.string.detalle_coste_inicio),
                value = Formatters.money(result.startFeeEur),
            )
            DetailRow(
                label = stringResource(R.string.detalle_coste_tiempo),
                value = Formatters.money(result.timeCostEur),
            )
            DetailRow(
                label = stringResource(R.string.detalle_coste_estacionamiento),
                value = Formatters.money(result.parkingFeeEur),
            )
            DetailDivider()
            DetailRow(
                label = stringResource(R.string.detalle_coste_total),
                value = Formatters.money(result.totalCostEur),
                emphasized = true,
            )
            DetailRow(
                label = stringResource(R.string.detalle_coste_por_minuto),
                value = Formatters.money(result.costPerMinuteEur),
            )
            DetailRow(
                label = stringResource(R.string.detalle_coste_por_100km),
                value = Formatters.money(result.costPer100KmEur),
            )
            DetailDivider()
            DetailRow(
                label = stringResource(R.string.detalle_precio_anunciado),
                value = Formatters.pricePerKWh(result.announcedPricePerKWh),
            )
            DetailRow(
                label = stringResource(R.string.detalle_precio_efectivo),
                value = Formatters.pricePerKWh(result.effectivePricePerKWh),
                emphasized = true,
            )
            if (result.hasExtraCosts) {
                DetailRow(
                    label = stringResource(R.string.detalle_sobrecoste),
                    value = Formatters.signedPricePerKWh(result.pricePremiumPerKWh),
                )
            }
        }

        SegmentsCard(result = result)

        GeneralWarningsCard(warnings = result.warnings.filterNot { it.isContextual })

        Text(
            text = stringResource(R.string.nota_valores_estimados),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
