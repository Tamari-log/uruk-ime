package com.belleval.enmerkar.type

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
import com.belleval.enmerkar.type.prefs.ImeUserPrefs
import com.belleval.enmerkar.type.prefs.darkThemeFlag
import com.belleval.enmerkar.type.prefs.imeUserPrefsFlow
import com.belleval.enmerkar.type.ui.settings.DiagnosticLogScreen
import com.belleval.enmerkar.type.ui.settings.ImeSettingsScreen
import com.belleval.enmerkar.type.ui.theme.UrukImeTheme
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
            var showDiagnosticLog by remember { mutableStateOf(false) }
            UrukImeTheme(darkTheme = darkTheme) {
                if (showDiagnosticLog) {
                    DiagnosticLogScreen(onBack = { showDiagnosticLog = false })
                } else {
                    ImeSettingsScreen(
                        prefs = prefs,
                        scope = scope,
                        onOpenDiagnosticLog = { showDiagnosticLog = true },
                    )
                }
            }
        }
    }
}
