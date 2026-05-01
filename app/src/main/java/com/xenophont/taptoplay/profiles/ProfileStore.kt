package com.xenophont.taptoplay.profiles

interface ProfileStore {
    fun profiles(): List<AdyenProfile>
    fun activeProfileId(): String?
    fun save(profile: AdyenProfile)
    fun setActive(profileId: String)
    fun remove(profileId: String)
}
