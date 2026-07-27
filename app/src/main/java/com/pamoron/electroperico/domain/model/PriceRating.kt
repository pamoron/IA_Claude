package com.pamoron.electroperico.domain.model

/** Valoración del precio de la recarga. */
enum class PriceRating {
    MUY_BARATO,
    BUEN_PRECIO,
    NORMAL,
    CARO,
    MUY_CARO,
}

/**
 * Umbrales de valoración del precio en €/kWh, configurables desde ajustes.
 *
 * Los valores iniciales corresponden al mercado español de recarga pública.
 * Cada umbral es el límite superior **incluido** de su categoría.
 */
data class PriceThresholds(
    val veryCheapMax: Double = 0.25,
    val goodMax: Double = 0.39,
    val normalMax: Double = 0.55,
    val expensiveMax: Double = 0.70,
) {

    /** Clasifica un precio en €/kWh. */
    fun rate(pricePerKWh: Double): PriceRating = when {
        pricePerKWh <= veryCheapMax -> PriceRating.MUY_BARATO
        pricePerKWh <= goodMax -> PriceRating.BUEN_PRECIO
        pricePerKWh <= normalMax -> PriceRating.NORMAL
        pricePerKWh <= expensiveMax -> PriceRating.CARO
        else -> PriceRating.MUY_CARO
    }

    /**
     * Comprueba que los umbrales sean estrictamente crecientes.
     * La pantalla de ajustes rechaza los valores que no lo cumplan.
     */
    fun isValid(): Boolean =
        veryCheapMax > 0.0 &&
            veryCheapMax < goodMax &&
            goodMax < normalMax &&
            normalMax < expensiveMax

    companion object {
        val DEFAULT: PriceThresholds = PriceThresholds()
    }
}
