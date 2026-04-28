package com.example.taptoplay.adyen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentResultParserTest {
    @Test
    fun parsesBoardingReturn() {
        val result = PaymentResultParser.parse("taptoplay://adyen-return?installationId=abc")

        assertEquals(PaymentResult.Boarding("abc"), result)
    }

    @Test
    fun parsesApprovedPayment() {
        val result = PaymentResultParser.parse("taptoplay://adyen-return?result=success&pspReference=psp-1")

        assertEquals(PaymentResult.Success("psp-1", "success"), result)
    }

    @Test
    fun parsesRefusedPayment() {
        val result = PaymentResultParser.parse("taptoplay://adyen-return?result=refused&reason=Not%20enough%20funds")

        assertEquals(PaymentResult.Refused("Not enough funds"), result)
    }

    @Test
    fun malformedReturnFailsSoftly() {
        val result = PaymentResultParser.parse("taptoplay://adyen-return")

        assertTrue(result is PaymentResult.Failure)
    }
}
