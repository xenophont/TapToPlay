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
    fun extractsPaymentReceipts() {
        val response = paymentResponse(
            additionalResponse = "pspReference=PSP123",
            paymentReceipt = """
                "PaymentReceipt": [
                  {
                    "DocumentQualifier": "CustomerReceipt",
                    "OutputContent": {
                      "OutputFormat": "Text",
                      "OutputText": [
                        {"Text": "TapToPlay", "Alignment": "Centred", "CharacterStyle": "Bold"},
                        {"Text": "Total%3A+EUR+12.00"}
                      ]
                    }
                  },
                  {
                    "DocumentQualifier": "CashierReceipt",
                    "RequiredSignatureFlag": true,
                    "OutputContent": {
                      "OutputFormat": "Text",
                      "OutputText": [
                        {"Text": "Merchant copy"}
                      ]
                    }
                  }
                ],
            """.trimIndent(),
        )

        val receipts = TerminalApiResponseInspector.inspect(response)!!.receipts

        assertEquals(2, receipts.size)
        assertEquals("CustomerReceipt", receipts[0].documentQualifier)
        assertEquals("TapToPlay", receipts[0].lines[0].text)
        assertEquals("Total: EUR 12.00", receipts[0].lines[1].text)
        assertEquals("CashierReceipt", receipts[1].documentQualifier)
        assertEquals(true, receipts[1].requiredSignature)
    }

    @Test
    fun ignoresMissingTerminalResponse() {
        assertNull(TerminalApiResponseInspector.inspect("""{"hello":"world"}"""))
    }

    private fun paymentResponse(additionalResponse: String, paymentReceipt: String = ""): String = """
        {
          "SaleToPOIResponse": {
            "PaymentResponse": {
              $paymentReceipt
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
