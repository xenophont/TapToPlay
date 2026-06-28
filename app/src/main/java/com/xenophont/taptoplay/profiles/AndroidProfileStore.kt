package com.xenophont.taptoplay.profiles

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class AndroidProfileStore(
    context: Context,
    private val secretVault: ProfileSecretVault = AndroidProfileSecretVault(context),
) : ProfileRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "adyen_profiles",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    init {
        migrateLegacyProfiles()
    }

    override fun profiles(): List<AdyenProfile> {
        val raw = prefs.getString(KEY_PROFILES_V2, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(AdyenProfile.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    override fun activeProfileId(): String? = prefs.getString(KEY_ACTIVE, null)

    /**
     * When [secrets] is supplied this method encrypts it in the Keystore-backed vault before
     * publishing the metadata. The caller remains responsible for closing its plaintext instance.
     */
    override fun save(profile: AdyenProfile, secrets: ProfileSecrets?) {
        secrets?.let { secretVault.put(profile.id, it) }
        require(secretVault.contains(profile.id)) { "Profile secrets are not available" }
        val updated = (profiles().filterNot { it.id == profile.id } + profile.copy(credentialsConfigured = true))
            .sortedWith(compareBy<AdyenProfile> { it.environment.name }.thenBy { it.profileName })
        writeProfiles(updated)
    }

    override fun setActive(profileId: String) {
        require(profiles().any { it.id == profileId }) { "Cannot activate an unknown profile" }
        check(prefs.edit().putString(KEY_ACTIVE, profileId).commit()) { "Could not activate profile" }
    }

    override fun remove(profileId: String) {
        val remaining = profiles().filterNot { it.id == profileId }
        val editor = prefs.edit().putString(
            KEY_PROFILES_V2,
            json.encodeToString(ListSerializer(AdyenProfile.serializer()), remaining),
        )
        if (activeProfileId() == profileId) {
            remaining.firstOrNull()?.let { editor.putString(KEY_ACTIVE, it.id) } ?: editor.remove(KEY_ACTIVE)
        }
        check(editor.commit()) { "Could not remove profile metadata" }
        secretVault.remove(profileId)
    }

    override fun <T> withSecrets(profileId: String, block: (ProfileSecrets) -> T): T? =
        secretVault.withSecrets(profileId, block)

    private fun migrateLegacyProfiles() {
        if (prefs.contains(KEY_PROFILES_V2)) return
        val raw = prefs.getString(KEY_LEGACY_PROFILES, null)
        if (raw == null) {
            writeProfiles(emptyList())
            return
        }
        val legacy = runCatching {
            json.decodeFromString(ListSerializer(LegacyAdyenProfile.serializer()), raw)
        }.getOrElse { return }
        val metadata = legacy.map { stored ->
            val profile = stored.toProfile()
            ProfileSecrets.fromStrings(stored.apiKey, stored.clientKey, stored.terminalPassphrase).use {
                secretVault.put(profile.id, it)
            }
            profile
        }
        val encoded = json.encodeToString(ListSerializer(AdyenProfile.serializer()), metadata)
        check(
            prefs.edit()
                .putString(KEY_PROFILES_V2, encoded)
                .remove(KEY_LEGACY_PROFILES)
                .commit(),
        ) { "Could not migrate encrypted profiles" }
    }

    private fun writeProfiles(profiles: List<AdyenProfile>) {
        val encoded = json.encodeToString(ListSerializer(AdyenProfile.serializer()), profiles)
        check(prefs.edit().putString(KEY_PROFILES_V2, encoded).commit()) {
            "Could not persist profile metadata"
        }
    }

    companion object {
        private const val KEY_LEGACY_PROFILES = "profiles"
        private const val KEY_PROFILES_V2 = "profiles_v2"
        private const val KEY_ACTIVE = "active"
    }
}

@Serializable
private data class LegacyAdyenProfile(
    val schema: String = AdyenProfile.SCHEMA,
    val displayName: String,
    val environment: PaymentEnvironment,
    val merchantId: String,
    val storeId: String? = null,
    val storeName: String? = null,
    val apiKey: String,
    val clientKey: String,
    val terminalKeyIdentifier: String,
    val terminalKeyVersion: Int,
    val terminalPassphrase: String,
    val currency: String,
    val countryCode: String,
) {
    fun toProfile() = AdyenProfile(
        schema = schema,
        displayName = displayName,
        environment = environment,
        merchantId = merchantId,
        storeId = storeId,
        storeName = storeName,
        terminalKeyIdentifier = terminalKeyIdentifier,
        terminalKeyVersion = terminalKeyVersion,
        currency = currency,
        countryCode = countryCode,
    )
}
