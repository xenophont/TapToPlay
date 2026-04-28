package com.example.taptoplay.profiles

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

    private fun profile(name: String, environment: PaymentEnvironment) = AdyenProfile(
        displayName = name,
        environment = environment,
        merchantId = "merchant",
        apiKey = "api",
        clientKey = "client",
        terminalKeyIdentifier = "key",
        terminalKeyVersion = 1,
        terminalPassphrase = "passphrase",
        currency = "EUR",
        countryCode = "ES",
    )
}

private class MemoryProfileStore : ProfileStore {
    private val profiles = mutableListOf<AdyenProfile>()
    private var active: String? = null

    override fun profiles(): List<AdyenProfile> = profiles
    override fun activeProfileId(): String? = active
    override fun save(profile: AdyenProfile) {
        profiles.removeAll { it.id == profile.id }
        profiles.add(profile)
    }

    override fun setActive(profileId: String) {
        require(profiles.any { it.id == profileId })
        active = profileId
    }
}
