package com.example.taptoplay.adyen

import com.example.taptoplay.profiles.AdyenProfile
import com.example.taptoplay.profiles.PaymentEnvironment
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

        val link = AdyenLinks.nexo(profile, "blob")

        assertTrue(link.contains("request=blob"))
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
