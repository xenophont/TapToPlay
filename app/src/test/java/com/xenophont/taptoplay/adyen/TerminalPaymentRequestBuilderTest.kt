package com.xenophont.taptoplay.adyen

import androidx.compose.ui.graphics.Color
import com.xenophont.taptoplay.cart.CartLine
import com.xenophont.taptoplay.catalog.Product
import com.xenophont.taptoplay.profiles.AdyenProfile
import com.xenophont.taptoplay.profiles.PaymentEnvironment
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TerminalPaymentRequestBuilderTest {
    @Test
    fun structuredPaymentRequestRecordsServiceAndSaleTransactionIds() {
        val request = TerminalPaymentRequestBuilder.buildDemoPaymentRequest(
            profile = profile(),
            installationId = "install-1",
            lines = listOf(CartLine(product(), 1)),
            totalMinor = 12900,
        )

        val requestJson = request.payload.decodeToString()
        val insight = TerminalApiRequestInspector.inspect(requestJson)
        val saleToPoi = Json.parseToJsonElement(requestJson).jsonObject["SaleToPOIRequest"]!!.jsonObject
        val amount = saleToPoi["PaymentRequest"]!!
            .jsonObject["PaymentTransaction"]!!
            .jsonObject["AmountsReq"]!!
            .jsonObject["RequestedAmount"]!!
            .jsonPrimitive
            .double

        assertEquals("Payment", request.messageCategory)
        assertEquals(request.serviceId, insight.serviceId)
        assertEquals(request.saleTransactionId, insight.saleTransactionId)
        assertEquals(129.0, amount, 0.0)
    }

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
    fun scannedAcquirerDataPaymentRequestDoesNotSendLocalCartItemsAsSaleItems() {
        val scannedConfig = SaleToAcquirerDataQrParser().parse(
            """
                {
                  "metadata": {
                    "qr": "scanned"
                  },
                  "additionalData": {
                    "authorisationType": "PreAuth"
                  }
                }
            """.trimIndent(),
        ).getOrThrow()
        val request = TerminalPaymentRequestBuilder.buildDemoRequest(
            profile = profile(),
            installationId = "install-1",
            lines = listOf(CartLine(product(), 2)),
            totalMinor = 25800,
            saleToAcquirerDataConfig = scannedConfig,
        )

        assertEquals(false, request.contains("SaleItem"))
        assertEquals(false, request.contains("saleItem"))
        assertEquals(true, TerminalApiRequestInspector.inspect(request).saleToAcquirerDataJson?.contains("\"qr\"") == true)
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
        terminalKeyIdentifier = "key",
        terminalKeyVersion = 1,
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
