package com.pamoron.electroperico

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.pamoron.electroperico.data.settings.SettingsRepository

/** Única instancia de DataStore de la aplicación. */
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ajustes",
)

/**
 * Contenedor de dependencias.
 *
 * Se prefiere una inyección manual sencilla a Hilt: la app solo tiene un
 * repositorio y dos ViewModels, y así el proyecto compila sin procesadores de
 * anotaciones ni sus incompatibilidades de versión.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(appContext.settingsDataStore)
    }
}
