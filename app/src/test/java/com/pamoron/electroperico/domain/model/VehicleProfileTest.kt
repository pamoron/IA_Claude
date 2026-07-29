package com.pamoron.electroperico.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Perfil del vehículo y umbrales de valoración del precio. */
class VehicleProfileTest {

    private fun perfil(
        maxDcPowerKw: Double = 155.0,
        maxAcPowerKw: Double = 11.0,
        acLossPercent: Double = 12.0,
        dcLossPercent: Double = 8.0,
    ) = VehicleProfile(
        brand = "Marca",
        model = "Modelo",
        grossCapacityKWh = 64.8,
        usableCapacityKWh = 62.0,
        maxDcPowerKw = maxDcPowerKw,
        maxAcPowerKw = maxAcPowerKw,
        consumptionKWhPer100Km = 16.5,
        acLossPercent = acLossPercent,
        dcLossPercent = dcLossPercent,
    )

    // --- maxPowerKwFor -------------------------------------------------------

    @Test
    fun `maxPowerKwFor devuelve la potencia maxima de continua en DC`() {
        val vehiculo = perfil(maxDcPowerKw = 155.0, maxAcPowerKw = 11.0)
        assertEquals(155.0, vehiculo.maxPowerKwFor(CurrentType.DC), 0.0)
    }

    @Test
    fun `maxPowerKwFor devuelve la potencia maxima de alterna en AC`() {
        val vehiculo = perfil(maxDcPowerKw = 155.0, maxAcPowerKw = 11.0)
        assertEquals(11.0, vehiculo.maxPowerKwFor(CurrentType.AC), 0.0)
    }

    // --- lossFractionFor ------------------------------------------------------

    @Test
    fun `lossFractionFor convierte el porcentaje de perdidas de DC en fraccion`() {
        val vehiculo = perfil(dcLossPercent = 8.0)
        assertEquals(0.08, vehiculo.lossFractionFor(CurrentType.DC), 1e-9)
    }

    @Test
    fun `lossFractionFor convierte el porcentaje de perdidas de AC en fraccion`() {
        val vehiculo = perfil(acLossPercent = 12.0)
        assertEquals(0.12, vehiculo.lossFractionFor(CurrentType.AC), 1e-9)
    }

    @Test
    fun `lossFractionFor con perdidas a cero da una fraccion nula`() {
        val vehiculo = perfil(acLossPercent = 0.0, dcLossPercent = 0.0)
        assertEquals(0.0, vehiculo.lossFractionFor(CurrentType.AC), 0.0)
        assertEquals(0.0, vehiculo.lossFractionFor(CurrentType.DC), 0.0)
    }

    // --- displayName -----------------------------------------------------------

    @Test
    fun `displayName junta la marca y el modelo separados por un espacio`() {
        val vehiculo = perfil().copy(brand = "BYD", model = "ATTO 2 Comfort")
        assertEquals("BYD ATTO 2 Comfort", vehiculo.displayName)
    }

    @Test
    fun `displayName recorta los espacios sobrantes cuando el modelo esta vacio`() {
        val vehiculo = perfil().copy(brand = "BYD", model = "")
        assertEquals("BYD", vehiculo.displayName)
    }

    @Test
    fun `displayName recorta los espacios sobrantes cuando la marca esta vacia`() {
        val vehiculo = perfil().copy(brand = "", model = "ATTO 2 Comfort")
        assertEquals("ATTO 2 Comfort", vehiculo.displayName)
    }

    // --- Perfil por defecto: BYD ATTO 2 Comfort ---------------------------------

    @Test
    fun `el perfil por defecto es el BYD ATTO 2 Comfort con sus datos de fabrica`() {
        val comfort = VehicleProfile.BYD_ATTO_2_COMFORT
        assertEquals(VehicleProfile.DEFAULT_ID, comfort.id)
        assertEquals("BYD", comfort.brand)
        assertEquals("ATTO 2 Comfort", comfort.model)
        assertEquals("BYD ATTO 2 Comfort", comfort.displayName)
        assertEquals(64.8, comfort.grossCapacityKWh, 0.0)
        assertEquals(62.0, comfort.usableCapacityKWh, 0.0)
        assertEquals(155.0, comfort.maxDcPowerKw, 0.0)
        assertEquals(11.0, comfort.maxAcPowerKw, 0.0)
        assertEquals(16.5, comfort.consumptionKWhPer100Km, 0.0)
        assertEquals(12.0, comfort.acLossPercent, 0.0)
        assertEquals(8.0, comfort.dcLossPercent, 0.0)
    }

    @Test
    fun `el perfil por defecto es coherente entre capacidad util y bruta`() {
        // La capacidad útil nunca puede superar a la bruta: es la que de verdad
        // usan los cálculos.
        val comfort = VehicleProfile.BYD_ATTO_2_COMFORT
        assertTrue(comfort.usableCapacityKWh <= comfort.grossCapacityKWh)
    }

