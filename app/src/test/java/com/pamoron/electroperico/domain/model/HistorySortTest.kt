package com.pamoron.electroperico.domain.model

import com.pamoron.electroperico.domain.calc.Money
import org.junit.Assert.assertEquals
import org.junit.Test

/** Ordenación del historial. */
class HistorySortTest {

    private fun entry(
        id: String,
        favorite: Boolean = false,
        timestamp: Long = 0L,
        cost: Double = 10.0,
        effective: Double = 0.40,
    ) = HistoryEntry(
        id = id,
        timestampMillis = timestamp,
        charger = ChargerOption(
            id = id,
            name = id,
            pricePerKWh = 0.40,
            chargerPowerKw = 150.0,
            currentType = CurrentType.DC,
            startSocPercent = 20,
            targetSocPercent = 80,
        ),
        totalCostEur = Money.eur(cost),
        minutes = 20,
        effectivePricePerKWh = Money.rate(effective),
        energyBilledKWh = 40.0,
        energyNeededKWh = 37.2,
        rangeAddedKm = 225.0,
        favorite = favorite,
    )

    private val antigua = entry("antigua", timestamp = 1_000L, cost = 5.0, effective = 0.20)
    private val reciente = entry("reciente", timestamp = 3_000L, cost = 30.0, effective = 0.70)
    private val favorita = entry("favorita", favorite = true, timestamp = 2_000L, cost = 20.0, effective = 0.50)

    private val todas = listOf(antigua, reciente, favorita)

    @Test
    fun `las recientes van primero y los favoritos por delante de todo`() {
        val orden = todas.orderedBy(HistorySort.RECIENTES).map { it.id }
        assertEquals(listOf("favorita", "reciente", "antigua"), orden)
    }

    @Test
    fun `ordena por coste total de menor a mayor`() {
        val orden = todas.orderedBy(HistorySort.MAS_BARATAS).map { it.id }
        assertEquals(listOf("antigua", "favorita", "reciente"), orden)
    }

    @Test
    fun `ordena por precio efectivo por kilovatio hora`() {
        val orden = todas.orderedBy(HistorySort.MEJOR_PRECIO_EFECTIVO).map { it.id }
        assertEquals(listOf("antigua", "favorita", "reciente"), orden)
    }

    @Test
    fun `al ordenar por coste el favorito no recibe trato especial`() {
        // Solo la vista de recientes prioriza favoritos; los demás criterios son
        // comparaciones puras, para que no engañen.
        val orden = todas.orderedBy(HistorySort.MAS_BARATAS).map { it.id }
        assertEquals("antigua", orden.first())
    }

    @Test
    fun `una lista vacia se ordena sin fallar`() {
        HistorySort.entries.forEach { criterio ->
            assertEquals(emptyList<HistoryEntry>(), emptyList<HistoryEntry>().orderedBy(criterio))
        }
    }

    @Test
    fun `la lectura de un criterio desconocido devuelve el predeterminado`() {
        assertEquals(HistorySort.MAS_BARATAS, HistorySort.fromName("MAS_BARATAS"))
        assertEquals(HistorySort.RECIENTES, HistorySort.fromName(null))
        assertEquals(HistorySort.RECIENTES, HistorySort.fromName("OTRO"))
    }

    @Test
    fun `la entrada se construye a partir de una simulacion calculada`() {
        val charger = ChargerOption(
            id = "x",
            name = "Iberdrola",
            pricePerKWh = 0.45,
            chargerPowerKw = 150.0,
            currentType = CurrentType.DC,
            startSocPercent = 20,
            targetSocPercent = 80,
        )
        val outcome = com.pamoron.electroperico.domain.calc.ChargeCalculator.calculate(
            input = charger.toChargeInput(),
            vehicle = VehicleProfile.BYD_ATTO_2_COMFORT,
        ) as CalculationOutcome.Success

        val entrada = HistoryEntry.from(
            id = "x",
            charger = charger,
            result = outcome.result,
            timestampMillis = 12345L,
        )

        assertEquals("Iberdrola", entrada.operatorName)
        assertEquals(12345L, entrada.timestampMillis)
        assertEquals(outcome.result.totalCostEur, entrada.totalCostEur)
        assertEquals(outcome.result.minutes, entrada.minutes)
        assertEquals(outcome.result.effectivePricePerKWh, entrada.effectivePricePerKWh)
        assertEquals(false, entrada.favorite)
    }
}
