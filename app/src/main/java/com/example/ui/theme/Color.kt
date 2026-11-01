package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

data class ExpressivePaletteInfo(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val accentColor: Color,
    val containerColor: Color,
    val darkScheme: ColorScheme,
    val lightScheme: ColorScheme
)

object ExpressivePalettes {
    // 1. Hyper Lime (Xiaomi HyperOS Signature)
    val HyperLime = ExpressivePaletteInfo(
        id = "HyperLime",
        nameEn = "Hyper Lime",
        nameAr = "لايم سايبر",
        accentColor = Color(0xFFC6FF00),
        containerColor = Color(0xFF1E2520),
        darkScheme = darkColorScheme(
            primary = Color(0xFFC6FF00),
            onPrimary = Color(0xFF1B2B00),
            primaryContainer = Color(0xFF2E4500),
            onPrimaryContainer = Color(0xFFD6FF47),
            secondary = Color(0xFFA5C973),
            onSecondary = Color(0xFF1C2D00),
            secondaryContainer = Color(0xFF2A3420),
            onSecondaryContainer = Color(0xFFDBF2BD),
            tertiary = Color(0xFF81E291),
            tertiaryContainer = Color(0xFF0E3D19),
            background = Color(0xFF0F1210),
            surface = Color(0xFF141815),
            onSurface = Color(0xFFE3E8E1),
            surfaceVariant = Color(0xFF1E2520),
            onSurfaceVariant = Color(0xFFC4CDC0),
            outline = Color(0xFF4C584A),
            outlineVariant = Color(0xFF333D32)
        ),
        lightScheme = lightColorScheme(
            primary = Color(0xFF537A00),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFDDF595),
            onPrimaryContainer = Color(0xFF162500),
            secondary = Color(0xFF4D6031),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFD3E4B8),
            onSecondaryContainer = Color(0xFF151F08),
            tertiary = Color(0xFF2A6836),
            tertiaryContainer = Color(0xFFB5EFBF),
            background = Color(0xFFF7F9F4),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF141812),
            surfaceVariant = Color(0xFFE4E9DE),
            onSurfaceVariant = Color(0xFF424A3D),
            outline = Color(0xFF75806E),
            outlineVariant = Color(0xFFCCD4C6)
        )
    )

    // 2. Electric Cobalt
    val ElectricCobalt = ExpressivePaletteInfo(
        id = "ElectricCobalt",
        nameEn = "Electric Cobalt",
        nameAr = "أزرق كوبالت",
        accentColor = Color(0xFF3B82F6),
        containerColor = Color(0xFF172036),
        darkScheme = darkColorScheme(
            primary = Color(0xFF60A5FA),
            onPrimary = Color(0xFF082766),
            primaryContainer = Color(0xFF1E3A8A),
            onPrimaryContainer = Color(0xFFBFDBFE),
            secondary = Color(0xFF93C5FD),
            onSecondary = Color(0xFF0F2B5C),
            secondaryContainer = Color(0xFF1E293B),
            onSecondaryContainer = Color(0xFFE2E8F0),
            tertiary = Color(0xFF38BDF8),
            tertiaryContainer = Color(0xFF0C4A6E),
            background = Color(0xFF090D16),
            surface = Color(0xFF0F1524),
            onSurface = Color(0xFFE2E8F0),
            surfaceVariant = Color(0xFF172036),
            onSurfaceVariant = Color(0xFF94A3B8),
            outline = Color(0xFF3B4E7A),
            outlineVariant = Color(0xFF243252)
        ),
        lightScheme = lightColorScheme(
            primary = Color(0xFF1D4ED8),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFDBEAFE),
            onPrimaryContainer = Color(0xFF1E3A8A),
            secondary = Color(0xFF3B82F6),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFE0E7FF),
            onSecondaryContainer = Color(0xFF1E1B4B),
            tertiary = Color(0xFF0284C7),
            tertiaryContainer = Color(0xFFBAE6FD),
            background = Color(0xFFF2F6FC),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFFDDE6F5),
            onSurfaceVariant = Color(0xFF334155),
            outline = Color(0xFF64748B),
            outlineVariant = Color(0xFFCBD5E1)
        )
    )

    // 3. Sunset Flame
    val SunsetFlame = ExpressivePaletteInfo(
        id = "SunsetFlame",
        nameEn = "Sunset Flame",
        nameAr = "لهب الغروب",
        accentColor = Color(0xFFFF7A00),
        containerColor = Color(0xFF261D18),
        darkScheme = darkColorScheme(
            primary = Color(0xFFFB923C),
            onPrimary = Color(0xFF431407),
            primaryContainer = Color(0xFF7C2D12),
            onPrimaryContainer = Color(0xFFFFEDD5),
            secondary = Color(0xFFFDBA74),
            onSecondary = Color(0xFF451A03),
            secondaryContainer = Color(0xFF2C1E18),
            onSecondaryContainer = Color(0xFFFED7AA),
            tertiary = Color(0xFFF87171),
            tertiaryContainer = Color(0xFF7F1D1D),
            background = Color(0xFF120E0C),
            surface = Color(0xFF1A1411),
            onSurface = Color(0xFFF5EBE6),
            surfaceVariant = Color(0xFF261D18),
            onSurfaceVariant = Color(0xFFD1BDB4),
            outline = Color(0xFF5A4439),
            outlineVariant = Color(0xFF3D2D25)
        ),
        lightScheme = lightColorScheme(
            primary = Color(0xFFC2410C),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFFEDD5),
            onPrimaryContainer = Color(0xFF431407),
            secondary = Color(0xFF9A3412),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFFFE4D6),
            onSecondaryContainer = Color(0xFF3A1204),
            tertiary = Color(0xFFB91C1C),
            tertiaryContainer = Color(0xFFFEE2E2),
            background = Color(0xFFFBF5F2),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF1C130E),
            surfaceVariant = Color(0xFFF2E2DA),
            onSurfaceVariant = Color(0xFF4C3B33),
            outline = Color(0xFF8C7164),
            outlineVariant = Color(0xFFDBC5BA)
        )
    )

    // 4. Cyber Violet
    val CyberViolet = ExpressivePaletteInfo(
        id = "CyberViolet",
        nameEn = "Cyber Violet",
        nameAr = "بنفسجي نيون",
        accentColor = Color(0xFFA855F7),
        containerColor = Color(0xFF241C36),
        darkScheme = darkColorScheme(
            primary = Color(0xFFC084FC),
            onPrimary = Color(0xFF3B0764),
            primaryContainer = Color(0xFF581C87),
            onPrimaryContainer = Color(0xFFF3E8FF),
            secondary = Color(0xFFE9D5FF),
            onSecondary = Color(0xFF3B0764),
            secondaryContainer = Color(0xFF251A36),
            onSecondaryContainer = Color(0xFFE9D5FF),
            tertiary = Color(0xFFF472B6),
            tertiaryContainer = Color(0xFF701A75),
            background = Color(0xFF100D18),
            surface = Color(0xFF181324),
            onSurface = Color(0xFFF0EBF7),
            surfaceVariant = Color(0xFF241C36),
            onSurfaceVariant = Color(0xFFCABEE0),
            outline = Color(0xFF56437A),
            outlineVariant = Color(0xFF382B52)
        ),
        lightScheme = lightColorScheme(
            primary = Color(0xFF7E22CE),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFF3E8FF),
            onPrimaryContainer = Color(0xFF3B0764),
            secondary = Color(0xFF6B21A8),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFE9D5FF),
            onSecondaryContainer = Color(0xFF2E0854),
            tertiary = Color(0xFFBE185D),
            tertiaryContainer = Color(0xFFFCE7F3),
            background = Color(0xFFF9F5FB),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF170F24),
            surfaceVariant = Color(0xFFEAE0F5),
            onSurfaceVariant = Color(0xFF463A57),
            outline = Color(0xFF7D6C94),
            outlineVariant = Color(0xFFD4C5E6)
        )
    )

    // 5. Emerald Jade
    val EmeraldJade = ExpressivePaletteInfo(
        id = "EmeraldJade",
        nameEn = "Emerald Jade",
        nameAr = "زمردي ملكي",
        accentColor = Color(0xFF10B981),
        containerColor = Color(0xFF172620),
        darkScheme = darkColorScheme(
            primary = Color(0xFF34D399),
            onPrimary = Color(0xFF022C22),
            primaryContainer = Color(0xFF064E3B),
            onPrimaryContainer = Color(0xFFA7F3D0),
            secondary = Color(0xFF6EE7B7),
            onSecondary = Color(0xFF022C22),
            secondaryContainer = Color(0xFF142921),
            onSecondaryContainer = Color(0xFFA7F3D0),
            tertiary = Color(0xFF2DD4BF),
            tertiaryContainer = Color(0xFF115E59),
            background = Color(0xFF0A110E),
            surface = Color(0xFF0F1A15),
            onSurface = Color(0xFFE2EBE6),
            surfaceVariant = Color(0xFF172620),
            onSurfaceVariant = Color(0xFFB5C7BF),
            outline = Color(0xFF3C5C4F),
            outlineVariant = Color(0xFF263D34)
        ),
        lightScheme = lightColorScheme(
            primary = Color(0xFF047857),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFD1FAE5),
            onPrimaryContainer = Color(0xFF022C22),
            secondary = Color(0xFF065F46),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFA7F3D0),
            onSecondaryContainer = Color(0xFF022C22),
            tertiary = Color(0xFF0F766E),
            tertiaryContainer = Color(0xFFCCFBF1),
            background = Color(0xFFF1F8F5),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF0E1A14),
            surfaceVariant = Color(0xFFDAEBE3),
            onSurfaceVariant = Color(0xFF354A40),
            outline = Color(0xFF628575),
            outlineVariant = Color(0xFFB8D4C7)
        )
    )

    // 6. Crimson Ruby
    val CrimsonRuby = ExpressivePaletteInfo(
        id = "CrimsonRuby",
        nameEn = "Crimson Ruby",
        nameAr = "ياقوتي قرمزي",
        accentColor = Color(0xFFEF4444),
        containerColor = Color(0xFF2A1818),
        darkScheme = darkColorScheme(
            primary = Color(0xFFF87171),
            onPrimary = Color(0xFF450A0A),
            primaryContainer = Color(0xFF7F1D1D),
            onPrimaryContainer = Color(0xFFFEE2E2),
            secondary = Color(0xFFFCA5A5),
            onSecondary = Color(0xFF450A0A),
            secondaryContainer = Color(0xFF2D1616),
            onSecondaryContainer = Color(0xFFFECACA),
            tertiary = Color(0xFFFB7185),
            tertiaryContainer = Color(0xFF881337),
            background = Color(0xFF130A0A),
            surface = Color(0xFF1C1010),
            onSurface = Color(0xFFF6E8E8),
            surfaceVariant = Color(0xFF2A1818),
            onSurfaceVariant = Color(0xFFCFB2B2),
            outline = Color(0xFF633B3B),
            outlineVariant = Color(0xFF422525)
        ),
        lightScheme = lightColorScheme(
            primary = Color(0xFFB91C1C),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFEE2E2),
            onPrimaryContainer = Color(0xFF450A0A),
            secondary = Color(0xFF991B1B),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFFECACA),
            onSecondaryContainer = Color(0xFF3B0707),
            tertiary = Color(0xFF9F1239),
            tertiaryContainer = Color(0xFFFFE4E6),
            background = Color(0xFFFBF2F2),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF1C0D0D),
            surfaceVariant = Color(0xFFF3DFDF),
            onSurfaceVariant = Color(0xFF4E3737),
            outline = Color(0xFF8B6464),
            outlineVariant = Color(0xFFD6BEBE)
        )
    )

    val allPalettes = listOf(
        HyperLime,
        ElectricCobalt,
        SunsetFlame,
        CyberViolet,
        EmeraldJade,
        CrimsonRuby
    )

    fun getPalette(id: String): ExpressivePaletteInfo {
        return allPalettes.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: HyperLime
    }
}

