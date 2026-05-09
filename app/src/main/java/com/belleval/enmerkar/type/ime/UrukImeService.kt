package com.belleval.enmerkar.type.ime

import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnLifecycleDestroyed
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.belleval.enmerkar.type.R
import com.belleval.enmerkar.type.diagnostic.DiagnosticLog
import com.belleval.enmerkar.type.prefs.ImeUserPrefs
import com.belleval.enmerkar.type.prefs.darkThemeFlag
import com.belleval.enmerkar.type.prefs.imeUserPrefsFlow
import com.belleval.enmerkar.type.ui.UrukImeKeyboardContent
import com.belleval.enmerkar.type.ui.theme.UrukImeTheme
import com.belleval.enmerkar.type.utf16LengthOfLastCodePoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG_IME = "UrukIme"
private const val DELETE_REPEAT_START_DELAY_MS = 350L
private const val DELETE_REPEAT_INTERVAL_MS = 45L

/**
 * [onCreateInputView] でキーボード [View] を返し、[InputMethodService] の inputArea（[android.R.id.inputArea]）に載せる。
 * inputArea が空のままだと [onComputeInsets] が誤った領域となり、アプリ側の IME インセット／スクロールがずれる。
 * [LifecycleInputMethodService] で decor に ViewTree 所有者を載せる。
 */
class UrukImeService : LifecycleInputMethodService() {

    private var composeInputRoot: View? = null
    private var serviceCreatedAtMs: Long = 0L
    private var inputViewCreatedAtMs: Long = 0L
    @Volatile
    private var persistTimingLogs: Boolean = false

    private fun timing(label: String) {
        val now = SystemClock.elapsedRealtime()
        val fromServiceCreate =
            if (serviceCreatedAtMs > 0L) {
                "${now - serviceCreatedAtMs}ms"
            } else {
                "n/a"
            }
        val fromInputViewCreate =
            if (inputViewCreatedAtMs > 0L) {
                "${now - inputViewCreatedAtMs}ms"
            } else {
                "n/a"
            }
        val msg =
            "timing: $label | tSinceServiceCreate=$fromServiceCreate | tSinceInputViewCreate=$fromInputViewCreate"
        Log.i(TAG_IME, msg)
        if (persistTimingLogs) {
            DiagnosticLog.i(TAG_IME, msg)
        }
    }

    override fun onCreate() {
        super.onCreate()
        serviceCreatedAtMs = SystemClock.elapsedRealtime()
        DiagnosticLog.i(TAG_IME, "UrukImeService onCreate")
        timing("onCreate")
        setExtractViewShown(false)
        window?.window?.let { w ->
            WindowCompat.setDecorFitsSystemWindows(w, false)
        }
        applyImeWindowLayout()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        timing("onStartInputView(restarting=$restarting)")
        applyImeWindowLayout()
        composeInputRoot?.post { composeInputRoot?.requestLayout() }
    }

