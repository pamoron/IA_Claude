package com.pamoron.cargaev.domain.model

/**
 * Avisos que acompañan a un resultado.
 *
 * El dominio solo devuelve el *tipo* de aviso; el texto vive en `strings.xml`
 * para mantener la capa de cálculo libre de recursos de Android.
 */
enum class ChargeWarning {

    /**
     * El cargador ofrece bastante más potencia de la que admite el vehículo.
     * El texto se compone con la potencia del cargador y la del coche.
     */
    CARGADOR_SOBREDIMENSIONADO,

    /** El objetivo supera el 80 %, donde la velocidad de carga cae mucho. */
    POR_ENCIMA_DEL_80,

    /** Se parte de un estado de carga muy bajo, donde la potencia también se limita. */
    SOC_INICIAL_MUY_BAJO,

    /** La potencia anunciada es un máximo, no un valor garantizado. */
    POTENCIA_NO_GARANTIZADA,

    /** En estaciones con varios conectores la potencia puede repartirse. */
    POTENCIA_COMPARTIDA,

    /** Conviene comprobar si el operador cobra por minuto, sesión o estacionamiento. */
    COMPROBAR_TARIFAS,

    /** Algunos operadores aplican precios distintos en su app y con tarjeta. */
    PRECIOS_APP_Y_TARJETA,

    /** El cargador puede facturar más energía de la que entra en la batería. */
    PERDIDAS_DE_CARGA,
    ;

    /**
     * Distingue los avisos que dependen de los datos introducidos (se muestran
     * arriba y destacados) de las advertencias generales, que siempre aplican.
     */
    val isContextual: Boolean
        get() = this == CARGADOR_SOBREDIMENSIONADO ||
            this == POR_ENCIMA_DEL_80 ||
            this == SOC_INICIAL_MUY_BAJO
}
