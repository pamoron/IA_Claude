package com.pamoron.cargaev.domain.calc

import com.pamoron.cargaev.domain.calc.TestFixtures.EPSILON
import com.pamoron.cargaev.domain.model.ChargeWarning
import com.pamoron.cargaev.domain.model.ChargerRating
import com.pamoron.cargaev.domain.model.CurrentType
import com.pamoron.cargaev.domain.model.PriceRating
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Caso 1 del enunciado.
 *
 * Capacidad útil 62 kWh, del 20 % al 80 %, 0,45 €/kWh, cargador DC de 150 kW
 * y pérdidas del 8 %.
 */
class Case1BasicDcSessionTest {

    private val result = TestFixtures.success(TestFixtures.input())

    @Test
    fun `la energia necesaria es la capacidad util por el salto de porcentaje`() {
        // 62 kWh x 60 / 100
        assertEquals(37.2, result.energyNeededKWh, EPSILON)
        assertEquals(60.0, result.socAddedPercent, EPSILON)
    }

    @Test
    fun `la energia facturada incluye el 8 por ciento de perdidas`() {
        // 37,2 / 0,92
        assertEquals(40.434782608, result.energyBilledKWh, 1e-6)
        assertEquals(0.08, result.lossFraction, EPSILON)
        assertTrue(
            "La energía facturada debe superar a la que entra en la batería",
            result.energyBilledKWh > result.energyNeededKWh,
        )
    }

    @Test
    fun `el coste es la energia facturada por el precio anunciado`() {
        // 40,4348 x 0,45 = 18,1957 -> 18,20 €
        assertEquals(BigDecimal("18.20"), result.energyCostEur)
        assertEquals(BigDecimal("18.20"), result.totalCostEur)
    }

    @Test
    fun `sin costes adicionales las partidas opcionales son cero`() {
        assertEquals(Money.ZERO, result.startFeeEur)
        assertEquals(Money.ZERO, result.timeCostEur)
        assertEquals(Money.ZERO, result.parkingFeeEur)
        assertFalse(result.hasExtraCosts)
    }

    @Test
    fun `el tiempo se reparte en los tres tramos atravesados`() {
        assertEquals(3, result.segments.size)

        val (first, second, third) = result.segments

        // Tramo 20-30 %: 6,2 kWh a 150 x 0,85 = 127,5 kW
        assertEquals(20.0, first.fromPercent, EPSILON)
        assertEquals(30.0, first.toPercent, EPSILON)
        assertEquals(6.2, first.energyKWh, EPSILON)
        assertEquals(127.5, first.averagePowerKw, EPSILON)
        assertEquals(2.917647058, first.minutes, 1e-6)

        // Tramo 30-60 %: 18,6 kWh a 150 x 0,90 = 135 kW
        assertEquals(30.0, second.fromPercent, EPSILON)
        assertEquals(60.0, second.toPercent, EPSILON)
        assertEquals(18.6, second.energyKWh, EPSILON)
        assertEquals(135.0, second.averagePowerKw, EPSILON)
        assertEquals(8.266666666, second.minutes, 1e-6)

        // Tramo 60-80 %: 12,4 kWh a 150 x 0,70 = 105 kW
        assertEquals(60.0, third.fromPercent, EPSILON)
        assertEquals(80.0, third.toPercent, EPSILON)
        assertEquals(12.4, third.energyKWh, EPSILON)
        assertEquals(105.0, third.averagePowerKw, EPSILON)
        assertEquals(7.085714285, third.minutes, 1e-6)
    }

    @Test
    fun `el tiempo total es la suma de los tramos redondeada a minutos`() {
        assertEquals(18.270028011, result.rawMinutes, 1e-6)
        assertEquals(18, result.minutes)
    }

    @Test
    fun `la energia de los tramos suma la energia necesaria`() {
        assertEquals(result.energyNeededKWh, result.segments.sumOf { it.energyKWh }, 1e-9)
    }

    @Test
    fun `el tiempo por tramos es mayor que dividir la energia entre la potencia maxima`() {
        // Es justo el motivo de calcular por tramos: 37,2 / 150 = 14,88 min.
        val naiveMinutes = result.energyNeededKWh / result.effectivePowerKw * 60.0
        assertTrue(
            "La estimación por tramos debe ser más conservadora que la ingenua",
            result.rawMinutes > naiveMinutes,
        )
    }

    @Test
    fun `la autonomia anadida usa el consumo medio del perfil`() {
        // 37,2 / 16,5 x 100
        assertEquals(225.454545454, result.rangeAddedKm, 1e-6)
    }

    @Test
    fun `el coste por cien kilometros y por minuto son coherentes con el total`() {
        // 18,20 x 100 / 225,4545
        assertEquals(BigDecimal("8.07"), result.costPer100KmEur)
        // 18,20 / 18 minutos
        assertEquals(BigDecimal("1.01"), result.costPerMinuteEur)
    }

    @Test
    fun `sin costes adicionales el precio efectivo coincide con el anunciado`() {
        assertEquals(BigDecimal("0.4500"), result.announcedPricePerKWh)
        // Solo difiere por el redondeo a céntimos del coste de la energía.
        assertEquals(BigDecimal("0.4501"), result.effectivePricePerKWh)
    }

    @Test
    fun `la potencia efectiva y las valoraciones son las esperadas`() {
        assertEquals(150.0, result.effectivePowerKw, EPSILON)
        assertEquals(155.0, result.vehicleMaxPowerKw, EPSILON)
        assertFalse(result.isOversized)
        assertEquals(ChargerRating.DC_MUY_ADECUADO, result.chargerRating)
        assertEquals(PriceRating.NORMAL, result.priceRating)
    }

    @Test
    fun `un objetivo del ochenta por ciento no dispara el aviso de carga lenta`() {
        assertFalse(result.warnings.contains(ChargeWarning.POR_ENCIMA_DEL_80))
        assertFalse(result.warnings.contains(ChargeWarning.CARGADOR_SOBREDIMENSIONADO))
    }

    @Test
    fun `los avisos generales acompanan siempre al resultado`() {
        assertTrue(result.warnings.contains(ChargeWarning.POTENCIA_NO_GARANTIZADA))
        assertTrue(result.warnings.contains(ChargeWarning.POTENCIA_COMPARTIDA))
        assertTrue(result.warnings.contains(ChargeWarning.COMPROBAR_TARIFAS))
        assertTrue(result.warnings.contains(ChargeWarning.PRECIOS_APP_Y_TARJETA))
        assertTrue(result.warnings.contains(ChargeWarning.PERDIDAS_DE_CARGA))
    }

    @Test
    fun `el tipo de corriente elegido determina las perdidas aplicadas`() {
        val ac = TestFixtures.success(
            TestFixtures.input(currentType = CurrentType.AC, chargerPowerKw = 11.0),
        )
        assertEquals(0.12, ac.lossFraction, EPSILON)
        assertEquals(0.08, result.lossFraction, EPSILON)
    }
}
