package com.pamoron.electroperico.domain.model

/**
 * Una opción del comparador ya calculada y puntuada.
 *
 * @param option datos introducidos por el usuario.
 * @param result resultado completo del motor de cálculo.
 * @param costScore 1,0 para la opción más barata y 0,0 para la más cara.
 * @param timeScore 1,0 para la más rápida y 0,0 para la más lenta.
 * @param balanceScore media ponderada de los dos anteriores.
 */
data class ComparedOption(
    val option: ChargerOption,
    val result: ChargeResult,
    val costScore: Double,
    val timeScore: Double,
    val balanceScore: Double,
)

/**
 * Resultado de comparar varias opciones.
 *
 * [options] va ya ordenada según el criterio pedido. Los identificadores
 * destacados pueden coincidir: la opción más barata puede ser también la más
 * rápida y la mejor en conjunto.
 *
 * @param invalidOptions opciones que no se han podido calcular, con su motivo.
 */
data class Comparison(
    val options: List<ComparedOption>,
    val invalidOptions: List<InvalidOption>,
    val cheapestId: String?,
    val fastestId: String?,
    val bestOverallId: String?,
    val sort: ComparisonSort,
) {

    /** `true` si hay al menos dos opciones válidas, que es cuando comparar tiene sentido. */
    val isComparable: Boolean get() = options.size >= 2

    /** Busca una opción por identificador. */
    fun find(id: String): ComparedOption? = options.firstOrNull { it.option.id == id }
}

/** Opción del comparador cuyos datos no permiten calcular. */
data class InvalidOption(
    val option: ChargerOption,
    val errors: List<ValidationError>,
)
