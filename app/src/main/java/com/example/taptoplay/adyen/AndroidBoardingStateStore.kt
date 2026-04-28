package com.example.taptoplay.adyen

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class AndroidBoardingStateStore(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "adyen_boarding_state",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun installationId(profileId: String): String? =
        prefs.getString(installationKey(profileId), null)

    fun boardingRequestToken(profileId: String): String? =
        prefs.getString(tokenKey(profileId), null)

    fun saveInstallationId(profileId: String, installationId: String) {
        prefs.edit()
            .putString(installationKey(profileId), installationId)
            .remove(tokenKey(profileId))
            .apply()
    }

    fun saveBoardingRequestToken(profileId: String, token: String) {
        prefs.edit().putString(tokenKey(profileId), token).apply()
    }

    fun clear(profileId: String) {
        prefs.edit()
            .remove(installationKey(profileId))
            .remove(tokenKey(profileId))
            .apply()
    }

    private fun installationKey(profileId: String): String = "installation:$profileId"
    private fun tokenKey(profileId: String): String = "boardingToken:$profileId"
}
