package com.example.taptoplay.profiles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileQrParserTest {
    private val parser = ProfileQrParser()

    @Test
    fun parsesValidTestProfile() {
        val profile = parser.parse(validPayload()).getOrThrow()

        assertEquals("Demo Store TEST", profile.displayName)
        assertEquals(PaymentEnvironment.TEST, profile.environment)
        assertEquals("ST322LJ223223K5F", profile.storeId)
        assertEquals("EUR", profile.currency)
    }

    @Test
    fun rejectsUnknownSchema() {
        val payload = validPayload().replace("taptoplay.adyen.profile.v1", "other.schema")

        val result = parser.parse(payload)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Unsupported schema") == true)
    }

    @Test
    fun rejectsInvalidEnvironment() {
        val payload = validPayload().replace("\"test\"", "\"sandbox\"")

        val result = parser.parse(payload)

        assertTrue(result.isFailure)
    }

    private fun validPayload(): String = """
        {
          "schema": "taptoplay.adyen.profile.v1",
          "displayName": "Demo Store TEST",
          "environment": "test",
          "merchantId": "YourMerchantAccount",
          "storeId": "ST322LJ223223K5F",
          "apiKey": "AQE-demo",
          "clientKey": "test_client",
          "terminalKeyIdentifier": "CryptoKeyIdentifier",
          "terminalKeyVersion": 1,
          "terminalPassphrase": "shared-key-passphrase",
          "currency": "EUR",
          "countryCode": "ES"
        }
    """.trimIndent()
}
