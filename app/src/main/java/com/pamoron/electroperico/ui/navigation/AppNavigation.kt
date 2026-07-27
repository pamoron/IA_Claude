package com.pamoron.electroperico.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pamoron.electroperico.ui.calculator.CalculatorScreen
import com.pamoron.electroperico.ui.settings.SettingsScreen

/**
 * Destinos de la aplicación.
 *
 * En la fase 2 se añadirán aquí "comparador", "historial" y "perfiles"; el
 * grafo ya está preparado para crecer.
 */
object Routes {
    const val CALCULADORA = "calculadora"
    const val AJUSTES = "ajustes"
}

/** Grafo de navegación completo. */
@Composable
fun ElectroPericoApp(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Routes.CALCULADORA,
    ) {
        composable(Routes.CALCULADORA) {
            CalculatorScreen(
                onOpenSettings = { navController.navigate(Routes.AJUSTES) },
            )
        }
        composable(Routes.AJUSTES) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
