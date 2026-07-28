package com.pamoron.electroperico.ui.comparator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pamoron.electroperico.R
import com.pamoron.electroperico.domain.model.ChargerOption
import com.pamoron.electroperico.domain.model.ComparedOption
import com.pamoron.electroperico.domain.model.Comparison
import com.pamoron.electroperico.domain.model.ComparisonSort
import com.pamoron.electroperico.ui.calculator.components.DetailRow
import com.pamoron.electroperico.ui.common.AppFooter
import com.pamoron.electroperico.ui.common.ChargerFormDialog
import com.pamoron.electroperico.ui.common.RatingTone
import com.pamoron.electroperico.ui.common.colors
import com.pamoron.electroperico.ui.common.labelRes
import com.pamoron.electroperico.ui.format.Formatters

/**
 * Comparador de cargadores.
 *
 * Guarda hasta cinco opciones y las calcula todas con el mismo perfil de
 * vehículo, para poder decidir con las tres cifras que importan: cuánto cuesta,
 * cuánto tarda y cuánto sale el kWh de verdad.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparatorScreen(
    onBack: () -> Unit,
    viewModel: ComparatorViewModel = viewModel(factory = ComparatorViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.titulo_comparador)) },
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
            Text(
                text = stringResource(R.string.comparador_intro, state.vehicleName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (state.isEmpty) {
                EmptyState()
            } else {
                SortSelector(selected = state.sort, onSelect = viewModel::onSortChange)
            }

            val comparison = state.comparison
            if (comparison != null) {
                comparison.options.forEach { compared ->
                    OptionCard(
                        compared = compared,
                        comparison = comparison,
                        onEdit = { viewModel.onEditClick(compared.option) },
                        onDuplicate = { viewModel.onDuplicate(compared.option) },
                        onDelete = { viewModel.onDelete(compared.option.id) },
                        canDuplicate = state.canAdd,
                    )
                }
                comparison.invalidOptions.forEach { invalid ->
                    InvalidOptionCard(
                        name = invalid.option.name,
                        onEdit = { viewModel.onEditClick(invalid.option) },
                        onDelete = { viewModel.onDelete(invalid.option.id) },
                    )
                }
            }

            TextButton(
                onClick = viewModel::onAddClick,
                enabled = state.canAdd,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (state.canAdd) {
                        stringResource(R.string.accion_anadir_opcion)
                    } else {
                        stringResource(R.string.comparador_limite, ChargerOption.MAX_OPTIONS)
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            if (comparison != null && comparison.isComparable) {
                BalanceExplanation()
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
            titleRes = if (editing.isEditing) {
                R.string.titulo_editar_opcion
            } else {
                R.string.titulo_nueva_opcion
            },
            onChange = viewModel::onFormChange,
            onStartSocChange = viewModel::onStartSocChange,
            onTargetSocChange = viewModel::onTargetSocChange,
            onSave = viewModel::onSaveForm,
            onDismiss = viewModel::onCancelForm,
        )
    }
}
/** Mensaje cuando todavía no hay nada guardado. */
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            text = stringResource(R.string.comparador_vacio),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(20.dp),
        )
    }
}

/** Selector del criterio de ordenación. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SortSelector(
    selected: ComparisonSort,
    onSelect: (ComparisonSort) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ComparisonSort.entries.forEach { sort ->
            FilterChip(
                selected = sort == selected,
                onClick = { onSelect(sort) },
                label = { Text(stringResource(sort.labelRes)) },
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
    }
}
/** Tarjeta de una opción calculada, con sus distintivos. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionCard(
    compared: ComparedOption,
    comparison: Comparison,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    canDuplicate: Boolean,
    modifier: Modifier = Modifier,
) {
    val id = compared.option.id
    val result = compared.result
    val isBest = id == comparison.bestOverallId && comparison.isComparable

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isBest) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (isBest) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = compared.option.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.accion_editar),
                    )
                }
                IconButton(onClick = onDuplicate, enabled = canDuplicate) {
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

            if (comparison.isComparable) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (id == comparison.cheapestId) {
                        Badge(
                            textRes = R.string.distintivo_mejor_precio,
                            icon = Icons.Filled.Savings,
                            tone = RatingTone.BUENO,
                        )
                    }
                    if (id == comparison.fastestId) {
                        Badge(
                            textRes = R.string.distintivo_mas_rapido,
                            icon = Icons.Filled.Speed,
                            tone = RatingTone.NEUTRO,
                        )
                    }
                    if (isBest) {
                        Badge(
                            textRes = R.string.distintivo_mejor_opcion,
                            icon = Icons.Filled.EmojiEvents,
                            tone = RatingTone.BUENO,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            Text(
                text = stringResource(
                    R.string.comparador_resumen_entrada,
                    Formatters.pricePerKWh(result.announcedPricePerKWh),
                    Formatters.power(compared.option.chargerPowerKw),
                    stringResource(compared.option.currentType.labelRes),
                    compared.option.startSocPercent,
                    compared.option.targetSocPercent,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))

            DetailRow(
                label = stringResource(R.string.detalle_coste_total),
                value = Formatters.money(result.totalCostEur),
                emphasized = true,
            )
            DetailRow(
                label = stringResource(R.string.resultado_tiempo_estimado),
                value = Formatters.duration(result.minutes),
                emphasized = true,
            )
            DetailRow(
                label = stringResource(R.string.detalle_precio_efectivo),
                value = Formatters.pricePerKWh(result.effectivePricePerKWh),
            )
            DetailRow(
                label = stringResource(R.string.detalle_coste_por_100km),
                value = Formatters.money(result.costPer100KmEur),
            )
            if (comparison.isComparable) {
                DetailRow(
                    label = stringResource(R.string.comparador_puntuacion),
                    value = stringResource(
                        R.string.comparador_puntuacion_valor,
                        (compared.balanceScore * 100).toInt(),
                    ),
                )
            }
        }
    }
}

/** Tarjeta de una opción cuyos datos no permiten calcular. */
@Composable
private fun InvalidOptionCard(
    name: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.comparador_opcion_invalida),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.accion_editar))
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.accion_eliminar),
                )
            }
        }
    }
}

/** Distintivo con icono y texto; el color nunca va solo. */
@Composable
private fun Badge(
    textRes: Int,
    icon: ImageVector,
    tone: RatingTone,
    modifier: Modifier = Modifier,
) {
    val toneColors = tone.colors()
    val label = stringResource(textRes)
    Surface(
        modifier = modifier.semantics { contentDescription = label },
        shape = RoundedCornerShape(10.dp),
        color = toneColors.container,
        contentColor = toneColors.content,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Explicación de la fórmula de equilibrio, para que la puntuación no sea magia. */
@Composable
private fun BalanceExplanation(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.comparador_como_se_puntua),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.comparador_formula),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

