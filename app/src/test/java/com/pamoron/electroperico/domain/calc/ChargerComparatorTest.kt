package com.pamoron.electroperico.domain.calc

import com.pamoron.electroperico.domain.model.ChargerOption
import com.pamoron.electroperico.domain.model.ComparisonSort
import com.pamoron.electroperico.domain.model.CurrentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Comparador de opciones de recarga. */
class ChargerComparatorTest {

    private val vehicle = TestFixtures.vehicle

    private fun option(
        id: String,
        name: String = id,
        pricePerKWh: Double = 0.45,
        chargerPowerKw: Double = 150.0,
        currentType: CurrentType = CurrentType.DC,
        startSoc: Int = 20,
        targetSoc: Int = 80,
        startFee: Double = 0.0,
        perMinute: Double = 0.0,
        parking: Double = 0.0,
    ) = ChargerOption(
        id = id,
        name = name,
        pricePerKWh = pricePerKWh,
        chargerPowerKw = chargerPowerKw,
        currentType = currentType,
        startSocPercent = startSoc,
        targetSocPercent = targetSoc,
        startFeeEur = startFee,
        pricePerMinuteEur = perMinute,
        parkingFeeEur = parking,
    )

    // Barato pero lento; caro pero rápido; intermedio.
    private val barato = option("barato", pricePerKWh = 0.29, chargerPowerKw = 50.0)
    private val rapido = option("rapido", pricePerKWh = 0.69, chargerPowerKw = 150.0)
    private val medio = option("medio", pricePerKWh = 0.45, chargerPowerKw = 100.0)

    private val comparacion = ChargerComparator.compare(
        options = listOf(barato, rapido, medio),
        vehicle = vehicle,
    )

    @Test
    fun `calcula todas las opciones y ninguna queda invalida`() {
        assertEquals(3, comparacion.options.size)
        assertTrue(comparacion.invalidOptions.isEmpty())
        assertTrue(comparacion.isComparable)
    }

    @Test
    fun `detecta la mas barata y la mas rapida`() {
        assertEquals("barato", comparacion.cheapestId)
        assertEquals("rapido", comparacion.fastestId)
    }

    @Test
    fun `la puntuacion de coste vale uno para la mas barata y cero para la mas cara`() {
        assertEquals(1.0, comparacion.find("barato")!!.costScore, 1e-9)
        assertEquals(0.0, comparacion.find("rapido")!!.costScore, 1e-9)
        val intermedia = comparacion.find("medio")!!.costScore
        assertTrue("Debe quedar entre las otras dos: $intermedia", intermedia in 0.0..1.0)
    }

    @Test
    fun `la puntuacion de tiempo vale uno para la mas rapida y cero para la mas lenta`() {
        assertEquals(1.0, comparacion.find("rapido")!!.timeScore, 1e-9)
        assertEquals(0.0, comparacion.find("barato")!!.timeScore, 1e-9)
    }

    @Test
    fun `el equilibrio pondera el coste al sesenta por ciento y el tiempo al cuarenta`() {
        assertEquals(0.60, ChargerComparator.COST_WEIGHT, 1e-9)
        assertEquals(0.40, ChargerComparator.TIME_WEIGHT, 1e-9)
        comparacion.options.forEach { o ->
            val esperado = 0.60 * o.costScore + 0.40 * o.timeScore
            assertEquals(esperado, o.balanceScore, 1e-9)
        }
    }

    @Test
    fun `el equilibrio premia a la opcion intermedia frente a los dos extremos`() {
        // Los extremos solo puntúan en su eje:
        //   barato: 0,60 x 1,00 + 0,40 x 0,00 = 0,600
        //   rapido: 0,60 x 0,00 + 0,40 x 1,00 = 0,400
        assertEquals(0.600, comparacion.find("barato")!!.balanceScore, 1e-9)
        assertEquals(0.400, comparacion.find("rapido")!!.balanceScore, 1e-9)

        // La intermedia puntúa bien en los dos y por eso gana:
        //   medio:  0,60 x 0,5999 + 0,40 x 0,7568 = 0,663
        val medio = comparacion.find("medio")!!
        assertEquals(0.5999, medio.costScore, 1e-4)
        assertEquals(0.7568, medio.timeScore, 1e-4)
        assertEquals(0.6626, medio.balanceScore, 1e-4)

        // Es justo lo que debe hacer un equilibrio: ni lo más barato y lentísimo,
        // ni lo más rápido y carísimo.
        assertEquals("medio", comparacion.bestOverallId)
        assertTrue(medio.balanceScore > comparacion.find("barato")!!.balanceScore)
        assertTrue(medio.balanceScore > comparacion.find("rapido")!!.balanceScore)
    }

    @Test
    fun `ordena por mas barato`() {
        val orden = ChargerComparator
            .compare(listOf(rapido, medio, barato), vehicle, ComparisonSort.MAS_BARATO)
            .options.map { it.option.id }
        assertEquals(listOf("barato", "medio", "rapido"), orden)
    }

    @Test
    fun `ordena por mas rapido`() {
        val orden = ChargerComparator
            .compare(listOf(barato, medio, rapido), vehicle, ComparisonSort.MAS_RAPIDO)
            .options.map { it.option.id }
        assertEquals(listOf("rapido", "medio", "barato"), orden)
    }

