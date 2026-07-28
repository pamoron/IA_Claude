package com.pamoron.electroperico.domain.calc

import com.pamoron.electroperico.domain.calc.TestFixtures.EPSILON
import com.pamoron.electroperico.domain.model.ChargerRating
import com.pamoron.electroperico.domain.model.ValidationError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas de casos límite del motor de cálculo.
 *
 * Cubren los bordes exactos de los tramos de la curva de carga, las fronteras
 * de clasificación y las entradas degeneradas (NaN, infinito y valores
 * absurdamente pequeños).
 *
 * Varias de estas pruebas nacieron de una revisión que encontró que el motor
 * dejaba escapar excepciones no controladas ante números no finitos. La
 * validación ya se ha endurecido; estas pruebas fijan el comportamiento
 * correcto para que no se pierda.
 */
class EdgeCasesTest {

    // -----------------------------------------------------------------------
    // Números no finitos.
    //
    // Un NaN hace que toda comparación («< 0», «<= 0») sea falsa, así que
    // antes se colaba hasta BigDecimal.valueOf y reventaba con una excepción
    // no controlada. Ahora la validación exige además que el número sea
    // finito, de modo que estas entradas devuelven un fallo ordenado.
    // -----------------------------------------------------------------------

    @Test
    fun `un coste de inicio NaN se rechaza en la validacion`() {
        val errores = TestFixtures.failure(TestFixtures.input(startFeeEur = Double.NaN)).errors
        assertTrue(errores.contains(ValidationError.COSTE_ADICIONAL_NEGATIVO))
    }

    @Test
    fun `un precio por minuto NaN se rechaza en la validacion`() {
        val errores = TestFixtures.failure(TestFixtures.input(pricePerMinuteEur = Double.NaN)).errors
        assertTrue(errores.contains(ValidationError.COSTE_ADICIONAL_NEGATIVO))
    }

    @Test
    fun `un coste de estacionamiento NaN se rechaza en la validacion`() {
        val errores = TestFixtures.failure(TestFixtures.input(parkingFeeEur = Double.NaN)).errors
        assertTrue(errores.contains(ValidationError.COSTE_ADICIONAL_NEGATIVO))
    }

    @Test
    fun `un precio por kwh infinito se rechaza en la validacion`() {
        val errores = TestFixtures
            .failure(TestFixtures.input(pricePerKWh = Double.POSITIVE_INFINITY)).errors
        assertTrue(errores.contains(ValidationError.PRECIO_NEGATIVO))
    }

    @Test
    fun `una potencia de cargador infinita se rechaza en la validacion`() {
        val errores = TestFixtures
            .failure(TestFixtures.input(chargerPowerKw = Double.POSITIVE_INFINITY)).errors
        assertTrue(errores.contains(ValidationError.POTENCIA_CARGADOR_INVALIDA))
    }

    @Test
    fun `un porcentaje NaN se rechaza en la validacion`() {
        val errores = TestFixtures.failure(TestFixtures.input(startSocPercent = Double.NaN)).errors
        assertTrue(errores.contains(ValidationError.PORCENTAJE_FUERA_DE_RANGO))
    }

    @Test
    fun `una capacidad util NaN en el perfil se rechaza en la validacion`() {
        val perfil = TestFixtures.vehicle.copy(usableCapacityKWh = Double.NaN)
        val errores = TestFixtures.failure(TestFixtures.input(), perfil).errors
        assertTrue(errores.contains(ValidationError.CAPACIDAD_INVALIDA))
    }

    @Test
    fun `unas perdidas NaN en el perfil se rechazan en la validacion`() {
        val perfil = TestFixtures.vehicle.copy(dcLossPercent = Double.NaN)
        val errores = TestFixtures.failure(TestFixtures.input(), perfil).errors
        assertTrue(errores.contains(ValidationError.PERDIDAS_INVALIDAS))
    }

    @Test
    fun `un consumo NaN en el perfil se rechaza en la validacion`() {
        val perfil = TestFixtures.vehicle.copy(consumptionKWhPer100Km = Double.NaN)
        val errores = TestFixtures.failure(TestFixtures.input(), perfil).errors
        assertTrue(errores.contains(ValidationError.CONSUMO_INVALIDO))
    }

    // -----------------------------------------------------------------------
    // Valores extremos pero formalmente válidos.
    //
    // No son entradas realistas, pero no deben reventar ni mostrar cifras
    // absurdas: el resultado se acota y se sigue pudiendo enseñar.
    // -----------------------------------------------------------------------

    @Test
    fun `un consumo diminuto no desborda el coste por cien kilometros`() {
        // La autonomía se desborda a infinito; el coste por 100 km cae a cero
        // en lugar de reventar al construir el BigDecimal.
        val perfil = TestFixtures.vehicle.copy(consumptionKWhPer100Km = Double.MIN_VALUE)
        val resultado = TestFixtures.success(TestFixtures.input(), perfil)
        assertEquals(Money.ZERO, resultado.costPer100KmEur)
    }

    @Test
    fun `una potencia de cargador irrisoria acota los minutos en lugar de desbordar`() {
        val resultado = TestFixtures.success(TestFixtures.input(chargerPowerKw = 1e-9))
        assertTrue(
            "Se esperaba un tiempo bruto astronómicamente alto",
            resultado.rawMinutes > 1.0e12,
        )
        // Siete días es el tope: por encima la estimación ya no dice nada.
        assertEquals(7 * 24 * 60, resultado.minutes)
        assertTrue(resultado.minutes < Int.MAX_VALUE)
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
