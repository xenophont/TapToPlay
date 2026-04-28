package com.example.taptoplay.adyen

import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TerminalApiResponseInspectorTest {
    @Test
    fun decodesBase64JsonAdditionalResponse() {
        val additionalJson = """{"pspReference":"PSP123","metadata":{"order":"A1"}}"""
        val encoded = Base64.getEncoder().encodeToString(additionalJson.toByteArray(Charsets.UTF_8))
        val response = paymentResponse(additionalResponse = encoded)

        val insight = TerminalApiResponseInspector.inspect(response)!!

        assertEquals("Payment", insight.category)
        assertEquals("Success", insight.result)
        assertEquals("PSP123", TerminalApiResponseInspector.importantAdditional("pspReference", insight))
        assertEquals("A1", TerminalApiResponseInspector.importantAdditional("order", insight))
    }

    @Test
    fun decodesBase64ValuesInsideFormAdditionalResponse() {
        val encodedValue = Base64.getEncoder().encodeToString("decoded value".toByteArray(Charsets.UTF_8))
        val response = paymentResponse(additionalResponse = "pspReference=PSP123&custom=$encodedValue")

        val insight = TerminalApiResponseInspector.inspect(response)!!
        val custom = insight.additionalResponseFields.first { it.name == "custom" }

        assertEquals("decoded value", custom.decodedValue)
        assertEquals("PSP123", TerminalApiResponseInspector.compactSummary(response)["PSP reference"])
    }

    @Test
    fun ignoresMissingTerminalResponse() {
        assertNull(TerminalApiResponseInspector.inspect("""{"hello":"world"}"""))
    }

    private fun paymentResponse(additionalResponse: String): String = """
        {
          "SaleToPOIResponse": {
            "PaymentResponse": {
              "Response": {
                "Result": "Success",
                "AdditionalResponse": "$additionalResponse"
              },
              "POIData": {
                "POITransactionID": {
                  "TransactionID": "tender.PSP123"
                }
              }
            }
          }
        }
    """.trimIndent()
}
