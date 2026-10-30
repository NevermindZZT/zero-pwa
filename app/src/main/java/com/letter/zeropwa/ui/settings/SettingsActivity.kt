package com.letter.zeropwa.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.letter.zeropwa.ui.theme.ZeroPwaTheme

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ZeroPwaTheme {
                SettingsScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}
