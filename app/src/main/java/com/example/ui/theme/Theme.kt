package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    paletteId: String = "HyperLime",
    content: @Composable () -> Unit,
) {
    val palette = ExpressivePalettes.getPalette(paletteId)
    val colorScheme = if (darkTheme) palette.darkScheme else palette.lightScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

