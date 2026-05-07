package com.belleval.enmerkar.type.sumerian

/**
 * 教育・入力補助向けの簡易「活用」チップ。
 * いずれも **ラテン転写** をバッファへ連結するだけ。古语文法の網羅ではない。
 */
data class MorphemeChip(val shortLabel: String, val insertsLatin: String)

@Suppress("ConstPropertyName")
object SumerianInflection {

    /**
     * 限定・非限定を含む、よくある動詞接頭辞（転写）。語幹の前に続けてから確定（スペース等）する。
     */
    val verbalPrefixes: List<MorphemeChip> =
        listOf(
            MorphemeChip("mu·", "mu"),
            MorphemeChip("nu·", "nu"),
            MorphemeChip("ḫa·", "ha"), // Latin h → /ḫ/
            MorphemeChip("u·", "u"),
            MorphemeChip("i·", "i"),
            MorphemeChip("ba·", "ba"),
            MorphemeChip("im·", "im"),
            MorphemeChip("in·", "in"),
            MorphemeChip("bi·", "bi"),
            MorphemeChip("a·", "a"),
            MorphemeChip("ga·", "ga"),
            MorphemeChip("na·", "na"),
            MorphemeChip("nam·", "nam"),
            MorphemeChip("ni·", "ni"),
            MorphemeChip("rá·", "ra"),
            MorphemeChip("ša·", "sha"),
        )

    /**
     * 名詞句の格・よく使う終端（転写）。e2ra の ra など既存音節辞書に載っているものを選ぶ。
     */
    val nominalCaseSuffixes: List<MorphemeChip> =
        listOf(
            MorphemeChip("-e (格)", "e"),
            MorphemeChip("-ra (与格)", "ra"),
            MorphemeChip("-ak (属)", "ak"),
            MorphemeChip("-ta (奪)", "ta"),
            MorphemeChip("-da", "da"),
            MorphemeChip("-a (位)", "a"),
            MorphemeChip("-bi", "bi"),
            MorphemeChip("-še", "she"),
        )

    /**
     * 人称接尾辞の一例（転写）。辞書に載る短い音節に限定。
     */
    val pronominalSuffixes: List<MorphemeChip> =
        listOf(
            MorphemeChip("-en", "en"),
            MorphemeChip("-ne", "ne"),
            MorphemeChip("-zu", "zu"),
        )
}
