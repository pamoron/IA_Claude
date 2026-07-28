package com.pamoron.electroperico.data.history

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pamoron.electroperico.domain.calc.Money
import com.pamoron.electroperico.domain.model.ChargerOption
import com.pamoron.electroperico.domain.model.CurrentType
import com.pamoron.electroperico.domain.model.HistoryEntry
import com.pamoron.electroperico.domain.model.HistorySort
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Versión serializable de una entrada del historial.
 *
 * Los importes viajan como [Double] y se reconstruyen con [Money] al leerlos,
 * de modo que el redondeo sigue estando controlado en un único sitio.
 */
@Serializable
private data class HistoryEntryDto(
    val id: String,
    val timestampMillis: Long,
    val name: String,
    val pricePerKWh: Double,
    val chargerPowerKw: Double,
    val currentType: String,
    val startSocPercent: Int,
    val targetSocPercent: Int,
    val startFeeEur: Double = 0.0,
    val pricePerMinuteEur: Double = 0.0,
    val parkingFeeEur: Double = 0.0,
    val totalCostEur: Double,
    val minutes: Int,
    val effectivePricePerKWh: Double,
    val energyBilledKWh: Double,
    val energyNeededKWh: Double,
    val rangeAddedKm: Double,
    val favorite: Boolean = false,
)

private fun HistoryEntry.toDto() = HistoryEntryDto(
    id = id,
    timestampMillis = timestampMillis,
    name = charger.name,
    pricePerKWh = charger.pricePerKWh,
    chargerPowerKw = charger.chargerPowerKw,
    currentType = charger.currentType.name,
    startSocPercent = charger.startSocPercent,
    targetSocPercent = charger.targetSocPercent,
    startFeeEur = charger.startFeeEur,
    pricePerMinuteEur = charger.pricePerMinuteEur,
    parkingFeeEur = charger.parkingFeeEur,
    totalCostEur = totalCostEur.toDouble(),
    minutes = minutes,
    effectivePricePerKWh = effectivePricePerKWh.toDouble(),
    energyBilledKWh = energyBilledKWh,
    energyNeededKWh = energyNeededKWh,
    rangeAddedKm = rangeAddedKm,
    favorite = favorite,
)

private fun HistoryEntryDto.toDomain() = HistoryEntry(
    id = id,
    timestampMillis = timestampMillis,
    charger = ChargerOption(
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
    ),
    totalCostEur = Money.eur(totalCostEur),
    minutes = minutes,
    effectivePricePerKWh = Money.rate(effectivePricePerKWh),
    energyBilledKWh = energyBilledKWh,
    energyNeededKWh = energyNeededKWh,
    rangeAddedKm = rangeAddedKm,
    favorite = favorite,
)

/**
 * Historial local de recargas simuladas.
 *
 * Es opcional: la app funciona igual sin guardar nada. No sale del dispositivo
 * y no requiere ni registro ni conexión.
 */
class HistoryRepository(private val dataStore: DataStore<Preferences>) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** Entradas guardadas, sin ordenar. */
    val entries: Flow<List<HistoryEntry>> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs -> decode(prefs[Keys.ENTRIES]) }

    /** Criterio de ordenación elegido. */
    val sort: Flow<HistorySort> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs -> HistorySort.fromName(prefs[Keys.SORT]) }

    /** Añade una entrada nueva o sustituye la que tenga el mismo identificador. */
    suspend fun upsert(entry: HistoryEntry) {
        dataStore.edit { prefs ->
            val current = decode(prefs[Keys.ENTRIES])
            val index = current.indexOfFirst { it.id == entry.id }
            val updated = if (index >= 0) {
                current.toMutableList().also { it[index] = entry }
            } else {
                // Las más recientes van al principio; trimToLimit() se encarga
                // de recortar sin perder los favoritos si se supera el tope.
                listOf(entry) + current
            }
            prefs[Keys.ENTRIES] = encode(trimToLimit(updated))
        }
    }

    /**
     * Si se supera [MAX_ENTRIES], conserva primero los favoritos y llena el
     * resto con las entradas no favoritas más recientes.
     *
     * Recortar sin más por la cola (quedarse solo con las `MAX_ENTRIES`
     * primeras) haría desaparecer sin aviso un cargador que el usuario marcó
     * como favorito en cuanto se acumularan suficientes recargas nuevas, que es
     * justo lo contrario de lo que promete la estrella.
     */
    private fun trimToLimit(entries: List<HistoryEntry>): List<HistoryEntry> {
        if (entries.size <= MAX_ENTRIES) return entries
        val (favorites, rest) = entries.partition { it.favorite }
        return (favorites + rest).take(MAX_ENTRIES)
    }

    /** Elimina una entrada. */
    suspend fun delete(id: String) {
        dataStore.edit { prefs ->
            prefs[Keys.ENTRIES] = encode(decode(prefs[Keys.ENTRIES]).filterNot { it.id == id })
        }
    }

    /** Marca o desmarca una entrada como favorita. */
    suspend fun toggleFavorite(id: String) {
        dataStore.edit { prefs ->
            val updated = decode(prefs[Keys.ENTRIES]).map {
                if (it.id == id) it.copy(favorite = !it.favorite) else it
            }
            prefs[Keys.ENTRIES] = encode(updated)
        }
    }

    /** Vacía el historial. */
    suspend fun clear() {
        dataStore.edit { prefs -> prefs[Keys.ENTRIES] = encode(emptyList()) }
    }

    /** Guarda el criterio de ordenación. */
    suspend fun setSort(sort: HistorySort) {
        dataStore.edit { prefs -> prefs[Keys.SORT] = sort.name }
    }

    private fun encode(entries: List<HistoryEntry>): String =
        json.encodeToString(entries.map { it.toDto() })

    /** Un JSON corrupto no debe impedir abrir la app: se trata como lista vacía. */
    private fun decode(raw: String?): List<HistoryEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString<List<HistoryEntryDto>>(raw).map { it.toDomain() }
        }.getOrDefault(emptyList())
    }

    private object Keys {
        val ENTRIES = stringPreferencesKey("history_entries")
        val SORT = stringPreferencesKey("history_sort")
    }

    companion object {
        /** Tope de entradas guardadas. */
        const val MAX_ENTRIES: Int = 200
    }
}
