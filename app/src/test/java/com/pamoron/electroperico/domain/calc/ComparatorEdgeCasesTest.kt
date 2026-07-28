package com.pamoron.electroperico.domain.calc

import com.pamoron.electroperico.domain.model.ChargerOption
import com.pamoron.electroperico.domain.model.ComparisonSort
import com.pamoron.electroperico.domain.model.CurrentType
import com.pamoron.electroperico.domain.model.ValidationError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Casos límite del comparador que [ChargerComparatorTest] no cubre: empates
 * exactos y cómo se desempatan, el comportamiento cuando hay más opciones de
 * las que caben en el comparador, varios errores de validación a la vez, y el
 * mapeo de [ChargerOption] hacia la entrada del motor de cálculo.
 */
class ComparatorEdgeCasesTest {

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

    // --- Empates exactos y sus desempates --------------------------------------

    @Test
    fun `un empate exacto en coste se desempata por el tiempo en el distintivo de mas barato`() {
        // Mismo precio y mismo rango de batería: el coste de la energía es
        // idéntico. Solo cambia la potencia, que no afecta al coste porque no
        // hay coste por minuto, así que el empate en coste es exacto.
        val lenta = option("lenta", pricePerKWh = 0.40, chargerPowerKw = 50.0)
        val rapida = option("rapida", pricePerKWh = 0.40, chargerPowerKw = 150.0)
        val c = ChargerComparator.compare(listOf(lenta, rapida), vehicle)

        val costeLenta = c.find("lenta")!!.result.totalCostEur
        val costeRapida = c.find("rapida")!!.result.totalCostEur
        assertEquals(0, costeLenta.compareTo(costeRapida))
        assertTrue(c.find("rapida")!!.result.minutes < c.find("lenta")!!.result.minutes)
        // Con el coste empatado, el distintivo de "más barato" debe ir a la más rápida.
        assertEquals("rapida", c.cheapestId)
    }

    @Test
    fun `un empate exacto en tiempo se desempata por el coste en el distintivo de mas rapido`() {
        // Misma potencia y mismo rango de batería: el tiempo es idéntico.
        // Solo cambia el precio, que no afecta al tiempo de carga.
        val cara = option("cara", pricePerKWh = 0.60, chargerPowerKw = 100.0)
        val barata = option("barata", pricePerKWh = 0.30, chargerPowerKw = 100.0)
        val c = ChargerComparator.compare(listOf(cara, barata), vehicle)

        assertEquals(c.find("cara")!!.result.minutes, c.find("barata")!!.result.minutes)
        assertTrue(
            c.find("barata")!!.result.totalCostEur < c.find("cara")!!.result.totalCostEur,
        )
        // Con el tiempo empatado, el distintivo de "más rápido" debe ir a la más barata.
        assertEquals("barata", c.fastestId)
    }

    @Test
    fun `dos opciones identicas salvo el nombre se ordenan alfabeticamente sin distinguir mayusculas`() {
        // Nombres elegidos a propósito: una comparación de cadenas sensible a
        // mayúsculas colocaría "ENDESA" antes que "acciona" (la 'E' mayúscula
        // vale menos que cualquier minúscula en ASCII), justo al revés del
        // orden alfabético real.
        val endesa = option("endesa-id", name = "ENDESA")
        val acciona = option("acciona-id", name = "acciona")
        val orden = ChargerComparator
            .compare(listOf(endesa, acciona), vehicle, ComparisonSort.MAS_BARATO)
            .options.map { it.option.name }
        assertEquals(listOf("acciona", "ENDESA"), orden)
    }

    @Test
    fun `tres opciones en empate total se ordenan por nombre sea cual sea el criterio`() {
        val zeta = option("z", name = "Zeta")
        val beta = option("b", name = "Beta")
        val alfa = option("a", name = "Alfa")
        ComparisonSort.entries.forEach { sort ->
            val orden = ChargerComparator
                .compare(listOf(zeta, beta, alfa), vehicle, sort)
                .options.map { it.option.name }
            assertEquals("Orden inesperado para $sort", listOf("Alfa", "Beta", "Zeta"), orden)
        }
    }

