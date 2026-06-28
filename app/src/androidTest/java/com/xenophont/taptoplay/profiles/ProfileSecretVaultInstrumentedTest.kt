package com.xenophont.taptoplay.profiles

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileSecretVaultInstrumentedTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun keystoreVaultRoundTripsAndRemovesSecrets() {
        val vault = AndroidProfileSecretVault(context)
        val profileId = "instrumented:vault"
        vault.remove(profileId)
        ProfileSecrets.fromStrings("api", "client", "passphrase").use {
            vault.put(profileId, it)
        }

        assertEquals("api", vault.withSecrets(profileId) { it.apiKeyString() })
        assertTrue(vault.contains(profileId))

        vault.remove(profileId)
        assertFalse(vault.contains(profileId))
    }

    @Test
    @Suppress("DEPRECATION")
    fun legacyEncryptedProfileMigratesToMetadataAndKeystoreVault() {
        val prefs = EncryptedSharedPreferences.create(
            context,
            "adyen_profiles",
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
        prefs.edit().clear().commit()
        context.getSharedPreferences("adyen_profile_secret_vault", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        val legacyJson = """
            [{
              "schema":"taptoplay.adyen.profile.v1",
              "displayName":"Legacy",
              "environment":"test",
              "merchantId":"merchant",
              "apiKey":"legacy-api",
              "clientKey":"legacy-client",
              "terminalKeyIdentifier":"key",
              "terminalKeyVersion":1,
              "terminalPassphrase":"legacy-pass",
              "currency":"EUR",
              "countryCode":"ES"
            }]
        """.trimIndent()
        prefs.edit().putString("profiles", legacyJson).commit()

        val repository = AndroidProfileStore(context)
        val profile = repository.profiles().single()

        assertEquals("Legacy", profile.displayName)
        assertEquals("legacy-api", repository.withSecrets(profile.id) { it.apiKeyString() })
        assertFalse(prefs.contains("profiles"))
        assertTrue(prefs.contains("profiles_v2"))

        repository.remove(profile.id)
        prefs.edit().clear().commit()
    }
}
