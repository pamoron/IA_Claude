package com.pamoron.cargaev.ui.format

import com.pamoron.cargaev.ui.format.Formatters.NBSP
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Formato español de importes, energía, potencia y tiempo. */
class FormattersTest {

    @Test
    fun `los importes usan coma decimal y espacio duro antes del euro`() {
        assertEquals("12,45${NBSP}€", Formatters.money(BigDecimal("12.45")))
        assertEquals("0,00${NBSP}€", Formatters.money(BigDecimal("0.00")))
        assertEquals("1.234,50${NBSP}€", Formatters.money(BigDecimal("1234.50")))
    }

    @Test
    fun `los precios unitarios se muestran con dos decimales`() {
        assertEquals("0,45${NBSP}€/kWh", Formatters.pricePerKWh(BigDecimal("0.4500")))
        assertEquals("0,47${NBSP}€/kWh", Formatters.pricePerKWh(BigDecimal("0.4692")))
    }

    @Test
    fun `las diferencias de precio llevan signo explicito`() {
        assertEquals("+0,07${NBSP}€/kWh", Formatters.signedPricePerKWh(BigDecimal("0.0692")))
        assertEquals("0,00${NBSP}€/kWh", Formatters.signedPricePerKWh(BigDecimal("0.0000")))
    }

    @Test
    fun `la energia se muestra con dos decimales`() {
        assertEquals("40,43${NBSP}kWh", Formatters.energy(40.434782608))
        assertEquals("6,20${NBSP}kWh", Formatters.energy(6.2))
    }

    @Test
    fun `la potencia se muestra sin decimales innecesarios`() {
        assertEquals("155${NBSP}kW", Formatters.power(155.0))
        assertEquals("7,4${NBSP}kW", Formatters.power(7.4))
        assertEquals("10,1${NBSP}kW", Formatters.power(10.12))
    }

    @Test
    fun `la distancia se redondea a kilometros enteros`() {
        assertEquals("225${NBSP}km", Formatters.distance(225.4545))
        assertEquals("1.010${NBSP}km", Formatters.distance(1009.6))
    }

    @Test
    fun `el tiempo se muestra en minutos y a partir de una hora en horas y minutos`() {
        assertEquals("27${NBSP}min", Formatters.duration(27))
        assertEquals("1${NBSP}min", Formatters.duration(1))
        assertEquals("59${NBSP}min", Formatters.duration(59))
        assertEquals("1${NBSP}h", Formatters.duration(60))
        assertEquals("1${NBSP}h 27${NBSP}min", Formatters.duration(87))
        assertEquals("3${NBSP}h 41${NBSP}min", Formatters.duration(221))
    }

    @Test
    fun `los porcentajes se muestran con la unidad separada`() {
        assertEquals("60${NBSP}%", Formatters.percent(60.0))
        assertEquals("12,5${NBSP}%", Formatters.percent(12.5))
    }

    @Test
    fun `la lectura de texto admite coma y punto decimal`() {
        assertEquals(0.45, Formatters.parseDecimal("0,45")!!, 1e-9)
        assertEquals(0.45, Formatters.parseDecimal("0.45")!!, 1e-9)
        assertEquals(150.0, Formatters.parseDecimal(" 150 ")!!, 1e-9)
    }

    @Test
    fun `el texto no numerico se rechaza`() {
        assertNull(Formatters.parseDecimal(""))
        assertNull(Formatters.parseDecimal("   "))
        assertNull(Formatters.parseDecimal("abc"))
        assertNull(Formatters.parseDecimal("0,4,5"))
    }

    @Test
    fun `los campos opcionales vacios valen cero`() {
        assertEquals(0.0, Formatters.parseOptionalDecimal("")!!, 1e-9)
        assertEquals(1.5, Formatters.parseOptionalDecimal("1,5")!!, 1e-9)
        assertNull(Formatters.parseOptionalDecimal("x"))
    }

    @Test
    fun `los valores se reescriben en los campos sin ceros sobrantes`() {
        assertEquals("", Formatters.toEditableText(0.0))
        assertEquals("0,45", Formatters.toEditableText(0.45))
        assertEquals("150", Formatters.toEditableText(150.0))
        assertEquals("62", Formatters.toEditableText(62.0))
        assertEquals("16,5", Formatters.toEditableText(16.5))
    }
}
