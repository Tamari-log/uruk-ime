package com.uruk.ime

/** 末尾の1コードポイント（サロゲートペア含む）を除いた文字列 */
internal fun String.withoutLastCodePoint(): String {
    if (isEmpty()) return this
    val cpc = codePointCount(0, length)
    if (cpc == 0) return this
    return substring(0, offsetByCodePoints(0, cpc - 1))
}

/** このシーケンス末尾1コードポイントが占める UTF-16 長（サロゲートペアなら2） */
internal fun CharSequence.utf16LengthOfLastCodePoint(): Int {
    val s = this.toString()
    if (s.isEmpty()) return 0
    val cpc = s.codePointCount(0, s.length)
    if (cpc == 0) return 0
    val start = s.offsetByCodePoints(0, cpc - 1)
    return s.length - start
}
