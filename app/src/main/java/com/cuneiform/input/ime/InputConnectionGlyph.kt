package com.cuneiform.input.ime

import android.util.Log
import android.view.inputmethod.InputConnection

/**
 * 公式ドキュメントどおり、[InputConnection.commitText] の newCursorPosition に **1** を渡すと
 * 挿入したテキスト全体の直後にカーソルが進む（LatinIME も同様）。
 * UTF-16 長さを渡すと実装によってはオフセット解釈が崩れ、接続先エディタや IME プロセスが落ちることがある。
 *
 * 変換中テキストを終了させてからコミットする（composing 状態のままコミットしない）。
 */
private const val TAG_IME = "CuneiformIME"

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
            Log.w(TAG_IME, "commitText returned false")
        }
    } catch (e: Throwable) {
        Log.e(TAG_IME, "commit failed; glyph len UTF-16=${glyph.length}", e)
    }
}
