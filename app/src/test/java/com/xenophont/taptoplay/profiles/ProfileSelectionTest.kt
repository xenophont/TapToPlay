package com.xenophont.taptoplay.profiles

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileSelectionTest {
    @Test
    fun activeProfileSwitchesOnlyWhenRequested() {
        val store = MemoryProfileStore()
        val testProfile = profile("Test", PaymentEnvironment.TEST)
        val liveProfile = profile("Live", PaymentEnvironment.LIVE)

        store.save(testProfile)
        store.save(liveProfile)
        store.setActive(testProfile.id)
        store.setActive(liveProfile.id)

        assertEquals(liveProfile.id, store.activeProfileId())
    }

    @Test
    fun removingActiveProfileSelectsNextAvailableProfile() {
        val store = MemoryProfileStore()
        val testProfile = profile("Test", PaymentEnvironment.TEST)
        val liveProfile = profile("Live", PaymentEnvironment.LIVE)

        store.save(testProfile)
        store.save(liveProfile)
        store.setActive(testProfile.id)
        store.remove(testProfile.id)

        assertEquals(liveProfile.id, store.activeProfileId())
        assertEquals(listOf(liveProfile), store.profiles())
    }

    @Test
    fun onlyLiveProfilesRequireChargeConfirmation() {
        assertEquals(false, profile("Test", PaymentEnvironment.TEST).requiresLivePaymentConfirmation())
        assertEquals(true, profile("Live", PaymentEnvironment.LIVE).requiresLivePaymentConfirmation())
    }

    @Test
    fun displayNameStaysPrimaryProfileNameWithoutChangingProfileId() {
        val profile = profile("Demo Store TEST", PaymentEnvironment.TEST).copy(
            storeId = "ST322LJ223223K5F",
            storeName = "Boutique Centro",
        )

        assertEquals("Demo Store TEST", profile.profileName)
        assertEquals("test:merchant:ST322LJ223223K5F:Demo Store TEST", profile.id)
    }

    @Test
    fun merchantScopedProfileUsesDisplayNameAsProfileName() {
        val profile = profile("Demo Store TEST", PaymentEnvironment.TEST)

        assertEquals("Demo Store TEST", profile.profileName)
        assertEquals("test:merchant::Demo Store TEST", profile.id)
    }

    private fun profile(name: String, environment: PaymentEnvironment) = AdyenProfile(
        displayName = name,
        environment = environment,
        merchantId = "merchant",
        terminalKeyIdentifier = "key",
        terminalKeyVersion = 1,
        currency = "EUR",
        countryCode = "ES",
    )
}

private class MemoryProfileStore : ProfileRepository {
    private val profiles = mutableListOf<AdyenProfile>()
    private val secrets = mutableMapOf<String, ProfileSecrets>()
    private var active: String? = null

    override fun profiles(): List<AdyenProfile> = profiles
    override fun activeProfileId(): String? = active
    override fun save(profile: AdyenProfile, secrets: ProfileSecrets?) {
        profiles.removeAll { it.id == profile.id }
        profiles.add(profile)
        secrets?.let {
            this.secrets.remove(profile.id)?.close()
            this.secrets[profile.id] = it.copy()
        }
    }

    override fun setActive(profileId: String) {
        require(profiles.any { it.id == profileId })
        active = profileId
    }

    override fun remove(profileId: String) {
        profiles.removeAll { it.id == profileId }
        secrets.remove(profileId)?.close()
        if (active == profileId) active = profiles.firstOrNull()?.id
    }

    override fun <T> withSecrets(profileId: String, block: (ProfileSecrets) -> T): T? =
        secrets[profileId]?.copy()?.use(block)
}
