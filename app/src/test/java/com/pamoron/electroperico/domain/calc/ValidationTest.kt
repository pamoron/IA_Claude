package com.pamoron.electroperico.domain.calc

import com.pamoron.electroperico.domain.model.CurrentType
import com.pamoron.electroperico.domain.model.ValidationError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Comprobaciones de las entradas no válidas. */
class ValidationTest {

    @Test
    fun `el objetivo no puede ser igual al porcentaje actual`() {
        val errores = TestFixtures.failure(
            TestFixtures.input(startSocPercent = 50.0, targetSocPercent = 50.0),
        ).errors
        assertTrue(errores.contains(ValidationError.OBJETIVO_NO_SUPERIOR_AL_ACTUAL))
    }

    @Test
    fun `el objetivo no puede ser inferior al porcentaje actual`() {
        val errores = TestFixtures.failure(
            TestFixtures.input(startSocPercent = 80.0, targetSocPercent = 40.0),
        ).errors
        assertTrue(errores.contains(ValidationError.OBJETIVO_NO_SUPERIOR_AL_ACTUAL))
    }

    @Test
    fun `los porcentajes deben estar entre cero y cien`() {
        assertTrue(
            TestFixtures.failure(TestFixtures.input(startSocPercent = -5.0))
                .errors.contains(ValidationError.PORCENTAJE_FUERA_DE_RANGO),
        )
        assertTrue(
            TestFixtures.failure(TestFixtures.input(targetSocPercent = 120.0))
                .errors.contains(ValidationError.PORCENTAJE_FUERA_DE_RANGO),
        )
    }

    @Test
    fun `la potencia del cargador debe ser mayor que cero`() {
        assertTrue(
            TestFixtures.failure(TestFixtures.input(chargerPowerKw = 0.0))
                .errors.contains(ValidationError.POTENCIA_CARGADOR_INVALIDA),
        )
        assertTrue(
            TestFixtures.failure(TestFixtures.input(chargerPowerKw = -50.0))
                .errors.contains(ValidationError.POTENCIA_CARGADOR_INVALIDA),
        )
    }

    @Test
    fun `el precio no puede ser negativo pero si cero`() {
        assertTrue(
            TestFixtures.failure(TestFixtures.input(pricePerKWh = -0.1))
                .errors.contains(ValidationError.PRECIO_NEGATIVO),
        )
        // Un cargador gratuito es un caso legítimo.
        val gratis = TestFixtures.success(TestFixtures.input(pricePerKWh = 0.0))
        assertEquals(Money.ZERO, gratis.totalCostEur)
    }

    @Test
    fun `los costes adicionales no pueden ser negativos`() {
        assertTrue(
            TestFixtures.failure(TestFixtures.input(startFeeEur = -1.0))
                .errors.contains(ValidationError.COSTE_ADICIONAL_NEGATIVO),
        )
        assertTrue(
            TestFixtures.failure(TestFixtures.input(pricePerMinuteEur = -0.5))
                .errors.contains(ValidationError.COSTE_ADICIONAL_NEGATIVO),
        )
        assertTrue(
            TestFixtures.failure(TestFixtures.input(parkingFeeEur = -2.0))
                .errors.contains(ValidationError.COSTE_ADICIONAL_NEGATIVO),
        )
    }

    @Test
    fun `un perfil con capacidad util cero no permite calcular`() {
        val perfil = TestFixtures.vehicle.copy(usableCapacityKWh = 0.0)
        assertTrue(
            TestFixtures.failure(TestFixtures.input(), perfil)
                .errors.contains(ValidationError.CAPACIDAD_INVALIDA),
        )
    }

    @Test
    fun `un perfil con consumo cero no permite calcular la autonomia`() {
        val perfil = TestFixtures.vehicle.copy(consumptionKWhPer100Km = 0.0)
        assertTrue(
            TestFixtures.failure(TestFixtures.input(), perfil)
                .errors.contains(ValidationError.CONSUMO_INVALIDO),
        )
    }

    @Test
    fun `unas perdidas del cien por cien no permiten calcular`() {
        val perfil = TestFixtures.vehicle.copy(dcLossPercent = 100.0)
        assertTrue(
            TestFixtures.failure(TestFixtures.input(), perfil)
                .errors.contains(ValidationError.PERDIDAS_INVALIDAS),
        )
    }

    @Test
    fun `un perfil sin potencia en alterna no permite calcular en alterna`() {
        val perfil = TestFixtures.vehicle.copy(maxAcPowerKw = 0.0)
        val errores = TestFixtures.failure(
            TestFixtures.input(currentType = CurrentType.AC, chargerPowerKw = 7.4),
            perfil,
        ).errors
        assertTrue(errores.contains(ValidationError.POTENCIA_VEHICULO_INVALIDA))
    }

    @Test
    fun `una sesion muy corta nunca se muestra como cero minutos`() {
        val resultado = TestFixtures.success(
            TestFixtures.input(startSocPercent = 50.0, targetSocPercent = 50.1),
        )
        assertTrue(resultado.rawMinutes < 1.0)
        assertEquals(1, resultado.minutes)
    }

    @Test
    fun `se acumulan todos los errores detectados`() {
        val errores = TestFixtures.failure(
            TestFixtures.input(
                pricePerKWh = -1.0,
                chargerPowerKw = 0.0,
                startSocPercent = 150.0,
            ),
        ).errors
        assertEquals(3, errores.size)
    }
}
