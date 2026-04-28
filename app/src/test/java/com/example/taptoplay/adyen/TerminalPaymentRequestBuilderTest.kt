package com.example.taptoplay.adyen

import androidx.compose.ui.graphics.Color
import com.example.taptoplay.cart.CartLine
import com.example.taptoplay.catalog.Product
import com.example.taptoplay.profiles.AdyenProfile
import com.example.taptoplay.profiles.PaymentEnvironment
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TerminalPaymentRequestBuilderTest {
    @Test
    fun embedsBase64JsonSaleToAcquirerData() {
        val request = TerminalPaymentRequestBuilder.buildDemoRequest(
            profile = profile(),
            installationId = "install-1",
            lines = listOf(CartLine(product(), 1)),
            totalMinor = 12900,
            saleToAcquirerDataConfig = SaleToAcquirerDataConfig(
                displayName = "Metadata test",
                properties = buildJsonObject {
                    put("metadata.experiment", "qr")
                },
            ),
        )
        val encoded = Regex("\"SaleToAcquirerData\"\\s*:\\s*\"([^\"]+)\"")
            .find(request)
            ?.groupValues
            ?.get(1)

        assertNotNull(encoded)
        val decoded = SaleToAcquirerDataEncoder.decodeBase64ForTest(encoded!!)
        assertEquals("TapToPlay", decoded["metadata.retailDemo"]?.jsonPrimitive?.content)
        assertEquals("qr", decoded["metadata.experiment"]?.jsonPrimitive?.content)
    }

    private fun profile() = AdyenProfile(
        displayName = "Demo",
        environment = PaymentEnvironment.TEST,
        merchantId = "merchant",
        apiKey = "api",
        clientKey = "client",
        terminalKeyIdentifier = "key",
        terminalKeyVersion = 1,
        terminalPassphrase = "passphrase",
        currency = "EUR",
        countryCode = "ES",
    )

    private fun product() = Product(
        id = "shirt",
        name = "Shirt",
        category = "Shirts",
        description = "Test shirt",
        priceMinor = 12900,
        color = Color.White,
        accentColor = Color.Black,
    )
}
