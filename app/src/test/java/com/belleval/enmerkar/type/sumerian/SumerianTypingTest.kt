package com.belleval.enmerkar.type.sumerian

import org.junit.Assert.assertEquals
import org.junit.Test

class SumerianTypingTest {
    private val dict = SumerianTransliteration.readingToCodepoint

    private fun glyphsFor(vararg readings: String): List<String> =
        readings.map { reading ->
            String(Character.toChars(dict.getValue(reading)))
        }

    @Test
    fun flushGreedy_prefersOpenSyllables_forAnaMeEn() {
        val buffer = StringBuilder("ana-me-en")

        val actual = flushGreedy(dict, buffer)

        assertEquals(glyphsFor("a", "na", "me", "en"), actual)
        assertEquals("", buffer.toString())
    }

    @Test
    fun flushGreedy_keepsExplicitHyphenSegmentation() {
        val buffer = StringBuilder("an-me-en")

        val actual = flushGreedy(dict, buffer)

        assertEquals(glyphsFor("an", "me", "en"), actual)
    }

    @Test
    fun flushGreedy_keepsSingleSignWord_whenBetter() {
        val buffer = StringBuilder("lugal")

        val actual = flushGreedy(dict, buffer)

        assertEquals(glyphsFor("lugal"), actual)
    }

    @Test
    fun flushGreedy_prefersOpenSyllables_forInana() {
        val buffer = StringBuilder("inana")

        val actual = flushGreedy(dict, buffer)

        assertEquals(glyphsFor("i", "na", "na"), actual)
    }

    @Test
    fun flushGreedy_prefersOpenSyllables_forAnana() {
        val buffer = StringBuilder("anana")

        val actual = flushGreedy(dict, buffer)

        assertEquals(glyphsFor("a", "na", "na"), actual)
    }

    @Test
    fun flushGreedy_prefersStandardSplit_forEnlil() {
        val buffer = StringBuilder("enlil")

        val actual = flushGreedy(dict, buffer)

        assertEquals(glyphsFor("en", "lil"), actual)
    }

    @Test
    fun flushGreedy_prefersLexicalSplit_forNinlil() {
        val buffer = StringBuilder("ninlil")

        val actual = flushGreedy(dict, buffer)

        assertEquals(glyphsFor("nin", "lil"), actual)
    }

    @Test
    fun flushGreedy_keepsExplicitHyphen_forEnLil() {
        val buffer = StringBuilder("en-lil")

        val actual = flushGreedy(dict, buffer)

        assertEquals(glyphsFor("en", "lil"), actual)
    }

    @Test
    fun flushGreedy_keepsExplicitHyphen_forLugalE() {
        val buffer = StringBuilder("lugal-e")

        val actual = flushGreedy(dict, buffer)

        assertEquals(glyphsFor("lugal", "e"), actual)
    }

    @Test
    fun flushGreedy_keepsExplicitHyphen_forLugalENe() {
        val buffer = StringBuilder("lugal-e-ne")

        val actual = flushGreedy(dict, buffer)

        assertEquals(glyphsFor("lugal", "e", "ne"), actual)
    }

    @Test
    fun flushGreedy_withoutHyphen_prefersOpenSyllables_forLugale() {
        val buffer = StringBuilder("lugale")

        val actual = flushGreedy(dict, buffer)

        assertEquals(glyphsFor("lu", "ga", "le"), actual)
    }
}
