package com.example.taptoplay.adyen

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class SaleToAcquirerDataFavoriteStore(context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "adyen_sale_to_acquirer_favorites",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun favorites(): List<SaleToAcquirerDataConfig> {
        val raw = prefs.getString(KEY_FAVORITES, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(SaleToAcquirerDataConfig.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    fun save(config: SaleToAcquirerDataConfig) {
        val updated = (listOf(config) + favorites().filterNot { it.displayName == config.displayName })
            .take(MAX_FAVORITES)
        write(updated)
    }

    fun remove(displayName: String) {
        write(favorites().filterNot { it.displayName == displayName })
    }

    private fun write(favorites: List<SaleToAcquirerDataConfig>) {
        prefs.edit()
            .putString(KEY_FAVORITES, json.encodeToString(ListSerializer(SaleToAcquirerDataConfig.serializer()), favorites))
            .apply()
    }

    companion object {
        private const val KEY_FAVORITES = "favorites"
        private const val MAX_FAVORITES = 20
    }
}
