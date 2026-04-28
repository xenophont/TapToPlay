package com.example.taptoplay.adyen

import com.example.taptoplay.profiles.AdyenProfile
import com.example.taptoplay.profiles.PaymentEnvironment
import java.net.URLEncoder
import java.util.Base64

object AdyenLinks {
    private const val RETURN_URL = "taptoplay://adyen-return"

    fun boarded(profile: AdyenProfile): String = withParams(
        base(profile.environment, "boarded"),
        "returnUrl" to RETURN_URL,
    )

    fun startReboard(profile: AdyenProfile): String = withParams(
        base(profile.environment, "boarded"),
        "returnUrl" to RETURN_URL,
        "reboard" to "true",
    )

    fun board(profile: AdyenProfile, boardingToken: String): String = withParams(
        base(profile.environment, "board"),
        "returnUrl" to RETURN_URL,
        "boardingToken" to encodeBase64Url(boardingToken),
    )

    fun nexo(profile: AdyenProfile, encodedRequest: String): String = withParams(
        base(profile.environment, "nexo"),
        "returnUrl" to RETURN_URL,
        "request" to encodedRequest,
    )

    fun encodeDemoNexoRequest(requestJson: String): String =
        encodeBase64Url(requestJson)

    private fun base(environment: PaymentEnvironment, action: String): String = when (environment) {
        PaymentEnvironment.TEST -> "https://www.adyen.com/test/$action"
        PaymentEnvironment.LIVE -> "https://www.adyen.com/$action"
    }

    private fun withParams(base: String, vararg params: Pair<String, String>): String =
        base + params.joinToString(prefix = "?", separator = "&") { (key, value) ->
            "${key.encode()}=${value.encode()}"
        }

    private fun encodeBase64Url(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))

    private fun String.encode(): String = URLEncoder.encode(this, Charsets.UTF_8.name())
}
