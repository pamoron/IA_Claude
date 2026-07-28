package com.pamoron.electroperico.domain.calc

import com.pamoron.electroperico.domain.calc.TestFixtures.EPSILON
import com.pamoron.electroperico.domain.model.ChargerRating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas de casos límite del motor de cálculo.
 *
 * Cubren bordes exactos de los tramos de la curva de carga, fronteras de
 * clasificación y entradas degeneradas (NaN / infinito) que la validación
 * actual no cubre.
 *
 * Varias pruebas de este fichero documentan defectos reales encontrados
 * durante la revisión (se explican en el comentario de cada una). No se
 * corrigen aquí: el encargo de esta fase es detectarlos y dejar constancia
 * del comportamiento actual con un test que lo reproduce; la corrección
 * pertenece a otra fase.
 */
class EdgeCasesTest {

    // -----------------------------------------------------------------------
    // DEFECTO REAL: la validación de los costes adicionales no detecta NaN.
    //
    // ChargeCalculator.kt (función `validate`, la comprobación de
    // startFeeEur/pricePerMinuteEur/parkingFeeEur) solo descarta valores
    // negativos con "< 0.0". Como cualquier comparación con NaN devuelve
    // false, un coste NaN atraviesa la validación sin generar
    // ValidationError.COSTE_ADICIONAL_NEGATIVO. Más adelante Money.eur() /
    // Money.eurFromProduct() llaman a BigDecimal.valueOf(NaN), que lanza
    // NumberFormatException porque BigDecimal no sabe interpretar la cadena
    // "NaN". El resultado es que una entrada inválida no produce un
    // CalculationOutcome.Failure controlado, sino una excepción no capturada
    // que llegaría intacta hasta la UI.
    // -----------------------------------------------------------------------

    @Test(expected = NumberFormatException::class)
    fun `un coste de inicio NaN no lo detecta la validacion y rompe el calculo`() {
        ChargeCalculator.calculate(
            TestFixtures.input(startFeeEur = Double.NaN),
            TestFixtures.vehicle,
        )
    }

    @Test(expected = NumberFormatException::class)
    fun `un precio por minuto NaN no lo detecta la validacion y rompe el calculo`() {
        ChargeCalculator.calculate(
            TestFixtures.input(pricePerMinuteEur = Double.NaN),
            TestFixtures.vehicle,
        )
    }

    @Test(expected = NumberFormatException::class)
    fun `un coste de estacionamiento NaN no lo detecta la validacion y rompe el calculo`() {
        ChargeCalculator.calculate(
            TestFixtures.input(parkingFeeEur = Double.NaN),
            TestFixtures.vehicle,
        )
    }

    // -----------------------------------------------------------------------
    // DEFECTO REAL: el precio por kWh solo se descarta si es negativo o NaN,
    // pero no si es infinito (ChargeCalculator.kt, comprobación de
    // input.pricePerKWh dentro de `validate`). Un precio "Infinity" pasa la
    // validación y revienta igual que los casos anteriores al construir el
    // BigDecimal del coste de la energía.
    // -----------------------------------------------------------------------

    @Test(expected = NumberFormatException::class)
    fun `un precio por kwh infinito no lo detecta la validacion y rompe el calculo`() {
        ChargeCalculator.calculate(
            TestFixtures.input(pricePerKWh = Double.POSITIVE_INFINITY),
            TestFixtures.vehicle,
        )
    }

    // -----------------------------------------------------------------------
    // DEFECTO REAL: el perfil del vehículo tampoco se protege frente a NaN.
    // ChargeCalculator.kt comprueba "usableCapacityKWh <= 0.0", que también
    // es false para NaN, así que un perfil con la capacidad en NaN pasa la
    // validación. Aquí el fallo aparece incluso antes que en los casos
    // anteriores: rawMinutes se convierte en NaN y Double.roundToInt()
    // (usado al calcular `minutes`) lanza directamente
    // IllegalArgumentException en lugar de NumberFormatException.
    // -----------------------------------------------------------------------

    @Test(expected = IllegalArgumentException::class)
    fun `una capacidad util nan en el perfil no la detecta la validacion y rompe el redondeo de minutos`() {
        val perfilInvalido = TestFixtures.vehicle.copy(usableCapacityKWh = Double.NaN)
        ChargeCalculator.calculate(TestFixtures.input(), perfilInvalido)
    }

