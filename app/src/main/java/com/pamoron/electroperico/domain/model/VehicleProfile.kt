package com.pamoron.electroperico.domain.model

/**
 * Perfil de un vehículo eléctrico.
 *
 * Todos los campos son editables desde la pantalla de ajustes. La primera
 * versión trabaja con un único perfil, pero el modelo ya lleva [id] y [brand] /
 * [model] para admitir varios perfiles en una fase posterior.
 *
 * @param grossCapacityKWh capacidad bruta de la batería, solo informativa.
 * @param usableCapacityKWh capacidad útil: es la que se usa en todos los cálculos.
 * @param maxDcPowerKw potencia máxima de carga en corriente continua.
 * @param maxAcPowerKw potencia máxima de carga en corriente alterna.
 * @param consumptionKWhPer100Km consumo medio para estimar la autonomía añadida.
 * @param acLossPercent pérdidas de carga en AC, en porcentaje (0..99).
 * @param dcLossPercent pérdidas de carga en DC, en porcentaje (0..99).
 */
data class VehicleProfile(
    val id: String = DEFAULT_ID,
    val brand: String,
    val model: String,
    val grossCapacityKWh: Double,
    val usableCapacityKWh: Double,
    val maxDcPowerKw: Double,
    val maxAcPowerKw: Double,
    val consumptionKWhPer100Km: Double,
    val acLossPercent: Double,
    val dcLossPercent: Double,
) {

    /** Nombre legible del vehículo, por ejemplo "BYD ATTO 2 Comfort". */
    val displayName: String get() = "$brand $model".trim()

    /** Potencia máxima que el vehículo puede aceptar según el tipo de corriente. */
    fun maxPowerKwFor(type: CurrentType): Double = when (type) {
        CurrentType.AC -> maxAcPowerKw
        CurrentType.DC -> maxDcPowerKw
    }

    /** Pérdidas aplicables, expresadas como fracción (0,08 = 8 %). */
    fun lossFractionFor(type: CurrentType): Double = when (type) {
        CurrentType.AC -> acLossPercent / 100.0
        CurrentType.DC -> dcLossPercent / 100.0
    }

    companion object {
        const val DEFAULT_ID: String = "byd-atto-2-comfort"

        /** Perfil predeterminado con los datos del vehículo del usuario. */
        val BYD_ATTO_2_COMFORT: VehicleProfile = VehicleProfile(
            id = DEFAULT_ID,
            brand = "BYD",
            model = "ATTO 2 Comfort",
            grossCapacityKWh = 64.8,
            usableCapacityKWh = 62.0,
            maxDcPowerKw = 155.0,
            maxAcPowerKw = 11.0,
            consumptionKWhPer100Km = 16.5,
            acLossPercent = 12.0,
            dcLossPercent = 8.0,
        )

        /** Modelos de referencia disponibles desde Ajustes. Todos se pueden editar. */
        val JAECOO_5_EV_EXCLUSIVE: VehicleProfile = VehicleProfile(
            id = "jaecoo-5-ev-exclusive",
            brand = "JAECOO",
            model = "5 EV Exclusive",
            grossCapacityKWh = 60.9,
            usableCapacityKWh = 60.9,
            maxDcPowerKw = 130.0,
            maxAcPowerKw = 11.0,
            consumptionKWhPer100Km = 16.5,
            acLossPercent = 12.0,
            dcLossPercent = 8.0,
        )

        val HYUNDAI_TUCSON_PHEV: VehicleProfile = VehicleProfile(
            id = "hyundai-tucson-phev",
            brand = "Hyundai",
            model = "TUCSON Híbrido Enchufable",
            grossCapacityKWh = 13.8,
            usableCapacityKWh = 13.8,
            // El cargador de a bordo admite CA; no dispone de carga rápida en CC.
            maxDcPowerKw = 0.0,
            maxAcPowerKw = 7.2,
            consumptionKWhPer100Km = 11.0,
            acLossPercent = 12.0,
            dcLossPercent = 8.0,
        )

        val EBRO_S800_PHEV: VehicleProfile = VehicleProfile(
            id = "ebro-s800-phev",
            brand = "EBRO",
            model = "s800 PHEV",
            grossCapacityKWh = 18.3,
            usableCapacityKWh = 18.3,
            maxDcPowerKw = 40.0,
            maxAcPowerKw = 6.6,
            consumptionKWhPer100Km = 22.2,
            acLossPercent = 12.0,
            dcLossPercent = 8.0,
        )

        val OMODA_5_EV: VehicleProfile = VehicleProfile(
            id = "omoda-5-ev",
            brand = "OMODA",
            model = "5 EV",
            grossCapacityKWh = 61.0566,
            usableCapacityKWh = 61.0566,
            maxDcPowerKw = 80.0,
            maxAcPowerKw = 11.0,
            consumptionKWhPer100Km = 15.5,
            acLossPercent = 12.0,
            dcLossPercent = 8.0,
        )

        val RECOMMENDED_PROFILES: List<VehicleProfile> = listOf(
            JAECOO_5_EV_EXCLUSIVE,
            HYUNDAI_TUCSON_PHEV,
            EBRO_S800_PHEV,
            OMODA_5_EV,
        )
    }
}
