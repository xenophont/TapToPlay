package com.example.taptoplay.adyen

import com.example.taptoplay.cart.CartLine
import com.example.taptoplay.profiles.AdyenProfile
import java.util.UUID

object TerminalPaymentRequestBuilder {
    fun buildDemoRequest(profile: AdyenProfile, lines: List<CartLine>, totalMinor: Long): String {
        val amount = totalMinor / 100.0
        val items = lines.joinToString(separator = ",") {
            """{"name":"${it.product.name.escapeJson()}","quantity":${it.quantity},"amount":${it.lineTotalMinor / 100.0}}"""
        }
        return """
            {
              "SaleToPOIRequest": {
                "MessageHeader": {
                  "ProtocolVersion": "3.0",
                  "MessageClass": "Service",
                  "MessageCategory": "Payment",
                  "MessageType": "Request",
                  "ServiceID": "${UUID.randomUUID().toString().take(10)}",
                  "SaleID": "TapToPlay",
                  "POIID": "PaymentsApp"
                },
                "PaymentRequest": {
                  "SaleData": {
                    "SaleTransactionID": {
                      "TransactionID": "${UUID.randomUUID()}",
                      "TimeStamp": "${java.time.Instant.now()}"
                    },
                    "SaleToAcquirerData": "metadata.retailDemo=TapToPlay"
                  },
                  "PaymentTransaction": {
                    "AmountsReq": {
                      "Currency": "${profile.currency}",
                      "RequestedAmount": $amount
                    },
                    "SaleItem": [$items]
                  }
                }
              }
            }
        """.trimIndent()
    }

    private fun String.escapeJson(): String = replace("\\", "\\\\").replace("\"", "\\\"")
}
