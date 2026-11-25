package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val TaviqoDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = DeepBlack,
    primaryContainer = RoyalBlueElevated,
    onPrimaryContainer = NeonCyan,
    secondary = ElectricCyan,
    onSecondary = DeepBlack,
    secondaryContainer = RoyalBlueDark,
    onSecondaryContainer = TextPrimary,
    tertiary = CoinGold,
    onTertiary = DeepBlack,
    background = DeepBlack,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = RoyalBlueDark,
    onSurfaceVariant = TextSecondary,
    outline = RoyalBlueBorder,
    error = LiveCrimson,
    onError = TextPrimary
)

val TaviqoShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

object TaviqoBrushes {
    val RoyalHeroGradient = Brush.verticalGradient(
        colors = listOf(RoyalBlueDark, ObsidianSurface, DeepBlack)
    )
    val NeonCyanPill = Brush.horizontalGradient(
        colors = listOf(NeonCyan, ElectricCyan)
    )
    val RoyalCardGradient = Brush.linearGradient(
        colors = listOf(RoyalBlueElevated.copy(alpha = 0.72f), ObsidianSurface.copy(alpha = 0.92f))
    )
}

@Composable
fun TaviqoTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = TaviqoDarkColorScheme,
        typography = TaviqoTypography,
        shapes = TaviqoShapes,
        content = content
    )
}
