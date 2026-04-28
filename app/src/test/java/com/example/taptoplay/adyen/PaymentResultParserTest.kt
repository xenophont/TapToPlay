package com.example.taptoplay.adyen

import java.net.URLEncoder
import java.util.Base64
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
    fun parsesFullTerminalApiResponsePayload() {
        val responseJson = """
            {
              "SaleToPOIResponse": {
                "PaymentResponse": {
                  "Response": {
                    "Result": "Success",
                    "AdditionalResponse": "tid=123&pspReference=PSP123"
                  },
                  "POIData": {
                    "POITransactionID": {
                      "TransactionID": "PSP123"
                    }
                  }
                }
              }
            }
        """.trimIndent()
        val encoded = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(responseJson.toByteArray(Charsets.UTF_8))
        val result = PaymentResultParser.parse("taptoplay://adyen-return?response=${encoded.urlEncode()}")

        assertEquals(PaymentResult.Success("PSP123", "Success", responseJson), result)
    }

    @Test
    fun parsesFullTerminalApiRefusalPayload() {
        val responseJson = """
            {
              "SaleToPOIResponse": {
                "PaymentResponse": {
                  "Response": {
                    "Result": "Failure",
                    "ErrorCondition": "Refusal",
                    "AdditionalResponse": "refusalReasonRaw=Not enough funds"
                  }
                }
              }
            }
        """.trimIndent()
        val encoded = Base64.getEncoder().encodeToString(responseJson.toByteArray(Charsets.UTF_8))
        val result = PaymentResultParser.parse("taptoplay://adyen-return?response=${encoded.urlEncode()}")

        assertEquals(PaymentResult.Refused("Refusal | refusalReasonRaw=Not enough funds", responseJson), result)
    }

    @Test
    fun malformedReturnFailsSoftly() {
        val result = PaymentResultParser.parse("taptoplay://adyen-return")

        assertTrue(result is PaymentResult.Failure)
    }

    private fun String.urlEncode(): String = URLEncoder.encode(this, Charsets.UTF_8.name())
}
