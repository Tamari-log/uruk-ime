package com.belleval.enmerkar.type.sumerian

/**
 * アラビア数字の入力バッファを **整数として解釈** し、楔形数字符号に変換する。
 *
 * - **1 桁**（または 60 未満への分割後の塊）: 1〜9 は [asciiDigitToCuneiformGlyph]、0 はコロン約物。
 * - **2 桁以上かつ値 < 60**: Unicode 上は「21」用の単一コードポイントが無いため、
 *   **10 の位**に BAN2 列（TWO BAN2 … ・Unicode 値は 2…6 だが、ここでは十の位ブロックのマーカーとして用いる）、
 *   **1 の位**に GESH2 列（ONE … NINE GESH2）を **1 つの数として連結** する（例: 21 → THREE BAN2 + ONE GESH2）。
 * - **60 以上**: 60 進の下位→上位へ再帰し、各 **60 未満の塊** を上記で表す。
 */
internal fun digitBufferToCuneiformClusters(digits: String): List<String> {
    if (digits.isEmpty()) return emptyList()
    if (!digits.all { it.isDigit() }) {
        return digits.map { asciiDigitToCuneiformGlyph(it) }
    }
    val n =
        digits.toIntOrNull()
            ?: return digits.map { asciiDigitToCuneiformGlyph(it) }
    return clustersForNonNegativeInt(n)
}

private fun clustersForNonNegativeInt(n: Int): List<String> {
    if (n < 0) {
        return listOf(asciiDigitToCuneiformGlyph('0'))
    }
    if (n == 0) {
        return listOf(asciiDigitToCuneiformGlyph('0'))
    }
    if (n < 60) {
        return listOf(decimalBelow60Bundled(n))
    }
    val hi = n / 60
    val lo = n % 60
    val out = mutableListOf<String>()
    out.addAll(clustersForNonNegativeInt(hi))
    if (lo > 0) {
        out.add(decimalBelow60Bundled(lo))
    }
    return out
}

/**
 * 1 … 59 を **一体の数** として表す（60 未満ブロック用）。
 */
private fun decimalBelow60Bundled(n: Int): String {
    require(n in 1..59)
    if (n < 10) {
        return asciiDigitToCuneiformGlyph(('0' + n))
    }
    val decade = n / 10
    val unit = n % 10
    val sb = StringBuilder()
    sb.appendCodePoint(0x1244F + decade)
    if (unit > 0) {
        sb.appendCodePoint(0x12415 + (unit - 1))
    }
    return sb.toString()
}

/**
 * アラビア数字 1 字 → 楔形数字（1〜9 用の単位系。0 は約物プレースホルダ）。
 */
internal fun asciiDigitToCuneiformGlyph(digit: Char): String {
    require(digit in '0'..'9')
    val cp =
        when (digit) {
            '0' -> 0x12471
            '1' -> 0x1244F
            '2' -> 0x12400
            '3' -> 0x12401
            '4' -> 0x12402
            '5' -> 0x12403
            '6' -> 0x12404
            '7' -> 0x12405
            '8' -> 0x12406
            '9' -> 0x12407
            else -> error("not a digit")
        }
    return String(Character.toChars(cp))
}
