package com.example.ui.theme

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
    primary = TasklyRedDark,
    onPrimary = Color.White,
    primaryContainer = TasklyRedContainerDark,
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFB0B0B0),
    onSecondary = Charcoal900,
    background = Charcoal900,
    onBackground = TextPrimaryDark,
    surface = Charcoal800,
    onSurface = TextPrimaryDark,
    surfaceVariant = Charcoal700,
    onSurfaceVariant = TextSecondaryDark,
    outline = Charcoal600
)

private val LightColorScheme = lightColorScheme(
    primary = TasklyRed,
    onPrimary = Color.White,
    primaryContainer = TasklyRedContainerLight,
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF5F5F5F),
    onSecondary = Color.White,
    background = OffWhite,
    onBackground = TextPrimaryLight,
    surface = PureWhite,
    onSurface = TextPrimaryLight,
    surfaceVariant = MutedGreyLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFDCDCDC)
)

@Composable
fun TasklyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep Taskly's signature red identity
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
