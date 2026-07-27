// Fichero de construcción de nivel raíz.
// Solo declara los plugins que usan los submódulos; no aplica ninguno aquí.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
