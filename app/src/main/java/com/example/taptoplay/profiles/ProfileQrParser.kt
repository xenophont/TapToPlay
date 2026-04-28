package com.example.taptoplay.profiles

import kotlinx.serialization.json.Json

class ProfileQrParser(
    private val json: Json = Json {
        ignoreUnknownKeys = false
        explicitNulls = false
    },
) {
    fun parse(payload: String): Result<AdyenProfile> = runCatching {
        val profile = json.decodeFromString<AdyenProfile>(payload)
        validate(profile)
        profile
    }.recoverCatching { error ->
        throw IllegalArgumentException(error.message ?: "Invalid QR credential payload", error)
    }

    fun encode(profile: AdyenProfile): String = json.encodeToString(AdyenProfile.serializer(), profile)

    private fun validate(profile: AdyenProfile) {
        require(profile.schema == AdyenProfile.SCHEMA) { "Unsupported schema: ${profile.schema}" }
        require(profile.displayName.isNotBlank()) { "displayName is required" }
        require(profile.merchantId.isNotBlank()) { "merchantId is required" }
        require(profile.apiKey.isNotBlank()) { "apiKey is required" }
        require(profile.clientKey.isNotBlank()) { "clientKey is required" }
        require(profile.terminalKeyIdentifier.isNotBlank()) { "terminalKeyIdentifier is required" }
        require(profile.terminalKeyVersion > 0) { "terminalKeyVersion must be greater than 0" }
        require(profile.terminalPassphrase.isNotBlank()) { "terminalPassphrase is required" }
        require(profile.currency.matches(Regex("[A-Z]{3}"))) { "currency must be ISO 4217 uppercase code" }
        require(profile.countryCode.matches(Regex("[A-Z]{2}"))) { "countryCode must be ISO 3166-1 alpha-2 uppercase code" }
    }
}
