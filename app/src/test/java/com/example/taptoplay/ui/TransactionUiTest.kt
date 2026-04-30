package com.example.taptoplay.ui

import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionUiTest {
    @Test
    fun formatsTransactionTimestampToLocalSeconds() {
        val formatted = formatTransactionTimestamp(
            value = "2026-04-29T18:57:21.010102Z",
            zoneId = ZoneId.of("Europe/Madrid"),
        )

        assertEquals("2026-04-29 20:57:21", formatted)
    }

    @Test
    fun keepsUnparseableTimestampAsFallback() {
        assertEquals("pending", formatTransactionTimestamp("pending", ZoneId.of("UTC")))
    }
}
