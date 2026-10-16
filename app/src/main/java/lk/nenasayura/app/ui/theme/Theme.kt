package lk.nenasayura.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NenaPrimaryDark,
    onPrimary = NenaOnPrimaryDark,
    secondary = NenaSecondaryDark,
    tertiary = NenaAccentDark,
    primaryContainer = Color(0xFF28408F),
    onPrimaryContainer = Color(0xFFDCE4FF),
    secondaryContainer = Color(0xFF0F4F47),
    onSecondaryContainer = Color(0xFFCDEDE7),
    tertiaryContainer = Color(0xFF5A4300),
    onTertiaryContainer = Color(0xFFFFE9B8),
    background = NenaBackgroundDark,
    onBackground = Color(0xFFE6E8F2),
    surface = NenaSurfaceDark,
    onSurface = Color(0xFFE6E8F2),
    surfaceVariant = Color(0xFF262B3D),
    onSurfaceVariant = Color(0xFFB0B6C8),
    outline = Color(0xFF6B7288),
    outlineVariant = Color(0xFF343A50)
)

private val LightColorScheme = lightColorScheme(
    primary = NenaPrimary,
    onPrimary = Color.White,
    primaryContainer = NenaPrimaryContainer,
    onPrimaryContainer = NenaPrimary,
    secondary = NenaSecondary,
    onSecondary = Color.White,
    secondaryContainer = NenaSecondaryContainer,
    onSecondaryContainer = NenaSecondary,
    tertiary = NenaAccent,
    onTertiary = Color.Black,
    tertiaryContainer = NenaAccentContainer,
    background = NenaBackground,
    surface = NenaSurface,
    surfaceVariant = Color(0xFFE9ECF4),
    onSurfaceVariant = Color(0xFF5B6478),
    outline = Color(0xFF8A93A8),
    outlineVariant = Color(0xFFE3E7F0)
)

@Composable
fun NenaSayuraAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Off by default so the Nena Sayura brand colours are always shown.
    // (With minSdk 31, dynamic colour would otherwise override them on every device.)
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}