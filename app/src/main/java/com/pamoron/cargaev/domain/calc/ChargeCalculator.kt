package com.pamoron.cargaev.domain.calc

import com.pamoron.cargaev.domain.model.CalculationOutcome
import com.pamoron.cargaev.domain.model.ChargeInput
import com.pamoron.cargaev.domain.model.ChargeResult
import com.pamoron.cargaev.domain.model.ChargeSegment
import com.pamoron.cargaev.domain.model.ChargeWarning
import com.pamoron.cargaev.domain.model.ChargerPowerThresholds
import com.pamoron.cargaev.domain.model.ChargerRating
import com.pamoron.cargaev.domain.model.CurrentType
import com.pamoron.cargaev.domain.model.EstimationMode
import com.pamoron.cargaev.domain.model.PriceThresholds
import com.pamoron.cargaev.domain.model.ValidationError
import com.pamoron.cargaev.domain.model.VehicleProfile
import java.math.BigDecimal
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Motor de cálculo de recargas.
 *
 * Es código Kotlin puro, sin dependencias de Android, para poder probarlo
 * íntegramente con pruebas unitarias de JVM. No formatea nada: devuelve valores
 * y tipos, y es la capa de presentación la que decide cómo mostrarlos.
 *
 * Todos los resultados son **estimaciones**.
 */
object ChargeCalculator {

    /**
     * Margen sobre la potencia máxima del vehículo a partir del cual se
     * considera que el cargador está sobredimensionado.
     *
     * Un cargador de 160 kW frente a un coche de 155 kW no merece un aviso;
     * uno de 300 kW sí.
     */
    private const val OVERSIZE_TOLERANCE = 1.05

    /**
     * Calcula el resultado de una sesión de recarga.
     *
     * @param input datos del cargador y de la sesión.
     * @param vehicle perfil del vehículo (capacidad útil, límites, pérdidas y consumo).
     * @param estimationMode grado de prudencia aplicado al tiempo.
     * @param priceThresholds umbrales de valoración del precio.
     * @param chargerThresholds umbrales de valoración de la potencia en DC.
     * @return [CalculationOutcome.Success] con el resultado, o
     *         [CalculationOutcome.Failure] con los errores de validación.
     */
    fun calculate(
        input: ChargeInput,
        vehicle: VehicleProfile,
        estimationMode: EstimationMode = EstimationMode.DEFAULT,
        priceThresholds: PriceThresholds = PriceThresholds.DEFAULT,
        chargerThresholds: ChargerPowerThresholds = ChargerPowerThresholds.DEFAULT,
    ): CalculationOutcome {

        val errors = validate(input, vehicle)
        if (errors.isNotEmpty()) return CalculationOutcome.Failure(errors)

        // --- Energía -------------------------------------------------------

        val socAdded = input.targetSocPercent - input.startSocPercent
        val energyNeededKWh = vehicle.usableCapacityKWh * socAdded / 100.0

        val lossFraction = vehicle.lossFractionFor(input.currentType)
        // La energía que factura el cargador incluye lo que se pierde por el camino.
        val energyBilledKWh = energyNeededKWh / (1.0 - lossFraction)

        // --- Potencia ------------------------------------------------------

        val vehicleMaxPowerKw = vehicle.maxPowerKwFor(input.currentType)
        val effectivePowerKw = min(input.chargerPowerKw, vehicleMaxPowerKw)
        val isOversized = input.chargerPowerKw > vehicleMaxPowerKw * OVERSIZE_TOLERANCE

        // --- Tiempo --------------------------------------------------------

        val segments = buildSegments(
            currentType = input.currentType,
            startSocPercent = input.startSocPercent,
            targetSocPercent = input.targetSocPercent,
            usableCapacityKWh = vehicle.usableCapacityKWh,
            effectivePowerKw = effectivePowerKw,
            estimationFactor = estimationMode.factor,
        )
        val rawMinutes = segments.sumOf { it.minutes }
        // Nunca se muestra "0 min": cualquier sesión ocupa al menos un minuto.
        val minutes = max(1, rawMinutes.roundToInt())

        // --- Dinero --------------------------------------------------------
        // Cada partida se redondea a dos decimales antes de sumar, para que el
        // total mostrado cuadre exactamente con el desglose mostrado.

        val energyCost = Money.eurFromProduct(energyBilledKWh, input.pricePerKWh)
        val startFee = Money.eur(input.startFeeEur)
        // El coste por tiempo se factura sobre los minutos redondeados que ve
        // el usuario, para que "minutos x €/min" cuadre con la partida.
        val timeCost = Money.eurFromProduct(minutes.toDouble(), input.pricePerMinuteEur)
        val parkingFee = Money.eur(input.parkingFeeEur)
        val totalCost = energyCost.add(startFee).add(timeCost).add(parkingFee)

        val costPerMinute = Money.eurFromDivision(totalCost, minutes.toDouble())
        val announcedPrice = Money.rate(input.pricePerKWh)
        val effectivePrice = Money.rateFromDivision(totalCost, energyBilledKWh)

        // --- Autonomía -----------------------------------------------------

        val rangeAddedKm = energyNeededKWh / vehicle.consumptionKWhPer100Km * 100.0
        val costPer100Km = costPer100Km(totalCost, rangeAddedKm)

        // --- Valoraciones --------------------------------------------------

        val chargerRating = rateCharger(
            currentType = input.currentType,
            chargerPowerKw = input.chargerPowerKw,
            vehicleMaxPowerKw = vehicleMaxPowerKw,
            thresholds = chargerThresholds,
        )
        // La valoración principal usa el precio efectivo, no el anunciado.
        val priceRating = priceThresholds.rate(effectivePrice.toDouble())
        val announcedPriceRating = priceThresholds.rate(announcedPrice.toDouble())

        return CalculationOutcome.Success(
            ChargeResult(
                energyNeededKWh = energyNeededKWh,
                energyBilledKWh = energyBilledKWh,
                lossFraction = lossFraction,
                chargerPowerKw = input.chargerPowerKw,
                vehicleMaxPowerKw = vehicleMaxPowerKw,
                effectivePowerKw = effectivePowerKw,
                isOversized = isOversized,
                segments = segments,
                rawMinutes = rawMinutes,
                minutes = minutes,
                estimationMode = estimationMode,
                energyCostEur = energyCost,
                startFeeEur = startFee,
                timeCostEur = timeCost,
                parkingFeeEur = parkingFee,
                totalCostEur = totalCost,
                costPerMinuteEur = costPerMinute,
                announcedPricePerKWh = announcedPrice,
                effectivePricePerKWh = effectivePrice,
                rangeAddedKm = rangeAddedKm,
                costPer100KmEur = costPer100Km,
                socAddedPercent = socAdded,
                priceRating = priceRating,
                announcedPriceRating = announcedPriceRating,
                chargerRating = chargerRating,
                warnings = buildWarnings(input, isOversized),
            ),
        )
    }

