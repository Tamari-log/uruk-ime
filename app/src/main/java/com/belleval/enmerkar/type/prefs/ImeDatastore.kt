package com.belleval.enmerkar.type.prefs

import android.content.Context
import android.util.Log
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import com.belleval.enmerkar.type.diagnostic.DiagnosticLog
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val KEY_HAPTIC = booleanPreferencesKey("haptic_on_keypress")
private val KEY_DEFAULT_TAB = intPreferencesKey("default_tab_index")
private val KEY_SETTINGS_VERSION = intPreferencesKey("settings_version")
private val KEY_KEY_SIZE = stringPreferencesKey("key_size")
private val KEY_SHOW_BORDERS = booleanPreferencesKey("show_key_borders")
private val KEY_THEME = stringPreferencesKey("theme_mode")

val Context.imeDataStore by preferencesDataStore(
    name = "uruk_ime_settings",
    corruptionHandler =
        ReplaceFileCorruptionHandler(
            produceNewData = { emptyPreferences() },
        ),
)

private const val TAG_PREFS = "UrukImePrefs"

fun Context.imeUserPrefsFlow(): Flow<ImeUserPrefs> =
    applicationContext.imeDataStore.data
        .catch { e ->
            Log.e(TAG_PREFS, "DataStore read failed; using defaults", e)
            DiagnosticLog.e(TAG_PREFS, "DataStore read failed; using defaults", e)
            emit(emptyPreferences())
        }
        .map { it.toImeUserPrefs() }
        .distinctUntilChanged()

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            when (value?.lowercase()) {
                "light" -> LIGHT
                "dark" -> DARK
                else -> SYSTEM
            }
    }
}

enum class KeySizeMode {
    COMPACT,
    STANDARD,
    COMFORTABLE,
    ;

    fun toStorageString(): String = name.lowercase()

    companion object {
        fun fromStorage(value: String?): KeySizeMode =
            when (value?.lowercase()) {
                "compact" -> COMPACT
                "comfortable" -> COMFORTABLE
                else -> STANDARD
            }
    }
}

data class ImeUserPrefs(
    val hapticOnKeypress: Boolean,
    val defaultTabIndex: Int,
    val keySizeMode: KeySizeMode,
    val showKeyBorders: Boolean,
    val themeMode: ThemeMode,
) {
    fun gridMinCellDp(): Float =
        when (keySizeMode) {
            KeySizeMode.COMPACT -> 40f
            KeySizeMode.STANDARD -> 48f
            KeySizeMode.COMFORTABLE -> 56f
        }

    fun gridCellHeightDp(): Float =
        when (keySizeMode) {
            KeySizeMode.COMPACT -> 44f
            KeySizeMode.STANDARD -> 52f
            KeySizeMode.COMFORTABLE -> 60f
        }

    fun keyboardSurfaceHeightDp(): Float =
        when (keySizeMode) {
            KeySizeMode.COMPACT -> 240f
            KeySizeMode.STANDARD -> 280f
            KeySizeMode.COMFORTABLE -> 320f
        }

    companion object {
        val DEFAULT =
            ImeUserPrefs(
                hapticOnKeypress = true,
                defaultTabIndex = 0,
                keySizeMode = KeySizeMode.STANDARD,
                showKeyBorders = true,
                themeMode = ThemeMode.SYSTEM,
            )
    }
}

fun Preferences.toImeUserPrefs(): ImeUserPrefs {
    val settingsVersion = this[KEY_SETTINGS_VERSION] ?: 0
    val rawDefaultTab = this[KEY_DEFAULT_TAB]
    val defaultTabIndex =
        if (settingsVersion < 2) {
            when (rawDefaultTab) {
                1 -> 2
                else -> 0
            }
        } else {
            (rawDefaultTab ?: 0).coerceIn(0, 3)
        }
    return ImeUserPrefs(
        hapticOnKeypress = this[KEY_HAPTIC] ?: true,
        defaultTabIndex = defaultTabIndex,
        keySizeMode = KeySizeMode.fromStorage(this[KEY_KEY_SIZE]),
        showKeyBorders = this[KEY_SHOW_BORDERS] ?: true,
        themeMode = ThemeMode.fromStorage(this[KEY_THEME]),
    )
}

fun ImeUserPrefs.darkThemeFlag(systemIsDark: Boolean): Boolean =
    when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemIsDark
    }

suspend fun Context.setImeHapticOnKeypress(enabled: Boolean) {
    applicationContext.imeDataStore.edit {
        it[KEY_SETTINGS_VERSION] = 3
        it[KEY_HAPTIC] = enabled
    }
}

suspend fun Context.setImeDefaultTabIndex(index: Int) {
    applicationContext.imeDataStore.edit {
        it[KEY_SETTINGS_VERSION] = 3
        it[KEY_DEFAULT_TAB] = index.coerceIn(0, 3)
    }
}

suspend fun Context.setImeKeySizeMode(mode: KeySizeMode) {
    applicationContext.imeDataStore.edit {
        it[KEY_SETTINGS_VERSION] = 3
        it[KEY_KEY_SIZE] = mode.toStorageString()
    }
}

suspend fun Context.setImeShowKeyBorders(show: Boolean) {
    applicationContext.imeDataStore.edit {
        it[KEY_SETTINGS_VERSION] = 3
        it[KEY_SHOW_BORDERS] = show
    }
}

suspend fun Context.setImeThemeMode(mode: ThemeMode) {
    applicationContext.imeDataStore.edit {
        it[KEY_SETTINGS_VERSION] = 3
        it[KEY_THEME] = mode.name.lowercase()
    }
}
