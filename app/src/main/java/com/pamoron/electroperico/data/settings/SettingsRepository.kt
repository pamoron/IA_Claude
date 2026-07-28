package com.pamoron.electroperico.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pamoron.electroperico.domain.model.CurrentType
import com.pamoron.electroperico.domain.model.EstimationMode
import com.pamoron.electroperico.domain.model.PriceThresholds
import com.pamoron.electroperico.domain.model.VehicleProfile
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Persistencia de los ajustes en DataStore Preferences.
 *
 * DataStore basta en esta fase: solo hay un puñado de valores escalares y un
 * perfil de vehículo. Room entrará cuando existan el historial y el comparador,
 * que sí son colecciones que hay que consultar y ordenar.
 */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    /** Ajustes actuales. Ante un fallo de lectura se devuelven los valores por defecto. */
    val settings: Flow<AppSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { it.toAppSettings() }

    /** Guarda el perfil del vehículo editado en ajustes. */
    suspend fun updateVehicle(vehicle: VehicleProfile) {
        dataStore.edit { prefs -> writeVehicle(prefs, vehicle) }
    }

    /** Guarda el grado de prudencia de la estimación de tiempo. */
    suspend fun updateEstimationMode(mode: EstimationMode) {
        dataStore.edit { prefs -> prefs[Keys.ESTIMATION_MODE] = mode.name }
    }

    /** Guarda los umbrales de valoración del precio. */
    suspend fun updatePriceThresholds(thresholds: PriceThresholds) {
        dataStore.edit { prefs -> writeThresholds(prefs, thresholds) }
    }

    /** Recuerda los últimos valores usados en la calculadora. */
    suspend fun saveLastSession(session: LastSession) {
        dataStore.edit { prefs ->
            prefs[Keys.LAST_PRICE] = session.pricePerKWh
            prefs[Keys.LAST_POWER] = session.chargerPowerKw
            prefs[Keys.LAST_CURRENT_TYPE] = session.currentType.name
            prefs[Keys.LAST_START_SOC] = session.startSocPercent
            prefs[Keys.LAST_TARGET_SOC] = session.targetSocPercent
            prefs[Keys.LAST_START_FEE] = session.startFeeEur
            prefs[Keys.LAST_PRICE_PER_MINUTE] = session.pricePerMinuteEur
            prefs[Keys.LAST_PARKING_FEE] = session.parkingFeeEur
        }
    }

    /**
     * Restaura el perfil de fábrica y los umbrales por defecto, sin tocar la última sesión.
     *
     * Las tres partes se escriben en una única transacción de DataStore: si el
     * proceso muriera a mitad de la restauración, o se aplican los tres valores
     * de fábrica o no se aplica ninguno, nunca una mezcla a medias.
     */
    suspend fun restoreDefaults() {
        dataStore.edit { prefs ->
            writeVehicle(prefs, VehicleProfile.BYD_ATTO_2_COMFORT)
            writeThresholds(prefs, PriceThresholds.DEFAULT)
            prefs[Keys.ESTIMATION_MODE] = EstimationMode.DEFAULT.name
        }
    }

    private fun writeVehicle(prefs: MutablePreferences, vehicle: VehicleProfile) {
        prefs[Keys.VEHICLE_BRAND] = vehicle.brand
        prefs[Keys.VEHICLE_MODEL] = vehicle.model
        prefs[Keys.GROSS_CAPACITY] = vehicle.grossCapacityKWh
        prefs[Keys.USABLE_CAPACITY] = vehicle.usableCapacityKWh
        prefs[Keys.MAX_DC_POWER] = vehicle.maxDcPowerKw
        prefs[Keys.MAX_AC_POWER] = vehicle.maxAcPowerKw
        prefs[Keys.CONSUMPTION] = vehicle.consumptionKWhPer100Km
        prefs[Keys.AC_LOSS] = vehicle.acLossPercent
        prefs[Keys.DC_LOSS] = vehicle.dcLossPercent
    }

    private fun writeThresholds(prefs: MutablePreferences, thresholds: PriceThresholds) {
        prefs[Keys.PRICE_VERY_CHEAP_MAX] = thresholds.veryCheapMax
        prefs[Keys.PRICE_GOOD_MAX] = thresholds.goodMax
        prefs[Keys.PRICE_NORMAL_MAX] = thresholds.normalMax
        prefs[Keys.PRICE_EXPENSIVE_MAX] = thresholds.expensiveMax
    }

    /** Traduce las preferencias almacenadas al modelo de dominio. */
    private fun Preferences.toAppSettings(): AppSettings {
        val defaultVehicle = VehicleProfile.BYD_ATTO_2_COMFORT
        val vehicle = VehicleProfile(
            id = VehicleProfile.DEFAULT_ID,
            brand = this[Keys.VEHICLE_BRAND] ?: defaultVehicle.brand,
            model = this[Keys.VEHICLE_MODEL] ?: defaultVehicle.model,
            // Un valor ausente, no numérico (NaN/±Infinito) o no positivo se
            // sustituye por el de fábrica: una capacidad o potencia a cero o en
            // negativo dejaría la app en un estado inválido al releerla.
            grossCapacityKWh = positiveOrDefault(this[Keys.GROSS_CAPACITY], defaultVehicle.grossCapacityKWh),
            usableCapacityKWh = positiveOrDefault(this[Keys.USABLE_CAPACITY], defaultVehicle.usableCapacityKWh),
            maxDcPowerKw = positiveOrDefault(this[Keys.MAX_DC_POWER], defaultVehicle.maxDcPowerKw),
            maxAcPowerKw = positiveOrDefault(this[Keys.MAX_AC_POWER], defaultVehicle.maxAcPowerKw),
            consumptionKWhPer100Km = positiveOrDefault(
                this[Keys.CONSUMPTION],
                defaultVehicle.consumptionKWhPer100Km,
            ),
            acLossPercent = lossPercentOrDefault(this[Keys.AC_LOSS], defaultVehicle.acLossPercent),
            dcLossPercent = lossPercentOrDefault(this[Keys.DC_LOSS], defaultVehicle.dcLossPercent),
        )
        val thresholds = PriceThresholds(
            veryCheapMax = this[Keys.PRICE_VERY_CHEAP_MAX] ?: PriceThresholds.DEFAULT.veryCheapMax,
            goodMax = this[Keys.PRICE_GOOD_MAX] ?: PriceThresholds.DEFAULT.goodMax,
            normalMax = this[Keys.PRICE_NORMAL_MAX] ?: PriceThresholds.DEFAULT.normalMax,
            expensiveMax = this[Keys.PRICE_EXPENSIVE_MAX] ?: PriceThresholds.DEFAULT.expensiveMax,
        )
        val lastSession = LastSession(
            pricePerKWh = this[Keys.LAST_PRICE] ?: 0.0,
            chargerPowerKw = this[Keys.LAST_POWER] ?: 0.0,
            currentType = currentTypeOf(this[Keys.LAST_CURRENT_TYPE]),
            startSocPercent = this[Keys.LAST_START_SOC] ?: 20,
            targetSocPercent = this[Keys.LAST_TARGET_SOC] ?: 80,
            startFeeEur = this[Keys.LAST_START_FEE] ?: 0.0,
            pricePerMinuteEur = this[Keys.LAST_PRICE_PER_MINUTE] ?: 0.0,
            parkingFeeEur = this[Keys.LAST_PARKING_FEE] ?: 0.0,
        )
        return AppSettings(
            vehicle = vehicle,
            estimationMode = EstimationMode.fromName(this[Keys.ESTIMATION_MODE]),
            // Unos umbrales incoherentes guardados por error no deben romper la app.
            priceThresholds = if (thresholds.isValid()) thresholds else PriceThresholds.DEFAULT,
            lastSession = lastSession,
        )
    }

    private fun currentTypeOf(name: String?): CurrentType =
        CurrentType.entries.firstOrNull { it.name == name } ?: CurrentType.DC

    /** Una capacidad, potencia o consumo debe ser finito y estrictamente positivo. */
    private fun positiveOrDefault(stored: Double?, default: Double): Double =
        stored?.takeIf { it.isFinite() && it > 0.0 } ?: default

    /** Un porcentaje de pérdidas debe ser finito y estar en [0, 100). */
    private fun lossPercentOrDefault(stored: Double?, default: Double): Double =
        stored?.takeIf { it.isFinite() && it >= 0.0 && it < 100.0 } ?: default

    /** Claves de DataStore. Se mantienen agrupadas para evitar duplicados. */
    private object Keys {
        val VEHICLE_BRAND = stringPreferencesKey("vehicle_brand")
        val VEHICLE_MODEL = stringPreferencesKey("vehicle_model")
        val GROSS_CAPACITY = doublePreferencesKey("vehicle_gross_capacity")
        val USABLE_CAPACITY = doublePreferencesKey("vehicle_usable_capacity")
        val MAX_DC_POWER = doublePreferencesKey("vehicle_max_dc_power")
        val MAX_AC_POWER = doublePreferencesKey("vehicle_max_ac_power")
        val CONSUMPTION = doublePreferencesKey("vehicle_consumption")
        val AC_LOSS = doublePreferencesKey("vehicle_ac_loss")
        val DC_LOSS = doublePreferencesKey("vehicle_dc_loss")

        val ESTIMATION_MODE = stringPreferencesKey("estimation_mode")

        val PRICE_VERY_CHEAP_MAX = doublePreferencesKey("price_very_cheap_max")
        val PRICE_GOOD_MAX = doublePreferencesKey("price_good_max")
        val PRICE_NORMAL_MAX = doublePreferencesKey("price_normal_max")
        val PRICE_EXPENSIVE_MAX = doublePreferencesKey("price_expensive_max")

        val LAST_PRICE = doublePreferencesKey("last_price")
        val LAST_POWER = doublePreferencesKey("last_power")
        val LAST_CURRENT_TYPE = stringPreferencesKey("last_current_type")
        val LAST_START_SOC = intPreferencesKey("last_start_soc")
        val LAST_TARGET_SOC = intPreferencesKey("last_target_soc")
        val LAST_START_FEE = doublePreferencesKey("last_start_fee")
        val LAST_PRICE_PER_MINUTE = doublePreferencesKey("last_price_per_minute")
        val LAST_PARKING_FEE = doublePreferencesKey("last_parking_fee")
    }
}
