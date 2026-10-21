package com.whopays.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = TitaniumWhite,
    secondary = TitaniumMuted,
    background = TitaniumBg,
    surface = TitaniumSurface,
    onPrimary = TitaniumBg,
    onSecondary = TitaniumWhite,
    onBackground = TitaniumWhite,
    onSurface = TitaniumWhite
)

@Composable
fun WhoPaysTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
