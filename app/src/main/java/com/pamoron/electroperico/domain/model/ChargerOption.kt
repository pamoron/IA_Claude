package com.pamoron.electroperico.domain.model

/**
 * Una opción de cargador guardada en el comparador.
 *
 * Es la entrada de la calculadora más un nombre para reconocerla. Cada opción
 * lleva sus propios porcentajes de batería porque a veces se compara "cargar
 * hasta el 80 % aquí" con "cargar hasta el 100 % allí".
 */
data class ChargerOption(
    val id: String,
    val name: String,
    val pricePerKWh: Double,
    val chargerPowerKw: Double,
    val currentType: CurrentType,
    val startSocPercent: Int,
    val targetSocPercent: Int,
    val startFeeEur: Double = 0.0,
    val pricePerMinuteEur: Double = 0.0,
    val parkingFeeEur: Double = 0.0,
) {

    /** Traduce la opción a la entrada que entiende el motor de cálculo. */
    fun toChargeInput(): ChargeInput = ChargeInput(
        pricePerKWh = pricePerKWh,
        chargerPowerKw = chargerPowerKw,
        currentType = currentType,
        startSocPercent = startSocPercent.toDouble(),
        targetSocPercent = targetSocPercent.toDouble(),
        startFeeEur = startFeeEur,
        pricePerMinuteEur = pricePerMinuteEur,
        parkingFeeEur = parkingFeeEur,
    )

    companion object {
        /** Número máximo de opciones que se pueden comparar a la vez. */
        const val MAX_OPTIONS: Int = 5
    }
}

/** Criterio de ordenación del comparador. */
enum class ComparisonSort {
    MAS_BARATO,
    MAS_RAPIDO,
    MEJOR_EQUILIBRIO,
    ;

    companion object {
        val DEFAULT: ComparisonSort = MEJOR_EQUILIBRIO

        fun fromName(name: String?): ComparisonSort =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
