package com.xenophont.taptoplay.profiles

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Decodes the public `taptoplay.adyen.profile.v1` QR schema into metadata plus short-lived
 * credentials. kotlinx.serialization and scanner APIs necessarily create temporary Strings; the
 * returned metadata does not retain those secret values.
 */
class ProfileQrParser(
    private val json: Json = Json {
        ignoreUnknownKeys = false
        explicitNulls = false
    },
) {
    fun parse(payload: String): Result<ImportedAdyenProfile> = runCatching {
        require(payload.length <= MAX_PAYLOAD_CHARS) { "QR credential payload is too large" }
        val wire = json.decodeFromString<AdyenProfileQrPayload>(payload)
        validate(wire)
        wire.toImportedProfile()
    }.recoverCatching { error ->
        throw IllegalArgumentException(error.message ?: "Invalid QR credential payload", error)
    }

    fun encode(imported: ImportedAdyenProfile): String {
        val profile = imported.profile
        val secrets = imported.secrets
        val passphrase = secrets.terminalPassphraseCopy()
        return try {
            json.encodeToString(
                AdyenProfileQrPayload.serializer(),
                AdyenProfileQrPayload(
                    schema = profile.schema,
                    displayName = profile.displayName,
                    environment = profile.environment,
                    merchantId = profile.merchantId,
                    storeId = profile.storeId,
                    storeName = profile.storeName,
                    apiKey = secrets.apiKeyString(),
                    clientKey = secrets.clientKeyString(),
                    terminalKeyIdentifier = profile.terminalKeyIdentifier,
                    terminalKeyVersion = profile.terminalKeyVersion,
                    terminalPassphrase = String(passphrase),
                    currency = profile.currency,
                    countryCode = profile.countryCode,
                ),
            )
        } finally {
            passphrase.fill('\u0000')
        }
    }

    private fun validate(profile: AdyenProfileQrPayload) {
        require(profile.schema == AdyenProfile.SCHEMA) { "Unsupported schema: ${profile.schema}" }
        require(profile.displayName.isNotBlank()) { "displayName is required" }
        require(profile.displayName.length <= MAX_DISPLAY_NAME_CHARS) { "displayName is too long" }
        require(profile.merchantId.isNotBlank()) { "merchantId is required" }
        require(profile.merchantId.length <= MAX_ID_CHARS) { "merchantId is too long" }
        require(profile.storeId == null || profile.storeId.length <= MAX_ID_CHARS) { "storeId is too long" }
        require(profile.storeName == null || !profile.storeId.isNullOrBlank()) { "storeName requires storeId" }
        require(profile.storeName == null || profile.storeName.length <= MAX_RESOLVED_NAME_CHARS) { "storeName is too long" }
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

    private fun AdyenProfileQrPayload.toImportedProfile() = ImportedAdyenProfile(
        profile = AdyenProfile(
            schema = schema,
            displayName = displayName,
            environment = environment,
            merchantId = merchantId,
            storeId = storeId,
            storeName = storeName,
            terminalKeyIdentifier = terminalKeyIdentifier,
            terminalKeyVersion = terminalKeyVersion,
            currency = currency,
            countryCode = countryCode,
        ),
        secrets = ProfileSecrets.fromStrings(apiKey, clientKey, terminalPassphrase),
    )

    private companion object {
        const val MAX_PAYLOAD_CHARS = 8_192
        const val MAX_DISPLAY_NAME_CHARS = 80
        const val MAX_RESOLVED_NAME_CHARS = 300
        const val MAX_ID_CHARS = 128
        const val MAX_SECRET_CHARS = 512
    }
}

@Serializable
internal data class AdyenProfileQrPayload(
    val schema: String = AdyenProfile.SCHEMA,
    val displayName: String,
    val environment: PaymentEnvironment,
    val merchantId: String,
    val storeId: String? = null,
    val storeName: String? = null,
    val apiKey: String,
    val clientKey: String,
    val terminalKeyIdentifier: String,
    val terminalKeyVersion: Int,
    val terminalPassphrase: String,
    val currency: String,
    val countryCode: String,
)
