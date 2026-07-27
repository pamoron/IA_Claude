package com.pamoron.cargaev.domain.calc

import com.pamoron.cargaev.domain.model.PriceRating
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Caso 4 del enunciado.
 *
 * 0,40 €/kWh más 0,10 €/min y 1 € de inicio de sesión: el precio efectivo debe
 * recoger todos los costes.
 */
class Case4ExtraCostsTest {

    private val result = TestFixtures.success(
        TestFixtures.input(
            pricePerKWh = 0.40,
            startFeeEur = 1.0,
            pricePerMinuteEur = 0.10,
        ),
    )

    @Test
    fun `cada partida se calcula por separado`() {
        // 40,4348 kWh x 0,40
        assertEquals(BigDecimal("16.17"), result.energyCostEur)
        assertEquals(BigDecimal("1.00"), result.startFeeEur)
        // 18 minutos x 0,10
        assertEquals(BigDecimal("1.80"), result.timeCostEur)
        assertEquals(Money.ZERO, result.parkingFeeEur)
        assertTrue(result.hasExtraCosts)
    }

    @Test
    fun `el total es exactamente la suma de las partidas mostradas`() {
        val suma = result.energyCostEur
            .add(result.startFeeEur)
            .add(result.timeCostEur)
            .add(result.parkingFeeEur)
        assertEquals(suma, result.totalCostEur)
        assertEquals(BigDecimal("18.97"), result.totalCostEur)
    }

    @Test
    fun `el coste por tiempo usa los minutos redondeados que ve el usuario`() {
        val esperado = BigDecimal.valueOf(result.minutes.toLong())
            .multiply(BigDecimal("0.10"))
            .setScale(2)
        assertEquals(esperado, result.timeCostEur)
    }

    @Test
    fun `el precio efectivo incluye todos los costes`() {
        assertEquals(BigDecimal("0.4000"), result.announcedPricePerKWh)
        // 18,97 / 40,4348 kWh
        assertEquals(BigDecimal("0.4692"), result.effectivePricePerKWh)
        assertTrue(result.effectivePricePerKWh > result.announcedPricePerKWh)
        assertEquals(BigDecimal("0.0692"), result.pricePremiumPerKWh)
    }

    @Test
    fun `se ofrecen las dos valoraciones y la principal es la efectiva`() {
        // Aquí ambos precios caen en la misma categoría, pero el efectivo es
        // sensiblemente peor y es el que manda en la valoración principal.
        assertEquals(PriceRating.NORMAL, result.announcedPriceRating)
        assertEquals(PriceRating.NORMAL, result.priceRating)
    }

    @Test
    fun `los costes adicionales pueden empeorar la categoria del precio`() {
        val conExtras = TestFixtures.success(
            TestFixtures.input(
                pricePerKWh = 0.35,
                startFeeEur = 1.0,
                pricePerMinuteEur = 0.10,
            ),
        )
        // 0,35 €/kWh es "buen precio"; con 1 € de inicio y 0,10 €/min el
        // precio efectivo sube a 0,4192 €/kWh y pasa a ser "normal".
        assertEquals(BigDecimal("16.95"), conExtras.totalCostEur)
        assertEquals(BigDecimal("0.4192"), conExtras.effectivePricePerKWh)
        assertEquals(PriceRating.BUEN_PRECIO, conExtras.announcedPriceRating)
        assertEquals(PriceRating.NORMAL, conExtras.priceRating)
    }

    @Test
    fun `el coste por minuto y por cien kilometros parten del total`() {
        // 18,97 / 18 min
        assertEquals(BigDecimal("1.05"), result.costPerMinuteEur)
        // 18,97 x 100 / 225,4545 km
        assertEquals(BigDecimal("8.41"), result.costPer100KmEur)
    }

    @Test
    fun `el coste de estacionamiento tambien entra en el total`() {
        val conParking = TestFixtures.success(
            TestFixtures.input(
                pricePerKWh = 0.40,
                startFeeEur = 1.0,
                pricePerMinuteEur = 0.10,
                parkingFeeEur = 2.50,
            ),
        )
        assertEquals(BigDecimal("2.50"), conParking.parkingFeeEur)
        assertEquals(BigDecimal("21.47"), conParking.totalCostEur)
        assertTrue(conParking.effectivePricePerKWh > result.effectivePricePerKWh)
    }
}
