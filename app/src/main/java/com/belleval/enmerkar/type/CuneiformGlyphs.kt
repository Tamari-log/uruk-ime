package com.belleval.enmerkar.type

internal fun cuneiformCodePoints(range: IntRange): List<Int> =
    range.filter { Character.isDefined(it) }
