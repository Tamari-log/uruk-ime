package com.uruk.ime.ime

import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnLifecycleDestroyed
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.uruk.ime.R
import com.uruk.ime.prefs.ImeUserPrefs
import com.uruk.ime.prefs.darkThemeFlag
import com.uruk.ime.prefs.imeUserPrefsFlow
import com.uruk.ime.ui.UrukImeKeyboardContent
import com.uruk.ime.ui.theme.UrukImeTheme
import com.uruk.ime.utf16LengthOfLastCodePoint

private const val TAG_IME = "UrukIme"

private const val CONTENT_ROOT_TAG = "uruk_ime_compose_root"

/**
 * FlorisBoard 方式: [android.R.id.content] に Compose を載せ、[onCreateInputView] は null。
 * [LifecycleInputMethodService] で decor に ViewTree 所有者を載せる。
 */
class UrukImeService : LifecycleInputMethodService() {

    private var composeInputRoot: View? = null

    override fun onCreate() {
        super.onCreate()
        setExtractViewShown(false)
        window?.window?.let { w ->
            WindowCompat.setDecorFitsSystemWindows(w, false)
        }
        applyImeWindowLayout()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        applyImeWindowLayout()
        composeInputRoot?.post { composeInputRoot?.requestLayout() }
    }

    /** IME ダイアログが 0 高さのままだと ImeTracker が即 hide することがある。 */
    private fun applyImeWindowLayout() {
        try {
            val w = window?.window ?: run {
                Log.w(TAG_IME, "applyImeWindowLayout: inner window is null")
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
            Log.e(TAG_IME, "applyImeWindowLayout failed", e)
        }
    }

    override fun onCreateCandidatesView(): View? = null

    override fun onCreateInputView(): View? {
        installViewTreeOwners()
        applyImeWindowLayout()
        val decorContent =
            window?.window?.findViewById<ViewGroup>(android.R.id.content)
                ?: run {
                    Log.e(TAG_IME, "onCreateInputView: content is null")
                    return null
                }
        decorContent.findViewWithTag<View>(CONTENT_ROOT_TAG)?.let {
            decorContent.removeView(it)
        }
        val compose = buildKeyboardComposeView()
        compose.tag = CONTENT_ROOT_TAG
        decorContent.addView(
            compose,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = Gravity.BOTTOM
            },
        )
        composeInputRoot = compose
        return null
    }

    private fun buildKeyboardComposeView(): ComposeView =
        ComposeView(this).apply {
            setViewCompositionStrategy(DisposeOnLifecycleDestroyed(lifecycle))
            setContent {
                var prefs by remember { mutableStateOf(ImeUserPrefs.DEFAULT) }
                LaunchedEffect(Unit) {
                    applicationContext.imeUserPrefsFlow().collect { prefs = it }
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
                                Text(
                                    text = stringResource(R.string.ime_keyboard_label),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .padding(start = 8.dp),
                                )
                                IconButton(
                                    onClick = {
                                        if (prefs.hapticOnKeypress) {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        }
                                        val ic = currentInputConnection ?: return@IconButton
                                        val before = ic.getTextBeforeCursor(8, 0) ?: return@IconButton
                                        val n = before.utf16LengthOfLastCodePoint()
                                        if (n > 0) ic.deleteSurroundingText(n, 0)
                                    },
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.action_delete),
                                    )
                                }
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
                        }
                    }
                }
            }
        }

    @Suppress("DEPRECATION")
    override fun onComputeInsets(outInsets: android.inputmethodservice.InputMethodService.Insets) {
        super.onComputeInsets(outInsets)
        val v = composeInputRoot
        if (v != null && v.isShown && v.width > 0 && v.height > 0) {
            val screenLoc = IntArray(2)
            v.getLocationOnScreen(screenLoc)
            outInsets.contentTopInsets = screenLoc[1]
            outInsets.visibleTopInsets = screenLoc[1]
            outInsets.touchableInsets =
                android.inputmethodservice.InputMethodService.Insets.TOUCHABLE_INSETS_REGION
            val winLoc = IntArray(2)
            v.getLocationInWindow(winLoc)
            outInsets.touchableRegion.set(
                winLoc[0],
                winLoc[1],
                winLoc[0] + v.width,
                winLoc[1] + v.height,
            )
        }
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

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
