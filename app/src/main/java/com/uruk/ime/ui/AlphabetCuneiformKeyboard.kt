package com.uruk.ime.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.KeyboardCapslock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uruk.ime.sumerian.SumerianTransliteration
import com.uruk.ime.sumerian.appendLatinAndConsume
import com.uruk.ime.sumerian.digitBufferToCuneiformClusters
import com.uruk.ime.sumerian.flushGreedy

private data class LetterKey(val main: Char)

private val rowLetters1 =
    listOf('q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p').map(::LetterKey)
private val rowLetters2 =
    listOf('a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', '\'').map(::LetterKey)
private val rowLetters3 =
    listOf('z', 'x', 'c', 'v', 'b', 'n', 'm').map(::LetterKey)

@Composable
fun AlphabetCuneiformKeyboard(
    modifier: Modifier = Modifier,
    rawBuffer: String,
    onRawBufferChange: (String) -> Unit,
    digitBuffer: String,
    onDigitBufferChange: (String) -> Unit,
    symbolsPage: Boolean,
    onSymbolsPageChange: (Boolean) -> Unit,
    keyHeight: Dp,
    cornerDp: Dp,
    showKeyBorders: Boolean,
    hapticOnKeypress: Boolean,
    onCommitGlyph: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
) {
    val view = LocalView.current
    val dict = SumerianTransliteration.readingToCodepoint
    var capsOn by remember { mutableStateOf(false) }

    fun haptic() {
        if (hapticOnKeypress) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    fun commitFlushedLatin(): List<String> {
        val sb = StringBuilder(rawBuffer)
        val g = flushGreedy(dict, sb)
        onRawBufferChange(sb.toString())
        return g
    }

    fun flushDigitsAsCuneiform(): List<String> {
        if (digitBuffer.isEmpty()) return emptyList()
        val g = digitBufferToCuneiformClusters(digitBuffer)
        onDigitBufferChange("")
        return g
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (symbolsPage) {
            SymbolRows(
                keyHeight = keyHeight,
                cornerDp = cornerDp,
                showKeyBorders = showKeyBorders,
                onKey = { token ->
                    haptic()
                    if (token.length == 1 && token[0] in '0'..'9') {
                        for (g in commitFlushedLatin()) {
                            onCommitGlyph(g)
                        }
                        onDigitBufferChange(digitBuffer + token)
                    } else {
                        for (g in commitFlushedLatin()) {
                            onCommitGlyph(g)
                        }
                        for (g in flushDigitsAsCuneiform()) {
                            onCommitGlyph(g)
                        }
                        onCommitGlyph(token)
                    }
                },
                onBackspace = {
                    haptic()
                    when {
                        digitBuffer.isNotEmpty() -> onDigitBufferChange(digitBuffer.dropLast(1))
                        rawBuffer.isNotEmpty() -> onRawBufferChange(rawBuffer.dropLast(1))
                        else -> onBackspace()
                    }
                },
                onToggleLetters = {
                    haptic()
                    for (g in commitFlushedLatin()) {
                        onCommitGlyph(g)
                    }
                    for (g in flushDigitsAsCuneiform()) {
                        onCommitGlyph(g)
                    }
                    onSymbolsPageChange(false)
                },
                onSpace = {
                    haptic()
                    for (g in commitFlushedLatin()) {
                        onCommitGlyph(g)
                    }
                    for (g in flushDigitsAsCuneiform()) {
                        onCommitGlyph(g)
                    }
                    onCommitGlyph(" ")
                },
                onEnter = {
                    haptic()
                    val hadPending = rawBuffer.isNotEmpty() || digitBuffer.isNotEmpty()
                    for (g in commitFlushedLatin()) {
                        onCommitGlyph(g)
                    }
                    for (g in flushDigitsAsCuneiform()) {
                        onCommitGlyph(g)
                    }
                    if (!hadPending) {
                        onEnter()
                    }
                },
            )
        } else {
            KeyRow(
                keys = rowLetters1,
                keyHeight = keyHeight,
                cornerDp = cornerDp,
                showKeyBorders = showKeyBorders,
                capsOn = capsOn,
                onLetter = { key ->
                    haptic()
                    val ch = key.main
                    when {
                        ch.lowercaseChar() in 'a'..'z' -> {
                            val sb = StringBuilder(rawBuffer)
                            val g =
                                appendLatinAndConsume(
                                    dict,
                                    sb,
                                    ch.lowercaseChar(),
                                )
                            onRawBufferChange(sb.toString())
                            g.forEach(onCommitGlyph)
                        }
                        else -> {
                            for (g in commitFlushedLatin()) {
                                onCommitGlyph(g)
                            }
                            onCommitGlyph(ch.toString())
                        }
                    }
                },
            )
            KeyRow(
                modifier = Modifier.padding(start = 12.dp),
                keys = rowLetters2,
                keyHeight = keyHeight,
                cornerDp = cornerDp,
                showKeyBorders = showKeyBorders,
                capsOn = capsOn,
                onLetter = { key ->
                    haptic()
                    val ch = key.main
                    when {
                        ch.lowercaseChar() in 'a'..'z' -> {
                            val sb = StringBuilder(rawBuffer)
                            val g =
                                appendLatinAndConsume(
                                    dict,
                                    sb,
                                    ch.lowercaseChar(),
                                )
                            onRawBufferChange(sb.toString())
                            g.forEach(onCommitGlyph)
                        }
                        else -> {
                            for (g in commitFlushedLatin()) {
                                onCommitGlyph(g)
                            }
                            onCommitGlyph(ch.toString())
                        }
                    }
                },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                ModifierKey(
                    modifier = Modifier.width(keyHeight * 1.25f),
                    height = keyHeight,
                    cornerDp = cornerDp,
                    showBorder = showKeyBorders,
                    onClick = {
                        haptic()
                        capsOn = !capsOn
                    },
                ) {
                    Icon(
                        Icons.Default.KeyboardCapslock,
                        contentDescription = null,
                        tint = if (capsOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                KeyRow(
                    modifier = Modifier.weight(1f),
                    keys = rowLetters3,
                    keyHeight = keyHeight,
                    cornerDp = cornerDp,
                    showKeyBorders = showKeyBorders,
                    capsOn = capsOn,
                    onLetter = { key ->
                        haptic()
                        val ch = key.main
                        val sb = StringBuilder(rawBuffer)
                        val g =
                            appendLatinAndConsume(
                                dict,
                                sb,
                                ch.lowercaseChar(),
                            )
                        onRawBufferChange(sb.toString())
                        g.forEach(onCommitGlyph)
                    },
                )
                ModifierKey(
                    modifier = Modifier.width(keyHeight * 1.35f),
                    height = keyHeight,
                    cornerDp = cornerDp,
                    showBorder = showKeyBorders,
                    onClick = {
                        haptic()
                        if (rawBuffer.isNotEmpty()) {
                            onRawBufferChange(rawBuffer.dropLast(1))
                        } else {
                            onBackspace()
                        }
                    },
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            BottomRow(
                keyHeight = keyHeight,
                cornerDp = cornerDp,
                showKeyBorders = showKeyBorders,
                onSymbols = {
                    haptic()
                    for (g in commitFlushedLatin()) {
                        onCommitGlyph(g)
                    }
                    onSymbolsPageChange(true)
                },
                onSpace = {
                    haptic()
                    for (g in commitFlushedLatin()) {
                        onCommitGlyph(g)
                    }
                    onCommitGlyph(" ")
                },
                onPeriod = {
                    haptic()
                    for (g in commitFlushedLatin()) {
                        onCommitGlyph(g)
                    }
                    onCommitGlyph(".")
                },
                onComma = {
                    haptic()
                    for (g in commitFlushedLatin()) {
                        onCommitGlyph(g)
                    }
                    onCommitGlyph(",")
                },
                onEnter = {
                    haptic()
                    val hadPending = rawBuffer.isNotEmpty() || digitBuffer.isNotEmpty()
                    for (g in commitFlushedLatin()) {
                        onCommitGlyph(g)
                    }
                    for (g in flushDigitsAsCuneiform()) {
                        onCommitGlyph(g)
                    }
                    if (!hadPending) {
                        onEnter()
                    }
                },
            )
        }
    }
}

@Composable
private fun KeyRow(
    keys: List<LetterKey>,
    modifier: Modifier = Modifier,
    keyHeight: Dp,
    cornerDp: Dp,
    showKeyBorders: Boolean,
    capsOn: Boolean,
    onLetter: (LetterKey) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        keys.forEach { key ->
            LetterKeyChip(
                key = key,
                modifier = Modifier.weight(1f),
                height = keyHeight,
                cornerDp = cornerDp,
                showKeyBorders = showKeyBorders,
                capsOn = capsOn,
                onClick = { onLetter(key) },
            )
        }
    }
}

@Composable
private fun LetterKeyChip(
    key: LetterKey,
    modifier: Modifier,
    height: Dp,
    cornerDp: Dp,
    showKeyBorders: Boolean,
    capsOn: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(cornerDp)
    val borderMod =
        if (showKeyBorders) {
            Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), shape)
        } else {
            Modifier
        }
    val bg =
        if (showKeyBorders) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
        }
    val display =
        if (key.main == '\'') {
            "'"
        } else if (capsOn) {
            key.main.uppercaseChar().toString()
        } else {
            key.main.toString()
        }
    Box(
        modifier =
            modifier
                .height(height)
                .clip(shape)
                .background(bg)
                .then(borderMod)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = display,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ModifierKey(
    modifier: Modifier,
    height: Dp,
    cornerDp: Dp,
    showBorder: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(cornerDp)
    val bg =
        if (showBorder) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
        }
    val borderMod =
        if (showBorder) Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), shape)
        else Modifier
    Box(
        modifier =
            modifier
                .height(height)
                .clip(shape)
                .background(bg)
                .then(borderMod)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun BottomRow(
    keyHeight: Dp,
    cornerDp: Dp,
    showKeyBorders: Boolean,
    onSymbols: () -> Unit,
    onSpace: () -> Unit,
    onComma: () -> Unit,
    onPeriod: () -> Unit,
    onEnter: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        TextChipKey(
            text = "123",
            modifier = Modifier.width(keyHeight * 1.35f),
            height = keyHeight * 0.92f,
            cornerDp = cornerDp,
            showKeyBorders = showKeyBorders,
            onClick = onSymbols,
        )
        TextChipKey(
            text = ",",
            modifier = Modifier.width(keyHeight * 0.9f),
            height = keyHeight * 0.92f,
            cornerDp = cornerDp,
            showKeyBorders = showKeyBorders,
            onClick = onComma,
        )
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .height(keyHeight * 0.92f)
                    .clip(RoundedCornerShape(cornerDp))
                    .background(
                        if (showKeyBorders) {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
                        },
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSpace,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Circle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.height(10.dp),
            )
        }
        TextChipKey(
            text = ".",
            modifier = Modifier.width(keyHeight * 0.9f),
            height = keyHeight * 0.92f,
            cornerDp = cornerDp,
            showKeyBorders = showKeyBorders,
            onClick = onPeriod,
        )
        ModifierKey(
            modifier = Modifier.width(keyHeight * 1.35f),
            height = keyHeight * 0.92f,
            cornerDp = cornerDp,
            showBorder = showKeyBorders,
            onClick = onEnter,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardReturn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TextChipKey(
    text: String,
    modifier: Modifier,
    height: Dp,
    cornerDp: Dp,
    showKeyBorders: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(cornerDp)
    val bg =
        if (showKeyBorders) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
        }
    val borderMod =
        if (showKeyBorders) Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), shape)
        else Modifier
    Box(
        modifier =
            modifier
                .height(height)
                .clip(shape)
                .background(bg)
                .then(borderMod)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SymbolKeysRow(
    chars: List<String>,
    keyHeight: Dp,
    cornerDp: Dp,
    showKeyBorders: Boolean,
    onKey: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        chars.forEach { ch ->
            TextChipKey(
                text = ch,
                modifier = Modifier.weight(1f),
                height = keyHeight * 0.88f,
                cornerDp = cornerDp,
                showKeyBorders = showKeyBorders,
                onClick = { onKey(ch) },
            )
        }
    }
}

@Composable
private fun SymbolRows(
    keyHeight: Dp,
    cornerDp: Dp,
    showKeyBorders: Boolean,
    onKey: (String) -> Unit,
    onBackspace: () -> Unit,
    onToggleLetters: () -> Unit,
    onSpace: () -> Unit,
    onEnter: () -> Unit,
) {
    val r1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    val r2 = listOf("-", "/", ":", ";", "(", ")", "$", "&", "@", "\"")
    val r3 = listOf("#", "+", "=", "*", "[", "]", "<", ">", "\\")

    SymbolKeysRow(r1, keyHeight, cornerDp, showKeyBorders, onKey)
    SymbolKeysRow(r2, keyHeight, cornerDp, showKeyBorders, onKey)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        ModifierKey(
            modifier = Modifier.width(keyHeight * 1.2f),
            height = keyHeight * 0.88f,
            cornerDp = cornerDp,
            showBorder = showKeyBorders,
            onClick = onBackspace,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        r3.forEach { ch ->
            TextChipKey(
                text = ch,
                modifier = Modifier.weight(1f),
                height = keyHeight * 0.88f,
                cornerDp = cornerDp,
                showKeyBorders = showKeyBorders,
                onClick = { onKey(ch) },
            )
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        TextChipKey(
            text = "ABC",
            modifier = Modifier.width(keyHeight * 1.35f),
            height = keyHeight * 0.88f,
            cornerDp = cornerDp,
            showKeyBorders = showKeyBorders,
            onClick = onToggleLetters,
        )
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .height(keyHeight * 0.88f)
                    .clip(RoundedCornerShape(cornerDp))
                    .background(
                        if (showKeyBorders) MaterialTheme.colorScheme.surfaceContainerHigh
                        else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSpace,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Circle,
                contentDescription = null,
                modifier = Modifier.height(10.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
            )
        }
        TextChipKey(
            text = ".",
            modifier = Modifier.width(keyHeight * 0.85f),
            height = keyHeight * 0.88f,
            cornerDp = cornerDp,
            showKeyBorders = showKeyBorders,
            onClick = { onKey(".") },
        )
        ModifierKey(
            modifier = Modifier.width(keyHeight * 1.35f),
            height = keyHeight * 0.88f,
            cornerDp = cornerDp,
            showBorder = showKeyBorders,
            onClick = onEnter,
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardReturn, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
