package com.pamoron.electroperico

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.pamoron.electroperico.ui.navigation.ElectroPericoApp
import com.pamoron.electroperico.ui.theme.ElectroPericoTheme

/**
 * Única actividad de la aplicación.
 *
 * Toda la navegación ocurre en Compose; aquí solo se instala el tema y el
 * grafo de navegación.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ElectroPericoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    ElectroPericoApp()
                }
            }
        }
    }
}
