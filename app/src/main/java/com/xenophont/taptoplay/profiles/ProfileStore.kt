package com.xenophont.taptoplay.profiles

interface ProfileSecretVault {
    fun put(profileId: String, secrets: ProfileSecrets)
    fun <T> withSecrets(profileId: String, block: (ProfileSecrets) -> T): T?
    fun contains(profileId: String): Boolean
    fun remove(profileId: String)
}

interface ProfileRepository {
    fun profiles(): List<AdyenProfile>
    fun activeProfileId(): String?
    fun save(profile: AdyenProfile, secrets: ProfileSecrets? = null)
    fun setActive(profileId: String)
    fun remove(profileId: String)
    fun <T> withSecrets(profileId: String, block: (ProfileSecrets) -> T): T?
}
