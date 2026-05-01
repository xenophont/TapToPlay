package com.xenophont.taptoplay.profiles

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class AndroidProfileStore(context: Context) : ProfileStore {
    private val json = Json { ignoreUnknownKeys = true }
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "adyen_profiles",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override fun profiles(): List<AdyenProfile> {
        val raw = prefs.getString(KEY_PROFILES, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(AdyenProfile.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    override fun activeProfileId(): String? = prefs.getString(KEY_ACTIVE, null)

    override fun save(profile: AdyenProfile) {
        val updated = (profiles().filterNot { it.id == profile.id } + profile)
            .sortedWith(compareBy<AdyenProfile> { it.environment.name }.thenBy { it.profileName })
        prefs.edit()
            .putString(KEY_PROFILES, json.encodeToString(ListSerializer(AdyenProfile.serializer()), updated))
            .apply()
    }

    override fun setActive(profileId: String) {
        require(profiles().any { it.id == profileId }) { "Cannot activate an unknown profile" }
        prefs.edit().putString(KEY_ACTIVE, profileId).apply()
    }

    override fun remove(profileId: String) {
        val remaining = profiles().filterNot { it.id == profileId }
        prefs.edit()
            .putString(KEY_PROFILES, json.encodeToString(ListSerializer(AdyenProfile.serializer()), remaining))
            .apply {
                if (activeProfileId() == profileId) {
                    remaining.firstOrNull()?.let { putString(KEY_ACTIVE, it.id) } ?: remove(KEY_ACTIVE)
                }
            }
            .apply()
    }

    companion object {
        private const val KEY_PROFILES = "profiles"
        private const val KEY_ACTIVE = "active"
    }
}
