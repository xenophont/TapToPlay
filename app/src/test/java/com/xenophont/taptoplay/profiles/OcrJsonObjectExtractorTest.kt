package com.xenophont.taptoplay.profiles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OcrJsonObjectExtractorTest {
    @Test
    fun extractsObjectFromSurroundingOcrText() {
        assertEquals("""{"schema":"v1"}""", OcrJsonObjectExtractor.extract("noise {\"schema\":\"v1\"} tail"))
    }

    @Test
    fun ignoresBracesAndEscapesInsideStrings() {
        val json = """{"label":"brace } and quote \"","nested":{"ok":true}}"""

        assertEquals(json, OcrJsonObjectExtractor.extract("prefix $json suffix"))
    }

    @Test
    fun rejectsIncompleteOrOversizedInput() {
        assertNull(OcrJsonObjectExtractor.extract("prefix {\"open\":true"))
        assertNull(OcrJsonObjectExtractor.extract("x".repeat(40), maxInputChars = 32))
    }
}
