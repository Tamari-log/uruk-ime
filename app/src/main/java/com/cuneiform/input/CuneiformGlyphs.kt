package com.cuneiform.input

internal fun cuneiformCodePoints(range: IntRange): List<Int> =
    range.filter { Character.isDefined(it) }
