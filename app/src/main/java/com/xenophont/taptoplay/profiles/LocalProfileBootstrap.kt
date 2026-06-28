package com.xenophont.taptoplay.profiles

import com.xenophont.taptoplay.BuildConfig

object LocalProfileBootstrap {
    fun profileOrNull(): ImportedAdyenProfile? {
        if (BuildConfig.ADYEN_MERCHANT_ID.isBlank() || BuildConfig.ADYEN_API_KEY.isBlank()) return null
        val environment = if (BuildConfig.ADYEN_ENVIRONMENT.equals("live", ignoreCase = true)) {
            PaymentEnvironment.LIVE
        } else {
            PaymentEnvironment.TEST
        }
        return ImportedAdyenProfile(
            profile = AdyenProfile(
                displayName = BuildConfig.ADYEN_PROFILE_NAME.ifBlank { "local.properties ${environment.name.lowercase()}" },
                environment = environment,
                merchantId = BuildConfig.ADYEN_MERCHANT_ID,
                storeId = BuildConfig.ADYEN_STORE_ID.ifBlank { null },
                terminalKeyIdentifier = BuildConfig.ADYEN_TERMINAL_KEY_IDENTIFIER,
                terminalKeyVersion = BuildConfig.ADYEN_TERMINAL_KEY_VERSION.toIntOrNull() ?: 1,
                currency = BuildConfig.ADYEN_CURRENCY.ifBlank { "EUR" },
                countryCode = BuildConfig.ADYEN_COUNTRY_CODE.ifBlank { "ES" },
            ),
            secrets = ProfileSecrets.fromStrings(
                BuildConfig.ADYEN_API_KEY,
                BuildConfig.ADYEN_CLIENT_KEY,
                BuildConfig.ADYEN_TERMINAL_PASSPHRASE,
            ),
        )
    }
}
