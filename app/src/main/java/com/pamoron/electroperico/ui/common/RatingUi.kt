package com.pamoron.electroperico.ui.common

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.pamoron.electroperico.R
import com.pamoron.electroperico.domain.model.ChargeWarning
import com.pamoron.electroperico.domain.model.ChargerRating
import com.pamoron.electroperico.domain.model.ComparisonSort
import com.pamoron.electroperico.domain.model.CurrentType
import com.pamoron.electroperico.domain.model.EstimationMode
import com.pamoron.electroperico.domain.model.PriceRating
import com.pamoron.electroperico.domain.model.ValidationError
import com.pamoron.electroperico.ui.theme.LocalRatingColors

/**
 * Traducción de los tipos del dominio a textos, iconos y colores.
 *
 * El dominio no conoce recursos de Android; este fichero es el único puente.
 * Toda valoración se comunica con **texto e icono además de color**, nunca solo
 * con color.
 */

/** Grado de una valoración, usado para elegir color e icono. */
enum class RatingTone { BUENO, NEUTRO, AVISO, MALO }

/** Colores de fondo y de texto asociados a un grado. */
data class ToneColors(val container: Color, val content: Color)

@Composable
@ReadOnlyComposable
fun RatingTone.colors(): ToneColors {
    val palette = LocalRatingColors.current
    return when (this) {
        RatingTone.BUENO -> ToneColors(palette.goodContainer, palette.onGood)
        RatingTone.NEUTRO -> ToneColors(palette.neutralContainer, palette.onNeutral)
        RatingTone.AVISO -> ToneColors(palette.warnContainer, palette.onWarn)
        RatingTone.MALO -> ToneColors(palette.badContainer, palette.onBad)
    }
}

// --- Valoración del precio --------------------------------------------------

@get:StringRes
val PriceRating.labelRes: Int
    get() = when (this) {
        PriceRating.MUY_BARATO -> R.string.precio_muy_barato
        PriceRating.BUEN_PRECIO -> R.string.precio_buen_precio
        PriceRating.NORMAL -> R.string.precio_normal
        PriceRating.CARO -> R.string.precio_caro
        PriceRating.MUY_CARO -> R.string.precio_muy_caro
    }

val PriceRating.tone: RatingTone
    get() = when (this) {
        PriceRating.MUY_BARATO, PriceRating.BUEN_PRECIO -> RatingTone.BUENO
        PriceRating.NORMAL -> RatingTone.NEUTRO
        PriceRating.CARO -> RatingTone.AVISO
        PriceRating.MUY_CARO -> RatingTone.MALO
    }

val PriceRating.icon: ImageVector
    get() = when (this) {
        PriceRating.MUY_BARATO -> Icons.Filled.Savings
        PriceRating.BUEN_PRECIO -> Icons.Filled.ThumbUp
        PriceRating.NORMAL -> Icons.Filled.Info
        PriceRating.CARO -> Icons.Filled.TrendingUp
        PriceRating.MUY_CARO -> Icons.Filled.Warning
    }

// --- Valoración de la potencia ----------------------------------------------

@get:StringRes
val ChargerRating.labelRes: Int
    get() = when (this) {
        ChargerRating.DC_LENTO -> R.string.cargador_dc_lento
        ChargerRating.DC_ADECUADO -> R.string.cargador_dc_adecuado
        ChargerRating.DC_MUY_ADECUADO -> R.string.cargador_dc_muy_adecuado
        ChargerRating.DC_SOBREDIMENSIONADO -> R.string.cargador_dc_sobredimensionado
        ChargerRating.AC_MUY_LENTO -> R.string.cargador_ac_muy_lento
        ChargerRating.AC_NORMAL -> R.string.cargador_ac_normal
        ChargerRating.AC_OPTIMO -> R.string.cargador_ac_optimo
        ChargerRating.AC_LIMITADO_POR_VEHICULO -> R.string.cargador_ac_limitado
    }

val ChargerRating.tone: RatingTone
    get() = when (this) {
        ChargerRating.DC_MUY_ADECUADO, ChargerRating.AC_OPTIMO -> RatingTone.BUENO
        ChargerRating.DC_ADECUADO, ChargerRating.AC_NORMAL -> RatingTone.NEUTRO
        ChargerRating.DC_LENTO,
        ChargerRating.AC_MUY_LENTO,
        ChargerRating.DC_SOBREDIMENSIONADO,
        ChargerRating.AC_LIMITADO_POR_VEHICULO,
        -> RatingTone.AVISO
    }