    @Test
    fun `el perfil por defecto expone sus potencias maximas y perdidas por tipo de corriente`() {
        val comfort = VehicleProfile.BYD_ATTO_2_COMFORT
        assertEquals(155.0, comfort.maxPowerKwFor(CurrentType.DC), 0.0)
        assertEquals(11.0, comfort.maxPowerKwFor(CurrentType.AC), 0.0)
        assertEquals(0.08, comfort.lossFractionFor(CurrentType.DC), 1e-9)
        assertEquals(0.12, comfort.lossFractionFor(CurrentType.AC), 1e-9)
    }

    @Test
    fun `los modelos recomendados tienen identificadores distintos y potencia de carga valida`() {
        val models = VehicleProfile.RECOMMENDED_PROFILES
        assertEquals(4, models.size)
        assertEquals(models.size, models.map { it.id }.distinct().size)
        assertTrue(models.all { it.maxAcPowerKw > 0.0 })
        assertTrue(models.all { it.maxDcPowerKw >= 0.0 })
    }

    @Test
    fun `el Tucson PHEV solo declara carga en alterna`() {
        val tucson = VehicleProfile.HYUNDAI_TUCSON_PHEV
        assertEquals(7.2, tucson.maxPowerKwFor(CurrentType.AC), 0.0)
        assertEquals(0.0, tucson.maxPowerKwFor(CurrentType.DC), 0.0)
    }

    // -----------------------------------------------------------------------
    // PriceThresholds
    // -----------------------------------------------------------------------

    @Test
    fun `rate clasifica un precio en cada categoria segun los umbrales`() {
        val umbrales = PriceThresholds(veryCheapMax = 0.25, goodMax = 0.39, normalMax = 0.55, expensiveMax = 0.70)
        assertEquals(PriceRating.MUY_BARATO, umbrales.rate(0.10))
        assertEquals(PriceRating.BUEN_PRECIO, umbrales.rate(0.30))
        assertEquals(PriceRating.NORMAL, umbrales.rate(0.50))
        assertEquals(PriceRating.CARO, umbrales.rate(0.60))
        assertEquals(PriceRating.MUY_CARO, umbrales.rate(0.80))
    }

    @Test
    fun `rate incluye el limite superior en la categoria mas barata`() {
        val umbrales = PriceThresholds(veryCheapMax = 0.25, goodMax = 0.39, normalMax = 0.55, expensiveMax = 0.70)
        // Cada umbral es el límite superior incluido de su categoría.
        assertEquals(PriceRating.MUY_BARATO, umbrales.rate(0.25))
        assertEquals(PriceRating.BUEN_PRECIO, umbrales.rate(0.26))
    }

    @Test
    fun `rate con los umbrales por defecto clasifica el precio limite de cada tramo`() {
        val umbrales = PriceThresholds.DEFAULT
        assertEquals(PriceRating.MUY_BARATO, umbrales.rate(umbrales.veryCheapMax))
        assertEquals(PriceRating.BUEN_PRECIO, umbrales.rate(umbrales.goodMax))
        assertEquals(PriceRating.NORMAL, umbrales.rate(umbrales.normalMax))
        assertEquals(PriceRating.CARO, umbrales.rate(umbrales.expensiveMax))
        assertEquals(PriceRating.MUY_CARO, umbrales.rate(umbrales.expensiveMax + 0.01))
    }

    @Test
    fun `isValid acepta unos umbrales estrictamente crecientes`() {
        assertTrue(PriceThresholds(0.25, 0.39, 0.55, 0.70).isValid())
        assertTrue(PriceThresholds.DEFAULT.isValid())
    }

    @Test
    fun `isValid rechaza un umbral inicial que no sea positivo`() {
        assertFalse(PriceThresholds(0.0, 0.39, 0.55, 0.70).isValid())
        assertFalse(PriceThresholds(-0.10, 0.39, 0.55, 0.70).isValid())
    }

    @Test
    fun `isValid rechaza umbrales iguales o decrecientes`() {
        // Deben ser estrictamente crecientes: dos umbrales iguales no son válidos.
        assertFalse(PriceThresholds(0.25, 0.25, 0.55, 0.70).isValid())
        assertFalse(PriceThresholds(0.25, 0.39, 0.39, 0.70).isValid())
        assertFalse(PriceThresholds(0.25, 0.39, 0.55, 0.55).isValid())
        // Umbrales guardados en un orden incoherente (por ejemplo, tras un error
        // de migración o una edición manual del fichero de preferencias).
        assertFalse(PriceThresholds(0.55, 0.39, 0.25, 0.70).isValid())
    }
}
