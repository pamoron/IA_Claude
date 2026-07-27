package com.pamoron.electroperico.data.comparator

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pamoron.electroperico.domain.model.ChargerOption
import com.pamoron.electroperico.domain.model.ComparisonSort
import com.pamoron.electroperico.domain.model.CurrentType
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Versión serializable de una opción del comparador.
 *
 * El modelo de dominio se mantiene libre de anotaciones de serialización; este
 * objeto de transferencia es el único que sabe de JSON.
 */
@Serializable
private data class ChargerOptionDto(
    val id: String,
    val name: String,
    val pricePerKWh: Double,
    val chargerPowerKw: Double,
    val currentType: String,
    val startSocPercent: Int,
    val targetSocPercent: Int,
    val startFeeEur: Double = 0.0,
    val pricePerMinuteEur: Double = 0.0,
    val parkingFeeEur: Double = 0.0,
)

private fun ChargerOption.toDto() = ChargerOptionDto(
    id = id,
    name = name,
    pricePerKWh = pricePerKWh,
    chargerPowerKw = chargerPowerKw,
    currentType = currentType.name,
    startSocPercent = startSocPercent,
    targetSocPercent = targetSocPercent,
    startFeeEur = startFeeEur,
    pricePerMinuteEur = pricePerMinuteEur,
    parkingFeeEur = parkingFeeEur,
)

private fun ChargerOptionDto.toDomain() = ChargerOption(
    id = id,
    name = name,
    pricePerKWh = pricePerKWh,
    chargerPowerKw = chargerPowerKw,
    currentType = CurrentType.entries.firstOrNull { it.name == currentType } ?: CurrentType.DC,
    startSocPercent = startSocPercent,
    targetSocPercent = targetSocPercent,
    startFeeEur = startFeeEur,
    pricePerMinuteEur = pricePerMinuteEur,
    parkingFeeEur = parkingFeeEur,
)

/**
 * Opciones guardadas en el comparador.
 *
 * Son como mucho cinco registros: no justifican una base de datos, así que se
 * guardan como JSON dentro del mismo DataStore de preferencias. Si en el futuro
 * el comparador crece, esta clase es el único punto que habría que cambiar.
 */
class ComparatorRepository(private val dataStore: DataStore<Preferences>) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** Opciones guardadas, en el orden en que se añadieron. */
    val options: Flow<List<ChargerOption>> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs -> decode(prefs[Keys.OPTIONS]) }

    /** Criterio de ordenación elegido. */
    val sort: Flow<ComparisonSort> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs -> ComparisonSort.fromName(prefs[Keys.SORT]) }

    /**
     * Añade una opción nueva o sustituye la que tenga el mismo identificador.
     * Si ya hay [ChargerOption.MAX_OPTIONS] opciones, la nueva se descarta.
     *
     * @return `true` si se ha guardado.
     */
    suspend fun upsert(option: ChargerOption): Boolean {
        var saved = false
        dataStore.edit { prefs ->
            val current = decode(prefs[Keys.OPTIONS])
            val index = current.indexOfFirst { it.id == option.id }
            val updated = when {
                index >= 0 -> current.toMutableList().also { it[index] = option }
                current.size < ChargerOption.MAX_OPTIONS -> current + option
                else -> null
            }
            if (updated != null) {
                prefs[Keys.OPTIONS] = encode(updated)
                saved = true
            }
        }
        return saved
    }

    /** Elimina una opción por identificador. */
    suspend fun delete(id: String) {
        dataStore.edit { prefs ->
            prefs[Keys.OPTIONS] = encode(decode(prefs[Keys.OPTIONS]).filterNot { it.id == id })
        }
    }

    /** Vacía el comparador. */
    suspend fun clear() {
        dataStore.edit { prefs -> prefs[Keys.OPTIONS] = encode(emptyList()) }
    }

    /** Guarda el criterio de ordenación. */
    suspend fun setSort(sort: ComparisonSort) {
        dataStore.edit { prefs -> prefs[Keys.SORT] = sort.name }
    }

    private fun encode(options: List<ChargerOption>): String =
        json.encodeToString(options.map { it.toDto() })

    /** Un JSON corrupto no debe impedir abrir la app: se trata como lista vacía. */
    private fun decode(raw: String?): List<ChargerOption> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString<List<ChargerOptionDto>>(raw).map { it.toDomain() }
        }.getOrDefault(emptyList())
    }

    private object Keys {
        val OPTIONS = stringPreferencesKey("comparator_options")
        val SORT = stringPreferencesKey("comparator_sort")
    }
}
