package com.belleval.enmerkar.type.sumerian

private fun glyphFor(cp: Int): String = String(Character.toChars(cp))

/**
 * ラテン転写だけ [buffer] に足す。楔形への変換は [flushGreedy] 等の確定処理でのみ行う。
 */
internal fun appendLatinRaw(buffer: StringBuilder, ch: Char) {
    when {
        ch.lowercaseChar() in 'a'..'z' -> buffer.append(ch.lowercaseChar())
        ch == '\'' -> buffer.append('\'')
        else -> Unit
    }
}

/**
 * 1 文字追加ごとの増分変換: バッファ先頭から確定できる分だけ楔形を列挙する。
 * まだ読みが延びうるときは空リストを返してバッファを保持する。
 */
internal fun appendLatinAndConsume(
    dict: Map<String, Int>,
    buffer: StringBuilder,
    latin: Char,
): List<String> {
    val c = latin.lowercaseChar()
    if (c !in 'a'..'z') {
        return emptyList()
    }
    buffer.append(c)
    return consumeIncremental(dict, buffer)
}

/**
 * スペース・句読点の直前などで、バッファを **貪欲に** 音節分割してから確定する。
 */
internal fun flushGreedy(dict: Map<String, Int>, buffer: StringBuilder): List<String> {
    val out = mutableListOf<String>()
    while (buffer.isNotEmpty()) {
        val b = buffer.toString()
        val m = dict.keys.filter { b.startsWith(it) }.maxByOrNull { it.length }
        if (m == null) {
            out.add(buffer[0].toString())
            buffer.deleteAt(0)
        } else {
            out.add(glyphFor(dict.getValue(m)))
            buffer.delete(0, m.length)
        }
    }
    return out
}

private fun consumeIncremental(
    dict: Map<String, Int>,
    buffer: StringBuilder,
): List<String> {
    val committed = mutableListOf<String>()
    val keys = dict.keys
    while (true) {
        if (buffer.isEmpty()) break
        val b = buffer.toString()
        val asPrefixOfBuffer = keys.filter { b.startsWith(it) }
        if (asPrefixOfBuffer.isEmpty()) {
            val couldGrow = keys.any { it.startsWith(b) }
            if (couldGrow) break
            committed.add(buffer[0].toString())
            buffer.deleteAt(0)
            continue
        }
        val longest = asPrefixOfBuffer.maxBy { it.length }
        when {
            longest.length < b.length -> {
                committed.add(glyphFor(dict.getValue(longest)))
                buffer.delete(0, longest.length)
            }
            longest.length == b.length -> {
                val canExtend = keys.any { it.startsWith(b) && it.length > b.length }
                if (canExtend) break
                committed.add(glyphFor(dict.getValue(longest)))
                buffer.setLength(0)
                break
            }
            else -> break
        }
    }
    return committed
}
