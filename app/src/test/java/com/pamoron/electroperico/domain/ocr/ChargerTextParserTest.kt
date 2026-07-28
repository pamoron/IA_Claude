package com.pamoron.electroperico.domain.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChargerTextParserTest {

    @Test
    fun `lee precio potencia y coste por minuto con formato espanol`() {
        val values = ChargerTextParser.parse("Tarifa 0,45 €/kWh · 150 kW · 0,10 €/min")

        assertEquals(0.45, values.pricePerKWh!!, 0.0001)
        assertEquals(150.0, values.chargerPowerKw!!, 0.0001)
        assertEquals(0.10, values.pricePerMinuteEur!!, 0.0001)
    }

    @Test
    fun `admite punto decimal y EUR sin barra`() {
        val values = ChargerTextParser.parse("DC 50kW - 0.39 EUR/kWh")

        assertEquals(0.39, values.pricePerKWh!!, 0.0001)
        assertEquals(50.0, values.chargerPowerKw!!, 0.0001)
        assertNull(values.pricePerMinuteEur)
    }

    @Test
    fun `descarta valores fuera de un rango razonable`() {
        val values = ChargerTextParser.parse("99 €/kWh · 0 kW · 0,55 €/kWh · 300 kW")

        assertEquals(0.55, values.pricePerKWh!!, 0.0001)
        assertEquals(300.0, values.chargerPowerKw!!, 0.0001)
    }
}
