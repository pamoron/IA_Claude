package com.pamoron.electroperico.domain.model

/**
 * Tramo de estado de carga recorrido durante la sesión.
 *
 * Se genera un tramo por cada franja de la curva de carga que la sesión
 * atraviesa, recortada a los porcentajes inicial y final reales.
 *
 * @param fromPercent porcentaje de batería al empezar el tramo.
 * @param toPercent porcentaje de batería al terminar el tramo.
 * @param energyKWh energía que entra en la batería durante el tramo.
 * @param averagePowerKw potencia media estimada en el tramo.
 * @param minutes duración del tramo en minutos, ya con el factor de estimación aplicado.
 */
data class ChargeSegment(
    val fromPercent: Double,
    val toPercent: Double,
    val energyKWh: Double,
    val averagePowerKw: Double,
    val minutes: Double,
)
