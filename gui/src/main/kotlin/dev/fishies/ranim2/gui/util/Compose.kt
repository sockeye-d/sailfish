package dev.fishies.ranim2.gui.util

import androidx.compose.material.Colors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.SkiaGraphicsContext
import dev.fishies.ranim2.theming.Theme

@Composable
@OptIn(InternalComposeUiApi::class)
fun rememberSkiaGraphicsContext(): GraphicsContext = remember { SkiaGraphicsContext() }

fun Theme.toComposeColors() = Colors(
    primary = primary,
    primaryVariant = primaryVariant,
    secondary = secondary,
    secondaryVariant = secondaryVariant,
    background = background,
    surface = surface,
    error = error,
    onPrimary = onPrimary,
    onSecondary = onSecondary,
    onBackground = onBackground,
    onSurface = onSurface,
    onError = onError,
    isLight = isLight
)
