package com.pamoron.cargaev.domain.calc

/**
 * Modelo simplificado de la curva de carga.
 *
 * Un coche eléctrico no mantiene la potencia máxima durante toda la sesión:
 * arranca algo limitado con la batería muy vacía, alcanza su mejor ritmo entre
 * el 10 % y el 60 % y reduce mucho a partir del 80 %. Dividir la sesión en
 * tramos de estado de carga y aplicar un factor a cada uno da una estimación
 * mucho más realista que dividir la energía total entre la potencia máxima.
 */
object ChargeCurve {

    /**
     * Franja de estado de carga con su factor de potencia.
     *
     * @param fromPercent inicio de la franja, incluido.
     * @param toPercent fin de la franja, excluido salvo en la última.
     * @param powerFactor fracción de la potencia efectiva que se mantiene de media.
     */
    data class Band(
        val fromPercent: Double,
        val toPercent: Double,
        val powerFactor: Double,
    )

    /**
     * Curva aproximada en corriente continua.
     *
     * Los factores son relativos a la potencia efectiva, es decir, al mínimo
     * entre la potencia del cargador y la potencia máxima DC del vehículo.
     */
    val DC_BANDS: List<Band> = listOf(
        Band(0.0, 10.0, 0.60),
        Band(10.0, 30.0, 0.85),
        Band(30.0, 60.0, 0.90),
        Band(60.0, 80.0, 0.70),
        Band(80.0, 90.0, 0.40),
        Band(90.0, 100.0, 0.20),
    )

    /**
     * Factor aplicado en corriente alterna.
     *
     * En AC la potencia es prácticamente constante porque la limita el cargador
     * de a bordo; el 0,92 recoge el rendimiento de la conversión AC/DC.
     */
    const val AC_POWER_FACTOR: Double = 0.92

    /** Estado de carga por debajo del cual se avisa de que la potencia también se limita. */
    const val LOW_SOC_WARNING_PERCENT: Double = 10.0

    /** Estado de carga a partir del cual la velocidad de carga cae de forma notable. */
    const val HIGH_SOC_WARNING_PERCENT: Double = 80.0
}