    // -----------------------------------------------------------------------
    // DEFECTO REAL: las pérdidas del perfil tampoco se protegen frente a NaN.
    // La comprobación "loss < 0.0 || loss >= 1.0" es false en ambos casos
    // para NaN, así que un perfil con dcLossPercent = NaN pasa la validación.
    // La energía facturada (energyNeeded / (1 - loss)) se convierte en NaN y
    // revienta más tarde en Money.eurFromProduct().
    // -----------------------------------------------------------------------

    @Test(expected = NumberFormatException::class)
    fun `unas perdidas nan en el perfil no las detecta la validacion y rompen el coste de la energia`() {
        val perfilInvalido = TestFixtures.vehicle.copy(dcLossPercent = Double.NaN)
        ChargeCalculator.calculate(TestFixtures.input(), perfilInvalido)
    }

    // -----------------------------------------------------------------------
    // DEFECTO REAL: costPer100Km solo se protege frente a
    // "rangeAddedKm <= 0.0"; no contempla que rangeAddedKm pueda desbordar a
    // infinito. La validación de perfil solo exige "consumptionKWhPer100Km >
    // 0.0", así que un consumo extremadamente pequeño (pero formalmente
    // válido) hace que "energyNeededKWh / consumo * 100" se desborde a
    // Infinity, y BigDecimal.valueOf(Infinity) revienta con la misma
    // NumberFormatException que los casos anteriores.
    // -----------------------------------------------------------------------

    @Test(expected = NumberFormatException::class)
    fun `un consumo extremadamente pequeno desborda la autonomia a infinito y rompe el coste por cien km`() {
        val perfilInvalido = TestFixtures.vehicle.copy(consumptionKWhPer100Km = Double.MIN_VALUE)
        ChargeCalculator.calculate(TestFixtures.input(), perfilInvalido)
    }

    // -----------------------------------------------------------------------
    // DEFECTO REAL (silencioso, no lanza excepción): con una potencia de
    // cargador formalmente válida (> 0) pero absurdamente pequeña, rawMinutes
    // se dispara a billones de minutos. Double.roundToInt() no lanza
    // excepción para un valor finito tan grande: satura el resultado a
    // Int.MAX_VALUE (2 147 483 647), de modo que el usuario vería
    // "2147483647 min" en pantalla en lugar de un aviso o un error de
    // validación. Se documenta el comportamiento actual porque corregirlo
    // (por ejemplo, exigiendo una potencia mínima razonable) no es tarea de
    // esta fase.
    // -----------------------------------------------------------------------

    @Test
    fun `una potencia de cargador irrisoria hace que los minutos se saturen en vez de fallar`() {
        val resultado = TestFixtures.success(TestFixtures.input(chargerPowerKw = 1e-9))
        assertTrue(
            "Se esperaba un tiempo bruto astronomicamente alto",
            resultado.rawMinutes > 1.0e12,
        )
        assertEquals(Int.MAX_VALUE, resultado.minutes)
    }

    // -----------------------------------------------------------------------
    // Casos límite de los tramos de la curva de carga: comportamiento
    // correcto que conviene fijar con pruebas explícitas para detectar
    // regresiones futuras (empezar/acabar dentro del mismo tramo, y bordes
    // exactos como 60 % o 90 %).
    // -----------------------------------------------------------------------

    @Test
    fun `una sesion que empieza y acaba dentro del mismo tramo genera un unico segmento`() {
        // 35 % a 50 %, ambos dentro del tramo 30-60 (factor 0,90).
        val resultado = TestFixtures.success(
            TestFixtures.input(startSocPercent = 35.0, targetSocPercent = 50.0),
        )
        assertEquals(1, resultado.segments.size)
        val tramo = resultado.segments.single()
        assertEquals(35.0, tramo.fromPercent, EPSILON)
        assertEquals(50.0, tramo.toPercent, EPSILON)
        assertEquals(9.3, tramo.energyKWh, EPSILON)
        assertEquals(135.0, tramo.averagePowerKw, EPSILON)
    }

    @Test
    fun `empezar justo en el borde de un tramo no genera un segmento vacio del tramo anterior`() {
        // Empieza exactamente en el 60 %, borde entre el tramo 30-60 y el 60-80.
        val resultado = TestFixtures.success(
            TestFixtures.input(startSocPercent = 60.0, targetSocPercent = 75.0),
        )
        assertEquals(1, resultado.segments.size)
        val tramo = resultado.segments.single()
        assertEquals(60.0, tramo.fromPercent, EPSILON)
        assertEquals(75.0, tramo.toPercent, EPSILON)
        assertEquals(105.0, tramo.averagePowerKw, EPSILON)
    }

