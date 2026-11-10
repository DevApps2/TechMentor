package com.example.techmentor.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle

// O roxo do TechMentor é a cor primária: cursor, seleção de texto, ripple e foco dos componentes usam ela
private val RoxoTechMentor = Color(0xFF7C3AED)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA78BFA),
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = RoxoTechMentor,
    onPrimary = Color.White,
    secondary = Color(0xFF6D28D9),
    tertiary = Color(0xFF2563EB)

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun TechMentorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Desligado de propósito: com as cores dinâmicas (Android 12+) o app herdava a cor do papel de parede,
    // e o foco, a seleção de texto e o toque ficavam verdes (ou de outra cor) em vez do roxo do TechMentor
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
        typography = Typography
    ) {
        // DM Sans como fonte padrão de todo texto do app
        ProvideTextStyle(TextStyle(fontFamily = DmSansFamily)) {
            content()
        }
    }
}