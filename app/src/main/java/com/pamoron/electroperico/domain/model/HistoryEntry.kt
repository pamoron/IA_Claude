package com.pamoron.electroperico.domain.model

import java.math.BigDecimal

/**
 * Una recarga guardada en el historial.
 *
 * Reutiliza [ChargerOption] para los datos de entrada, que son exactamente los
 * mismos que en el comparador: nombre del operador, precio, potencia, tipo de
 * corriente, porcentajes y costes adicionales.
 *
 * Las cifras calculadas se guardan como **fotografía del momento**: si más
 * adelante se edita el perfil del vehículo, una recarga ya registrada debe
 * seguir contando lo que costó de verdad, no lo que costaría hoy.
 */
data class HistoryEntry(
    val id: String,
    /** Momento en que se guardó, en milisegundos desde la época. */
    val timestampMillis: Long,
    val charger: ChargerOption,
    val totalCostEur: BigDecimal,
    val minutes: Int,
    val effectivePricePerKWh: BigDecimal,
    val energyBilledKWh: Double,
    val energyNeededKWh: Double,
    val rangeAddedKm: Double,
    /** Cargadores marcados como favoritos; se muestran primero. */
    val favorite: Boolean = false,
) {

    /** Nombre del operador o del cargador. */
    val operatorName: String get() = charger.name

    companion object {

        /** Construye una entrada a partir de una simulación recién calculada. */
        fun from(
            id: String,
            charger: ChargerOption,
            result: ChargeResult,
            timestampMillis: Long,
            favorite: Boolean = false,
        ): HistoryEntry = HistoryEntry(
            id = id,
            timestampMillis = timestampMillis,
            charger = charger,
            totalCostEur = result.totalCostEur,
            minutes = result.minutes,
            effectivePricePerKWh = result.effectivePricePerKWh,
            energyBilledKWh = result.energyBilledKWh,
            energyNeededKWh = result.energyNeededKWh,
            rangeAddedKm = result.rangeAddedKm,
            favorite = favorite,
        )
    }
}

/** Criterio de ordenación del historial. */
enum class HistorySort {
    /** Las más recientes primero, con los favoritos por delante. */
    RECIENTES,

    /** Por coste total, de menor a mayor. */
    MAS_BARATAS,

    /** Por precio efectivo por kWh, de menor a mayor. */
    MEJOR_PRECIO_EFECTIVO,
    ;

    companion object {
        val DEFAULT: HistorySort = RECIENTES

        fun fromName(name: String?): HistorySort =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

/** Ordena una lista de entradas según el criterio elegido. */
fun List<HistoryEntry>.orderedBy(sort: HistorySort): List<HistoryEntry> = when (sort) {
    // Los favoritos siempre encabezan la lista de recientes: son los cargadores
    // a los que se vuelve.
    HistorySort.RECIENTES -> sortedWith(
        compareByDescending<HistoryEntry> { it.favorite }.thenByDescending { it.timestampMillis },
    )

    HistorySort.MAS_BARATAS -> sortedWith(
        compareBy<HistoryEntry> { it.totalCostEur }.thenByDescending { it.timestampMillis },
    )

    HistorySort.MEJOR_PRECIO_EFECTIVO -> sortedWith(
        compareBy<HistoryEntry> { it.effectivePricePerKWh }.thenByDescending { it.timestampMillis },
    )
}
