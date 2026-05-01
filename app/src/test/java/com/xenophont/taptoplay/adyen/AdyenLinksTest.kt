package com.xenophont.taptoplay.adyen

import com.xenophont.taptoplay.profiles.AdyenProfile
import com.xenophont.taptoplay.profiles.PaymentEnvironment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdyenLinksTest {
    @Test
    fun testEnvironmentUsesTestPaths() {
        val profile = profile(PaymentEnvironment.TEST)

        assertTrue(AdyenLinks.boarded(profile).startsWith("https://www.adyen.com/test/boarded"))
        assertTrue(AdyenLinks.board(profile, "token").startsWith("https://www.adyen.com/test/board"))
        assertTrue(AdyenLinks.nexo(profile, "blob").startsWith("https://www.adyen.com/test/nexo"))
    }

    @Test
    fun liveEnvironmentUsesLivePaths() {
        val profile = profile(PaymentEnvironment.LIVE)

        assertTrue(AdyenLinks.boarded(profile).startsWith("https://www.adyen.com/boarded"))
        assertTrue(AdyenLinks.startReboard(profile).contains("reboard=true"))
        assertTrue(AdyenLinks.nexo(profile, "blob").startsWith("https://www.adyen.com/nexo"))
    }

    @Test
    fun paymentLinkUsesDocumentedRequestParameter() {
        val profile = profile(PaymentEnvironment.TEST)

        val link = AdyenLinks.nexo(profile, "encryptedPayload")

        assertTrue(link.contains("request=encryptedPayload"))
    }

    @Test
    fun paymentsAppDownloadTargetsFollowEnvironment() {
        val testProfile = profile(PaymentEnvironment.TEST)
        val liveProfile = profile(PaymentEnvironment.LIVE)

        assertEquals("com.adyen.ipp.mobile.companion.test", AdyenLinks.paymentsAppPackageName(testProfile))
        assertEquals(
            "https://play.google.com/store/apps/details?id=com.adyen.ipp.mobile.companion.test",
            AdyenLinks.paymentsAppPlayStore(testProfile),
        )
        assertEquals("com.adyen.ipp.mobile.companion.live", AdyenLinks.paymentsAppPackageName(liveProfile))
        assertEquals(
            "https://play.google.com/store/apps/details?id=com.adyen.ipp.mobile.companion.live",
            AdyenLinks.paymentsAppPlayStore(liveProfile),
        )
    }

    private fun profile(environment: PaymentEnvironment) = AdyenProfile(
        displayName = "Profile",
        environment = environment,
        merchantId = "merchant",
        apiKey = "api",
        clientKey = "client",
        terminalKeyIdentifier = "key",
        terminalKeyVersion = 1,
        terminalPassphrase = "passphrase",
        currency = "EUR",
        countryCode = "ES",
    )
}
