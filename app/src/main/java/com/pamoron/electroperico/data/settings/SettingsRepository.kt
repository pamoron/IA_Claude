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
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Representación serializable de un perfil, aislada de la capa de dominio. */
@Serializable
private data class VehicleProfileDto(
    val id: String,
    val brand: String,
    val model: String,
    val grossCapacityKWh: Double,
    val usableCapacityKWh: Double,
    val maxDcPowerKw: Double,
    val maxAcPowerKw: Double,
    val consumptionKWhPer100Km: Double,
    val acLossPercent: Double,
    val dcLossPercent: Double,
)

private fun VehicleProfile.toDto() = VehicleProfileDto(
    id = id,
    brand = brand,
    model = model,
    grossCapacityKWh = grossCapacityKWh,
    usableCapacityKWh = usableCapacityKWh,
    maxDcPowerKw = maxDcPowerKw,
    maxAcPowerKw = maxAcPowerKw,
    consumptionKWhPer100Km = consumptionKWhPer100Km,
    acLossPercent = acLossPercent,
    dcLossPercent = dcLossPercent,
)

private fun VehicleProfileDto.toDomain() = VehicleProfile(
    id = id,
    brand = brand,
    model = model,
    grossCapacityKWh = grossCapacityKWh,
    usableCapacityKWh = usableCapacityKWh,
    maxDcPowerKw = maxDcPowerKw,
    maxAcPowerKw = maxAcPowerKw,
    consumptionKWhPer100Km = consumptionKWhPer100Km,
    acLossPercent = acLossPercent,
    dcLossPercent = dcLossPercent,
)

private fun VehicleProfile.isValid(): Boolean =
    id.isNotBlank() && (brand.isNotBlank() || model.isNotBlank()) &&
        grossCapacityKWh.isFinite() && grossCapacityKWh > 0.0 &&
        usableCapacityKWh.isFinite() && usableCapacityKWh > 0.0 &&
        maxDcPowerKw.isFinite() && maxDcPowerKw >= 0.0 &&
        maxAcPowerKw.isFinite() && maxAcPowerKw > 0.0 &&
        consumptionKWhPer100Km.isFinite() && consumptionKWhPer100Km > 0.0 &&
        acLossPercent.isFinite() && acLossPercent >= 0.0 && acLossPercent < 100.0 &&
        dcLossPercent.isFinite() && dcLossPercent >= 0.0 && dcLossPercent < 100.0