    @Test
    fun `ordena por mejor equilibrio de mayor a menor puntuacion`() {
        val ordenada = ChargerComparator
            .compare(listOf(rapido, barato, medio), vehicle, ComparisonSort.MEJOR_EQUILIBRIO)
            .options
        val puntuaciones = ordenada.map { it.balanceScore }
        assertEquals(puntuaciones.sortedDescending(), puntuaciones)
        assertEquals(listOf("medio", "barato", "rapido"), ordenada.map { it.option.id })
    }

    @Test
    fun `con una sola opcion todas las puntuaciones son maximas`() {
        val una = ChargerComparator.compare(listOf(medio), vehicle)
        assertEquals(1, una.options.size)
        assertFalse(una.isComparable)
        val sola = una.options.single()
        assertEquals(1.0, sola.costScore, 1e-9)
        assertEquals(1.0, sola.timeScore, 1e-9)
        assertEquals(1.0, sola.balanceScore, 1e-9)
        assertEquals("medio", una.cheapestId)
        assertEquals("medio", una.fastestId)
        assertEquals("medio", una.bestOverallId)
    }

    @Test
    fun `con opciones identicas nadie queda penalizado`() {
        val a = option("a")
        val b = option("b")
        val empate = ChargerComparator.compare(listOf(a, b), vehicle)
        empate.options.forEach {
            assertEquals(1.0, it.costScore, 1e-9)
            assertEquals(1.0, it.timeScore, 1e-9)
            assertEquals(1.0, it.balanceScore, 1e-9)
        }
    }

    @Test
    fun `las opciones que no se pueden calcular se apartan sin romper la comparacion`() {
        val rota = option("rota", chargerPowerKw = 0.0)
        val con = ChargerComparator.compare(listOf(barato, rota, rapido), vehicle)
        assertEquals(2, con.options.size)
        assertEquals(1, con.invalidOptions.size)
        assertEquals("rota", con.invalidOptions.single().option.id)
        assertTrue(con.invalidOptions.single().errors.isNotEmpty())
        assertEquals("barato", con.cheapestId)
    }

    @Test
    fun `si ninguna opcion es valida no hay destacados`() {
        val vacia = ChargerComparator.compare(listOf(option("x", chargerPowerKw = 0.0)), vehicle)
        assertTrue(vacia.options.isEmpty())
        assertEquals(1, vacia.invalidOptions.size)
        assertNull(vacia.cheapestId)
        assertNull(vacia.fastestId)
        assertNull(vacia.bestOverallId)
    }

    @Test
    fun `una lista vacia devuelve una comparacion vacia`() {
        val vacia = ChargerComparator.compare(emptyList(), vehicle)
        assertTrue(vacia.options.isEmpty())
        assertTrue(vacia.invalidOptions.isEmpty())
        assertNull(vacia.bestOverallId)
    }

    @Test
    fun `los costes adicionales cuentan en la comparacion`() {
        val sinExtras = option("sin", pricePerKWh = 0.40)
        val conExtras = option("con", pricePerKWh = 0.40, startFee = 3.0, perMinute = 0.20)
        val c = ChargerComparator.compare(listOf(sinExtras, conExtras), vehicle)
        assertEquals("sin", c.cheapestId)
        assertTrue(
            c.find("con")!!.result.totalCostEur > c.find("sin")!!.result.totalCostEur,
        )
    }

    @Test
    fun `compara correctamente opciones con objetivos de bateria distintos`() {
        val hasta80 = option("hasta80", targetSoc = 80)
        val hasta100 = option("hasta100", targetSoc = 100)
        val c = ChargerComparator.compare(listOf(hasta80, hasta100), vehicle)
        // Cargar hasta el 100 % cuesta más y tarda mucho más.
        assertEquals("hasta80", c.cheapestId)
        assertEquals("hasta80", c.fastestId)
        assertEquals("hasta80", c.bestOverallId)
        assertTrue(
            c.find("hasta100")!!.result.minutes > c.find("hasta80")!!.result.minutes * 2,
        )
    }

    @Test
    fun `mezcla alterna y continua en la misma comparacion`() {
        val ac = option("ac", chargerPowerKw = 22.0, currentType = CurrentType.AC, pricePerKWh = 0.25)
        val dc = option("dc", chargerPowerKw = 150.0, pricePerKWh = 0.59)
        val c = ChargerComparator.compare(listOf(ac, dc), vehicle)
        assertEquals("ac", c.cheapestId)
        assertEquals("dc", c.fastestId)
        // El poste de alterna queda limitado a los 11 kW del coche.
        assertEquals(11.0, c.find("ac")!!.result.effectivePowerKw, 1e-9)
    }

    @Test
    fun `el limite de opciones del comparador es cinco`() {
        assertEquals(5, ChargerOption.MAX_OPTIONS)
    }

    @Test
    fun `la lectura de un criterio desconocido devuelve el predeterminado`() {
        assertEquals(ComparisonSort.MAS_BARATO, ComparisonSort.fromName("MAS_BARATO"))
        assertEquals(ComparisonSort.MEJOR_EQUILIBRIO, ComparisonSort.fromName(null))
        assertEquals(ComparisonSort.MEJOR_EQUILIBRIO, ComparisonSort.fromName("OTRO"))
    }
}
