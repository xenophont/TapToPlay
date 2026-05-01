package com.example.taptoplay.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyPolicyTest {
    @Test
    fun privacyPolicyUsesPublicGitHubPagesHtmlUrl() {
        assertEquals(
            "https://xenophont.github.io/TapToPlay/privacy-policy.html",
            PrivacyPolicy.URL,
        )
        assertTrue(PrivacyPolicy.URL.startsWith("https://"))
        assertTrue(PrivacyPolicy.URL.endsWith("/privacy-policy.html"))
    }
}
