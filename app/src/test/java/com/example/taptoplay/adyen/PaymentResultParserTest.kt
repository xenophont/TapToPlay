package com.example.taptoplay.adyen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentResultParserTest {
    @Test
    fun parsesBoardingReturn() {
        val result = PaymentResultParser.parse("taptoplay://adyen-return?boarded=false&installationId=abc&boardingRequestToken=req")

        assertEquals(
            PaymentResult.BoardingStatus(
                boarded = false,
                installationId = "abc",
                boardingRequestToken = "req",
                error = null,
                data = null,
            ),
            result,
        )
    }

    @Test
    fun parsesAlreadyBoardedReturn() {
        val result = PaymentResultParser.parse("taptoplay://adyen-return?boarded=true&installationId=abc")

        assertEquals(
            PaymentResult.BoardingStatus(
                boarded = true,
                installationId = "abc",
                boardingRequestToken = null,
                error = null,
                data = null,
            ),
            result,
        )
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
