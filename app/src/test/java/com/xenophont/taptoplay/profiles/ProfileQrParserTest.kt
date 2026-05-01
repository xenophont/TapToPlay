package com.xenophont.taptoplay.profiles

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
        assertEquals(null, profile.storeName)
        assertEquals("Demo Store TEST", profile.profileName)
        assertEquals("EUR", profile.currency)
    }

    @Test
    fun usesResolvedStoreNameAsProfileNameWhenPresent() {
        val payload = validPayload().replace(
            "\"storeId\": \"ST322LJ223223K5F\",",
            "\"storeId\": \"ST322LJ223223K5F\",\n          \"storeName\": \"Boutique Centro\",",
        )

        val profile = parser.parse(payload).getOrThrow()

        assertEquals("Boutique Centro", profile.storeName)
        assertEquals("Boutique Centro", profile.profileName)
    }

    @Test
    fun usesMerchantIdAsProfileNameForMerchantScopedProfiles() {
        val payload = validPayload().replace("\"storeId\": \"ST322LJ223223K5F\",", "")

        val profile = parser.parse(payload).getOrThrow()

        assertEquals(null, profile.storeId)
        assertEquals("YourMerchantAccount", profile.profileName)
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

    @Test
    fun rejectsOversizedCredentialPayloads() {
        val result = parser.parse("x".repeat(9_000))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("too large") == true)
    }

    @Test
    fun rejectsOversizedSecretFields() {
        val payload = validPayload().replace("AQE-demo", "a".repeat(600))

        val result = parser.parse(payload)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("apiKey is too long") == true)
    }

    @Test
    fun rejectsStoreNameWithoutStoreId() {
        val payload = validPayload()
            .replace(
                "\"storeId\": \"ST322LJ223223K5F\",",
                "\"storeName\": \"Boutique Centro\",",
            )

        val result = parser.parse(payload)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("storeName requires storeId") == true)
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
