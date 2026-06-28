package com.xenophont.taptoplay.profiles

/**
 * Recovers the first balanced JSON object from noisy OCR text.
 *
 * This is not a JSON parser. It only isolates a candidate object while respecting quoted strings;
 * callers must still decode and validate the result with their domain JSON serializer.
 */
object OcrJsonObjectExtractor {
    fun extract(text: String, maxInputChars: Int = MAX_INPUT_CHARS): String? {
        if (text.length > maxInputChars) return null
        val start = text.indexOf('{')
        if (start < 0) return null
        var depth = 0
        var inString = false
        var escaped = false
        for (index in start until text.length) {
            val char = text[index]
            when {
                escaped -> escaped = false
                char == '\\' && inString -> escaped = true
                char == '"' -> inString = !inString
                !inString && char == '{' -> depth++
                !inString && char == '}' -> {
                    depth--
                    if (depth == 0) return text.substring(start, index + 1)
                }
            }
        }
        return null
    }

    private const val MAX_INPUT_CHARS = 32_768
}
