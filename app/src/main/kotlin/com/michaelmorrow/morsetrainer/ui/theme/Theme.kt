package com.michaelmorrow.morsetrainer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.michaelmorrow.morsetrainer.R

val Report1942Font = FontFamily(Font(R.font.report1942))

private val DarkColorScheme = darkColorScheme(
    primary = AppAccent,
    background = AppBackground,
    surface = AppBackground,
    onBackground = HeaderText,
    onSurface = HeaderText,
)

@Composable
fun MorseTrainerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content,
    )
}
