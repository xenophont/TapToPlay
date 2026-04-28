package com.example.taptoplay.profiles

import com.example.taptoplay.BuildConfig

object LocalProfileBootstrap {
    fun profileOrNull(): AdyenProfile? {
        if (BuildConfig.ADYEN_MERCHANT_ID.isBlank() || BuildConfig.ADYEN_API_KEY.isBlank()) return null
        val environment = if (BuildConfig.ADYEN_ENVIRONMENT.equals("live", ignoreCase = true)) {
            PaymentEnvironment.LIVE
        } else {
            PaymentEnvironment.TEST
        }
        return AdyenProfile(
            displayName = BuildConfig.ADYEN_PROFILE_NAME.ifBlank { "local.properties ${environment.name.lowercase()}" },
            environment = environment,
            merchantId = BuildConfig.ADYEN_MERCHANT_ID,
            storeId = BuildConfig.ADYEN_STORE_ID.ifBlank { null },
            apiKey = BuildConfig.ADYEN_API_KEY,
            clientKey = BuildConfig.ADYEN_CLIENT_KEY,
            terminalKeyIdentifier = BuildConfig.ADYEN_TERMINAL_KEY_IDENTIFIER,
            terminalKeyVersion = BuildConfig.ADYEN_TERMINAL_KEY_VERSION.toIntOrNull() ?: 1,
            terminalPassphrase = BuildConfig.ADYEN_TERMINAL_PASSPHRASE,
            currency = BuildConfig.ADYEN_CURRENCY.ifBlank { "EUR" },
            countryCode = BuildConfig.ADYEN_COUNTRY_CODE.ifBlank { "ES" },
        )
    }
}
