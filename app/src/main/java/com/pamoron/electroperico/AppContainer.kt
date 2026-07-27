package com.pamoron.electroperico

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.pamoron.electroperico.data.comparator.ComparatorRepository
import com.pamoron.electroperico.data.settings.SettingsRepository

/** Ajustes, perfil del vehículo y última sesión. */
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ajustes",
)

/** Opciones guardadas en el comparador. */
private val Context.comparatorDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "comparador",
)

/**
 * Contenedor de dependencias.
 *
 * Se prefiere una inyección manual sencilla a Hilt: la app tiene dos
 * repositorios y unos pocos ViewModels, y así el proyecto compila sin
 * procesadores de anotaciones ni sus incompatibilidades de versión.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(appContext.settingsDataStore)
    }

    val comparatorRepository: ComparatorRepository by lazy {
        ComparatorRepository(appContext.comparatorDataStore)
    }
}
