package com.cuneiform.input

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.cuneiform.input.prefs.ImeUserPrefs
import com.cuneiform.input.prefs.darkThemeFlag
import com.cuneiform.input.prefs.imeUserPrefsFlow
import com.cuneiform.input.ui.settings.ImeSettingsScreen
import com.cuneiform.input.ui.theme.CuneiformInputTheme
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            var prefs by remember { mutableStateOf(ImeUserPrefs.DEFAULT) }
            LaunchedEffect(Unit) {
                context.applicationContext.imeUserPrefsFlow().collect { prefs = it }
            }
            val systemDark = isSystemInDarkTheme()
            val darkTheme = prefs.darkThemeFlag(systemDark)
            val scope = rememberCoroutineScope()
            CuneiformInputTheme(darkTheme = darkTheme) {
                ImeSettingsScreen(prefs = prefs, scope = scope)
            }
        }
    }
}
