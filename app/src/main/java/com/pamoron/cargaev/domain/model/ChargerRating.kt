package com.pamoron.cargaev.domain.model

/**
 * Valoración de la potencia del cargador **respecto al vehículo**.
 *
 * Un cargador no es mejor solo por tener más potencia: a partir del límite del
 * coche, los kW adicionales no aportan nada. Por eso hay una categoría
 * explícita de sobredimensionado.
 */
enum class ChargerRating {
    /** DC por debajo de 50 kW: sirve, pero es lento para viajes. */
    DC_LENTO,

    /** DC de 50 a 99 kW. */
    DC_ADECUADO,

    /** DC de 100 a 179 kW. */
    DC_MUY_ADECUADO,

    /** DC de 180 kW o más: por encima de lo que el coche puede aprovechar. */
    DC_SOBREDIMENSIONADO,

    /** AC hasta 3,7 kW: carga de emergencia o nocturna muy larga. */
    AC_MUY_LENTO,

    /** AC de más de 3,7 y hasta 7,4 kW. */
    AC_NORMAL,

    /** AC que aprovecha por completo el cargador de a bordo. */
    AC_OPTIMO,

    /** AC con más potencia de la que admite el cargador de a bordo del coche. */
    AC_LIMITADO_POR_VEHICULO,
}

/**
 * Umbrales de clasificación de la potencia de los cargadores DC.
 *
 * Los valores por defecto son los definidos para el BYD ATTO 2 Comfort.
 * En AC no hay umbrales configurables: los niveles 3,7 / 7,4 kW son los
 * estándar de instalación y el límite superior lo marca el propio vehículo.
 */
data class ChargerPowerThresholds(
    val dcSlowMaxKw: Double = 50.0,
    val dcAdequateMaxKw: Double = 100.0,
    val dcVeryAdequateMaxKw: Double = 180.0,
) {
    companion object {
        val DEFAULT: ChargerPowerThresholds = ChargerPowerThresholds()

        /** Niveles estándar de potencia en corriente alterna. */
        const val AC_VERY_SLOW_MAX_KW: Double = 3.7
        const val AC_NORMAL_MAX_KW: Double = 7.4
    }
}
