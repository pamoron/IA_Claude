package com.pamoron.electroperico.domain.model

import java.math.BigDecimal

/**
 * Resultado completo de una simulación de recarga.
 *
 * Todos los importes son [BigDecimal] ya redondeados: los costes con dos
 * decimales y los precios unitarios con cuatro. Las magnitudes físicas siguen
 * siendo [Double] y se redondean únicamente al mostrarlas.
 *
 * Ningún valor debe presentarse como exacto: son estimaciones.
 */
data class ChargeResult(

    // --- Energía -----------------------------------------------------------

    /** Energía que necesita la batería, sin pérdidas. */
    val energyNeededKWh: Double,

    /** Energía que previsiblemente facturará el cargador, con pérdidas incluidas. */
    val energyBilledKWh: Double,

    /** Pérdidas aplicadas, como fracción (0,08 = 8 %). */
    val lossFraction: Double,

    // --- Potencia ----------------------------------------------------------

    /** Potencia anunciada por el cargador. */
    val chargerPowerKw: Double,

    /** Potencia máxima del vehículo para el tipo de corriente elegido. */
    val vehicleMaxPowerKw: Double,

    /** Potencia máxima que el coche podrá aprovechar realmente. */
    val effectivePowerKw: Double,

    /** `true` si el cargador ofrece bastante más potencia de la que admite el coche. */
    val isOversized: Boolean,

    // --- Tiempo ------------------------------------------------------------

    /** Desglose del tiempo por tramos de estado de carga. Vacío en AC. */
    val segments: List<ChargeSegment>,

    /** Minutos estimados sin redondear, con el factor de estimación ya aplicado. */
    val rawMinutes: Double,

    /** Minutos estimados redondeados; es el valor que se muestra y el que se factura. */
    val minutes: Int,

    /** Modo de estimación usado. */
    val estimationMode: EstimationMode,

    // --- Dinero ------------------------------------------------------------

    /** Coste de la energía facturada. */
    val energyCostEur: BigDecimal,

    /** Coste fijo de inicio de sesión. */
    val startFeeEur: BigDecimal,

    /** Coste asociado al tiempo de ocupación. */
    val timeCostEur: BigDecimal,

    /** Coste de estacionamiento. */
    val parkingFeeEur: BigDecimal,

    /** Suma exacta de las cuatro partidas anteriores. */
    val totalCostEur: BigDecimal,

    /** Coste aproximado por minuto de sesión. */
    val costPerMinuteEur: BigDecimal,

    /** Precio anunciado por el cargador, en €/kWh. */
    val announcedPricePerKWh: BigDecimal,

    /** Precio realmente pagado por kWh facturado, incluyendo todos los costes. */
    val effectivePricePerKWh: BigDecimal,

    // --- Autonomía ---------------------------------------------------------

    /** Autonomía aproximada añadida, en kilómetros. */
    val rangeAddedKm: Double,

    /** Coste por cada 100 km de autonomía añadida. */
    val costPer100KmEur: BigDecimal,

    /** Puntos porcentuales de batería añadidos. */
    val socAddedPercent: Double,

    // --- Valoraciones ------------------------------------------------------

    /** Valoración basada en el precio efectivo. Es la valoración principal. */
    val priceRating: PriceRating,

    /** Valoración del precio anunciado, para poder comparar ambas. */
    val announcedPriceRating: PriceRating,

    /** Valoración de la potencia del cargador respecto al vehículo. */
    val chargerRating: ChargerRating,

    /** Avisos aplicables, con los contextuales primero. */
    val warnings: List<ChargeWarning>,
) {

    /** `true` si algún coste adicional hace que el precio efectivo supere al anunciado. */
    val hasExtraCosts: Boolean
        get() = startFeeEur.signum() > 0 || timeCostEur.signum() > 0 || parkingFeeEur.signum() > 0

    /** Diferencia entre el precio efectivo y el anunciado, en €/kWh. */
    val pricePremiumPerKWh: BigDecimal
        get() = effectivePricePerKWh.subtract(announcedPricePerKWh)
}

/**
 * Salida del motor de cálculo: o bien un resultado, o bien la lista de motivos
 * por los que no se ha podido calcular.
 */
sealed interface CalculationOutcome {

    /** Cálculo realizado con éxito. */
    data class Success(val result: ChargeResult) : CalculationOutcome

    /** Datos de entrada no válidos. [errors] nunca está vacía. */
    data class Failure(val errors: List<ValidationError>) : CalculationOutcome
}
