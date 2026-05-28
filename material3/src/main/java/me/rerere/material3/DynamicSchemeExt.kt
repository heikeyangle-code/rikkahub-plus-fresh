package me.rerere.material3

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import dynamiccolor.DynamicScheme

fun DynamicScheme.toColorScheme(): ColorScheme {
    val s = this
    return if (isDark) {
        darkColorScheme(
            primary = Color(s.primary),
            onPrimary = Color(s.onPrimary),
            primaryContainer = Color(s.primaryContainer),
            onPrimaryContainer = Color(s.onPrimaryContainer),
            inversePrimary = Color(s.inversePrimary),
            secondary = Color(s.secondary),
            onSecondary = Color(s.onSecondary),
            secondaryContainer = Color(s.secondaryContainer),
            onSecondaryContainer = Color(s.onSecondaryContainer),
            tertiary = Color(s.tertiary),
            onTertiary = Color(s.onTertiary),
            tertiaryContainer = Color(s.tertiaryContainer),
            onTertiaryContainer = Color(s.onTertiaryContainer),
            background = Color(s.background),
            onBackground = Color(s.onBackground),
            surface = Color(s.surface),
            onSurface = Color(s.onSurface),
            surfaceVariant = Color(s.surfaceVariant),
            onSurfaceVariant = Color(s.onSurfaceVariant),
            surfaceTint = Color(s.surfaceTint),
            inverseSurface = Color(s.inverseSurface),
            inverseOnSurface = Color(s.inverseOnSurface),
            error = Color(s.error),
            onError = Color(s.onError),
            errorContainer = Color(s.errorContainer),
            onErrorContainer = Color(s.onErrorContainer),
            outline = Color(s.outline),
            outlineVariant = Color(s.outlineVariant),
            scrim = Color(s.scrim),
            surfaceBright = Color(s.surfaceBright),
            surfaceDim = Color(s.surfaceDim),
            surfaceContainer = Color(s.surfaceContainer),
            surfaceContainerHigh = Color(s.surfaceContainerHigh),
            surfaceContainerHighest = Color(s.surfaceContainerHighest),
            surfaceContainerLow = Color(s.surfaceContainerLow),
            surfaceContainerLowest = Color(s.surfaceContainerLowest),
        )
    } else {
        lightColorScheme(
            primary = Color(s.primary),
            onPrimary = Color(s.onPrimary),
            primaryContainer = Color(s.primaryContainer),
            onPrimaryContainer = Color(s.onPrimaryContainer),
            inversePrimary = Color(s.inversePrimary),
            secondary = Color(s.secondary),
            onSecondary = Color(s.onSecondary),
            secondaryContainer = Color(s.secondaryContainer),
            onSecondaryContainer = Color(s.onSecondaryContainer),
            tertiary = Color(s.tertiary),
            onTertiary = Color(s.onTertiary),
            tertiaryContainer = Color(s.tertiaryContainer),
            onTertiaryContainer = Color(s.onTertiaryContainer),
            background = Color(s.background),
            onBackground = Color(s.onBackground),
            surface = Color(s.surface),
            onSurface = Color(s.onSurface),
            surfaceVariant = Color(s.surfaceVariant),
            onSurfaceVariant = Color(s.onSurfaceVariant),
            surfaceTint = Color(s.surfaceTint),
            inverseSurface = Color(s.inverseSurface),
            inverseOnSurface = Color(s.inverseOnSurface),
            error = Color(s.error),
            onError = Color(s.onError),
            errorContainer = Color(s.errorContainer),
            onErrorContainer = Color(s.onErrorContainer),
            outline = Color(s.outline),
            outlineVariant = Color(s.outlineVariant),
            scrim = Color(s.scrim),
            surfaceBright = Color(s.surfaceBright),
            surfaceDim = Color(s.surfaceDim),
            surfaceContainer = Color(s.surfaceContainer),
            surfaceContainerHigh = Color(s.surfaceContainerHigh),
            surfaceContainerHighest = Color(s.surfaceContainerHighest),
            surfaceContainerLow = Color(s.surfaceContainerLow),
            surfaceContainerLowest = Color(s.surfaceContainerLowest),
        )
    }
}
