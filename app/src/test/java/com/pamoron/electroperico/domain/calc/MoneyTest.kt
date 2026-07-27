package com.pamoron.electroperico.domain.calc

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

/** Redondeo monetario controlado. */
class MoneyTest {

    @Test
    fun `los importes se redondean a dos decimales al alza en el punto medio`() {
        assertEquals(BigDecimal("1.24"), Money.eur(1.235))
        assertEquals(BigDecimal("1.23"), Money.eur(1.234))
        assertEquals(BigDecimal("0.00"), Money.eur(0.0))
    }

    @Test
    fun `los precios unitarios se redondean a cuatro decimales`() {
        assertEquals(BigDecimal("0.4500"), Money.rate(0.45))
        assertEquals(BigDecimal("0.4568"), Money.rate(0.45678))
    }

    @Test
    fun `el producto no arrastra el ruido binario del coma flotante`() {
        // 0,1 x 3 en Double da 0,30000000000000004.
        assertEquals(BigDecimal("0.30"), Money.eurFromProduct(3.0, 0.1))
        assertEquals(BigDecimal("18.20"), Money.eurFromProduct(40.434782608695650, 0.45))
    }

    @Test
    fun `las divisiones entre cero devuelven cero en lugar de fallar`() {
        assertEquals(BigDecimal.ZERO.setScale(4), Money.rateFromDivision(BigDecimal("10.00"), 0.0))
        assertEquals(Money.ZERO, Money.eurFromDivision(BigDecimal("10.00"), 0.0))
        assertEquals(Money.ZERO, Money.eurFromDivision(BigDecimal("10.00"), -3.0))
    }

    @Test
    fun `las divisiones normales mantienen la escala esperada`() {
        assertEquals(BigDecimal("0.5000"), Money.rateFromDivision(BigDecimal("10.00"), 20.0))
        assertEquals(BigDecimal("2.50"), Money.eurFromDivision(BigDecimal("10.00"), 4.0))
    }

    @Test
    fun `el cero tiene la escala de los importes`() {
        assertEquals(2, Money.ZERO.scale())
        assertEquals(BigDecimal("0.00"), Money.ZERO)
    }
}
