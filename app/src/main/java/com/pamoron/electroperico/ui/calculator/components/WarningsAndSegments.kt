package com.pamoron.electroperico.ui.calculator.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pamoron.electroperico.R
import com.pamoron.electroperico.domain.model.ChargeResult
import com.pamoron.electroperico.domain.model.ChargeSegment
import com.pamoron.electroperico.domain.model.ChargeWarning
import com.pamoron.electroperico.ui.common.textRes
import com.pamoron.electroperico.ui.format.Formatters
import kotlin.math.roundToInt

/**
 * Aviso destacado que depende de los datos introducidos.
 *
 * Se muestra arriba, antes del detalle, porque cambia la lectura del resultado.
 */
@Composable
fun ContextualWarning(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = Icons.Filled.WarningAmber,
                contentDescription = stringResource(R.string.etiqueta_aviso),
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(text = text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/**
 * Lista plegable con las advertencias generales.
 *
 * Aplican siempre, así que van recogidas para no competir con los resultados,
 * pero a un solo toque de distancia.
 */
@Composable
fun GeneralWarningsCard(
    warnings: List<ChargeWarning>,
    modifier: Modifier = Modifier,
) {
    if (warnings.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val title = stringResource(R.string.titulo_ten_en_cuenta)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .semantics {
                        contentDescription = if (expanded) {
                            "$title, plegar"
                        } else {
                            "$title, desplegar"
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    warnings.forEach { warning ->
                        Row(modifier = Modifier.padding(vertical = 6.dp)) {
                            Text(text = "•", style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = stringResource(warning.textRes),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Desglose del tiempo por tramos de estado de carga.
 *
 * Explica por qué el tiempo no es simplemente la energía dividida entre la
 * potencia máxima, que es la principal fuente de sorpresas al llegar al poste.
 */
@Composable
fun SegmentsCard(
    result: ChargeResult,
    modifier: Modifier = Modifier,
) {
    if (result.segments.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val title = stringResource(R.string.titulo_desglose_tiempo)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .semantics {
                        contentDescription = if (expanded) {
                            "$title, plegar"
                        } else {
                            "$title, desplegar"
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    result.segments.forEach { segment -> SegmentRow(segment) }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.nota_tiempo_orientativo),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

/** Una fila del desglose: intervalo de batería, potencia media y minutos. */
@Composable
private fun SegmentRow(segment: ChargeSegment, modifier: Modifier = Modifier) {
    val range = stringResource(
        R.string.formato_tramo,
        segment.fromPercent.roundToInt(),
        segment.toPercent.roundToInt(),
    )
    val power = Formatters.power(segment.averagePowerKw)
    val minutes = Formatters.duration(segment.minutes.roundToInt().coerceAtLeast(1))

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$range, $power de media, $minutes"
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = range, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = power,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Text(
            text = minutes,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
