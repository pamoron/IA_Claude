package com.pamoron.cargaev.domain.model

/**
 * Datos de una sesión de recarga tal y como los introduce el usuario.
 *
 * Los importes se reciben como [Double] porque provienen de campos de texto;
 * el motor los convierte a `BigDecimal` con [java.math.BigDecimal.valueOf]
 * antes de operar, de modo que el redondeo monetario está siempre controlado.
 *
 * @param pricePerKWh precio anunciado por el cargador, en €/kWh.
 * @param chargerPowerKw potencia anunciada por el cargador, en kW.
 * @param currentType AC o DC.
 * @param startSocPercent porcentaje de batería actual (0..100).
 * @param targetSocPercent porcentaje de batería objetivo (0..100), mayor que el actual.
 * @param startFeeEur coste fijo de inicio de sesión, en €.
 * @param pricePerMinuteEur coste por minuto, en €.
 * @param parkingFeeEur coste de estacionamiento de la sesión, en €.
 */
data class ChargeInput(
    val pricePerKWh: Double,
    val chargerPowerKw: Double,
    val currentType: CurrentType,
    val startSocPercent: Double,
    val targetSocPercent: Double,
    val startFeeEur: Double = 0.0,
    val pricePerMinuteEur: Double = 0.0,
    val parkingFeeEur: Double = 0.0,
)
