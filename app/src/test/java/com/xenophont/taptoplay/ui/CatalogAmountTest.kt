package com.xenophont.taptoplay.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogAmountTest {
    @Test
    fun parsesEuroAmountsWithCommaOrDot() {
        assertEquals(100L, parseEuroAmountMinor("1.00"))
        assertEquals(250L, parseEuroAmountMinor("2,50"))
        assertEquals(7L, parseEuroAmountMinor("0.07"))
    }

    @Test
    fun rejectsEmptyZeroAndOverPreciseAmounts() {
        assertNull(parseEuroAmountMinor(""))
        assertNull(parseEuroAmountMinor("0"))
        assertNull(parseEuroAmountMinor("1.234"))
        assertNull(parseEuroAmountMinor("EUR 1.00"))
    }
}
