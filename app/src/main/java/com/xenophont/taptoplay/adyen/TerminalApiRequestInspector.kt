package com.xenophont.taptoplay.adyen

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class TerminalApiRequestInsight(
    val messageCategory: String?,
    val serviceId: String?,
    val saleTransactionId: String?,
    val saleToAcquirerDataBase64: String?,
    val saleToAcquirerDataJson: String?,
)

object TerminalApiRequestInspector {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun inspect(requestJson: String): TerminalApiRequestInsight {
        val saleToPoi = runCatching {
            json.parseToJsonElement(requestJson)
                .jsonObject["SaleToPOIRequest"]
                ?.jsonObject
        }.getOrNull()
        val category = saleToPoi
            ?.get("MessageHeader")
            ?.jsonObject
            ?.get("MessageCategory")
            ?.jsonPrimitive
            ?.content
        val serviceId = saleToPoi
            ?.get("MessageHeader")
            ?.jsonObject
            ?.get("ServiceID")
            ?.jsonPrimitive
            ?.content
        val saleTransactionId = saleToPoi
            ?.get("PaymentRequest")
            ?.jsonObject
            ?.get("SaleData")
            ?.jsonObject
            ?.get("SaleTransactionID")
            ?.jsonObject
            ?.get("TransactionID")
            ?.jsonPrimitive
            ?.content
        val saleToAcquirerData = saleToPoi
            ?.get("PaymentRequest")
            ?.jsonObject
            ?.get("SaleData")
            ?.jsonObject
            ?.get("SaleToAcquirerData")
            ?.jsonPrimitive
            ?.content
        val decoded = saleToAcquirerData
            ?.let { SaleToAcquirerDataEncoder.decodeBase64(it).getOrNull() }
            ?.let { json.encodeToString(JsonObject.serializer(), it) }
        return TerminalApiRequestInsight(
            messageCategory = category,
            serviceId = serviceId,
            saleTransactionId = saleTransactionId,
            saleToAcquirerDataBase64 = saleToAcquirerData,
            saleToAcquirerDataJson = decoded,
        )
    }
}
