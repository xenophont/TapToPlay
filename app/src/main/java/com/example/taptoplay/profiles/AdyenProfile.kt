package com.example.taptoplay.profiles

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PaymentEnvironment {
    @SerialName("test")
    TEST,

    @SerialName("live")
    LIVE,
}

@Serializable
data class AdyenProfile(
    val schema: String = SCHEMA,
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
) {
    val id: String
        get() = "${environment.name.lowercase()}:$merchantId:${storeId.orEmpty()}:$displayName"

    val profileName: String
        get() = storeName?.takeIf { it.isNotBlank() }
            ?: merchantId.takeIf { storeId.isNullOrBlank() && it.isNotBlank() }
            ?: displayName

    fun maskedApiKey(): String = apiKey.mask()
    fun maskedPassphrase(): String = terminalPassphrase.maskPresence()

    companion object {
        const val SCHEMA = "taptoplay.adyen.profile.v1"
    }
}

fun String.mask(): String = when {
    isBlank() -> "not set"
    length <= 8 -> "****"
    else -> take(4) + "..." + takeLast(4)
}

private fun String.maskPresence(): String = if (isBlank()) "not set" else "set (hidden)"

fun AdyenProfile.requiresLivePaymentConfirmation(): Boolean =
    environment == PaymentEnvironment.LIVE
