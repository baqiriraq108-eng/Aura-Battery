package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    paletteId: String = "AuraEarthy",
    content: @Composable () -> Unit,
) {
    val palette = ExpressivePalettes.getPalette(paletteId)
    val clayPalette = ExpressivePalettes.getClayPalette(paletteId, darkTheme)
    val colorScheme = if (darkTheme) palette.darkScheme else palette.lightScheme

    CompositionLocalProvider(LocalAuraClayPalette provides clayPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