/**
 * Persistencia de los ajustes en DataStore Preferences.
 *
 * DataStore basta en esta fase: solo hay un puñado de valores escalares y un
 * perfil de vehículo. Room entrará cuando existan el historial y el comparador,
 * que sí son colecciones que hay que consultar y ordenar.
 */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Ajustes actuales. Ante un fallo de lectura se devuelven los valores por defecto. */
    val settings: Flow<AppSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { it.toAppSettings() }

    /** Guarda el perfil del vehículo editado en ajustes. */
    suspend fun updateVehicle(vehicle: VehicleProfile) {
        dataStore.edit { prefs ->
            val current = profilesFrom(prefs)
            val updated = current.map { if (it.id == vehicle.id) vehicle else it }
            writeProfiles(prefs, if (updated.any { it.id == vehicle.id }) updated else current + vehicle)
            prefs[Keys.ACTIVE_PROFILE_ID] = vehicle.id
        }
    }

    /** Activa uno de los perfiles guardados. */
    suspend fun selectProfile(id: String) {
        dataStore.edit { prefs ->
            if (profilesFrom(prefs).any { it.id == id }) prefs[Keys.ACTIVE_PROFILE_ID] = id
        }
    }

    /** Elimina un perfil, manteniendo siempre al menos uno disponible. */
    suspend fun deleteProfile(id: String) {
        dataStore.edit { prefs ->
            val current = profilesFrom(prefs)
            if (current.size <= 1) return@edit
            val updated = current.filterNot { it.id == id }
            writeProfiles(prefs, updated)
            if (prefs[Keys.ACTIVE_PROFILE_ID] == id) prefs[Keys.ACTIVE_PROFILE_ID] = updated.first().id
        }
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
            writeProfiles(prefs, listOf(VehicleProfile.BYD_ATTO_2_COMFORT))
            prefs[Keys.ACTIVE_PROFILE_ID] = VehicleProfile.DEFAULT_ID
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

    private fun writeProfiles(prefs: MutablePreferences, profiles: List<VehicleProfile>) {
        prefs[Keys.PROFILES] = json.encodeToString(profiles.map { it.toDto() })
    }

    private fun writeThresholds(prefs: MutablePreferences, thresholds: PriceThresholds) {
        prefs[Keys.PRICE_VERY_CHEAP_MAX] = thresholds.veryCheapMax
        prefs[Keys.PRICE_GOOD_MAX] = thresholds.goodMax
        prefs[Keys.PRICE_NORMAL_MAX] = thresholds.normalMax
        prefs[Keys.PRICE_EXPENSIVE_MAX] = thresholds.expensiveMax
    }

    /** Traduce las preferencias almacenadas al modelo de dominio. */
    private fun Preferences.toAppSettings(): AppSettings {
        val profiles = profilesFrom(this)
        val activeProfileId = this[Keys.ACTIVE_PROFILE_ID]
            ?.takeIf { id -> profiles.any { it.id == id } }
            ?: profiles.first().id
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
            profiles = profiles,
            activeProfileId = activeProfileId,
            estimationMode = EstimationMode.fromName(this[Keys.ESTIMATION_MODE]),
            priceThresholds = if (thresholds.isValid()) thresholds else PriceThresholds.DEFAULT,
            lastSession = lastSession,
        )
    }

    /** Lee JSON nuevo o reconstruye el perfil de instalaciones anteriores. */
    private fun profilesFrom(prefs: Preferences): List<VehicleProfile> {
        val decoded = prefs[Keys.PROFILES]?.let { raw ->
            runCatching { json.decodeFromString<List<VehicleProfileDto>>(raw).map { it.toDomain() } }
                .getOrDefault(emptyList())
        }.orEmpty().filter { it.isValid() }
        return decoded.ifEmpty { listOf(legacyVehicleFrom(prefs)) }
    }

    private fun legacyVehicleFrom(prefs: Preferences): VehicleProfile {
        val defaultVehicle = VehicleProfile.BYD_ATTO_2_COMFORT
        return VehicleProfile(
            id = VehicleProfile.DEFAULT_ID,
            brand = prefs[Keys.VEHICLE_BRAND] ?: defaultVehicle.brand,
            model = prefs[Keys.VEHICLE_MODEL] ?: defaultVehicle.model,
            // Un valor ausente, no numérico (NaN/±Infinito) o no positivo se
            // sustituye por el de fábrica: una capacidad o potencia a cero o en
            // negativo dejaría la app en un estado inválido al releerla.
            grossCapacityKWh = positiveOrDefault(prefs[Keys.GROSS_CAPACITY], defaultVehicle.grossCapacityKWh),
            usableCapacityKWh = positiveOrDefault(prefs[Keys.USABLE_CAPACITY], defaultVehicle.usableCapacityKWh),
            maxDcPowerKw = positiveOrDefault(prefs[Keys.MAX_DC_POWER], defaultVehicle.maxDcPowerKw),
            maxAcPowerKw = positiveOrDefault(prefs[Keys.MAX_AC_POWER], defaultVehicle.maxAcPowerKw),
            consumptionKWhPer100Km = positiveOrDefault(
                prefs[Keys.CONSUMPTION],
                defaultVehicle.consumptionKWhPer100Km,
            ),
            acLossPercent = lossPercentOrDefault(prefs[Keys.AC_LOSS], defaultVehicle.acLossPercent),
            dcLossPercent = lossPercentOrDefault(prefs[Keys.DC_LOSS], defaultVehicle.dcLossPercent),
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
        val PROFILES = stringPreferencesKey("vehicle_profiles")
        val ACTIVE_PROFILE_ID = stringPreferencesKey("active_vehicle_profile_id")

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
