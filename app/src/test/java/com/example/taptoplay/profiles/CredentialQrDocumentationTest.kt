package com.example.taptoplay.profiles

import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialQrDocumentationTest {
    @Test
    fun credentialQrDocumentationLinkPointsToQrGuide() {
        assertTrue(CredentialQrDocumentation.URL.startsWith("https://github.com/xenophont/TapToPlay/"))
        assertTrue(CredentialQrDocumentation.URL.contains("docs/QR_CREDENTIALS.md"))
        assertTrue(CredentialQrDocumentation.URL.contains("creating-and-using-a-credential-qr"))
    }
}
