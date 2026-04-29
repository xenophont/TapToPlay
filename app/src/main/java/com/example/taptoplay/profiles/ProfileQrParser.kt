package com.example.taptoplay.profiles

import kotlinx.serialization.json.Json

class ProfileQrParser(
    private val json: Json = Json {
        ignoreUnknownKeys = false
        explicitNulls = false
    },
) {
    fun parse(payload: String): Result<AdyenProfile> = runCatching {
        require(payload.length <= MAX_PAYLOAD_CHARS) { "QR credential payload is too large" }
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
        require(profile.displayName.length <= MAX_DISPLAY_NAME_CHARS) { "displayName is too long" }
        require(profile.merchantId.isNotBlank()) { "merchantId is required" }
        require(profile.merchantId.length <= MAX_ID_CHARS) { "merchantId is too long" }
        require(profile.storeId == null || profile.storeId.length <= MAX_ID_CHARS) { "storeId is too long" }
        require(profile.apiKey.isNotBlank()) { "apiKey is required" }
        require(profile.apiKey.length <= MAX_SECRET_CHARS) { "apiKey is too long" }
        require(profile.clientKey.isNotBlank()) { "clientKey is required" }
        require(profile.clientKey.length <= MAX_SECRET_CHARS) { "clientKey is too long" }
        require(profile.terminalKeyIdentifier.isNotBlank()) { "terminalKeyIdentifier is required" }
        require(profile.terminalKeyIdentifier.length <= MAX_ID_CHARS) { "terminalKeyIdentifier is too long" }
        require(profile.terminalKeyVersion > 0) { "terminalKeyVersion must be greater than 0" }
        require(profile.terminalPassphrase.isNotBlank()) { "terminalPassphrase is required" }
        require(profile.terminalPassphrase.length <= MAX_SECRET_CHARS) { "terminalPassphrase is too long" }
        require(profile.currency.matches(Regex("[A-Z]{3}"))) { "currency must be ISO 4217 uppercase code" }
        require(profile.countryCode.matches(Regex("[A-Z]{2}"))) { "countryCode must be ISO 3166-1 alpha-2 uppercase code" }
    }

    private companion object {
        const val MAX_PAYLOAD_CHARS = 8_192
        const val MAX_DISPLAY_NAME_CHARS = 80
        const val MAX_ID_CHARS = 128
        const val MAX_SECRET_CHARS = 512
    }
}
