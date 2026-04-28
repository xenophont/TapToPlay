package com.example.taptoplay.adyen

import com.example.taptoplay.profiles.AdyenProfile
import com.example.taptoplay.profiles.PaymentEnvironment
import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NexoCryptoTest {
    private val crypto = NexoCrypto()
    private val profile = AdyenProfile(
        displayName = "Demo",
        environment = PaymentEnvironment.TEST,
        merchantId = "merchant",
        apiKey = "api",
        clientKey = "client",
        terminalKeyIdentifier = "key-123",
        terminalKeyVersion = 7,
        terminalPassphrase = "shared-key-passphrase",
        currency = "EUR",
        countryCode = "ES",
    )
    private val requestJson = """
        {
          "SaleToPOIRequest": {
            "MessageHeader": {
              "ProtocolVersion": "3.0",
              "MessageClass": "Service",
              "MessageCategory": "Payment",
              "MessageType": "Request",
              "ServiceID": "123456",
              "SaleID": "TapToPlay",
              "POIID": "installation-1"
            },
            "PaymentRequest": {
              "SaleData": {
                "SaleTransactionID": {
                  "TransactionID": "txn-1",
                  "TimeStamp": "2026-04-28T12:00:00Z"
                }
              },
              "PaymentTransaction": {
                "AmountsReq": {
                  "Currency": "EUR",
                  "RequestedAmount": 12.95
                }
              }
            }
          }
        }
    """.trimIndent()

    @Test
    fun encryptedEnvelopeContainsNexoBlobAndSecurityTrailer() {
        val encrypted = crypto.encrypt(profile, requestJson, ByteArray(16) { it.toByte() })
        val saleToPoi = Json.parseToJsonElement(encrypted).jsonObject["SaleToPOIRequest"]!!.jsonObject

        assertTrue(saleToPoi.containsKey("MessageHeader"))
        assertTrue(saleToPoi.containsKey("NexoBlob"))
        assertTrue(saleToPoi.containsKey("SecurityTrailer"))
        assertNotEquals(requestJson, encrypted)
        assertEquals(requestJson, crypto.decryptForTest(profile, encrypted))
    }

    @Test
    fun encryptedRequestForAppLinkIsBase64UrlEnvelope() {
        val encoded = crypto.encryptToBase64Url(profile, requestJson)
        val decoded = Base64.getUrlDecoder().decode(encoded).toString(Charsets.UTF_8)

        assertTrue(decoded.contains("NexoBlob"))
        assertTrue(decoded.contains("SecurityTrailer"))
    }
}