    @Test
    fun `con un empate total entre varias opciones el destacado es la primera de la lista recibida`() {
        // Todas idénticas: al no haber nada que las distinga, los destacados
        // deben ser estables y corresponder a la primera opción de la lista
        // recibida (que es el orden en que se guardaron), no a la primera del
        // orden alfabético ni de ningún otro criterio de vista.
        val primera = option("primera")
        val segunda = option("segunda")
        val tercera = option("tercera")
        val c = ChargerComparator.compare(listOf(primera, segunda, tercera), vehicle)
        assertEquals("primera", c.cheapestId)
        assertEquals("primera", c.fastestId)
        assertEquals("primera", c.bestOverallId)
    }

    // --- Límite de opciones -----------------------------------------------------

    @Test
    fun `el comparador en si no impone el limite de cinco opciones, eso es cosa del repositorio`() {
        // ChargerComparator.compare() es puro y no conoce límites de
        // almacenamiento: calcula lo que le llega. El límite de
        // ChargerOption.MAX_OPTIONS se aplica en ComparatorRepository.upsert(),
        // no aquí, así que seis opciones válidas deben calcularse las seis.
        val seis = (1..6).map { option("op$it", pricePerKWh = 0.30 + it * 0.05) }
        val c = ChargerComparator.compare(seis, vehicle)
        assertEquals(6, c.options.size)
        assertTrue(c.invalidOptions.isEmpty())
    }

    // --- Varios errores de validación a la vez ----------------------------------

    @Test
    fun `una opcion con varios problemas a la vez acumula todos los errores, no solo el primero`() {
        val rota = option("rota", pricePerKWh = -1.0, chargerPowerKw = -10.0)
        val c = ChargerComparator.compare(listOf(rota), vehicle)
        val errores = c.invalidOptions.single().errors
        assertTrue(ValidationError.PRECIO_NEGATIVO in errores)
        assertTrue(ValidationError.POTENCIA_CARGADOR_INVALIDA in errores)
        assertTrue("Debe acumular más de un error: $errores", errores.size >= 2)
    }

    @Test
    fun `un objetivo no superior al inicial se detecta aunque ambos porcentajes sean validos`() {
        val invertida = option("invertida", startSoc = 80, targetSoc = 20)
        val c = ChargerComparator.compare(listOf(invertida), vehicle)
        assertEquals(
            listOf(ValidationError.OBJETIVO_NO_SUPERIOR_AL_ACTUAL),
            c.invalidOptions.single().errors,
        )
    }

    @Test
    fun `varias opciones invalidas mantienen el orden en que llegaron`() {
        val validaA = option("validaA")
        val invalidaB = option("invalidaB", chargerPowerKw = 0.0)
        val validaC = option("validaC")
        val invalidaD = option("invalidaD", chargerPowerKw = -1.0)
        val c = ChargerComparator.compare(listOf(validaA, invalidaB, validaC, invalidaD), vehicle)
        assertEquals(2, c.options.size)
        assertEquals(
            listOf("invalidaB", "invalidaD"),
            c.invalidOptions.map { it.option.id },
        )
    }

    // --- Comparison.find ----------------------------------------------------------

    @Test
    fun `find devuelve null para un identificador que no existe`() {
        val c = ChargerComparator.compare(listOf(option("unica")), vehicle)
        assertNull(c.find("no-existe"))
    }

    // --- ChargerOption a ChargeInput ------------------------------------------------

    @Test
    fun `toChargeInput traslada los porcentajes enteros a double sin perder el resto de campos`() {
        val opcion = option(
            "op",
            pricePerKWh = 0.55,
            chargerPowerKw = 22.0,
            currentType = CurrentType.AC,
            startSoc = 10,
            targetSoc = 90,
            startFee = 1.5,
            perMinute = 0.1,
            parking = 2.0,
        )
        val input = opcion.toChargeInput()
        assertEquals(0.55, input.pricePerKWh, TestFixtures.EPSILON)
        assertEquals(22.0, input.chargerPowerKw, TestFixtures.EPSILON)
        assertEquals(CurrentType.AC, input.currentType)
        assertEquals(10.0, input.startSocPercent, TestFixtures.EPSILON)
        assertEquals(90.0, input.targetSocPercent, TestFixtures.EPSILON)
        assertEquals(1.5, input.startFeeEur, TestFixtures.EPSILON)
        assertEquals(0.1, input.pricePerMinuteEur, TestFixtures.EPSILON)
        assertEquals(2.0, input.parkingFeeEur, TestFixtures.EPSILON)
    }
}
