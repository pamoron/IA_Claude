package com.pamoron.cargaev.domain.calc

import com.pamoron.cargaev.domain.model.CalculationOutcome
import com.pamoron.cargaev.domain.model.ChargeInput
import com.pamoron.cargaev.domain.model.ChargeResult
import com.pamoron.cargaev.domain.model.CurrentType
import com.pamoron.cargaev.domain.model.EstimationMode
import com.pamoron.cargaev.domain.model.PriceThresholds
import com.pamoron.cargaev.domain.model.VehicleProfile
import org.junit.Assert.assertTrue
import org.junit.Assert.fail

/** Utilidades compartidas por las pruebas del motor de cálculo. */
object TestFixtures {

    /** Tolerancia para comparar magnitudes físicas en coma flotante. */
    const val EPSILON: Double = 1e-6

    /** Perfil de referencia: el BYD ATTO 2 Comfort con sus valores de fábrica. */
    val vehicle: VehicleProfile = VehicleProfile.BYD_ATTO_2_COMFORT

    /** Construye una entrada con valores por defecto razonables. */
    fun input(
        pricePerKWh: Double = 0.45,
        chargerPowerKw: Double = 150.0,
        currentType: CurrentType = CurrentType.DC,
        startSocPercent: Double = 20.0,
        targetSocPercent: Double = 80.0,
        startFeeEur: Double = 0.0,
        pricePerMinuteEur: Double = 0.0,
        parkingFeeEur: Double = 0.0,
    ): ChargeInput = ChargeInput(
        pricePerKWh = pricePerKWh,
        chargerPowerKw = chargerPowerKw,
        currentType = currentType,
        startSocPercent = startSocPercent,
        targetSocPercent = targetSocPercent,
        startFeeEur = startFeeEur,
        pricePerMinuteEur = pricePerMinuteEur,
        parkingFeeEur = parkingFeeEur,
    )

    /** Calcula y exige que el resultado sea correcto, devolviéndolo ya desempaquetado. */
    fun success(
        input: ChargeInput,
        vehicle: VehicleProfile = TestFixtures.vehicle,
        estimationMode: EstimationMode = EstimationMode.NORMAL,
        priceThresholds: PriceThresholds = PriceThresholds.DEFAULT,
    ): ChargeResult {
        val outcome = ChargeCalculator.calculate(
            input = input,
            vehicle = vehicle,
            estimationMode = estimationMode,
            priceThresholds = priceThresholds,
        )
        if (outcome !is CalculationOutcome.Success) {
            fail("Se esperaba un cálculo correcto, pero se obtuvo: $outcome")
        }
        return (outcome as CalculationOutcome.Success).result
    }

    /** Calcula y exige que la validación falle, devolviendo los errores. */
    fun failure(
        input: ChargeInput,
        vehicle: VehicleProfile = TestFixtures.vehicle,
    ): CalculationOutcome.Failure {
        val outcome = ChargeCalculator.calculate(input = input, vehicle = vehicle)
        if (outcome !is CalculationOutcome.Failure) {
            fail("Se esperaba un fallo de validación, pero se obtuvo: $outcome")
        }
        val failure = outcome as CalculationOutcome.Failure
        assertTrue("La lista de errores no puede estar vacía", failure.errors.isNotEmpty())
        return failure
    }
}
