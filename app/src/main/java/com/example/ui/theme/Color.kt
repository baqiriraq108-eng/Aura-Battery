package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Core 3D Clay colors and diversified non-repeating tile/hill palette.
 * Ensures green appears at most twice on any screen (Header Wave + 1 Mint Hill),
 * while metric cards and hills showcase Slate-Indigo, Terracotta, Berry Rust,
 * Mustard Ochre, Warm Plum, Coral, and Warm Sand clay tones.
 */
object AuraTactileColors {
    // Light & Dark Mode Default Header & Parchment Colors
    val ForestHeaderLight = Color(0xFF2B4C3E)
    val ForestHeaderDark = Color(0xFF1B3328)

    val ParchmentBgLight = Color(0xFFF6F2E6)
    val ParchmentBgDark = Color(0xFF131C18)

    val SandMedallionLight = Color(0xFFE5DEC9)
    val SandMedallionLipLight = Color(0xFFCBC2A8)
    val SandMedallionDark = Color(0xFF25322C)
    val SandMedallionLipDark = Color(0xFF161F1B)

    // Diversified 3D Clay Metric Tiles (Synchronized between Dashboard & Settings)
    val TileSlateFace = Color(0xFF3D5266)
    val TileSlateLip = Color(0xFF253442)

    // Calibrated Sage-Emerald Green for Battery Health (matches saturation & lightness weight of Slate, Rust & Mustard)
    val TileHealthGreenFace = Color(0xFF3B7A57)
    val TileHealthGreenLip = Color(0xFF234D36)

    val TileTerracottaFace = Color(0xFFC86441)
    val TileTerracottaLip = Color(0xFF873D23)

    val TileRustFace = Color(0xFFA84A31)
    val TileRustLip = Color(0xFF6E2B19)

    val TileMustardFace = Color(0xFFD8AC2C)
    val TileMustardLip = Color(0xFF967314)

    val TilePlumFace = Color(0xFF7D4B6D)
    val TilePlumLip = Color(0xFF4F2C44)

    val TileSandFace = Color(0xFFE4DEC9)
    val TileSandLip = Color(0xFFBFB69C)

    // 25h Activity Ribbon & Diversified Clay Hills (Only 1 Green Hill!)
    val RibbonTopFace = Color(0xFFC45A37)
    val RibbonHighlight = Color(0xFFDC7654)
    val RibbonExtrusion = Color(0xFF7D341D)

    val HillSlate = Color(0xFF3D5266)
    val HillPlum = Color(0xFF9E4784)
    val HillCoral = Color(0xFFF36A55)
    val HillCopper = Color(0xFFC97848)
    val HillGold = Color(0xFFE7B82E)
    val HillMint = Color(0xFF5BB982)

    val InkDark = Color(0xFF222222)
    val InkMuted = Color(0xFF222222)
    val CreamText = Color(0xFFFFFFFF)
    val StarGold = Color(0xFFE8DFC8)

    /**
     * Returns #222222 on yellow/mustard or light clay surfaces, and #FFFFFF on dark clay surfaces
     * (navy/slate, terracotta/brick red, green, plum/purple, cocoa).
     */
    fun textOn(faceColor: Color): Color {
        return if (faceColor == TileSandFace) {
            InkDark
        } else {
            CreamText
        }
    }
}

/**
 * Dynamic 3D Clay Theme Palette propagated via CompositionLocal so selecting any
 * color option in Settings immediately transforms the entire application's colors.
 */
data class AuraClayThemePalette(
    val id: String,
    val nameEn: String,
    val headerWaveFace: Color,
    val headerWaveLip: Color,
    val screenBackground: Color,
    val cardFace: Color,
    val cardLip: Color,
    val recessedTrackBg: Color,
    val recessedTrackBorder: Color,
    val primaryAccentFace: Color,
    val primaryAccentLip: Color,
    val secondaryAccentFace: Color,
    val secondaryAccentLip: Color,
    val tertiaryAccentFace: Color,
    val tertiaryAccentLip: Color,
    val inkColor: Color,
    val mutedInkColor: Color
)

val LocalAuraClayPalette = staticCompositionLocalOf {
    ExpressivePalettes.getClayPalette("AuraEarthy", isDark = false)
}

