package com.pamoron.electroperico.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pamoron.electroperico.R
import com.pamoron.electroperico.domain.calc.Money
import com.pamoron.electroperico.domain.model.HistoryEntry
import com.pamoron.electroperico.domain.model.HistorySort
import com.pamoron.electroperico.ui.calculator.components.DetailRow
import com.pamoron.electroperico.ui.common.AppFooter
import com.pamoron.electroperico.ui.common.ChargerFormDialog
import com.pamoron.electroperico.ui.common.labelRes
import com.pamoron.electroperico.ui.format.Formatters

/**
 * Historial local de recargas.
 *
 * Es opcional y no sale del dispositivo. Guarda lo que costó de verdad cada
 * recarga, de modo que sirve de referencia real frente a la estimación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.titulo_historial)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.accion_volver),
                        )
                    }
                },
                actions = {
                    if (!state.isEmpty) {
                        TextButton(onClick = viewModel::onClearAll) {
                            Text(stringResource(R.string.accion_vaciar))
                        }
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
            if (state.isEmpty) {
                EmptyState()
            } else {
                SortSelector(selected = state.sort, onSelect = viewModel::onSortChange)
                state.entries.forEach { entry ->
                    EntryCard(
                        entry = entry,
                        onEdit = { viewModel.onEdit(entry) },
                        onDuplicate = { viewModel.onDuplicate(entry) },
                        onDelete = { viewModel.onDelete(entry.id) },
                        onToggleFavorite = { viewModel.onToggleFavorite(entry.id) },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            AppFooter()
            Spacer(Modifier.height(16.dp))
        }
    }

    val editing = state.editing
    if (editing != null) {
        ChargerFormDialog(
            form = editing,
            titleRes = R.string.titulo_editar_recarga,
            onChange = viewModel::onFormChange,
            onStartSocChange = viewModel::onStartSocChange,
            onTargetSocChange = viewModel::onTargetSocChange,
            onSave = viewModel::onSaveForm,
            onDismiss = viewModel::onCancelForm,
        )
    }
}

/** Mensaje cuando el historial está vacío. */
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            text = stringResource(R.string.historial_vacio),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(20.dp),
        )
    }
}

/** Selector del criterio de ordenación. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SortSelector(
    selected: HistorySort,
    onSelect: (HistorySort) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HistorySort.entries.forEach { sort ->
            FilterChip(
                selected = sort == selected,
                onClick = { onSelect(sort) },
                label = { Text(stringResource(sort.labelRes)) },
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
    }
}

/** Tarjeta de una recarga guardada. */
@Composable
private fun EntryCard(
    entry: HistoryEntry,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.operatorName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = Formatters.dateTime(entry.timestampMillis),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (entry.favorite) {
                            Icons.Filled.Star
                        } else {
                            Icons.Filled.StarBorder
                        },
                        contentDescription = stringResource(
                            if (entry.favorite) {
                                R.string.accion_quitar_favorito
                            } else {
                                R.string.accion_marcar_favorito
                            },
                        ),
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.accion_editar),
                    )
                }
                IconButton(onClick = onDuplicate) {
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = stringResource(R.string.accion_duplicar),
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.accion_eliminar),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.comparador_resumen_entrada,
                    Formatters.pricePerKWh(Money.rate(entry.charger.pricePerKWh)),
                    Formatters.power(entry.charger.chargerPowerKw),
                    stringResource(entry.charger.currentType.labelRes),
                    entry.charger.startSocPercent,
                    entry.charger.targetSocPercent,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))

            DetailRow(
                label = stringResource(R.string.detalle_coste_total),
                value = Formatters.money(entry.totalCostEur),
                emphasized = true,
            )
            DetailRow(
                label = stringResource(R.string.resultado_tiempo_estimado),
                value = Formatters.duration(entry.minutes),
            )
            DetailRow(
                label = stringResource(R.string.detalle_precio_efectivo),
                value = Formatters.pricePerKWh(entry.effectivePricePerKWh),
            )
            DetailRow(
                label = stringResource(R.string.detalle_energia_facturada),
                value = Formatters.energy(entry.energyBilledKWh),
            )
        }
    }
}
