package com.example.taptoplay.adyen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransactionRecordTest {
    @Test
    fun mapsApprovedResultToTransactionSummary() {
        val result = PaymentResult.Success(pspReference = "psp-123", rawResult = "success")

        assertEquals(TransactionStatus.APPROVED, result.toTransactionStatus())
        assertEquals("Approved | PSP psp-123", result.toTransactionSummary())
        assertNull(result.failureReasonOrNull())
    }

    @Test
    fun keepsPspReferenceSeparateFromTerminalTransactionId() {
        val result = PaymentResult.Success(
            pspReference = "PSP123",
            rawResult = "Success",
            terminalTransactionId = "tender.PSP123",
        )

        assertEquals("PSP123", result.pspReferenceOrNull())
        assertEquals("tender.PSP123", result.transactionIdOrNull())
    }

    @Test
    fun mapsRefusedResultToFailureDetailForUser() {
        val result = PaymentResult.Refused(reason = "Not enough funds")

        assertEquals(TransactionStatus.REFUSED, result.toTransactionStatus())
        assertEquals("Refused | Not enough funds", result.toTransactionSummary())
        assertEquals("Not enough funds", result.failureReasonOrNull())
    }

    @Test
    fun mapsUnknownFailureToFailureDetailForUser() {
        val result = PaymentResult.Failure(message = "Malformed response")

        assertEquals(TransactionStatus.FAILED, result.toTransactionStatus())
        assertEquals("Failed | Malformed response", result.toTransactionSummary())
        assertEquals("Malformed response", result.failureReasonOrNull())
    }

    @Test
    fun extractsPspReferenceFromSavedTerminalApiResponse() {
        val record = TransactionRecord(
            id = "record-1",
            createdAt = "2026-04-29T12:00:00Z",
            amountLabel = "EUR 12.00",
            itemCount = 1,
            saleToAcquirerDataName = "Default",
            requestJson = "{}",
            responseBody = """
                {
                  "SaleToPOIResponse": {
                    "PaymentResponse": {
                      "Response": {
                        "Result": "Success",
                        "AdditionalResponse": "tid=123&pspReference=PSP123"
                      }
                    }
                  }
                }
            """.trimIndent(),
        )

        assertEquals("PSP123", record.pspReferenceOrNull())
    }
}
