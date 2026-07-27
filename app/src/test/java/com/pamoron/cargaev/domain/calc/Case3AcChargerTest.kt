package com.pamoron.cargaev.domain.calc

import com.pamoron.cargaev.domain.calc.TestFixtures.EPSILON
import com.pamoron.cargaev.domain.model.ChargeWarning
import com.pamoron.cargaev.domain.model.ChargerRating
import com.pamoron.cargaev.domain.model.CurrentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Caso 3 del enunciado.
 *
 * Cargador de corriente alterna de 22 kW: el cargador de a bordo del coche
 * solo admite 11 kW.
 */
class Case3AcChargerTest {

    private val result = TestFixtures.success(
        TestFixtures.input(currentType = CurrentType.AC, chargerPowerKw = 22.0),
    )

    @Test
    fun `el vehiculo solo aprovecha once kilovatios`() {
        assertEquals(22.0, result.chargerPowerKw, EPSILON)
        assertEquals(11.0, result.vehicleMaxPowerKw, EPSILON)
        assertEquals(11.0, result.effectivePowerKw, EPSILON)
    }

    @Test
    fun `aparece el aviso de potencia desaprovechada`() {
        assertTrue(result.isOversized)
        assertTrue(result.warnings.contains(ChargeWarning.CARGADOR_SOBREDIMENSIONADO))
        assertEquals(ChargerRating.AC_LIMITADO_POR_VEHICULO, result.chargerRating)
    }

    @Test
    fun `en alterna se usa un unico tramo con potencia estable`() {
        assertEquals(1, result.segments.size)
        val tramo = result.segments.single()
        assertEquals(20.0, tramo.fromPercent, EPSILON)
        assertEquals(80.0, tramo.toPercent, EPSILON)
        // 11 kW x 0,92
        assertEquals(10.12, tramo.averagePowerKw, EPSILON)
        assertEquals(37.2, tramo.energyKWh, EPSILON)
    }

    @Test
    fun `el tiempo en alterna es mucho mayor que en continua`() {
        // 37,2 / 10,12 x 60
        assertEquals(220.553359683, result.rawMinutes, 1e-6)
        assertEquals(221, result.minutes)
    }

    @Test
    fun `en alterna se aplican las perdidas de alterna`() {
        assertEquals(0.12, result.lossFraction, EPSILON)
        // 37,2 / 0,88
        assertEquals(42.272727272, result.energyBilledKWh, 1e-6)
    }

    @Test
    fun `un cargador de once kilovatios es optimo y no dispara el aviso`() {
        val optimo = TestFixtures.success(
            TestFixtures.input(currentType = CurrentType.AC, chargerPowerKw = 11.0),
        )
        assertEquals(ChargerRating.AC_OPTIMO, optimo.chargerRating)
        assertTrue(!optimo.isOversized)
    }
}
