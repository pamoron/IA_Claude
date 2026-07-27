package com.pamoron.electroperico.domain.model

/**
 * Grado de prudencia de la estimación de tiempo.
 *
 * El [factor] multiplica el tiempo total calculado por tramos. La estimación
 * conservadora absorbe pérdidas de potencia habituales (frío, batería sin
 * acondicionar, reparto de potencia entre conectores).
 */
enum class EstimationMode(val factor: Double) {
    OPTIMISTA(0.90),
    NORMAL(1.00),
    CONSERVADORA(1.15),
    ;

    companion object {
        val DEFAULT: EstimationMode = NORMAL

        /** Lectura tolerante para valores persistidos: si no se reconoce, devuelve [DEFAULT]. */
        fun fromName(name: String?): EstimationMode =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
