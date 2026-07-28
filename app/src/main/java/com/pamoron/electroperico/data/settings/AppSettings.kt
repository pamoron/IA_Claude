package com.pamoron.electroperico.data.settings

import com.pamoron.electroperico.domain.model.CurrentType
import com.pamoron.electroperico.domain.model.EstimationMode
import com.pamoron.electroperico.domain.model.PriceThresholds
import com.pamoron.electroperico.domain.model.VehicleProfile

/**
 * Ajustes persistentes de la aplicación.
 *
 * Los perfiles se guardan como una lista, pero los cálculos siguen recibiendo
 * solamente [vehicle], que es el perfil activo. Así el dominio no necesita
 * conocer ni la persistencia ni la interfaz de selección.
 */
data class AppSettings(
    val profiles: List<VehicleProfile> = listOf(VehicleProfile.BYD_ATTO_2_COMFORT),
    val activeProfileId: String = VehicleProfile.DEFAULT_ID,
    val estimationMode: EstimationMode = EstimationMode.DEFAULT,
    val priceThresholds: PriceThresholds = PriceThresholds.DEFAULT,
    val lastSession: LastSession = LastSession(),
) {
    /** Perfil que usan la calculadora, el comparador y las ediciones del historial. */
    val vehicle: VehicleProfile
        get() = profiles.firstOrNull { it.id == activeProfileId } ?: profiles.first()
}

/**
 * Últimos valores introducidos en la calculadora.
 *
 * Se recuerdan para que, al volver a abrir la app junto a un cargador, la
 * pantalla ya esté casi rellena.
 */
data class LastSession(
    val pricePerKWh: Double = 0.0,
    val chargerPowerKw: Double = 0.0,
    val currentType: CurrentType = CurrentType.DC,
    val startSocPercent: Int = 20,
    val targetSocPercent: Int = 80,
    val startFeeEur: Double = 0.0,
    val pricePerMinuteEur: Double = 0.0,
    val parkingFeeEur: Double = 0.0,
)
