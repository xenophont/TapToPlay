package com.xenophont.taptoplay.adyen

import com.xenophont.taptoplay.profiles.AdyenProfile
import com.xenophont.taptoplay.profiles.PaymentEnvironment
import java.net.URLEncoder
import java.util.Base64

object AdyenLinks {
    private const val RETURN_URL = "taptoplay://adyen-return"
    private const val TEST_PAYMENTS_APP_PACKAGE = "com.adyen.ipp.mobile.companion.test"
    private const val LIVE_PAYMENTS_APP_PACKAGE = "com.adyen.ipp.mobile.companion.live"
    private const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id="

    /**
     * Boarding follows Adyen's check -> token exchange -> finish sequence:
     * https://docs.adyen.com/point-of-sale/mobile-android/build/payments-app/
     */
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
        "boardingToken" to boardingToken.base64Url(),
    )

    /**
     * The Payments App requires the encrypted Base64URL Nexo envelope in the documented `request`
     * query parameter.
     */
    fun nexo(profile: AdyenProfile, encodedRequest: String): String = withParams(
        base(profile.environment, "nexo"),
        "returnUrl" to RETURN_URL,
        "request" to encodedRequest,
    )

    fun paymentsAppPackageName(profile: AdyenProfile): String = when (profile.environment) {
        PaymentEnvironment.TEST -> TEST_PAYMENTS_APP_PACKAGE
        PaymentEnvironment.LIVE -> LIVE_PAYMENTS_APP_PACKAGE
    }

    fun paymentsAppPlayStore(profile: AdyenProfile): String =
        PLAY_STORE_URL + paymentsAppPackageName(profile)

    private fun base(environment: PaymentEnvironment, action: String): String = when (environment) {
        PaymentEnvironment.TEST -> "https://www.adyen.com/test/$action"
        PaymentEnvironment.LIVE -> "https://www.adyen.com/$action"
    }

    private fun withParams(base: String, vararg params: Pair<String, String>): String =
        base + params.joinToString(prefix = "?", separator = "&") { (key, value) ->
            "${key.encode()}=${value.encode()}"
        }

    private fun String.encode(): String = URLEncoder.encode(this, Charsets.UTF_8.name())

    private fun String.base64Url(): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(toByteArray(Charsets.UTF_8))
}
