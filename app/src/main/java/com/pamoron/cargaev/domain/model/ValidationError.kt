package com.pamoron.cargaev.domain.model

/**
 * Motivos por los que unos datos de entrada no permiten calcular.
 *
 * Igual que con los avisos, el dominio devuelve el tipo y la UI resuelve el
 * texto traducido.
 */
enum class ValidationError {

    /** El precio por kWh es negativo. */
    PRECIO_NEGATIVO,

    /** La potencia del cargador debe ser mayor que cero. */
    POTENCIA_CARGADOR_INVALIDA,

    /** Algún porcentaje de batería está fuera del rango 0..100. */
    PORCENTAJE_FUERA_DE_RANGO,

    /** El porcentaje final debe ser estrictamente mayor que el inicial. */
    OBJETIVO_NO_SUPERIOR_AL_ACTUAL,

    /** La capacidad útil del perfil debe ser mayor que cero. */
    CAPACIDAD_INVALIDA,

    /** El consumo medio del perfil debe ser mayor que cero. */
    CONSUMO_INVALIDO,

    /** Las pérdidas deben estar en el rango [0, 100). */
    PERDIDAS_INVALIDAS,

    /** La potencia máxima del vehículo para ese tipo de corriente debe ser mayor que cero. */
    POTENCIA_VEHICULO_INVALIDA,

    /** Algún coste adicional es negativo. */
    COSTE_ADICIONAL_NEGATIVO,
}
