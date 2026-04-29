package com.michaelmorrow.morsetrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.michaelmorrow.morsetrainer.ui.MorseTrainerScreen
import com.michaelmorrow.morsetrainer.ui.theme.MorseTrainerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MorseTrainerTheme {
                MorseTrainerScreen()
            }
        }
    }
}
