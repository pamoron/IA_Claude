package com.pamoron.electroperico.ui.calculator

import com.pamoron.electroperico.data.settings.LastSession
import com.pamoron.electroperico.domain.model.ChargeInput
import com.pamoron.electroperico.domain.model.ChargeResult
import com.pamoron.electroperico.domain.model.CurrentType
import com.pamoron.electroperico.domain.model.EstimationMode
import com.pamoron.electroperico.domain.model.ValidationError
import com.pamoron.electroperico.domain.model.VehicleProfile
import com.pamoron.electroperico.ui.format.Formatters

/**
 * Valores tal y como están escritos en los campos de la pantalla.
 *
 * Se guardan como texto porque un campo puede estar a medio escribir; la
 * conversión a números ocurre en [toChargeInput].
 */
data class CalculatorInputs(
    val priceText: String = "",
    val powerText: String = "",
    val currentType: CurrentType = CurrentType.DC,
    val startSoc: Int = 20,
    val targetSoc: Int = 80,
    val startFeeText: String = "",
    val pricePerMinuteText: String = "",
    val parkingFeeText: String = "",
    val extrasExpanded: Boolean = false,
) {

    /** `true` si los dos campos obligatorios tienen contenido. */
    val hasRequiredFields: Boolean
        get() = priceText.isNotBlank() && powerText.isNotBlank()

    /** `true` si hay algo escrito que merezca la pena borrar. */
    val isEmpty: Boolean
        get() = priceText.isBlank() &&
            powerText.isBlank() &&
            startFeeText.isBlank() &&
            pricePerMinuteText.isBlank() &&
            parkingFeeText.isBlank()

    /** Suma de los costes adicionales rellenados, solo para la etiqueta de la sección. */
    val extrasCount: Int
        get() = listOf(startFeeText, pricePerMinuteText, parkingFeeText)
            .count { it.isNotBlank() && (Formatters.parseDecimal(it) ?: 0.0) > 0.0 }

    /**
     * Convierte los textos en una entrada del motor de cálculo.
     * Devuelve `null` si falta algún dato obligatorio o si algo no es un número.
     */
    fun toChargeInput(): ChargeInput? {
        val price = Formatters.parseDecimal(priceText) ?: return null
        val power = Formatters.parseDecimal(powerText) ?: return null
        val startFee = Formatters.parseOptionalDecimal(startFeeText) ?: return null
        val perMinute = Formatters.parseOptionalDecimal(pricePerMinuteText) ?: return null
        val parking = Formatters.parseOptionalDecimal(parkingFeeText) ?: return null
        return ChargeInput(
            pricePerKWh = price,
            chargerPowerKw = power,
            currentType = currentType,
            startSocPercent = startSoc.toDouble(),
            targetSocPercent = targetSoc.toDouble(),
            startFeeEur = startFee,
            pricePerMinuteEur = perMinute,
            parkingFeeEur = parking,
        )
    }

    /** Vuelca los valores actuales para recordarlos en la próxima sesión. */
    fun toLastSession(): LastSession = LastSession(
        pricePerKWh = Formatters.parseDecimal(priceText) ?: 0.0,
        chargerPowerKw = Formatters.parseDecimal(powerText) ?: 0.0,
        currentType = currentType,
        startSocPercent = startSoc,
        targetSocPercent = targetSoc,
        startFeeEur = Formatters.parseOptionalDecimal(startFeeText) ?: 0.0,
        pricePerMinuteEur = Formatters.parseOptionalDecimal(pricePerMinuteText) ?: 0.0,
        parkingFeeEur = Formatters.parseOptionalDecimal(parkingFeeText) ?: 0.0,
    )

    companion object {
        /** Reconstruye los campos a partir de la última sesión guardada. */
        fun from(session: LastSession): CalculatorInputs {
            val extras = session.startFeeEur > 0.0 ||
                session.pricePerMinuteEur > 0.0 ||
                session.parkingFeeEur > 0.0
            return CalculatorInputs(
                priceText = Formatters.toEditableText(session.pricePerKWh),
                powerText = Formatters.toEditableText(session.chargerPowerKw),
                currentType = session.currentType,
                startSoc = session.startSocPercent,
                targetSoc = session.targetSocPercent,
                startFeeText = Formatters.toEditableText(session.startFeeEur),
                pricePerMinuteText = Formatters.toEditableText(session.pricePerMinuteEur),
                parkingFeeText = Formatters.toEditableText(session.parkingFeeEur),
                extrasExpanded = extras,
            )
        }
    }
}

/** Estado del cálculo derivado de los campos actuales. */
sealed interface CalculationState {

    /** Faltan datos obligatorios o alguno no es un número válido. */
    data object Incompleto : CalculationState

    /** Los datos son numéricos pero no permiten calcular. */
    data class Invalido(val errors: List<ValidationError>) : CalculationState

    /** Hay un resultado disponible. */
    data class Listo(val result: ChargeResult) : CalculationState
}

/** Estado completo de la pantalla de la calculadora. */
data class CalculatorUiState(
    val inputs: CalculatorInputs = CalculatorInputs(),
    val vehicle: VehicleProfile = VehicleProfile.BYD_ATTO_2_COMFORT,
    val estimationMode: EstimationMode = EstimationMode.DEFAULT,
    val calculation: CalculationState = CalculationState.Incompleto,
    /** Los resultados se muestran a partir del primer "Calcular" y luego se actualizan solos. */
    val resultsRequested: Boolean = false,
) {

    /** Resultado a mostrar, o `null` si todavía no hay nada que enseñar. */
    val visibleResult: ChargeResult?
        get() = (calculation as? CalculationState.Listo)
            ?.result
            ?.takeIf { resultsRequested }

    /** Errores a mostrar, solo después de que el usuario haya pulsado "Calcular". */
    val visibleErrors: List<ValidationError>
        get() = if (resultsRequested) {
            (calculation as? CalculationState.Invalido)?.errors.orEmpty()
        } else {
            emptyList()
        }

    /** El botón "Calcular" solo se habilita cuando hay algo que calcular. */
    val canCalculate: Boolean
        get() = inputs.hasRequiredFields
}
