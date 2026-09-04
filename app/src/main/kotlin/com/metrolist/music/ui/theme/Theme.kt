/**
 * flex-player Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.flexplayer.music.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score
import com.flexplayer.music.R

val DefaultThemeColor = Color(0xFFED5564)

@Composable
fun flexPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val useSystemDynamicColor = (themeColor == DefaultThemeColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

    val baseColorScheme = if (useSystemDynamicColor) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        rememberDynamicColorScheme(
            seedColor = themeColor,
            isDark = darkTheme,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
            style = PaletteStyle.TonalSpot
        )
    }

    val colorScheme = remember(baseColorScheme, pureBlack, darkTheme) {
        if (darkTheme && pureBlack) {
            baseColorScheme.pureBlack(true)
        } else {
            baseColorScheme
        }
    }.neumorphicColors(darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}

private fun ColorScheme.neumorphicColors(darkTheme: Boolean): ColorScheme {
    val bgLight = Color(0xFFE0E5EC)
    val bgDark = Color(0xFF2D3436)
    val background = if (darkTheme) bgDark else bgLight
    val surface = if (darkTheme) bgDark else bgLight
    val surfaceVariant = if (darkTheme) Color(0xFF353B48) else Color(0xFFE4E9F1)
    val surfaceContainerLow = if (darkTheme) Color(0xFF313845) else Color(0xFFE8ECF2)
    val surfaceContainer = if (darkTheme) Color(0xFF373E4A) else Color(0xFFEFF2F7)
    val surfaceContainerHigh = if (darkTheme) Color(0xFF3D454F) else Color(0xFFF5F7FA)
    val surfaceContainerHighest = if (darkTheme) Color(0xFF424B55) else Color(0xFFFAFBFC)
    val onSurface = if (darkTheme) Color(0xFFE2E4E9) else Color(0xFF2D3436)
    val onSurfaceVariant = if (darkTheme) Color(0xFFC4C7CF) else Color(0xFF5A5F6B)
    val primary = if (darkTheme) Color(0xFFEF5564) else Color(0xFFEF5564)
    val onPrimary = if (darkTheme) Color.White else Color.White
    val outline = if (darkTheme) Color(0xFF8B919D) else Color(0xFFBCC3CF)
    val outlineVariant = if (darkTheme) Color(0xFF3D454F) else Color(0xFFE4E9F1)

    return copy(
        background = background,
        surface = surface,
        surfaceVariant = surfaceVariant,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        onSurface = onSurface,
        onSurfaceVariant = onSurfaceVariant,
        primary = primary,
        onPrimary = onPrimary,
        outline = outline,
        outlineVariant = outlineVariant,
    )
}

fun Bitmap.extractThemeColor(): Color = Color(
    Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .rankedColors(1, DefaultThemeColor.toArgb())
        .first()
)

internal fun Palette.rankedColors(
    desiredColorCount: Int,
    fallbackColor: Int,
): List<Int> = Score.score(
    swatches.associate { it.rgb to it.population },
    desiredColorCount,
    fallbackColor,
    true,
)

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
