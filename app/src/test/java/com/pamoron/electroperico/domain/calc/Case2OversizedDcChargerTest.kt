package com.pamoron.electroperico.domain.calc

import com.pamoron.electroperico.domain.calc.TestFixtures.EPSILON
import com.pamoron.electroperico.domain.model.ChargeWarning
import com.pamoron.electroperico.domain.model.ChargerRating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Caso 2 del enunciado.
 *
 * Del 70 % al 100 % en un cargador DC de 300 kW: el coche solo puede aceptar
 * 155 kW y la velocidad debe desplomarse a partir del 80 %.
 */
class Case2OversizedDcChargerTest {

    private val result = TestFixtures.success(
        TestFixtures.input(
            chargerPowerKw = 300.0,
            startSocPercent = 70.0,
            targetSocPercent = 100.0,
        ),
    )

    @Test
    fun `la potencia queda limitada a la maxima del vehiculo`() {
        assertEquals(300.0, result.chargerPowerKw, EPSILON)
        assertEquals(155.0, result.vehicleMaxPowerKw, EPSILON)
        assertEquals(155.0, result.effectivePowerKw, EPSILON)
    }

    @Test
    fun `aparece el aviso de cargador sobredimensionado`() {
        assertTrue(result.isOversized)
        assertTrue(result.warnings.contains(ChargeWarning.CARGADOR_SOBREDIMENSIONADO))
        assertEquals(ChargerRating.DC_SOBREDIMENSIONADO, result.chargerRating)
    }

    @Test
    fun `aparece el aviso de carga lenta por encima del ochenta por ciento`() {
        assertTrue(result.warnings.contains(ChargeWarning.POR_ENCIMA_DEL_80))
    }

    @Test
    fun `la estimacion refleja la reduccion de velocidad despues del ochenta`() {
        assertEquals(3, result.segments.size)
        val (from70, from80, from90) = result.segments

        // Los tres tramos mueven la misma energía (10 puntos de SOC cada uno)...
        assertEquals(6.2, from70.energyKWh, EPSILON)
        assertEquals(6.2, from80.energyKWh, EPSILON)
        assertEquals(6.2, from90.energyKWh, EPSILON)

        // ...pero a potencias cada vez menores: 70 %, 40 % y 20 % de 155 kW.
        assertEquals(108.5, from70.averagePowerKw, EPSILON)
        assertEquals(62.0, from80.averagePowerKw, EPSILON)
        assertEquals(31.0, from90.averagePowerKw, EPSILON)

        // Y por tanto tardan cada vez más.
        assertEquals(3.428571428, from70.minutes, 1e-6)
        assertEquals(6.0, from80.minutes, EPSILON)
        assertEquals(12.0, from90.minutes, EPSILON)

        assertTrue(
            "El último 10 % debe tardar bastante más que el primero",
            from90.minutes > from70.minutes * 3,
        )
    }

    @Test
    fun `el tiempo total suma los tres tramos`() {
        assertEquals(21.428571428, result.rawMinutes, 1e-6)
        assertEquals(21, result.minutes)
    }

    @Test
    fun `un cargador de 150 kW tarda practicamente lo mismo que uno de 300 kW`() {
        val con150 = TestFixtures.success(
            TestFixtures.input(
                chargerPowerKw = 150.0,
                startSocPercent = 70.0,
                targetSocPercent = 100.0,
            ),
        )
        // Es el mensaje que debe transmitir la app: por encima del límite del
        // coche, más kW anunciados no significan una carga apreciablemente
        // más rápida.
        val diferencia = con150.rawMinutes - result.rawMinutes
        assertTrue("Diferencia inesperada: $diferencia min", diferencia < 1.5)
    }

    @Test
    fun `la energia se calcula sobre la capacidad util`() {
        assertEquals(18.6, result.energyNeededKWh, EPSILON)
        assertEquals(20.217391304, result.energyBilledKWh, 1e-6)
    }
}
