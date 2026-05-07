package com.belleval.enmerkar.type.ime

import android.view.inputmethod.InputConnection
import com.belleval.enmerkar.type.diagnostic.DiagnosticLog

/**
 * 公式ドキュメントどおり、[InputConnection.commitText] の newCursorPosition に **1** を渡すと
 * 挿入したテキスト全体の直後にカーソルが進む（LatinIME も同様）。
 * UTF-16 長さを渡すと実装によってはオフセット解釈が崩れ、接続先エディタや IME プロセスが落ちることがある。
 *
 * 変換中テキストを終了させてからコミットする（composing 状態のままコミットしない）。
 */
private const val TAG_IME = "UrukIme"

internal fun InputConnection.commitGlyphText(glyph: String): Boolean {
    if (glyph.isEmpty()) return false
    finishComposingText()
    beginBatchEdit()
    return try {
        commitText(glyph, 1)
    } finally {
        endBatchEdit()
    }
}

internal fun InputConnection.tryCommitGlyphText(glyph: String) {
    try {
        if (!commitGlyphText(glyph)) {
            DiagnosticLog.w(TAG_IME, "commitText returned false (glyph UTF-16 len=${glyph.length})")
        }
    } catch (e: Throwable) {
        DiagnosticLog.e(TAG_IME, "commit failed; glyph UTF-16 len=${glyph.length}", e)
    }
}
