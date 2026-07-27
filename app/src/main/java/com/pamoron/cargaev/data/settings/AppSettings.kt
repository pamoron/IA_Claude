package com.pamoron.cargaev.data.settings

import com.pamoron.cargaev.domain.model.CurrentType
import com.pamoron.cargaev.domain.model.EstimationMode
import com.pamoron.cargaev.domain.model.PriceThresholds
import com.pamoron.cargaev.domain.model.VehicleProfile

/**
 * Ajustes persistentes de la aplicación.
 *
 * En esta primera fase hay un único perfil de vehículo. Cuando se admitan
 * varios, [vehicle] pasará a ser "el perfil activo" y la lista completa se
 * guardará en una base de datos.
 */
data class AppSettings(
    val vehicle: VehicleProfile = VehicleProfile.BYD_ATTO_2_COMFORT,
    val estimationMode: EstimationMode = EstimationMode.DEFAULT,
    val priceThresholds: PriceThresholds = PriceThresholds.DEFAULT,
    val lastSession: LastSession = LastSession(),
)

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
