package com.secondream.aitower

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var audioManager: AudioManager
    private lateinit var hapticManager: HapticManager
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("ai_tower", Context.MODE_PRIVATE)
        audioManager = AudioManager(this)
        hapticManager = HapticManager(this)
        audioManager.enabled = prefs.getBoolean("sound", true)
        hapticManager.enabled = prefs.getBoolean("haptics", true)

        val initialBest = prefs.getInt("best", 0)
        val deviceLang = if (Locale.getDefault().language == "it") Lang.IT else Lang.EN
        val initialLang = when (prefs.getString("lang", null)) {
            "IT" -> Lang.IT
            "EN" -> Lang.EN
            else -> deviceLang
        }

        setContent {
            val scheme = darkColorScheme(
                primary = Color(0xFF4A9EFF),
                secondary = Color(0xFFF2C14E),
                background = Color(0xFF0A1024),
                surface = Color(0xFF101A30)
            )
            var lang by remember { mutableStateOf(initialLang) }
            val strings = stringsFor(lang)

            MaterialTheme(colorScheme = scheme) {
                CompositionLocalProvider(
                    LocalTextStyle provides TextStyle(fontFamily = Rajdhani, color = Color.White)
                ) {
                    Surface {
                        GameScreen(
                            audioManager = audioManager,
                            hapticManager = hapticManager,
                            strings = strings,
                            lang = lang,
                            initialBest = initialBest,
                            onLang = {
                                lang = it
                                prefs.edit().putString("lang", it.name).apply()
                            },
                            onToggleSound = { prefs.edit().putBoolean("sound", it).apply() },
                            onToggleHaptics = { prefs.edit().putBoolean("haptics", it).apply() },
                            onSaveBest = { prefs.edit().putInt("best", it).apply() }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioManager.release()
    }
}