    // -----------------------------------------------------------------------
    // Validación
    // -----------------------------------------------------------------------

    /** Comprueba las entradas y el perfil. Devuelve una lista vacía si todo es correcto. */
    private fun validate(input: ChargeInput, vehicle: VehicleProfile): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        if (input.pricePerKWh < 0.0 || input.pricePerKWh.isNaN()) {
            errors += ValidationError.PRECIO_NEGATIVO
        }
        if (input.chargerPowerKw <= 0.0 || input.chargerPowerKw.isNaN()) {
            errors += ValidationError.POTENCIA_CARGADOR_INVALIDA
        }
        if (!input.startSocPercent.isInSocRange() || !input.targetSocPercent.isInSocRange()) {
            errors += ValidationError.PORCENTAJE_FUERA_DE_RANGO
        } else if (input.targetSocPercent <= input.startSocPercent) {
            // Solo tiene sentido comprobarlo si ambos porcentajes son válidos.
            errors += ValidationError.OBJETIVO_NO_SUPERIOR_AL_ACTUAL
        }
        if (input.startFeeEur < 0.0 || input.pricePerMinuteEur < 0.0 || input.parkingFeeEur < 0.0) {
            errors += ValidationError.COSTE_ADICIONAL_NEGATIVO
        }
        if (vehicle.usableCapacityKWh <= 0.0) {
            errors += ValidationError.CAPACIDAD_INVALIDA
        }
        if (vehicle.consumptionKWhPer100Km <= 0.0) {
            errors += ValidationError.CONSUMO_INVALIDO
        }
        if (vehicle.maxPowerKwFor(input.currentType) <= 0.0) {
            errors += ValidationError.POTENCIA_VEHICULO_INVALIDA
        }
        val loss = vehicle.lossFractionFor(input.currentType)
        if (loss < 0.0 || loss >= 1.0) {
            errors += ValidationError.PERDIDAS_INVALIDAS
        }
        return errors
    }

    private fun Double.isInSocRange(): Boolean = !isNaN() && this >= 0.0 && this <= 100.0

    // -----------------------------------------------------------------------
    // Tiempo
    // -----------------------------------------------------------------------

    /**
     * Construye los tramos de la sesión y su duración.
     *
     * En DC se recorren las franjas de [ChargeCurve.DC_BANDS] que la sesión
     * atraviesa; en AC se genera un único tramo con potencia estable.
     *
     * El tiempo se calcula con la energía que **entra en la batería**, no con
     * la facturada: los factores de la curva describen la potencia que acepta
     * la batería y el 0,92 de AC es el rendimiento del cargador de a bordo, de
     * modo que contar además las pérdidas las estaría contando dos veces.
     */
    private fun buildSegments(
        currentType: CurrentType,
        startSocPercent: Double,
        targetSocPercent: Double,
        usableCapacityKWh: Double,
        effectivePowerKw: Double,
        estimationFactor: Double,
    ): List<ChargeSegment> {
        if (currentType == CurrentType.AC) {
            val averagePowerKw = effectivePowerKw * ChargeCurve.AC_POWER_FACTOR
            val energyKWh = usableCapacityKWh * (targetSocPercent - startSocPercent) / 100.0
            val minutes = energyKWh / averagePowerKw * 60.0 * estimationFactor
            return listOf(
                ChargeSegment(
                    fromPercent = startSocPercent,
                    toPercent = targetSocPercent,
                    energyKWh = energyKWh,
                    averagePowerKw = averagePowerKw,
                    minutes = minutes,
                ),
            )
        }

        return ChargeCurve.DC_BANDS.mapNotNull { band ->
            val from = max(startSocPercent, band.fromPercent)
            val to = min(targetSocPercent, band.toPercent)
            if (to <= from) return@mapNotNull null

            val energyKWh = usableCapacityKWh * (to - from) / 100.0
            val averagePowerKw = effectivePowerKw * band.powerFactor
            ChargeSegment(
                fromPercent = from,
                toPercent = to,
                energyKWh = energyKWh,
                averagePowerKw = averagePowerKw,
                minutes = energyKWh / averagePowerKw * 60.0 * estimationFactor,
            )
        }
    }

    // -----------------------------------------------------------------------
    // Autonomía
    // -----------------------------------------------------------------------

    /** Coste por cada 100 km añadidos. Devuelve cero si no hay autonomía que repartir. */
    private fun costPer100Km(totalCost: BigDecimal, rangeAddedKm: Double): BigDecimal =
        if (rangeAddedKm <= 0.0) {
            Money.ZERO
        } else {
            totalCost
                .multiply(BigDecimal.valueOf(100L))
                .divide(BigDecimal.valueOf(rangeAddedKm), Money.MONEY_SCALE, Money.ROUNDING)
        }

    // -----------------------------------------------------------------------
    // Valoraciones y avisos
    // -----------------------------------------------------------------------

    /**
     * Clasifica la potencia del cargador respecto al vehículo.
     *
     * En AC el límite superior lo marca el propio coche, de modo que la
     * clasificación sigue siendo correcta si se edita el perfil.
     */
    private fun rateCharger(
        currentType: CurrentType,
        chargerPowerKw: Double,
        vehicleMaxPowerKw: Double,
        thresholds: ChargerPowerThresholds,
    ): ChargerRating = when (currentType) {
        CurrentType.AC -> when {
            chargerPowerKw > vehicleMaxPowerKw -> ChargerRating.AC_LIMITADO_POR_VEHICULO
            chargerPowerKw <= ChargerPowerThresholds.AC_VERY_SLOW_MAX_KW -> ChargerRating.AC_MUY_LENTO
            chargerPowerKw <= ChargerPowerThresholds.AC_NORMAL_MAX_KW -> ChargerRating.AC_NORMAL
            else -> ChargerRating.AC_OPTIMO
        }

        CurrentType.DC -> when {
            chargerPowerKw < thresholds.dcSlowMaxKw -> ChargerRating.DC_LENTO
            chargerPowerKw < thresholds.dcAdequateMaxKw -> ChargerRating.DC_ADECUADO
            chargerPowerKw < thresholds.dcVeryAdequateMaxKw -> ChargerRating.DC_MUY_ADECUADO
            else -> ChargerRating.DC_SOBREDIMENSIONADO
        }
    }

    /** Avisos aplicables: primero los que dependen de los datos, después los generales. */
    private fun buildWarnings(input: ChargeInput, isOversized: Boolean): List<ChargeWarning> =
        buildList {
            if (isOversized) add(ChargeWarning.CARGADOR_SOBREDIMENSIONADO)
            if (input.targetSocPercent > ChargeCurve.HIGH_SOC_WARNING_PERCENT) {
                add(ChargeWarning.POR_ENCIMA_DEL_80)
            }
            if (input.currentType == CurrentType.DC &&
                input.startSocPercent < ChargeCurve.LOW_SOC_WARNING_PERCENT
            ) {
                add(ChargeWarning.SOC_INICIAL_MUY_BAJO)
            }
            add(ChargeWarning.POTENCIA_NO_GARANTIZADA)
            add(ChargeWarning.POTENCIA_COMPARTIDA)
            add(ChargeWarning.COMPROBAR_TARIFAS)
            add(ChargeWarning.PRECIOS_APP_Y_TARJETA)
            add(ChargeWarning.PERDIDAS_DE_CARGA)
        }
}
