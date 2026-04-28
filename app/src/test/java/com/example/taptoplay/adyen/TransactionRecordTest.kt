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
}
