package com.pamoron.electroperico.domain.model

/**
 * Tipo de corriente del punto de recarga.
 *
 * Determina qué límite de potencia del vehículo se aplica, qué porcentaje de
 * pérdidas se usa y qué modelo de tiempo se emplea (curva por tramos en DC,
 * potencia estable en AC).
 */
enum class CurrentType {
    /** Corriente alterna: cargador de a bordo del vehículo, potencias bajas y estables. */
    AC,

    /** Corriente continua: carga rápida, la potencia varía mucho según el estado de carga. */
    DC,
}
