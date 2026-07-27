package com.pamoron.electroperico.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.pamoron.electroperico.R

/**
 * Pie de página común a todas las pantallas.
 *
 * Reproduce el pie de padelgram.es: "Idea de", el avatar y "by GREAT", en una
 * barra redondeada de poca altura que no compite con el contenido.
 *
 * El avatar es [R.drawable.avatar_great]; basta con sustituir ese único fichero
 * para cambiar la ilustración.
 */
@Composable
fun AppFooter(modifier: Modifier = Modifier) {
    val ideaDe = stringResource(R.string.footer_idea_de)
    val by = stringResource(R.string.footer_by)
    val great = stringResource(R.string.footer_great)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 16.dp)
                // El lector de pantalla lo anuncia como una sola frase.
                .semantics(mergeDescendants = true) {
                    contentDescription = "$ideaDe $by $great"
                },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = ideaDe,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.width(10.dp))
            Image(
                painter = painterResource(R.drawable.avatar_great),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = buildAnnotatedString {
                    append(by)
                    append(" ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(great) }
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
