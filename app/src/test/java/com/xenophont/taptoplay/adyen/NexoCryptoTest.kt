package com.xenophont.taptoplay.adyen

import com.xenophont.taptoplay.profiles.AdyenProfile
import com.xenophont.taptoplay.profiles.PaymentEnvironment
import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class NexoCryptoTest {
    private val crypto = NexoCrypto()
    private val profile = AdyenProfile(
        displayName = "Demo",
        environment = PaymentEnvironment.TEST,
        merchantId = "merchant",
        terminalKeyIdentifier = "key-123",
        terminalKeyVersion = 7,
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

    private fun passphrase() = "shared-key-passphrase".toCharArray()

    @Test
    fun encryptedEnvelopeContainsNexoBlobAndSecurityTrailer() {
        val encrypted = crypto.encrypt(profile, passphrase(), requestJson.encodeToByteArray(), ByteArray(16) { it.toByte() })
        val saleToPoi = Json.parseToJsonElement(encrypted).jsonObject["SaleToPOIRequest"]!!.jsonObject

        assertTrue(saleToPoi.containsKey("MessageHeader"))
        assertTrue(saleToPoi.containsKey("NexoBlob"))
        assertTrue(saleToPoi.containsKey("SecurityTrailer"))
        assertNotEquals(requestJson, encrypted)
        val plaintext = crypto.decryptForTest(profile, passphrase(), encrypted)
        try {
            assertEquals(requestJson, plaintext.decodeToString())
        } finally {
            plaintext.fill(0)
        }
    }

    @Test
    fun encryptedRequestForAppLinkIsBase64UrlEnvelopeAndInputIsWiped() {
        val input = requestJson.encodeToByteArray()
        val password = passphrase()
        val encoded = crypto.encryptToBase64Url(profile, password, input)
        val decoded = Base64.getUrlDecoder().decode(encoded).toString(Charsets.UTF_8)

        assertTrue(decoded.contains("NexoBlob"))
        assertTrue(decoded.contains("SecurityTrailer"))
        assertTrue(input.all { it == 0.toByte() })
        assertTrue(password.all { it == '\u0000' })
    }

    @Test
    fun rejectsTamperedHmac() {
        val encrypted = crypto.encrypt(profile, passphrase(), requestJson.encodeToByteArray(), ByteArray(16) { it.toByte() })
        val hmac = Json.parseToJsonElement(encrypted)
            .jsonObject["SaleToPOIRequest"]!!.jsonObject["SecurityTrailer"]!!.jsonObject["Hmac"]
            .toString()
            .trim('"')
        val replacement = (if (hmac.first() == 'A') "B" else "A") + hmac.drop(1)
        val tampered = encrypted.replace(hmac, replacement)

        assertThrows(IllegalArgumentException::class.java) {
            crypto.decryptForTest(profile, passphrase(), tampered)
        }
    }

    @Test
    fun rejectsOuterHeaderThatDoesNotMatchAuthenticatedPlaintext() {
        val encrypted = crypto.encrypt(profile, passphrase(), requestJson.encodeToByteArray(), ByteArray(16) { it.toByte() })
        val tampered = encrypted.replaceFirst("\"ServiceID\":\"123456\"", "\"ServiceID\":\"654321\"")

        assertThrows(IllegalArgumentException::class.java) {
            crypto.decryptForTest(profile, passphrase(), tampered)
        }
    }

    @Test
    fun rejectsUnexpectedKeyMetadata() {
        val encrypted = crypto.encrypt(profile, passphrase(), requestJson.encodeToByteArray(), ByteArray(16) { it.toByte() })
        val tampered = encrypted.replace("\"KeyVersion\":7", "\"KeyVersion\":8")

        assertThrows(IllegalArgumentException::class.java) {
            crypto.decryptForTest(profile, passphrase(), tampered)
        }
    }

    @Test
    fun authenticatesResponseEnvelopeAndMatchingHeaders() {
        val responseJson = requestJson
            .replace("SaleToPOIRequest", "SaleToPOIResponse")
            .replace("\"MessageType\": \"Request\"", "\"MessageType\": \"Response\"")
        val encrypted = crypto.encrypt(
            profile,
            passphrase(),
            responseJson.encodeToByteArray(),
            ByteArray(16) { it.toByte() },
        )

        assertTrue(encrypted.contains("\"SaleToPOIResponse\""))
        val plaintext = crypto.decryptForTest(profile, passphrase(), encrypted)
        try {
            assertEquals(responseJson, plaintext.decodeToString())
        } finally {
            plaintext.fill(0)
        }
    }

    @Test
    fun matchesIndependentNexoCipherAndHmacVector() {
        val payload = """{"SaleToPOIRequest":{"MessageHeader":{"ServiceID":"1"}}}"""
        val encrypted = crypto.encrypt(
            profile,
            passphrase(),
            payload.encodeToByteArray(),
            ByteArray(16) { it.toByte() },
        )
        val trailer = Json.parseToJsonElement(encrypted)
            .jsonObject["SaleToPOIRequest"]!!.jsonObject

        assertEquals(
            "t0q9ZDI07heBaBGUmkx1QtHSGTO+Lasyz8WX+Swwlx9SmvH0rNio6xTtCxTeAkDA1A3zR9ja7wDHq/DcM/e53g==",
            trailer["NexoBlob"]!!.toString().trim('"'),
        )
        assertEquals(
            "rgZG9UopHkX9+2HmmPtWLSJ128AdrZJEjrNuupIfEpA=",
            trailer["SecurityTrailer"]!!.jsonObject["Hmac"]!!.toString().trim('"'),
        )
    }
}
