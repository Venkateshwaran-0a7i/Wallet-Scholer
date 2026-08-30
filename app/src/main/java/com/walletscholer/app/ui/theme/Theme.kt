package com.walletscholer.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class WalletCustomColors(
    val appBg: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val surfaceAlt: Color,
    val border: Color,
    val borderSoft: Color,
    val text: Color,
    val subtext: Color,
    val faint: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentText: Color,
    val success: Color,
    val successSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val warn: Color,
    val warnSoft: Color,
    val navBg: Color
)

val LocalWalletColors = staticCompositionLocalOf {
    WalletCustomColors(
        appBg = DarkAppBg,
        surface = DarkSurface,
        surfaceRaised = DarkSurfaceRaised,
        surfaceAlt = DarkSurfaceRaised,
        border = DarkBorder,
        borderSoft = DarkBorderSoft,
        text = DarkText,
        subtext = DarkSubtext,
        faint = DarkFaint,
        accent = DarkAccent,
        accentSoft = DarkAccentSoft,
        accentText = DarkAccentText,
        success = DarkSuccess,
        successSoft = DarkSuccessSoft,
        danger = DarkDanger,
        dangerSoft = DarkDangerSoft,
        warn = DarkWarn,
        warnSoft = DarkWarnSoft,
        navBg = DarkNavBg
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = DarkAccent,
    onPrimary = DarkAccentText,
    primaryContainer = DarkAccentSoft,
    onPrimaryContainer = DarkAccent,
    background = DarkAppBg,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurfaceRaised,
    onSurfaceVariant = DarkSubtext,
    outline = DarkBorder,
    outlineVariant = DarkBorderSoft
)

private val LightColorScheme = lightColorScheme(
    primary = LightAccent,
    onPrimary = LightAccentText,
    primaryContainer = LightAccentSoft,
    onPrimaryContainer = LightAccent,
    background = LightAppBg,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightSurfaceRaised,
    onSurfaceVariant = LightSubtext,
    outline = LightBorder,
    outlineVariant = LightBorderSoft
)

@Composable
fun WalletScholarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val customColors = if (darkTheme) {
        WalletCustomColors(
            appBg = DarkAppBg,
            surface = DarkSurface,
            surfaceRaised = DarkSurfaceRaised,
            surfaceAlt = DarkSurfaceRaised,
            border = DarkBorder,
            borderSoft = DarkBorderSoft,
            text = DarkText,
            subtext = DarkSubtext,
            faint = DarkFaint,
            accent = DarkAccent,
            accentSoft = DarkAccentSoft,
            accentText = DarkAccentText,
            success = DarkSuccess,
            successSoft = DarkSuccessSoft,
            danger = DarkDanger,
            dangerSoft = DarkDangerSoft,
            warn = DarkWarn,
            warnSoft = DarkWarnSoft,
            navBg = DarkNavBg
        )
    } else {
        WalletCustomColors(
            appBg = LightAppBg,
            surface = LightSurface,
            surfaceRaised = LightSurfaceRaised,
            surfaceAlt = LightSurfaceRaised,
            border = LightBorder,
            borderSoft = LightBorderSoft,
            text = LightText,
            subtext = LightSubtext,
            faint = LightFaint,
            accent = LightAccent,
            accentSoft = LightAccentSoft,
            accentText = LightAccentText,
            success = LightSuccess,
            successSoft = LightSuccessSoft,
            danger = LightDanger,
            dangerSoft = LightDangerSoft,
            warn = LightWarn,
            warnSoft = LightWarnSoft,
            navBg = LightNavBg
        )
    }

    CompositionLocalProvider(LocalWalletColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}

object WalletTheme {
    val colors: WalletCustomColors
        @Composable
        get() = LocalWalletColors.current
}
