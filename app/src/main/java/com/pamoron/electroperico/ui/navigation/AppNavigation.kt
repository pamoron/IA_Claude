package com.pamoron.electroperico.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.currentBackStackEntryAsState
import com.pamoron.electroperico.R
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
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val destinations = listOf(
        NavigationDestination(Routes.CALCULADORA, R.string.navegacion_calcular, Icons.Filled.Calculate),
        NavigationDestination(Routes.COMPARADOR, R.string.navegacion_comparar, Icons.Filled.Compare),
        NavigationDestination(Routes.HISTORIAL, R.string.navegacion_historial, Icons.Filled.History),
        NavigationDestination(Routes.AJUSTES, R.string.navegacion_ajustes, Icons.Filled.Settings),
    )
    Scaffold(
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                            }
                        },
                        icon = { androidx.compose.material3.Icon(destination.icon, null) },
                        label = { Text(stringResource(destination.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding -> NavHost(
        navController = navController,
        startDestination = Routes.CALCULADORA,
        modifier = Modifier.padding(innerPadding),
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
                onBack = { navController.navigate(Routes.CALCULADORA) },
            )
        }
        composable(Routes.HISTORIAL) {
            HistoryScreen(
                onBack = { navController.navigate(Routes.CALCULADORA) },
            )
        }
        composable(Routes.AJUSTES) {
            SettingsScreen(
                onBack = { navController.navigate(Routes.CALCULADORA) },
            )
        }
    }
}
}

private data class NavigationDestination(
    val route: String,
    val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)
