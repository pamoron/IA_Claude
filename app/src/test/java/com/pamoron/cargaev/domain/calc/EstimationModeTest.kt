package com.pamoron.cargaev.domain.calc

import com.pamoron.cargaev.domain.model.CurrentType
import com.pamoron.cargaev.domain.model.EstimationMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Efecto del selector optimista / normal / conservadora sobre el tiempo. */
class EstimationModeTest {

    private val entrada = TestFixtures.input()

    private val normal = TestFixtures.success(entrada, estimationMode = EstimationMode.NORMAL)

    @Test
    fun `el modo optimista reduce el tiempo un diez por ciento`() {
        val optimista = TestFixtures.success(entrada, estimationMode = EstimationMode.OPTIMISTA)
        assertEquals(normal.rawMinutes * 0.90, optimista.rawMinutes, 1e-9)
        assertEquals(16, optimista.minutes)
    }

    @Test
    fun `el modo conservador aumenta el tiempo un quince por ciento`() {
        val conservadora = TestFixtures.success(entrada, estimationMode = EstimationMode.CONSERVADORA)
        assertEquals(normal.rawMinutes * 1.15, conservadora.rawMinutes, 1e-9)
        assertEquals(21, conservadora.minutes)
    }

    @Test
    fun `el modo no altera la energia ni el coste de la energia`() {
        val conservadora = TestFixtures.success(entrada, estimationMode = EstimationMode.CONSERVADORA)
        assertEquals(normal.energyNeededKWh, conservadora.energyNeededKWh, 1e-9)
        assertEquals(normal.energyBilledKWh, conservadora.energyBilledKWh, 1e-9)
        assertEquals(normal.energyCostEur, conservadora.energyCostEur)
    }

    @Test
    fun `el modo si altera el coste cuando el operador cobra por minuto`() {
        val base = TestFixtures.input(pricePerMinuteEur = 0.10)
        val optimista = TestFixtures.success(base, estimationMode = EstimationMode.OPTIMISTA)
        val conservadora = TestFixtures.success(base, estimationMode = EstimationMode.CONSERVADORA)
        assertTrue(conservadora.timeCostEur > optimista.timeCostEur)
        assertTrue(conservadora.totalCostEur > optimista.totalCostEur)
    }

    @Test
    fun `el factor tambien se aplica en corriente alterna`() {
        val entradaAc = TestFixtures.input(currentType = CurrentType.AC, chargerPowerKw = 11.0)
        val acNormal = TestFixtures.success(entradaAc, estimationMode = EstimationMode.NORMAL)
        val acConservadora = TestFixtures.success(entradaAc, estimationMode = EstimationMode.CONSERVADORA)
        assertEquals(acNormal.rawMinutes * 1.15, acConservadora.rawMinutes, 1e-9)
    }

    @Test
    fun `los factores del enunciado son los configurados`() {
        assertEquals(0.90, EstimationMode.OPTIMISTA.factor, 1e-9)
        assertEquals(1.00, EstimationMode.NORMAL.factor, 1e-9)
        assertEquals(1.15, EstimationMode.CONSERVADORA.factor, 1e-9)
        assertEquals(EstimationMode.NORMAL, EstimationMode.DEFAULT)
    }

    @Test
    fun `la lectura de un modo desconocido devuelve el predeterminado`() {
        assertEquals(EstimationMode.CONSERVADORA, EstimationMode.fromName("CONSERVADORA"))
        assertEquals(EstimationMode.NORMAL, EstimationMode.fromName(null))
        assertEquals(EstimationMode.NORMAL, EstimationMode.fromName("VALOR_ANTIGUO"))
    }
}
