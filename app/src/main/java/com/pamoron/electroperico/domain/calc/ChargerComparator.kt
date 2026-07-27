package com.pamoron.electroperico.domain.calc

import com.pamoron.electroperico.domain.model.CalculationOutcome
import com.pamoron.electroperico.domain.model.ChargeResult
import com.pamoron.electroperico.domain.model.ChargerOption
import com.pamoron.electroperico.domain.model.ChargerPowerThresholds
import com.pamoron.electroperico.domain.model.ComparedOption
import com.pamoron.electroperico.domain.model.Comparison
import com.pamoron.electroperico.domain.model.ComparisonSort
import com.pamoron.electroperico.domain.model.EstimationMode
import com.pamoron.electroperico.domain.model.InvalidOption
import com.pamoron.electroperico.domain.model.PriceThresholds
import com.pamoron.electroperico.domain.model.VehicleProfile

/**
 * Comparador de opciones de recarga.
 *
 * Calcula cada opción con el mismo motor y el mismo perfil de vehículo, las
 * puntúa y las ordena. Como todas las opciones se calculan igual, la comparación
 * es justa aunque tengan porcentajes de batería distintos.
 *
 * Igual que [ChargeCalculator], es Kotlin puro y se prueba entero en la JVM.
 */
object ChargerComparator {

    /**
     * Peso del coste en la puntuación de equilibrio.
     *
     * La fórmula es deliberadamente sencilla y se explica en la pantalla:
     * primero se normaliza cada magnitud entre las opciones comparadas (1,0 a la
     * mejor y 0,0 a la peor) y después se hace la media ponderada.
     *
     *     equilibrio = 0,60 × puntuaciónCoste + 0,40 × puntuaciónTiempo
     */
    const val COST_WEIGHT: Double = 0.60

    /** Peso del tiempo en la puntuación de equilibrio. */
    const val TIME_WEIGHT: Double = 1.0 - COST_WEIGHT

    /**
     * Compara un conjunto de opciones.
     *
     * @param options opciones guardadas; se ignoran las que no se puedan calcular,
     *        que se devuelven aparte en [Comparison.invalidOptions].
     * @param sort criterio de ordenación de la lista devuelta.
     */
    fun compare(
        options: List<ChargerOption>,
        vehicle: VehicleProfile,
        sort: ComparisonSort = ComparisonSort.DEFAULT,
        estimationMode: EstimationMode = EstimationMode.DEFAULT,
        priceThresholds: PriceThresholds = PriceThresholds.DEFAULT,
        chargerThresholds: ChargerPowerThresholds = ChargerPowerThresholds.DEFAULT,
    ): Comparison {

        val valid = mutableListOf<Pair<ChargerOption, ChargeResult>>()
        val invalid = mutableListOf<InvalidOption>()

        options.forEach { option ->
            when (
                val outcome = ChargeCalculator.calculate(
                    input = option.toChargeInput(),
                    vehicle = vehicle,
                    estimationMode = estimationMode,
                    priceThresholds = priceThresholds,
                    chargerThresholds = chargerThresholds,
                )
            ) {
                is CalculationOutcome.Success -> valid += option to outcome.result
                is CalculationOutcome.Failure -> invalid += InvalidOption(option, outcome.errors)
            }
        }

        if (valid.isEmpty()) {
            return Comparison(
                options = emptyList(),
                invalidOptions = invalid,
                cheapestId = null,
                fastestId = null,
                bestOverallId = null,
                sort = sort,
            )
        }

        val costs = valid.map { it.second.totalCostEur.toDouble() }
        val times = valid.map { it.second.minutes.toDouble() }

        val scored = valid.mapIndexed { index, (option, result) ->
            // 1,0 al mejor valor y 0,0 al peor; si todos empatan, 1,0 para todos.
            val costScore = normalizeLowerIsBetter(costs[index], costs)
            val timeScore = normalizeLowerIsBetter(times[index], times)
            ComparedOption(
                option = option,
                result = result,
                costScore = costScore,
                timeScore = timeScore,
                balanceScore = COST_WEIGHT * costScore + TIME_WEIGHT * timeScore,
            )
        }

        // Los destacados se calculan sobre la lista completa, no sobre la ordenada.
        val cheapest = scored.minWithOrNull(
            compareBy({ it.result.totalCostEur }, { it.result.minutes }),
        )
        val fastest = scored.minWithOrNull(
            compareBy({ it.result.minutes }, { it.result.totalCostEur }),
        )
        val bestOverall = scored.maxWithOrNull(
            compareBy({ it.balanceScore }, { -it.result.totalCostEur.toDouble() }),
        )

        return Comparison(
            options = scored.sortedWith(comparatorFor(sort)),
            invalidOptions = invalid,
            cheapestId = cheapest?.option?.id,
            fastestId = fastest?.option?.id,
            bestOverallId = bestOverall?.option?.id,
            sort = sort,
        )
    }

    /**
     * Normaliza un valor en el que "menos es mejor" a una puntuación de 0 a 1.
     * Si todas las opciones valen lo mismo no hay nada que distinguir y todas
     * reciben la puntuación máxima.
     */
    private fun normalizeLowerIsBetter(value: Double, all: List<Double>): Double {
        val min = all.min()
        val max = all.max()
        if (max - min <= 0.0) return 1.0
        return (max - value) / (max - min)
    }

    /** Comparador correspondiente a cada criterio, con desempates estables. */
    private fun comparatorFor(sort: ComparisonSort): Comparator<ComparedOption> = when (sort) {
        ComparisonSort.MAS_BARATO -> compareBy(
            { it.result.totalCostEur },
            { it.result.minutes },
            { it.option.name.lowercase() },
        )

        ComparisonSort.MAS_RAPIDO -> compareBy(
            { it.result.minutes },
            { it.result.totalCostEur },
            { it.option.name.lowercase() },
        )

        ComparisonSort.MEJOR_EQUILIBRIO -> compareByDescending<ComparedOption> { it.balanceScore }
            .thenBy { it.result.totalCostEur }
            .thenBy { it.option.name.lowercase() }
    }
}
