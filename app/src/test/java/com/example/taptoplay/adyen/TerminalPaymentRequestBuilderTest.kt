package com.example.taptoplay.adyen

import androidx.compose.ui.graphics.Color
import com.example.taptoplay.cart.CartLine
import com.example.taptoplay.catalog.Product
import com.example.taptoplay.profiles.AdyenProfile
import com.example.taptoplay.profiles.PaymentEnvironment
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
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
                data = buildJsonObject {
                    put("metadata", buildJsonObject {
                        put("experiment", "qr")
                    })
                },
            ),
        )
        val encoded = Regex("\"SaleToAcquirerData\"\\s*:\\s*\"([^\"]+)\"")
            .find(request)
            ?.groupValues
            ?.get(1)

        assertNotNull(encoded)
        val decoded = SaleToAcquirerDataEncoder.decodeBase64ForTest(encoded!!)
        assertEquals("TapToPlay", decoded["metadata"]?.jsonObject?.get("retailDemo")?.jsonPrimitive?.content)
        assertEquals("qr", decoded["metadata"]?.jsonObject?.get("experiment")?.jsonPrimitive?.content)
    }

    @Test
    fun paymentRequestDoesNotSendLocalCartItemsAsTerminalApiSaleItems() {
        val request = TerminalPaymentRequestBuilder.buildDemoRequest(
            profile = profile(),
            installationId = "install-1",
            lines = listOf(CartLine(product(), 2)),
            totalMinor = 25800,
        )

        assertEquals(false, request.contains("SaleItem"))
    }

    @Test
    fun requestInspectorDecodesSaleToAcquirerData() {
        val request = TerminalPaymentRequestBuilder.buildDemoRequest(
            profile = profile(),
            installationId = "install-1",
            lines = listOf(CartLine(product(), 1)),
            totalMinor = 12900,
            saleToAcquirerDataConfig = SaleToAcquirerDataConfig(
                displayName = "Plain",
                mergeWithDefaults = false,
                data = buildJsonObject {
                    put("metadata", buildJsonObject {
                        put("order", "demo")
                    })
                },
            ),
        )

        val insight = TerminalApiRequestInspector.inspect(request)

        assertEquals("Payment", insight.messageCategory)
        assertEquals(true, insight.saleToAcquirerDataJson?.contains("\"order\"") == true)
        assertEquals(true, insight.saleToAcquirerDataJson?.contains("\"demo\"") == true)
    }

    @Test
    fun buildsReferencedRefundReversalRequest() {
        val request = TerminalPaymentRequestBuilder.buildReferencedRefundRequest(
            installationId = "install-1",
            originalTransactionId = "tender.PSP123",
            originalTimestamp = "2026-04-29T12:00:00Z",
        )

        assertEquals(true, request.contains("\"MessageCategory\": \"Reversal\""))
        assertEquals(true, request.contains("\"ReversalReason\": \"MerchantCancel\""))
        assertEquals(true, request.contains("\"TransactionID\": \"tender.PSP123\""))
        assertEquals(true, request.contains("\"POIID\": \"install-1\""))
    }

    @Test
    fun decodedSaleToAcquirerDataCanBePrettyPrinted() {
        val encoded = SaleToAcquirerDataEncoder.encodeBase64(
            SaleToAcquirerDataConfig(
                displayName = "Plain",
                mergeWithDefaults = false,
                data = buildJsonObject {
                    put("metadata", buildJsonObject {
                        put("order", "demo")
                    })
                },
            ),
        )

        val decoded = SaleToAcquirerDataEncoder.decodeBase64(encoded).getOrThrow()
        val pretty = SaleToAcquirerDataEncoder.prettyPrint(decoded)

        assertEquals("demo", decoded["metadata"]?.jsonObject?.get("order")?.jsonPrimitive?.content)
        assertEquals(true, pretty.contains("\"metadata\""))
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
