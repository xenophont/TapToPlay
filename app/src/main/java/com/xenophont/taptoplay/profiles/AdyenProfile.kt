package com.xenophont.taptoplay.profiles

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
    val terminalKeyIdentifier: String,
    val terminalKeyVersion: Int,
    val currency: String,
    val countryCode: String,
    val credentialsConfigured: Boolean = true,
) {
    val id: String
        get() = "${environment.name.lowercase()}:$merchantId:${storeId.orEmpty()}:$displayName"

    val profileName: String
        get() = displayName

    companion object {
        const val SCHEMA = "taptoplay.adyen.profile.v1"
    }
}

fun String.mask(): String = when {
    isBlank() -> "not set"
    length <= 8 -> "****"
    else -> take(4) + "..." + takeLast(4)
}

fun AdyenProfile.requiresLivePaymentConfirmation(): Boolean =
    environment == PaymentEnvironment.LIVE
