package com.pamoron.cargaev.ui.calculator.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EuroSymbol
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pamoron.cargaev.R
import com.pamoron.cargaev.ui.common.RatingTone
import com.pamoron.cargaev.ui.common.colors

/**
 * Tarjeta destacada con una cifra grande.
 *
 * Es el bloque principal de la pantalla de resultados: se lee de un vistazo,
 * con el móvil en la mano y junto al poste.
 */
@Composable
fun HeadlineCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    caption: String? = null,
    valueStyle: TextStyle = MaterialTheme.typography.headlineMedium,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 18.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = listOfNotNull(title, value, caption).joinToString(". ")
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = valueStyle,
                textAlign = TextAlign.Center,
                modifier = Modifier.clearAndSetSemantics { },
            )
            if (caption != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clearAndSetSemantics { },
                )
            }
        }
    }
}

/** Par de tarjetas grandes con el coste y el tiempo estimados. */
@Composable
fun HeadlineRow(
    cost: String,
    time: String,
    costCaption: String,
    timeCaption: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HeadlineCard(
            title = stringResource(R.string.resultado_coste_estimado),
            value = cost,
            icon = Icons.Filled.EuroSymbol,
            caption = costCaption,
            valueStyle = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.weight(1f),
        )
        HeadlineCard(
            title = stringResource(R.string.resultado_tiempo_estimado),
            value = time,
            icon = Icons.Filled.Schedule,
            caption = timeCaption,
            valueStyle = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Tarjeta con un título y un bloque de filas "etiqueta / valor". */
@Composable
fun DetailCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

/** Fila "etiqueta a la izquierda, valor a la derecha". */
@Composable
fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            // El lector de pantalla anuncia la fila entera de una vez.
            .semantics(mergeDescendants = true) { contentDescription = "$label: $value" },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .clearAndSetSemantics { },
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = if (emphasized) {
                MaterialTheme.typography.titleLarge
            } else {
                MaterialTheme.typography.bodyLarge
            },
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

/** Separador fino entre bloques de una misma tarjeta. */
@Composable
fun DetailDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
    )
}

/**
 * Etiqueta de valoración.
 *
 * Lleva siempre icono y texto además del color de fondo: el color nunca es el
 * único medio para transmitir la valoración.
 */
@Composable
fun RatingBanner(
    title: String,
    detail: String,
    icon: ImageVector,
    tone: RatingTone,
    modifier: Modifier = Modifier,
) {
    val toneColors = tone.colors()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = toneColors.container,
        contentColor = toneColors.content,
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = "$title. $detail"
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.clearAndSetSemantics { }) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(text = detail, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
