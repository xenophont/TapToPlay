package com.example.taptoplay.adyen

import com.example.taptoplay.cart.CartLine
import com.example.taptoplay.profiles.AdyenProfile
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class TerminalPaymentRequest(
    val json: String,
    val serviceId: String,
    val saleTransactionId: String?,
    val messageCategory: String,
)

object TerminalPaymentRequestBuilder {
    fun buildDemoPaymentRequest(
        profile: AdyenProfile,
        installationId: String,
        lines: List<CartLine>,
        totalMinor: Long,
        saleToAcquirerDataConfig: SaleToAcquirerDataConfig = SaleToAcquirerDataConfig.default(),
    ): TerminalPaymentRequest {
        require(lines.isNotEmpty()) { "A payment request requires at least one cart line" }
        require(totalMinor > 0) { "A payment request amount must be positive" }
        val serviceId = newServiceId()
        val saleTransactionId = UUID.randomUUID().toString()
        val timestamp = Instant.now().toString()
        val saleToAcquirerData = SaleToAcquirerDataEncoder.encodeBase64(saleToAcquirerDataConfig)
        val request = buildJsonObject {
            put("SaleToPOIRequest", buildJsonObject {
                put("MessageHeader", messageHeader("Payment", serviceId, installationId))
                put("PaymentRequest", buildJsonObject {
                    put("SaleData", buildJsonObject {
                        put("SaleTransactionID", buildJsonObject {
                            put("TransactionID", saleTransactionId)
                            put("TimeStamp", timestamp)
                        })
                        put("SaleToAcquirerData", saleToAcquirerData)
                    })
                    put("PaymentTransaction", buildJsonObject {
                        put("AmountsReq", buildJsonObject {
                            put("Currency", profile.currency)
                            put("RequestedAmount", JsonPrimitive(totalMinor.toMajorAmount()))
                        })
                    })
                })
            })
        }
        return TerminalPaymentRequest(
            json = json.encodeToString(JsonObject.serializer(), request),
            serviceId = serviceId,
            saleTransactionId = saleTransactionId,
            messageCategory = "Payment",
        )
    }

    fun buildDemoRequest(
        profile: AdyenProfile,
        installationId: String,
        lines: List<CartLine>,
        totalMinor: Long,
        saleToAcquirerDataConfig: SaleToAcquirerDataConfig = SaleToAcquirerDataConfig.default(),
    ): String = buildDemoPaymentRequest(
        profile = profile,
        installationId = installationId,
        lines = lines,
        totalMinor = totalMinor,
        saleToAcquirerDataConfig = saleToAcquirerDataConfig,
    ).json

    fun buildReferencedRefundPaymentRequest(
        installationId: String,
        originalTransactionId: String,
        originalTimestamp: String,
    ): TerminalPaymentRequest {
        val serviceId = newServiceId()
        val request = buildJsonObject {
            put("SaleToPOIRequest", buildJsonObject {
                put("MessageHeader", messageHeader("Reversal", serviceId, installationId))
                put("ReversalRequest", buildJsonObject {
                    put("OriginalPOITransaction", buildJsonObject {
                        put("POITransactionID", buildJsonObject {
                            put("TransactionID", originalTransactionId)
                            put("TimeStamp", originalTimestamp)
                        })
                    })
                    put("ReversalReason", "MerchantCancel")
                })
            })
        }
        return TerminalPaymentRequest(
            json = json.encodeToString(JsonObject.serializer(), request),
            serviceId = serviceId,
            saleTransactionId = null,
            messageCategory = "Reversal",
        )
    }

    fun buildReferencedRefundRequest(
        installationId: String,
        originalTransactionId: String,
        originalTimestamp: String,
    ): String = buildReferencedRefundPaymentRequest(
        installationId = installationId,
        originalTransactionId = originalTransactionId,
        originalTimestamp = originalTimestamp,
    ).json

    private fun messageHeader(messageCategory: String, serviceId: String, installationId: String) =
        buildJsonObject {
            put("ProtocolVersion", "3.0")
            put("MessageClass", "Service")
            put("MessageCategory", messageCategory)
            put("MessageType", "Request")
            put("ServiceID", serviceId)
            put("SaleID", "TapToPlay")
            put("POIID", installationId)
        }

    private fun Long.toMajorAmount(): BigDecimal =
        BigDecimal.valueOf(this, 2).stripTrailingZeros()

    private fun newServiceId(): String =
        UUID.randomUUID().toString().replace("-", "").take(10)

    private val json = Json {
        explicitNulls = false
        prettyPrint = true
    }
}
