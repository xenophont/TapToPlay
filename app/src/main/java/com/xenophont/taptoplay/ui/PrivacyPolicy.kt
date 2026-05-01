package com.xenophont.taptoplay.ui

import android.content.Intent
import android.net.Uri

internal object PrivacyPolicy {
    const val URL = "https://xenophont.github.io/TapToPlay/privacy-policy.html"

    val intentSpec = ExternalUrlIntentSpec(
        url = URL,
        opensFromAnyContext = true,
        browsable = true,
    )

    fun viewIntent(): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(intentSpec.url)).apply {
            if (intentSpec.browsable) {
                addCategory(Intent.CATEGORY_BROWSABLE)
            }
            if (intentSpec.opensFromAnyContext) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
}

internal data class ExternalUrlIntentSpec(
    val url: String,
    val opensFromAnyContext: Boolean,
    val browsable: Boolean,
)
