package com.pamoron.electroperico

import android.app.Application

/** Punto de entrada de la aplicación; construye el contenedor de dependencias. */
class ElectroPericoApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
