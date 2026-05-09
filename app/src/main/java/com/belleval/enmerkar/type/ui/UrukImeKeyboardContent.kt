package com.belleval.enmerkar.type.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.belleval.enmerkar.type.R
import com.belleval.enmerkar.type.cuneiformCodePoints
import com.belleval.enmerkar.type.sumerian.MorphemeChip
import com.belleval.enmerkar.type.sumerian.SumerianInflection
import com.belleval.enmerkar.type.sumerian.SumerianTransliteration
import com.belleval.enmerkar.type.sumerian.digitBufferToCuneiformClusters
import com.belleval.enmerkar.type.sumerian.flushGreedy
import com.belleval.enmerkar.type.ui.theme.GridGlyphStyle

private const val TAB_ALPHABET = 0
private const val TAB_MAIN = 1
private const val TAB_NUMBERS = 2
private const val TAB_INFLECT = 3
private const val TAB_LAST = TAB_INFLECT

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

    val dict = remember { SumerianTransliteration.readingToCodepoint }

    fun haptic() {
        if (hapticOnKeypress) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    /** ラテン／数字バッファを楔形にし、[onGlyphSelected] へ出す。 */
    fun flushTransliterationBuffers() {
        if (alphabetRawBuffer.isNotEmpty()) {
            val sb = StringBuilder(alphabetRawBuffer)
            for (g in flushGreedy(dict, sb)) {
                onGlyphSelected(g)
            }
            alphabetRawBuffer = sb.toString()
        }
        if (alphabetDigitBuffer.isNotEmpty()) {
            for (g in digitBufferToCuneiformClusters(alphabetDigitBuffer)) {
                onGlyphSelected(g)
            }
            alphabetDigitBuffer = ""
        }
    }

    LaunchedEffect(tabIndex) {
        when (tabIndex) {
            TAB_ALPHABET -> Unit
            TAB_INFLECT -> {
                alphabetSymbolsPage = false
                alphabetDigitBuffer = ""
            }
            TAB_MAIN,
            TAB_NUMBERS -> {
                alphabetSymbolsPage = false
                alphabetDigitBuffer = ""
            }
            else -> Unit
        }
    }

    val composingDisplay =
        remember(tabIndex, alphabetRawBuffer, alphabetDigitBuffer, alphabetSymbolsPage) {
            when (tabIndex) {
                TAB_ALPHABET ->
                    if (alphabetSymbolsPage) {
                        alphabetDigitBuffer
                    } else {
                        alphabetRawBuffer
                    }
                TAB_INFLECT -> alphabetRawBuffer
                else -> ""
            }
        }

    var mainBlockCache by remember { mutableStateOf<List<Int>?>(null) }
    var numberBlockCache by remember { mutableStateOf<List<Int>?>(null) }
    val activePoints =
        when (tabIndex) {
            TAB_MAIN -> {
                if (mainBlockCache == null) {
                    mainBlockCache = cuneiformCodePoints(0x12000..0x123FF)
                }
                mainBlockCache
            }
            TAB_NUMBERS -> {
                if (numberBlockCache == null) {
                    numberBlockCache = cuneiformCodePoints(0x12400..0x1247F)
                }
                numberBlockCache
            }
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
        TabRow(selectedTabIndex = tabIndex.coerceIn(TAB_ALPHABET, TAB_LAST)) {
            Tab(
                selected = tabIndex == TAB_ALPHABET,
                onClick = {
                    haptic()
                    onTabChange(TAB_ALPHABET)
                },
                text = { Text(stringResource(R.string.tab_alphabet)) },
            )
            Tab(
                selected = tabIndex == TAB_MAIN,
                onClick = {
                    haptic()
                    onTabChange(TAB_MAIN)
                },
                text = { Text(stringResource(R.string.tab_main)) },
            )
            Tab(
                selected = tabIndex == TAB_NUMBERS,
                onClick = {
                    haptic()
                    onTabChange(TAB_NUMBERS)
                },
                text = { Text(stringResource(R.string.tab_numbers)) },
            )
            Tab(
                selected = tabIndex == TAB_INFLECT,
                onClick = {
                    haptic()
                    onTabChange(TAB_INFLECT)
                },
                text = { Text(stringResource(R.string.tab_inflect)) },
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
            TAB_INFLECT -> {
                InflectionKeyboardPanel(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(keyboardAreaHeightDp.dp),
                    gridMinCellDp = gridMinCellDp,
                    gridCellHeightDp = gridCellHeightDp,
                    showKeyBorders = showKeyBorders,
                    onAppendLatin = { piece ->
                        haptic()
                        alphabetRawBuffer += piece
                    },
                    onConfirmFlush = {
                        haptic()
                        flushTransliterationBuffers()
                    },
                )
            }
            else -> {
                if (mainBlockCache == null) {
                    mainBlockCache = cuneiformCodePoints(0x12000..0x123FF)
                }
                val gridPoints = activePoints ?: mainBlockCache.orEmpty()
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
                                    haptic()
                                    flushTransliterationBuffers()
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InflectionKeyboardPanel(
    modifier: Modifier = Modifier,
    gridMinCellDp: Float,
    gridCellHeightDp: Float,
    showKeyBorders: Boolean,
    onAppendLatin: (String) -> Unit,
    onConfirmFlush: () -> Unit,
) {
    val morphemeLists =
        listOf(
            stringResource(R.string.inflect_section_verbal) to SumerianInflection.verbalPrefixes,
            stringResource(R.string.inflect_section_nominal) to SumerianInflection.nominalCaseSuffixes,
            stringResource(R.string.inflect_section_pronoun) to SumerianInflection.pronominalSuffixes,
        )

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = gridMinCellDp.dp),
        modifier = modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        contentPadding = PaddingValues(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for ((title, chips) in morphemeLists) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = title,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(items = chips, key = { it.shortLabel + it.insertsLatin }) { chip ->
                MorphemeChipKey(
                    chip = chip,
                    height = gridCellHeightDp.dp,
                    showKeyBorders = showKeyBorders,
                    onClick = { onAppendLatin(chip.insertsLatin) },
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            val borderMod =
                if (showKeyBorders) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                } else {
                    Modifier
                }
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height((gridCellHeightDp * 0.95f).dp)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f))
                        .then(borderMod)
                        .clickable { onConfirmFlush() }
                        .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.inflect_confirm_flush),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun MorphemeChipKey(
    chip: MorphemeChip,
    height: Dp,
    showKeyBorders: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(6.dp)
    val borderMod =
        if (showKeyBorders) {
            Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), shape)
        } else {
            Modifier
        }
    Box(
        modifier =
            Modifier
                .height(height)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape)
                .then(borderMod)
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = chip.shortLabel,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
        )
    }
}
