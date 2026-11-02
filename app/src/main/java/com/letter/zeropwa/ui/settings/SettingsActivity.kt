package com.letter.zeropwa.ui.settings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.letter.zeropwa.data.AppLanguage
import com.letter.zeropwa.ui.theme.ZeroPwaTheme

class SettingsActivity : AppCompatActivity() {

    private val languageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AppLanguage.ACTION_CHANGED) {
                recreate()
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        AppLanguage.initialize(newBase)
        super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val filter = IntentFilter(AppLanguage.ACTION_CHANGED)
        ContextCompat.registerReceiver(this, languageReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        setContent {
            ZeroPwaTheme {
                SettingsScreen(
                    onBack = { finish() }
                )
            }
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(languageReceiver)
        } catch (_: Exception) {}
        super.onDestroy()
    }
}