    /** IME ダイアログが 0 高さのままだと ImeTracker が即 hide することがある。 */
    private fun applyImeWindowLayout() {
        try {
            val w = window?.window ?: run {
                DiagnosticLog.w(TAG_IME, "applyImeWindowLayout: inner window is null")
                return
            }
            val lp = w.attributes
            lp.width = WindowManager.LayoutParams.MATCH_PARENT
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT
            lp.gravity = Gravity.BOTTOM
            lp.horizontalMargin = 0f
            lp.verticalMargin = 0f
            w.attributes = lp
            w.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
            )
        } catch (e: Throwable) {
            DiagnosticLog.e(TAG_IME, "applyImeWindowLayout failed", e)
        }
    }

    override fun onCreateCandidatesView(): View? = null

    override fun onCreateInputView(): View? {
        return try {
            timing("onCreateInputView:start")
            installViewTreeOwners()
            applyImeWindowLayout()
            val compose = buildKeyboardComposeView()
            composeInputRoot = compose
            inputViewCreatedAtMs = SystemClock.elapsedRealtime()
            timing("onCreateInputView:end")
            compose
        } catch (t: Throwable) {
            DiagnosticLog.e(TAG_IME, "onCreateInputView failed", t)
            null
        }
    }

    private fun buildKeyboardComposeView(): ComposeView =
        ComposeView(this).apply {
            setViewCompositionStrategy(DisposeOnLifecycleDestroyed(lifecycle))
            setContent {
                var prefs by remember { mutableStateOf(ImeUserPrefs.DEFAULT) }
                var showFullKeyboard by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    applicationContext.imeUserPrefsFlow().collect {
                        prefs = it
                        persistTimingLogs = it.persistTimingLogs
                    }
                }
                LaunchedEffect(Unit) {
                    timing("compose:initial-shell")
                    withFrameNanos { }
                    showFullKeyboard = true
                    timing("compose:full-keyboard")
                }
                val systemDark = isSystemInDarkTheme()
                val darkTheme = prefs.darkThemeFlag(systemDark)
                UrukImeTheme(darkTheme = darkTheme) {
                    var tabIndex by remember { mutableIntStateOf(prefs.defaultTabIndex) }
                    LaunchedEffect(prefs.defaultTabIndex) {
                        tabIndex = prefs.defaultTabIndex
                    }
                    val view = LocalView.current
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp,
                    ) {
                        timing("compose:Surface")
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                                    .padding(bottom = 10.dp),
                        ) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                fun deleteOnce() {
                                    val ic = currentInputConnection ?: return
                                    val before = ic.getTextBeforeCursor(8, 0) ?: return
                                    val n = before.utf16LengthOfLastCodePoint()
                                    if (n > 0) ic.deleteSurroundingText(n, 0)
                                }
                                Text(
                                    text = stringResource(R.string.ime_keyboard_label),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .padding(start = 8.dp),
                                )
                                RepeatDeleteIconButton(
                                    hapticOnKeypress = prefs.hapticOnKeypress,
                                    onDeleteOnce = { deleteOnce() },
                                    onHaptic = {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    },
                                )
                                IconButton(
                                    onClick = {
                                        if (prefs.hapticOnKeypress) {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        }
                                        requestHideSelf(0)
                                    },
                                ) {
                                    Icon(
                                        Icons.Default.KeyboardHide,
                                        contentDescription = stringResource(R.string.ime_hide_keyboard),
                                    )
                                }
                            }
                            HorizontalDivider()
                            if (showFullKeyboard) {
                                UrukImeKeyboardContent(
                                    tabIndex = tabIndex,
                                    onTabChange = { tabIndex = it },
                                    onGlyphSelected = { ch ->
                                        val ic = currentInputConnection ?: return@UrukImeKeyboardContent
                                        ic.tryCommitGlyphText(ch)
                                    },
                                    onBackspace = {
                                        val ic = currentInputConnection ?: return@UrukImeKeyboardContent
                                        val before = ic.getTextBeforeCursor(8, 0) ?: return@UrukImeKeyboardContent
                                        val n = before.utf16LengthOfLastCodePoint()
                                        if (n > 0) ic.deleteSurroundingText(n, 0)
                                    },
                                    onEditorEnter = { performEditorEnter() },
                                    keyboardAreaHeightDp = prefs.keyboardSurfaceHeightDp(),
                                    hapticOnKeypress = prefs.hapticOnKeypress,
                                    gridMinCellDp = prefs.gridMinCellDp(),
                                    gridCellHeightDp = prefs.gridCellHeightDp(),
                                    showKeyBorders = prefs.showKeyBorders,
                                )
                            } else {
                                WarmupShell(
                                    keyboardHeightDp = prefs.keyboardSurfaceHeightDp(),
                                    onSpace = {
                                        currentInputConnection?.commitText(" ", 1)
                                    },
                                    onEnter = { performEditorEnter() },
                                )
                            }
                        }
                    }
                }
            }
        }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
    }

    override fun onFinishInput() {
        super.onFinishInput()
    }

    private fun performEditorEnter() {
        val ic = currentInputConnection ?: return
        ic.finishComposingText()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            sendDefaultEditorAction(false)
        } else {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        }
    }
}

@Composable
private fun RepeatDeleteIconButton(
    hapticOnKeypress: Boolean,
    onDeleteOnce: () -> Unit,
    onHaptic: () -> Unit,
) {
    var repeating by remember { mutableStateOf(false) }
    LaunchedEffect(repeating) {
        if (!repeating) return@LaunchedEffect
        while (repeating) {
            delay(DELETE_REPEAT_INTERVAL_MS)
            onDeleteOnce()
        }
    }
    Icon(
        imageVector = Icons.Default.Delete,
        contentDescription = stringResource(R.string.action_delete),
        modifier =
            Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .awaitRepeatDeleteGesture(
                    onSingleTapDelete = {
                        if (hapticOnKeypress) onHaptic()
                        onDeleteOnce()
                    },
                    onRepeatStart = {
                        if (hapticOnKeypress) onHaptic()
                        onDeleteOnce()
                        repeating = true
                    },
                    onRepeatStop = { repeating = false },
                ),
    )
}

private fun Modifier.awaitRepeatDeleteGesture(
    onSingleTapDelete: () -> Unit,
    onRepeatStart: () -> Unit,
    onRepeatStop: () -> Unit,
): Modifier =
    pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown()
            val tapUp = withTimeoutOrNull(DELETE_REPEAT_START_DELAY_MS) { waitForUpOrCancellation() }
            if (tapUp != null) {
                onSingleTapDelete()
                down.consume()
                tapUp.consume()
                return@awaitEachGesture
            }
            onRepeatStart()
            val longPressUp = waitForUpOrCancellation()
            onRepeatStop()
            down.consume()
            longPressUp?.consume()
        }
    }

@Composable
private fun WarmupShell(
    keyboardHeightDp: Float,
    onSpace: () -> Unit,
    onEnter: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(keyboardHeightDp.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.ime_starting),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 40.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                onClick = onSpace,
                tonalElevation = 1.dp,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(R.string.ime_quick_space),
                    modifier = Modifier.padding(vertical = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                onClick = onEnter,
                tonalElevation = 1.dp,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(R.string.ime_quick_enter),
                    modifier = Modifier.padding(vertical = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