val ChargerRating.icon: ImageVector
    get() = when (this) {
        ChargerRating.DC_MUY_ADECUADO, ChargerRating.AC_OPTIMO -> Icons.Filled.CheckCircle
        ChargerRating.DC_ADECUADO, ChargerRating.AC_NORMAL -> Icons.Filled.Bolt
        ChargerRating.DC_LENTO, ChargerRating.AC_MUY_LENTO -> Icons.Filled.HourglassBottom
        ChargerRating.DC_SOBREDIMENSIONADO,
        ChargerRating.AC_LIMITADO_POR_VEHICULO,
        -> Icons.Filled.RemoveCircleOutline
    }

// --- Avisos ------------------------------------------------------------------

/**
 * Texto de un aviso.
 *
 * [ChargeWarning.CARGADOR_SOBREDIMENSIONADO] necesita las potencias, así que se
 * resuelve aparte en la pantalla con `stringResource(id, cargador, vehículo)`.
 */
@get:StringRes
val ChargeWarning.textRes: Int
    get() = when (this) {
        ChargeWarning.CARGADOR_SOBREDIMENSIONADO -> R.string.aviso_sobredimensionado
        ChargeWarning.POR_ENCIMA_DEL_80 -> R.string.aviso_por_encima_del_80
        ChargeWarning.SOC_INICIAL_MUY_BAJO -> R.string.aviso_soc_inicial_bajo
        ChargeWarning.POTENCIA_NO_GARANTIZADA -> R.string.aviso_potencia_no_garantizada
        ChargeWarning.POTENCIA_COMPARTIDA -> R.string.aviso_potencia_compartida
        ChargeWarning.COMPROBAR_TARIFAS -> R.string.aviso_comprobar_tarifas
        ChargeWarning.PRECIOS_APP_Y_TARJETA -> R.string.aviso_precios_app_tarjeta
        ChargeWarning.PERDIDAS_DE_CARGA -> R.string.aviso_perdidas_de_carga
    }

// --- Errores de validación ---------------------------------------------------

@get:StringRes
val ValidationError.messageRes: Int
    get() = when (this) {
        ValidationError.PRECIO_NEGATIVO -> R.string.error_precio_negativo
        ValidationError.POTENCIA_CARGADOR_INVALIDA -> R.string.error_potencia_cargador
        ValidationError.PORCENTAJE_FUERA_DE_RANGO -> R.string.error_porcentaje_rango
        ValidationError.OBJETIVO_NO_SUPERIOR_AL_ACTUAL -> R.string.error_objetivo_no_superior
        ValidationError.CAPACIDAD_INVALIDA -> R.string.error_capacidad
        ValidationError.CONSUMO_INVALIDO -> R.string.error_consumo
        ValidationError.PERDIDAS_INVALIDAS -> R.string.error_perdidas
        ValidationError.POTENCIA_VEHICULO_INVALIDA -> R.string.error_potencia_vehiculo
        ValidationError.COSTE_ADICIONAL_NEGATIVO -> R.string.error_coste_adicional
    }

// --- Enumeraciones sueltas ---------------------------------------------------

@get:StringRes
val EstimationMode.labelRes: Int
    get() = when (this) {
        EstimationMode.OPTIMISTA -> R.string.estimacion_optimista
        EstimationMode.NORMAL -> R.string.estimacion_normal
        EstimationMode.CONSERVADORA -> R.string.estimacion_conservadora
    }

/** Versión corta, para el pie de las tarjetas de resultado. */
@get:StringRes
val EstimationMode.shortLabelRes: Int
    get() = when (this) {
        EstimationMode.OPTIMISTA -> R.string.estimacion_corta_optimista
        EstimationMode.NORMAL -> R.string.estimacion_corta_normal
        EstimationMode.CONSERVADORA -> R.string.estimacion_corta_conservadora
    }

@get:StringRes
val ComparisonSort.labelRes: Int
    get() = when (this) {
        ComparisonSort.MAS_BARATO -> R.string.orden_mas_barato
        ComparisonSort.MAS_RAPIDO -> R.string.orden_mas_rapido
        ComparisonSort.MEJOR_EQUILIBRIO -> R.string.orden_mejor_equilibrio
    }

@get:StringRes
val CurrentType.labelRes: Int
    get() = when (this) {
        CurrentType.AC -> R.string.corriente_ac
        CurrentType.DC -> R.string.corriente_dc
    }
