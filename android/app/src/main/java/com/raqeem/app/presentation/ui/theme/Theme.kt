package com.raqeem.app.presentation.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val RaqeemColorScheme = darkColorScheme(
    primary          = AppColors.purple500,
    onPrimary        = Color.White,
    primaryContainer = AppColors.purple400,
    secondary        = AppColors.purple300,
    background       = AppColors.bgBase,
    onBackground     = AppColors.textPrimary,
    surface          = AppColors.bgSurface,
    onSurface        = AppColors.textPrimary,
    surfaceVariant   = AppColors.bgElevated,
    onSurfaceVariant = AppColors.textSecondary,
    outline          = AppColors.borderDefault,
    error            = AppColors.negative,
    onError          = Color.White,
    surfaceContainer = AppColors.bgSurface,
    surfaceContainerHigh = AppColors.bgElevated,
    surfaceContainerHighest = AppColors.bgSubtle,
)

@Composable
fun RaqeemTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }
    MaterialTheme(
        colorScheme = RaqeemColorScheme,
        typography = RaqeemTypography,
        content = content,
    )
}
