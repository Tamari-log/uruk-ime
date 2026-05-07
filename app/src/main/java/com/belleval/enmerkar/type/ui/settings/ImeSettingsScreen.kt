package com.belleval.enmerkar.type.ui.settings

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.belleval.enmerkar.type.R
import com.belleval.enmerkar.type.prefs.ImeUserPrefs
import com.belleval.enmerkar.type.prefs.KeySizeMode
import com.belleval.enmerkar.type.prefs.ThemeMode
import com.belleval.enmerkar.type.prefs.setImeDefaultTabIndex
import com.belleval.enmerkar.type.prefs.setImeHapticOnKeypress
import com.belleval.enmerkar.type.prefs.setImeKeySizeMode
import com.belleval.enmerkar.type.prefs.setImeShowKeyBorders
import com.belleval.enmerkar.type.prefs.setImeThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImeSettingsScreen(
    prefs: ImeUserPrefs,
    scope: CoroutineScope,
    onOpenDiagnosticLog: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val versionLabel = packageVersionString(context)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                SettingsSectionLabel(stringResource(R.string.settings_section_system))
            }
            item {
                SettingsNavCard(
                    title = stringResource(R.string.settings_open_android_ime_settings),
                    subtitle = stringResource(R.string.settings_open_android_ime_settings_sub),
                    icon = Icons.Default.Settings,
                    onClick = {
                        context.startActivity(
                            android.content.Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            },
                        )
                    },
                )
            }
            item {
                SettingsNavCard(
                    title = stringResource(R.string.settings_open_ime_picker),
                    subtitle = stringResource(R.string.settings_open_ime_picker_sub),
                    icon = Icons.Default.Keyboard,
                    onClick = {
                        val imm =
                            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showInputMethodPicker()
                    },
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsSectionLabel(stringResource(R.string.settings_section_keyboard))
            }
            item {
                SettingsToggleCard(
                    title = stringResource(R.string.settings_haptic_keys),
                    subtitle = stringResource(R.string.settings_haptic_keys_sub),
                    icon = Icons.Default.Vibration,
                    checked = prefs.hapticOnKeypress,
                    onCheckedChange = { v ->
                        scope.launch { context.setImeHapticOnKeypress(v) }
                    },
                )
            }
            item {
                SegmentedChoiceCard(
                    title = stringResource(R.string.settings_default_tab),
                    options =
                        listOf(
                            stringResource(R.string.tab_alphabet) to 0,
                            stringResource(R.string.tab_main) to 1,
                            stringResource(R.string.tab_numbers) to 2,
                            stringResource(R.string.tab_inflect) to 3,
                        ),
                    selectedIndex = prefs.defaultTabIndex,
                    onSelect = { idx ->
                        scope.launch { context.setImeDefaultTabIndex(idx) }
                    },
                )
            }
            item {
                SegmentedTriCard(
                    title = stringResource(R.string.settings_key_size),
                    labels =
                        listOf(
                            stringResource(R.string.settings_key_size_compact),
                            stringResource(R.string.settings_key_size_standard),
                            stringResource(R.string.settings_key_size_comfortable),
                        ),
                    selected = prefs.keySizeMode,
                    onSelect = { mode ->
                        scope.launch { context.setImeKeySizeMode(mode) }
                    },
                )
            }
            item {
                SettingsToggleCard(
                    title = stringResource(R.string.settings_key_borders),
                    subtitle = stringResource(R.string.settings_key_borders_sub),
                    icon = Icons.Default.Info,
                    checked = prefs.showKeyBorders,
                    onCheckedChange = { v ->
                        scope.launch { context.setImeShowKeyBorders(v) }
                    },
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsSectionLabel(stringResource(R.string.settings_section_appearance))
            }
            item {
                SegmentedTriThemeCard(
                    title = stringResource(R.string.settings_theme_keyboard_and_app),
                    selected = prefs.themeMode,
                    onSelect = { mode ->
                        scope.launch { context.setImeThemeMode(mode) }
                    },
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsSectionLabel(stringResource(R.string.settings_section_about))
            }
            item {
                SettingsNavCard(
                    title = stringResource(R.string.settings_diagnostic_log_title),
                    subtitle = stringResource(R.string.settings_diagnostic_log_sub),
                    icon = Icons.Default.BugReport,
                    onClick = onOpenDiagnosticLog,
                )
            }
            item {
                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_version)) },
                        supportingContent = { Text(versionLabel) },
                        colors =
                            ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                    )
                }
            }
        }
    }
}

private fun packageVersionString(context: Context): String {
    return try {
        val pm = context.packageManager
        val pkg = context.packageName
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0)).versionName ?: ""
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(pkg, 0).versionName ?: ""
        }
    } catch (_: PackageManager.NameNotFoundException) {
        ""
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
    )
}

@Composable
private fun SettingsNavCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick,
    ) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(subtitle) },
            leadingContent = {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            trailingContent = {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            colors =
                ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
        )
    }
}

@Composable
private fun SettingsToggleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(subtitle) },
            leadingContent = {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            trailingContent = {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                )
            },
            colors =
                ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
        )
    }
}

@Composable
private fun SegmentedChoiceCard(
    title: String,
    options: List<Pair<String, Int>>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
            ) {
                options.forEachIndexed { index, pair ->
                    val (label, value) = pair
                    SegmentedButton(
                        selected = selectedIndex == value,
                        onClick = { onSelect(value) },
                        modifier = Modifier.weight(1f),
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        icon = { SegmentedButtonDefaults.Icon(active = false) },
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentedTriCard(
    title: String,
    labels: List<String>,
    selected: KeySizeMode,
    onSelect: (KeySizeMode) -> Unit,
) {
    val modes = listOf(KeySizeMode.COMPACT, KeySizeMode.STANDARD, KeySizeMode.COMFORTABLE)
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
            ) {
                modes.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = mode == selected,
                        onClick = { onSelect(mode) },
                        modifier = Modifier.weight(1f),
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                        icon = { SegmentedButtonDefaults.Icon(active = false) },
                    ) {
                        Text(
                            text = labels[index],
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentedTriThemeCard(
    title: String,
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    val modes = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)
    val labels =
        listOf(
            stringResource(R.string.settings_theme_system),
            stringResource(R.string.settings_theme_light),
            stringResource(R.string.settings_theme_dark),
        )
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
            ) {
                modes.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = mode == selected,
                        onClick = { onSelect(mode) },
                        modifier = Modifier.weight(1f),
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                        icon = { SegmentedButtonDefaults.Icon(active = false) },
                    ) {
                        Text(
                            text = labels[index],
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