data class ExpressivePaletteInfo(
    val id: String,
    val nameEn: String,
    val accentColor: Color,
    val secondarySwatchColor: Color,
    val containerColor: Color,
    val darkScheme: ColorScheme,
    val lightScheme: ColorScheme
)

object ExpressivePalettes {
    // 0. Aura Earthy (Signature Forest & Terracotta Clay)
    val AuraEarthy = ExpressivePaletteInfo(
        id = "AuraEarthy",
        nameEn = "Aura Earthy",
        accentColor = Color(0xFFC86441),
        secondarySwatchColor = Color(0xFF2B4C3E),
        containerColor = Color(0xFF2B4C3E),
        darkScheme = darkColorScheme(
            primary = Color(0xFFD97754),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFF2B4C3E),
            onPrimaryContainer = Color(0xFFFFFFFF),
            secondary = Color(0xFFD8AC2C),
            onSecondary = Color(0xFF222222),
            secondaryContainer = Color(0xFF25352C),
            onSecondaryContainer = Color(0xFFFFFFFF),
            tertiary = Color(0xFF3D5266),
            tertiaryContainer = Color(0xFF1D3328),
            background = Color(0xFF131C18),
            onBackground = Color(0xFF222222),
            surface = Color(0xFF18241E),
            onSurface = Color(0xFF222222),
            surfaceVariant = Color(0xFF24332B),
            onSurfaceVariant = Color(0xFF222222),
            outline = Color(0xFF53665B),
            outlineVariant = Color(0xFF33443B),
            error = Color(0xFFF36A55),
            errorContainer = Color(0xFF6E2B19),
            onErrorContainer = Color(0xFFFFFFFF)
        ),
        lightScheme = lightColorScheme(
            primary = Color(0xFFC86441),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFF2B4C3E),
            onPrimaryContainer = Color(0xFFFFFFFF),
            secondary = Color(0xFFD8AC2C),
            onSecondary = Color(0xFF222222),
            secondaryContainer = Color(0xFFE5DEC9),
            onSecondaryContainer = Color(0xFF222222),
            tertiary = Color(0xFF3D5266),
            tertiaryContainer = Color(0xFFD5E6DC),
            background = Color(0xFFF6F2E6),
            onBackground = Color(0xFF222222),
            surface = Color(0xFFF6F2E6),
            onSurface = Color(0xFF222222),
            surfaceVariant = Color(0xFFE5DEC9),
            onSurfaceVariant = Color(0xFF222222),
            outline = Color(0xFF8C8470),
            outlineVariant = Color(0xFFCBC2A8),
            error = Color(0xFFA84A31),
            errorContainer = Color(0xFFF9DEDC),
            onErrorContainer = Color(0xFF222222)
        )
    )

    // 1. Slate & Amber (Deep Slate-Indigo Header + Golden Ochre Clay)
    val HyperLime = ExpressivePaletteInfo(
        id = "HyperLime",
        nameEn = "Slate & Amber",
        accentColor = Color(0xFFD8AC2C),
        secondarySwatchColor = Color(0xFF2E4357),
        containerColor = Color(0xFF2E4357),
        darkScheme = AuraEarthy.darkScheme.copy(
            primary = Color(0xFFE7B82E),
            onPrimary = Color(0xFF222222),
            primaryContainer = Color(0xFF223344),
            secondary = Color(0xFFC86441),
            onSecondary = Color(0xFFFFFFFF),
            background = Color(0xFF121A22),
            surface = Color(0xFF18222C)
        ),
        lightScheme = AuraEarthy.lightScheme.copy(
            primary = Color(0xFFD8AC2C),
            onPrimary = Color(0xFF222222),
            primaryContainer = Color(0xFF2E4357),
            secondary = Color(0xFFC86441),
            onSecondary = Color(0xFFFFFFFF),
            background = Color(0xFFF3F5F8),
            surface = Color(0xFFF3F5F8)
        )
    )

    // 2. Ochre & Espresso (Warm Cocoa Clay Header + Ochre & Terracotta)
    val ElectricCobalt = ExpressivePaletteInfo(
        id = "ElectricCobalt",
        nameEn = "Ochre & Cocoa",
        accentColor = Color(0xFFD8AC2C),
        secondarySwatchColor = Color(0xFF4E3629),
        containerColor = Color(0xFF4E3629),
        darkScheme = AuraEarthy.darkScheme.copy(
            primary = Color(0xFFE7B82E),
            onPrimary = Color(0xFF222222),
            primaryContainer = Color(0xFF3B281E),
            secondary = Color(0xFFC86441),
            onSecondary = Color(0xFFFFFFFF),
            background = Color(0xFF1C1512),
            surface = Color(0xFF261D19)
        ),
        lightScheme = AuraEarthy.lightScheme.copy(
            primary = Color(0xFFB58917),
            onPrimary = Color(0xFF222222),
            primaryContainer = Color(0xFF4E3629),
            secondary = Color(0xFFC86441),
            onSecondary = Color(0xFFFFFFFF),
            background = Color(0xFFF8F2E8),
            surface = Color(0xFFF8F2E8)
        )
    )

    // 3. Terracotta & Rust (Canyon Clay Header + Coral & Gold)
    val SunsetFlame = ExpressivePaletteInfo(
        id = "SunsetFlame",
        nameEn = "Canyon Coral",
        accentColor = Color(0xFFF36A55),
        secondarySwatchColor = Color(0xFF6B2F23),
        containerColor = Color(0xFF6B2F23),
        darkScheme = AuraEarthy.darkScheme.copy(
            primary = Color(0xFFF36A55),
            primaryContainer = Color(0xFF4A2018),
            secondary = Color(0xFFE7B82E),
            background = Color(0xFF1E1311),
            surface = Color(0xFF281A17)
        ),
        lightScheme = AuraEarthy.lightScheme.copy(
            primary = Color(0xFFA84A31),
            primaryContainer = Color(0xFF6B2F23),
            secondary = Color(0xFFD8AC2C),
            background = Color(0xFFFAF1EC),
            surface = Color(0xFFFAF1EC)
        )
    )

    // 4. Plum & Rosebud (Mulberry Clay Header + Rose & Ochre)
    val CyberViolet = ExpressivePaletteInfo(
        id = "CyberViolet",
        nameEn = "Plum & Velvet",
        accentColor = Color(0xFF9E4784),
        secondarySwatchColor = Color(0xFF472940),
        containerColor = Color(0xFF472940),
        darkScheme = AuraEarthy.darkScheme.copy(
            primary = Color(0xFFB85D9C),
            primaryContainer = Color(0xFF361E30),
            secondary = Color(0xFFD8AC2C),
            background = Color(0xFF1A1318),
            surface = Color(0xFF241B22)
        ),
        lightScheme = AuraEarthy.lightScheme.copy(
            primary = Color(0xFF9E4784),
            primaryContainer = Color(0xFF472940),
            secondary = Color(0xFFD8AC2C),
            background = Color(0xFFF7F0F5),
            surface = Color(0xFFF7F0F5)
        )
    )

    // 5.Nordicfjord & Copper (Deep Teal-Slate Header + Copper Clay)
    val EmeraldJade = ExpressivePaletteInfo(
        id = "EmeraldJade",
        nameEn = "Fjord & Copper",
        accentColor = Color(0xFFC97848),
        secondarySwatchColor = Color(0xFF23464A),
        containerColor = Color(0xFF23464A),
        darkScheme = AuraEarthy.darkScheme.copy(
            primary = Color(0xFFD98757),
            primaryContainer = Color(0xFF193336),
            secondary = Color(0xFFD8AC2C),
            background = Color(0xFF111B1D),
            surface = Color(0xFF182528)
        ),
        lightScheme = AuraEarthy.lightScheme.copy(
            primary = Color(0xFFC97848),
            primaryContainer = Color(0xFF23464A),
            secondary = Color(0xFFD8AC2C),
            background = Color(0xFFF0F4F4),
            surface = Color(0xFFF0F4F4)
        )
    )

    val allPalettes = listOf(
        AuraEarthy,
        HyperLime,
        ElectricCobalt,
        SunsetFlame,
        CyberViolet,
        EmeraldJade
    )

    fun getPalette(id: String): ExpressivePaletteInfo {
        return allPalettes.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: AuraEarthy
    }

    fun getClayPalette(id: String, isDark: Boolean): AuraClayThemePalette {
        return when (id.lowercase()) {
            "hyperlime" -> AuraClayThemePalette(
                id = "HyperLime",
                nameEn = "Slate & Amber",
                headerWaveFace = if (isDark) Color(0xFF1F3040) else Color(0xFF2E4357),
                headerWaveLip = if (isDark) Color(0xFF121D28) else Color(0xFF1B2B38),
                screenBackground = if (isDark) Color(0xFF121A22) else Color(0xFFF2F4F7),
                cardFace = if (isDark) Color(0xFF21303E) else Color(0xFFE1E6EC),
                cardLip = if (isDark) Color(0xFF141E28) else Color(0xFFBCC5D0),
                recessedTrackBg = if (isDark) Color(0xFF16212B) else Color(0xFFD0D8E2),
                recessedTrackBorder = if (isDark) Color(0xFF33475B) else Color(0xFFB3Becc),
                primaryAccentFace = Color(0xFFD8AC2C),
                primaryAccentLip = Color(0xFF967314),
                secondaryAccentFace = Color(0xFFC86441),
                secondaryAccentLip = Color(0xFF873D23),
                tertiaryAccentFace = Color(0xFF3D5266),
                tertiaryAccentLip = Color(0xFF253442),
                inkColor = Color(0xFF222222),
                mutedInkColor = Color(0xFF222222)
            )
            "electriccobalt" -> AuraClayThemePalette(
                id = "ElectricCobalt",
                nameEn = "Ochre & Cocoa",
                headerWaveFace = if (isDark) Color(0xFF38261D) else Color(0xFF4E3629),
                headerWaveLip = if (isDark) Color(0xFF221610) else Color(0xFF332118),
                screenBackground = if (isDark) Color(0xFF1A1411) else Color(0xFFF8F1E7),
                cardFace = if (isDark) Color(0xFF2E231E) else Color(0xFFE9DFCF),
                cardLip = if (isDark) Color(0xFF1C1511) else Color(0xFFC9B9A3),
                recessedTrackBg = if (isDark) Color(0xFF211814) else Color(0xFFD9CBB6),
                recessedTrackBorder = if (isDark) Color(0xFF4A382F) else Color(0xFFBFB099),
                primaryAccentFace = Color(0xFFD8AC2C),
                primaryAccentLip = Color(0xFF967314),
                secondaryAccentFace = Color(0xFFA84A31),
                secondaryAccentLip = Color(0xFF6E2B19),
                tertiaryAccentFace = Color(0xFF6B4C3B),
                tertiaryAccentLip = Color(0xFF452F23),
                inkColor = Color(0xFF222222),
                mutedInkColor = Color(0xFF222222)
            )
            "sunsetflame" -> AuraClayThemePalette(
                id = "SunsetFlame",
                nameEn = "Canyon Coral",
                headerWaveFace = if (isDark) Color(0xFF4A2018) else Color(0xFF6B2F23),
                headerWaveLip = if (isDark) Color(0xFF2B110C) else Color(0xFF471C13),
                screenBackground = if (isDark) Color(0xFF1C1210) else Color(0xFFFAF0EB),
                cardFace = if (isDark) Color(0xFF30201C) else Color(0xFFEEDCD4),
                cardLip = if (isDark) Color(0xFF1E1310) else Color(0xFFCDB4A8),
                recessedTrackBg = if (isDark) Color(0xFF241613) else Color(0xFFE0C8BE),
                recessedTrackBorder = if (isDark) Color(0xFF52342D) else Color(0xFFC4A89C),
                primaryAccentFace = Color(0xFFF06752),
                primaryAccentLip = Color(0xFF9E3828),
                secondaryAccentFace = Color(0xFFD8AC2C),
                secondaryAccentLip = Color(0xFF967314),
                tertiaryAccentFace = Color(0xFF7D4B6D),
                tertiaryAccentLip = Color(0xFF4F2C44),
                inkColor = Color(0xFF222222),
                mutedInkColor = Color(0xFF222222)
            )
            "cyberviolet" -> AuraClayThemePalette(
                id = "CyberViolet",
                nameEn = "Plum & Velvet",
                headerWaveFace = if (isDark) Color(0xFF341D2F) else Color(0xFF4A2A43),
                headerWaveLip = if (isDark) Color(0xFF1F101C) else Color(0xFF30192B),
                screenBackground = if (isDark) Color(0xFF171116) else Color(0xFFF7F0F5),
                cardFace = if (isDark) Color(0xFF2B2029) else Color(0xFFE8DCE5),
                cardLip = if (isDark) Color(0xFF1B1319) else Color(0xFFC6B4C1),
                recessedTrackBg = if (isDark) Color(0xFF20171E) else Color(0xFFD8C7D3),
                recessedTrackBorder = if (isDark) Color(0xFF493545) else Color(0xFFBCA7B6),
                primaryAccentFace = Color(0xFF9E4784),
                primaryAccentLip = Color(0xFF662753),
                secondaryAccentFace = Color(0xFFD8AC2C),
                secondaryAccentLip = Color(0xFF967314),
                tertiaryAccentFace = Color(0xFFC86441),
                tertiaryAccentLip = Color(0xFF873D23),
                inkColor = Color(0xFF222222),
                mutedInkColor = Color(0xFF222222)
            )
            "emeraldjade" -> AuraClayThemePalette(
                id = "EmeraldJade",
                nameEn = "Fjord & Copper",
                headerWaveFace = if (isDark) Color(0xFF183134) else Color(0xFF23464A),
                headerWaveLip = if (isDark) Color(0xFF0E1E20) else Color(0xFF152D30),
                screenBackground = if (isDark) Color(0xFF111B1D) else Color(0xFFF1F5F4),
                cardFace = if (isDark) Color(0xFF1F3033) else Color(0xFFDCE5E4),
                cardLip = if (isDark) Color(0xFF131E20) else Color(0xFFB7C5C3),
                recessedTrackBg = if (isDark) Color(0xFF162426) else Color(0xFFCBD7D5),
                recessedTrackBorder = if (isDark) Color(0xFF314D52) else Color(0xFFAABAB8),
                primaryAccentFace = Color(0xFFC97848),
                primaryAccentLip = Color(0xFF874B27),
                secondaryAccentFace = Color(0xFFD8AC2C),
                secondaryAccentLip = Color(0xFF967314),
                tertiaryAccentFace = Color(0xFF3D5266),
                tertiaryAccentLip = Color(0xFF253442),
                inkColor = Color(0xFF222222),
                mutedInkColor = Color(0xFF222222)
            )
            else -> AuraClayThemePalette(
                id = "AuraEarthy",
                nameEn = "Aura Earthy",
                headerWaveFace = if (isDark) AuraTactileColors.ForestHeaderDark else AuraTactileColors.ForestHeaderLight,
                headerWaveLip = if (isDark) Color(0xFF0E1D16) else Color(0xFF1E372C),
                screenBackground = if (isDark) AuraTactileColors.ParchmentBgDark else AuraTactileColors.ParchmentBgLight,
                cardFace = if (isDark) AuraTactileColors.SandMedallionDark else AuraTactileColors.TileSandFace,
                cardLip = if (isDark) AuraTactileColors.SandMedallionLipDark else AuraTactileColors.TileSandLip,
                recessedTrackBg = if (isDark) Color(0xFF16221C) else Color(0xFFD6CEB8),
                recessedTrackBorder = if (isDark) Color(0xFF33473C) else Color(0xFFBDB39A),
                primaryAccentFace = AuraTactileColors.TileTerracottaFace,
                primaryAccentLip = AuraTactileColors.TileTerracottaLip,
                secondaryAccentFace = AuraTactileColors.TileMustardFace,
                secondaryAccentLip = AuraTactileColors.TileMustardLip,
                tertiaryAccentFace = AuraTactileColors.TileSlateFace,
                tertiaryAccentLip = AuraTactileColors.TileSlateLip,
                inkColor = Color(0xFF222222),
                mutedInkColor = Color(0xFF222222)
            )
        }
    }
}
