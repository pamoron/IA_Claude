package com.pamoron.electroperico.domain.calc

import com.pamoron.electroperico.domain.model.ChargerRating
import com.pamoron.electroperico.domain.model.CurrentType
import com.pamoron.electroperico.domain.model.PriceRating
import com.pamoron.electroperico.domain.model.PriceThresholds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Valoración del precio y de la potencia del cargador. */
class RatingsTest {

    private val umbrales = PriceThresholds.DEFAULT

    // --- Precio ------------------------------------------------------------

    @Test
    fun `los umbrales de precio del enunciado se respetan`() {
        assertEquals(PriceRating.MUY_BARATO, umbrales.rate(0.10))
        assertEquals(PriceRating.MUY_BARATO, umbrales.rate(0.25))
        assertEquals(PriceRating.BUEN_PRECIO, umbrales.rate(0.26))
        assertEquals(PriceRating.BUEN_PRECIO, umbrales.rate(0.39))
        assertEquals(PriceRating.NORMAL, umbrales.rate(0.40))
        assertEquals(PriceRating.NORMAL, umbrales.rate(0.55))
        assertEquals(PriceRating.CARO, umbrales.rate(0.56))
        assertEquals(PriceRating.CARO, umbrales.rate(0.70))
        assertEquals(PriceRating.MUY_CARO, umbrales.rate(0.71))
        assertEquals(PriceRating.MUY_CARO, umbrales.rate(1.20))
    }

    @Test
    fun `unos umbrales personalizados cambian la valoracion`() {
        val estrictos = PriceThresholds(
            veryCheapMax = 0.20,
            goodMax = 0.30,
            normalMax = 0.40,
            expensiveMax = 0.50,
        )
        val resultado = TestFixtures.success(
            TestFixtures.input(pricePerKWh = 0.45),
            priceThresholds = estrictos,
        )
        assertEquals(PriceRating.CARO, resultado.priceRating)
    }

    @Test
    fun `los umbrales deben ser crecientes`() {
        assertTrue(PriceThresholds.DEFAULT.isValid())
        assertFalse(PriceThresholds(0.40, 0.30, 0.50, 0.60).isValid())
        assertFalse(PriceThresholds(0.0, 0.30, 0.50, 0.60).isValid())
    }

    // --- Potencia en continua ----------------------------------------------

    @Test
    fun `clasificacion de los cargadores de continua`() {
        assertEquals(ChargerRating.DC_LENTO, ratingDc(24.0))
        assertEquals(ChargerRating.DC_LENTO, ratingDc(49.9))
        assertEquals(ChargerRating.DC_ADECUADO, ratingDc(50.0))
        assertEquals(ChargerRating.DC_ADECUADO, ratingDc(99.0))
        assertEquals(ChargerRating.DC_MUY_ADECUADO, ratingDc(100.0))
        assertEquals(ChargerRating.DC_MUY_ADECUADO, ratingDc(179.0))
        assertEquals(ChargerRating.DC_SOBREDIMENSIONADO, ratingDc(180.0))
        assertEquals(ChargerRating.DC_SOBREDIMENSIONADO, ratingDc(400.0))
    }

    @Test
    fun `un cargador ligeramente por encima del limite no genera aviso`() {
        // 160 kW frente a 155 kW: la diferencia no merece un aviso.
        assertFalse(TestFixtures.success(TestFixtures.input(chargerPowerKw = 160.0)).isOversized)
        // 200 kW sí.
        assertTrue(TestFixtures.success(TestFixtures.input(chargerPowerKw = 200.0)).isOversized)
    }

    // --- Potencia en alterna -----------------------------------------------

    @Test
    fun `clasificacion de los cargadores de alterna`() {
        assertEquals(ChargerRating.AC_MUY_LENTO, ratingAc(2.3))
        assertEquals(ChargerRating.AC_MUY_LENTO, ratingAc(3.7))
        assertEquals(ChargerRating.AC_NORMAL, ratingAc(4.6))
        assertEquals(ChargerRating.AC_NORMAL, ratingAc(7.4))
        assertEquals(ChargerRating.AC_OPTIMO, ratingAc(9.0))
        assertEquals(ChargerRating.AC_OPTIMO, ratingAc(11.0))
        assertEquals(ChargerRating.AC_LIMITADO_POR_VEHICULO, ratingAc(22.0))
        assertEquals(ChargerRating.AC_LIMITADO_POR_VEHICULO, ratingAc(43.0))
    }

    @Test
    fun `la clasificacion en alterna se adapta al perfil del vehiculo`() {
        // Un coche con cargador de a bordo de 22 kW sí aprovecha un poste de 22 kW.
        val perfil22 = TestFixtures.vehicle.copy(maxAcPowerKw = 22.0)
        val resultado = TestFixtures.success(
            TestFixtures.input(currentType = CurrentType.AC, chargerPowerKw = 22.0),
            vehicle = perfil22,
        )
        assertEquals(ChargerRating.AC_OPTIMO, resultado.chargerRating)
        assertFalse(resultado.isOversized)
    }

    private fun ratingDc(kw: Double): ChargerRating =
        TestFixtures.success(TestFixtures.input(chargerPowerKw = kw)).chargerRating

    private fun ratingAc(kw: Double): ChargerRating =
        TestFixtures.success(
            TestFixtures.input(currentType = CurrentType.AC, chargerPowerKw = kw),
        ).chargerRating
}
