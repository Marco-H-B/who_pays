package com.whopays.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.whopays.app.ui.WhoPaysScreen
import com.whopays.app.ui.theme.WhoPaysTheme

/**
 * Actividad principal nativa de Android para WHO PAYS?.
 * Configura el modo inmersivo de pantalla completa (Edge-to-Edge) bajo Apple HIG Titanium.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Habilitar diseño de borde a borde (Edge-to-Edge)
        enableEdgeToEdge()

        // Ocultar barras del sistema para interacción táctil inmersiva
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            WhoPaysTheme {
                WhoPaysScreen()
            }
        }
    }
}
