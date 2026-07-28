package com.pamoron.electroperico.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pamoron.electroperico.ui.calculator.CalculatorScreen
import com.pamoron.electroperico.ui.comparator.ComparatorScreen
import com.pamoron.electroperico.ui.history.HistoryScreen
import com.pamoron.electroperico.ui.settings.SettingsScreen

/**
 * Destinos de la aplicación.
 *
 * En una fase posterior se añadirán aquí los perfiles múltiples de vehículo;
 * el grafo ya está preparado para crecer.
 */
object Routes {
    const val CALCULADORA = "calculadora"
    const val COMPARADOR = "comparador"
    const val HISTORIAL = "historial"
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
                onOpenComparator = { navController.navigate(Routes.COMPARADOR) },
                onOpenHistory = { navController.navigate(Routes.HISTORIAL) },
            )
        }
        composable(Routes.COMPARADOR) {
            ComparatorScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.HISTORIAL) {
            HistoryScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.AJUSTES) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
