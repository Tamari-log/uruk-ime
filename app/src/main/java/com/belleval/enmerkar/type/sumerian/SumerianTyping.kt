package com.belleval.enmerkar.type.sumerian

private fun glyphFor(cp: Int): String = String(Character.toChars(cp))

/**
 * ラテン転写だけ [buffer] に足す。楔形への変換は [flushGreedy] 等の確定処理でのみ行う。
 * **-** は音節境界（確定時にセグメント分割）。
 */
internal fun appendLatinRaw(buffer: StringBuilder, ch: Char) {
    when {
        ch.lowercaseChar() in 'a'..'z' -> buffer.append(ch.lowercaseChar())
        ch == '\'' -> buffer.append('\'')
        ch == '-' -> buffer.append('-')
        else -> Unit
    }
}

/**
 * バッファを **-** で区切った各セグメントごとに貪欲分割し、楔形の文字列リストにする。
 * 例: `ma-e` → `ma` と `e` を別音節として変換（`mae` のような一続きの読みと区別）。
 */
internal fun flushGreedy(dict: Map<String, Int>, buffer: StringBuilder): List<String> {
    val text = buffer.toString()
    buffer.clear()
    if (text.isEmpty()) return emptyList()
    val out = mutableListOf<String>()
    for (part in text.split('-')) {
        if (part.isEmpty()) continue
        val preferred = preferredWholeSegmentTokenization(part, dict)
        if (preferred != null) {
            out.addAll(preferred.map { glyphFor(dict.getValue(it)) })
            continue
        }
        val seg = StringBuilder(part)
        out.addAll(flushGreedySingleSegment(dict, seg))
    }
    return out
}

/** ラテン転写の複合子音（CV 化は [glyphForClusterWithEpenthesis]）。長いものを先に照合する。 */
private val consonantClusters: List<String> =
    listOf(
        "sh",
    )

private fun flushGreedySingleSegment(
    dict: Map<String, Int>,
    buffer: StringBuilder,
): List<String> {
    val out = mutableListOf<String>()
    while (buffer.isNotEmpty()) {
        val b = buffer.toString()
        val m = dict.keys.filter { b.startsWith(it) }.maxByOrNull { it.length }
        if (m != null) {
            out.add(glyphFor(dict.getValue(m)))
            buffer.delete(0, m.length)
            continue
        }
        // 辞書に先頭から合うキーがないとき、いずれかのキーの接頭辞になっている最長部分はまだ伸びうるので
        // その分だけラテンにまとめる。その後、単独子音なら仮母音（e,a,i,u）で楔形化を試みる。
        var len = buffer.length
        while (len > 0 && dict.keys.any { key -> key.startsWith(buffer.substring(0, len)) }) {
            len--
        }
        if (len <= 0) {
            val cluster =
                consonantClusters.firstOrNull { c ->
                    buffer.startsWith(c) && buffer.length == c.length
                }
            if (cluster != null) {
                buffer.delete(0, cluster.length)
                out.add(glyphForClusterWithEpenthesis(cluster, dict) ?: cluster)
            } else {
                val lone = buffer[0]
                buffer.deleteAt(0)
                val glyph =
                    if (lone in 'a'..'z' && lone !in "aeiou") {
                        glyphForConsonantWithEpenthesis(lone, dict)
                    } else {
                        null
                    }
                out.add(glyph ?: lone.toString())
            }
        } else {
            val chunk = buffer.substring(0, len)
            buffer.delete(0, len)
            val glyph =
                when {
                    consonantClusters.contains(chunk) -> glyphForClusterWithEpenthesis(chunk, dict)
                    len == 1 && chunk[0] in 'a'..'z' && chunk[0] !in "aeiou" ->
                        glyphForConsonantWithEpenthesis(chunk[0], dict)
                    else -> null
                }
            out.add(glyph ?: chunk)
        }
    }
    return out
}

private data class WholeSegmentTokenization(
    val tokens: List<String>,
    val closedSyllableCount: Int,
)

/**
 * セグメント全体を辞書キーだけで分割できる場合、語末子音で閉じる音節（VC/CVC）を最小化する。
 * 同点時はトークン数が少ない方を優先し、IME として自然な `a-na` 系の分割を選びやすくする。
 */
private fun preferredWholeSegmentTokenization(
    segment: String,
    dict: Map<String, Int>,
): List<String>? {
    if (segment.isEmpty()) return emptyList()
    val memo = mutableMapOf<Int, WholeSegmentTokenization?>()
    val vowels = "aeiou"
    val keysByHead: Map<Char, List<String>> =
        dict.keys.groupBy { it.firstOrNull() ?: '\u0000' }

    fun isBetter(
        candidate: WholeSegmentTokenization,
        best: WholeSegmentTokenization?,
    ): Boolean {
        if (best == null) return true
        if (candidate.closedSyllableCount != best.closedSyllableCount) {
            return candidate.closedSyllableCount < best.closedSyllableCount
        }
        if (candidate.tokens.size != best.tokens.size) {
            return candidate.tokens.size < best.tokens.size
        }
        return candidate.tokens.joinToString("") < best.tokens.joinToString("")
    }

    fun solve(index: Int): WholeSegmentTokenization? {
        if (memo.containsKey(index)) return memo[index]
        if (index == segment.length) {
            val done = WholeSegmentTokenization(emptyList(), 0)
            memo[index] = done
            return done
        }
        val head = segment[index]
        val candidates = keysByHead[head].orEmpty()
        var best: WholeSegmentTokenization? = null
        for (key in candidates) {
            if (!segment.startsWith(key, index)) continue
            val suffix = solve(index + key.length) ?: continue
            val closed = if (key.last() in vowels) 0 else 1
            val tokenized =
                WholeSegmentTokenization(
                    tokens = listOf(key) + suffix.tokens,
                    closedSyllableCount = closed + suffix.closedSyllableCount,
                )
            if (isBetter(tokenized, best)) {
                best = tokenized
            }
        }
        memo[index] = best
        return best
    }

    return solve(0)?.tokens
}

/**
 * 子音のみが残ったとき、仮の母音を e,a,i,u の順に付けて辞書にある音節に変換（CV 形）。
 * 符号は「その C+V 転写で通じる」ものを選ぶ（別音価マップ含む）。
 */
private fun glyphForConsonantWithEpenthesis(c: Char, dict: Map<String, Int>): String? {
    if (c !in 'a'..'z' || c in "aeiou") return null
    for (v in listOf('e', 'a', 'i', 'u')) {
        dict["$c$v"]?.let { return glyphFor(it) }
    }
    return null
}

/**
 * [consonantClusters]（例: sh）だけが残ったとき、**sh**e → **sh**a → … と単子音と同様に仮母音を付与。
 */
private fun glyphForClusterWithEpenthesis(cluster: String, dict: Map<String, Int>): String? {
    if (cluster.isEmpty()) return null
    for (v in listOf('e', 'a', 'i', 'u')) {
        dict["$cluster$v"]?.let { return glyphFor(it) }
    }
    return null
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
            val cluster =
                consonantClusters.firstOrNull { c ->
                    b.startsWith(c) && b.length == c.length
                }
            if (cluster != null) {
                val gCluster = glyphForClusterWithEpenthesis(cluster, dict)
                if (gCluster != null) {
                    committed.add(gCluster)
                    buffer.delete(0, cluster.length)
                    continue
                }
            }
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
