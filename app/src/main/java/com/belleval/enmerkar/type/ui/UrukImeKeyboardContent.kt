package com.belleval.enmerkar.type.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.belleval.enmerkar.type.R
import com.belleval.enmerkar.type.cuneiformCodePoints
import com.belleval.enmerkar.type.ui.theme.GridGlyphStyle

private const val TAB_ALPHABET = 0
private const val TAB_MAIN = 1
private const val TAB_NUMBERS = 2

@Composable
private fun ImeComposingStrip(text: String) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun UrukImeKeyboardContent(
    tabIndex: Int,
    onTabChange: (Int) -> Unit,
    onGlyphSelected: (String) -> Unit,
    onBackspace: () -> Unit,
    onEditorEnter: () -> Unit,
    keyboardAreaHeightDp: Float,
    hapticOnKeypress: Boolean = true,
    gridMinCellDp: Float = 48f,
    gridCellHeightDp: Float = 52f,
    showKeyBorders: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    var alphabetRawBuffer by remember { mutableStateOf("") }
    var alphabetDigitBuffer by remember { mutableStateOf("") }
    var alphabetSymbolsPage by remember { mutableStateOf(false) }

    LaunchedEffect(tabIndex) {
        if (tabIndex != TAB_ALPHABET) {
            alphabetRawBuffer = ""
            alphabetDigitBuffer = ""
            alphabetSymbolsPage = false
        }
    }

    val composingDisplay =
        remember(tabIndex, alphabetRawBuffer, alphabetDigitBuffer, alphabetSymbolsPage) {
            when {
                tabIndex != TAB_ALPHABET -> ""
                alphabetSymbolsPage -> alphabetDigitBuffer
                else -> alphabetRawBuffer
            }
        }

    val mainBlock = remember { cuneiformCodePoints(0x12000..0x123FF) }
    val numberBlock = remember { cuneiformCodePoints(0x12400..0x1247F) }
    val activePoints =
        when (tabIndex) {
            TAB_MAIN -> mainBlock
            TAB_NUMBERS -> numberBlock
            else -> null
        }

    val alphabetKeyHeightDp =
        remember(keyboardAreaHeightDp) {
            ((keyboardAreaHeightDp - 20f) / 5.85f).coerceIn(34f, 52f)
        }

    Column(modifier = modifier.fillMaxWidth()) {
        if (composingDisplay.isNotEmpty()) {
            ImeComposingStrip(composingDisplay)
        }
        TabRow(selectedTabIndex = tabIndex.coerceIn(TAB_ALPHABET, TAB_NUMBERS)) {
            Tab(
                selected = tabIndex == TAB_ALPHABET,
                onClick = {
                    if (hapticOnKeypress) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                    onTabChange(TAB_ALPHABET)
                },
                text = { Text(stringResource(R.string.tab_alphabet)) },
            )
            Tab(
                selected = tabIndex == TAB_MAIN,
                onClick = {
                    if (hapticOnKeypress) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                    onTabChange(TAB_MAIN)
                },
                text = { Text(stringResource(R.string.tab_main)) },
            )
            Tab(
                selected = tabIndex == TAB_NUMBERS,
                onClick = {
                    if (hapticOnKeypress) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                    onTabChange(TAB_NUMBERS)
                },
                text = { Text(stringResource(R.string.tab_numbers)) },
            )
        }

        when (tabIndex) {
            TAB_ALPHABET -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(keyboardAreaHeightDp.dp),
                ) {
                    AlphabetCuneiformKeyboard(
                        modifier = Modifier.fillMaxHeight(),
                        rawBuffer = alphabetRawBuffer,
                        onRawBufferChange = { alphabetRawBuffer = it },
                        digitBuffer = alphabetDigitBuffer,
                        onDigitBufferChange = { alphabetDigitBuffer = it },
                        symbolsPage = alphabetSymbolsPage,
                        onSymbolsPageChange = { alphabetSymbolsPage = it },
                        keyHeight = alphabetKeyHeightDp.dp,
                        cornerDp = 6.dp,
                        showKeyBorders = showKeyBorders,
                        hapticOnKeypress = hapticOnKeypress,
                        onCommitGlyph = onGlyphSelected,
                        onBackspace = onBackspace,
                        onEnter = onEditorEnter,
                    )
                }
            }
            else -> {
                val gridPoints = activePoints ?: mainBlock
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = gridMinCellDp.dp),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(keyboardAreaHeightDp.dp),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(items = gridPoints, key = { it }) { cp ->
                        val ch = String(Character.toChars(cp))
                        val borderMod =
                            if (showKeyBorders) {
                                Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                            } else {
                                Modifier
                            }
                        Box(
                            modifier = borderMod
                                .clickable {
                                    if (hapticOnKeypress) {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    }
                                    onGlyphSelected(ch)
                                }
                                .height(gridCellHeightDp.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = ch,
                                style = GridGlyphStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                            )
                        }
                    }
                }
            }
        }
    }
}