    @Test
    fun `terminar justo en el borde de un tramo no genera un segmento vacio del tramo siguiente`() {
        // Termina exactamente en el 60 %, borde entre el tramo 30-60 y el 60-80.
        val resultado = TestFixtures.success(
            TestFixtures.input(startSocPercent = 45.0, targetSocPercent = 60.0),
        )
        assertEquals(1, resultado.segments.size)
        val tramo = resultado.segments.single()
        assertEquals(45.0, tramo.fromPercent, EPSILON)
        assertEquals(60.0, tramo.toPercent, EPSILON)
        assertEquals(135.0, tramo.averagePowerKw, EPSILON)
    }

    @Test
    fun `una sesion completa de 0 a 100 recorre los seis tramos sin huecos ni solapes`() {
        val resultado = TestFixtures.success(
            TestFixtures.input(startSocPercent = 0.0, targetSocPercent = 100.0),
        )
        assertEquals(6, resultado.segments.size)

        // Los tramos deben encadenarse exactamente por los bordes 0/10/30/60/80/90/100,
        // sin huecos (porcentajes que no pertenecen a ningún tramo) ni solapes
        // (porcentajes contados dos veces).
        val limites = listOf(0.0, 10.0, 30.0, 60.0, 80.0, 90.0, 100.0)
        resultado.segments.forEachIndexed { indice, tramo ->
            assertEquals(limites[indice], tramo.fromPercent, EPSILON)
            assertEquals(limites[indice + 1], tramo.toPercent, EPSILON)
        }

        // Los seis tramos deben sumar exactamente la energía necesaria total.
        assertEquals(
            resultado.energyNeededKWh,
            resultado.segments.sumOf { it.energyKWh },
            1e-6,
        )

        // El último tramo (90-100 %, factor 0,20) mueve la misma energía que
        // el primero (0-10 %, factor 0,60) pero debe tardar bastante más.
        val primero = resultado.segments.first()
        val ultimo = resultado.segments.last()
        assertEquals(primero.energyKWh, ultimo.energyKWh, EPSILON)
        assertTrue(ultimo.minutes > primero.minutes * 2.5)
    }

    // -----------------------------------------------------------------------
    // Frontera exacta del margen de sobredimensionado (OVERSIZE_TOLERANCE =
    // 1,05 sobre la potencia máxima del vehículo). Con el perfil de fábrica
    // (155 kW en DC) el límite exacto son 162,75 kW.
    // -----------------------------------------------------------------------

    @Test
    fun `justo en el limite del margen de sobredimensionado no se activa el aviso`() {
        val resultado = TestFixtures.success(TestFixtures.input(chargerPowerKw = 162.75))
        assertTrue(!resultado.isOversized)
    }

    @Test
    fun `justo por encima del limite del margen de sobredimensionado si se activa el aviso`() {
        val resultado = TestFixtures.success(TestFixtures.input(chargerPowerKw = 162.750001))
        assertTrue(resultado.isOversized)
    }

    // -----------------------------------------------------------------------
    // Posible incoherencia de clasificación (se documenta, no se corrige en
    // esta fase): el aviso "cargador sobredimensionado" usa un margen del
    // 5 % sobre la potencia máxima del vehículo (162,75 kW para el perfil de
    // fábrica), mientras que la valoración ChargerRating.DC_SOBREDIMENSIONADO
    // usa el umbral configurable de ChargerPowerThresholds (180 kW por
    // defecto). Entre esos dos valores (162,75-180 kW) un cargador dispara el
    // aviso de sobredimensionado y al mismo tiempo se clasifica como
    // "DC_MUY_ADECUADO", lo que puede resultar confuso para quien lea ambos
    // datos a la vez.
    // -----------------------------------------------------------------------

    @Test
    fun `entre 162,75 y 180 kw el aviso de sobredimensionado y la valoracion de potencia no coinciden`() {
        val resultado = TestFixtures.success(TestFixtures.input(chargerPowerKw = 170.0))
        assertTrue(resultado.isOversized)
        assertEquals(ChargerRating.DC_MUY_ADECUADO, resultado.chargerRating)
    }
}
