package com.tinnomore.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary            = TinBlue,
    onPrimary          = TinWhite,
    primaryContainer   = TinBlueCont,
    onPrimaryContainer = TinBlueDark,
    secondary          = TinTeal,
    onSecondary        = TinWhite,
    background         = TinSurface,
    surface            = TinWhite,
    error              = TinRed,
    onError            = TinWhite
)

@Composable
fun TinNoMoreTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography  = Typography,
        content     = content
    )
}
