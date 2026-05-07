package com.uruk.ime

internal fun cuneiformCodePoints(range: IntRange): List<Int> =
    range.filter { Character.isDefined(it) }
