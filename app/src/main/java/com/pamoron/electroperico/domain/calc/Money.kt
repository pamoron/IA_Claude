package com.pamoron.electroperico.domain.calc

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Utilidades de redondeo monetario.
 *
 * Regla del proyecto: **cada partida se redondea a dos decimales antes de
 * sumarse**, de modo que el total mostrado es exactamente la suma de las
 * partidas mostradas y el usuario nunca ve un desajuste de un céntimo.
 *
 * Se usa [BigDecimal.valueOf] en lugar del constructor `BigDecimal(double)`
 * porque el primero parte de la representación decimal corta del número y
 * evita arrastrar el ruido binario del `Double`.
 */
object Money {

    /** Decimales de un importe en euros. */
    const val MONEY_SCALE: Int = 2

    /** Decimales de un precio unitario en €/kWh. */
    const val RATE_SCALE: Int = 4

    /** Redondeo usado en toda la aplicación. */
    val ROUNDING: RoundingMode = RoundingMode.HALF_UP

    /** Cero con la escala de importes. */
    val ZERO: BigDecimal = BigDecimal.ZERO.setScale(MONEY_SCALE)

    /** Convierte un [Double] en un importe en euros redondeado a dos decimales. */
    fun eur(value: Double): BigDecimal =
        BigDecimal.valueOf(value).setScale(MONEY_SCALE, ROUNDING)

    /** Convierte un [Double] en un precio unitario redondeado a cuatro decimales. */
    fun rate(value: Double): BigDecimal =
        BigDecimal.valueOf(value).setScale(RATE_SCALE, ROUNDING)

    /** Multiplica una cantidad por un precio unitario y devuelve un importe en euros. */
    fun eurFromProduct(quantity: Double, unitPrice: Double): BigDecimal =
        BigDecimal.valueOf(quantity)
            .multiply(BigDecimal.valueOf(unitPrice))
            .setScale(MONEY_SCALE, ROUNDING)

    /**
     * Divide un importe entre una cantidad y devuelve un precio unitario.
     * Si el divisor no es positivo devuelve cero, evitando divisiones entre cero.
     */
    fun rateFromDivision(amount: BigDecimal, quantity: Double): BigDecimal =
        if (quantity <= 0.0) {
            BigDecimal.ZERO.setScale(RATE_SCALE)
        } else {
            amount.divide(BigDecimal.valueOf(quantity), RATE_SCALE, ROUNDING)
        }

    /**
     * Divide un importe entre una cantidad y devuelve un importe en euros.
     * Si el divisor no es positivo devuelve cero.
     */
    fun eurFromDivision(amount: BigDecimal, quantity: Double): BigDecimal =
        if (quantity <= 0.0) {
            ZERO
        } else {
            amount.divide(BigDecimal.valueOf(quantity), MONEY_SCALE, ROUNDING)
        }
}
