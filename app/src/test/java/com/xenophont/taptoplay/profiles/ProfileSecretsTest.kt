package com.xenophont.taptoplay.profiles

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileSecretsTest {
    @Test
    fun binaryCodecRoundTripsAllSecretFields() {
        val original = ProfileSecrets.fromStrings("api-value", "client-value", "passphrase-value")
        val encoded = SecretBinaryCodec.encode(original)
        val decoded = SecretBinaryCodec.decode(encoded)
        try {
            assertEquals("api-value", decoded.apiKeyString())
            assertEquals("client-value", decoded.clientKeyString())
            assertEquals("passphrase-value", String(decoded.terminalPassphraseCopy()))
        } finally {
            original.close()
            decoded.close()
            encoded.fill(0)
        }
    }

    @Test
    fun vaultClearsPlaintextAfterExceptionalUseAndIsolatesProfiles() {
        val vault = TestProfileSecretVault()
        ProfileSecrets.fromStrings("api-one", "client-one", "pass-one").use {
            vault.put("one", it)
        }
        ProfileSecrets.fromStrings("api-two", "client-two", "pass-two").use {
            vault.put("two", it)
        }
        var exposed: ProfileSecrets? = null

        assertThrows(IllegalStateException::class.java) {
            vault.withSecrets("one") {
                exposed = it
                assertEquals("api-one", it.apiKeyString())
                error("stop")
            }
        }

        assertTrue(exposed!!.isClearedForTest())
        assertEquals("api-two", vault.withSecrets("two") { it.apiKeyString() })
    }

    @Test
    fun serializedProfileMetadataContainsNoCredentials() {
        val profile = AdyenProfile(
            displayName = "Demo",
            environment = PaymentEnvironment.TEST,
            merchantId = "merchant",
            terminalKeyIdentifier = "key",
            terminalKeyVersion = 1,
            currency = "EUR",
            countryCode = "ES",
        )

        val encoded = Json.encodeToString(AdyenProfile.serializer(), profile)

        assertFalse(encoded.contains("apiKey"))
        assertFalse(encoded.contains("clientKey"))
        assertFalse(encoded.contains("terminalPassphrase"))
    }
}

private class TestProfileSecretVault : ProfileSecretVault {
    private val records = mutableMapOf<String, ByteArray>()

    override fun put(profileId: String, secrets: ProfileSecrets) {
        records.remove(profileId)?.fill(0)
        records[profileId] = SecretBinaryCodec.encode(secrets)
    }

    override fun <T> withSecrets(profileId: String, block: (ProfileSecrets) -> T): T? {
        val encoded = records[profileId] ?: return null
        val secrets = SecretBinaryCodec.decode(encoded)
        return try {
            block(secrets)
        } finally {
            secrets.close()
        }
    }

    override fun contains(profileId: String): Boolean = records.containsKey(profileId)

    override fun remove(profileId: String) {
        records.remove(profileId)?.fill(0)
    }
}
