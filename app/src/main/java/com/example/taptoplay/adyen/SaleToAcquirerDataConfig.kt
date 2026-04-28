package com.example.taptoplay.adyen

import java.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class SaleToAcquirerDataConfig(
    val schema: String = SCHEMA,
    val displayName: String,
    val properties: JsonObject,
) {
    companion object {
        const val SCHEMA = "taptoplay.adyen.saleToAcquirerData.v1"

        fun default(): SaleToAcquirerDataConfig = SaleToAcquirerDataConfig(
            displayName = "Retail demo defaults",
            properties = buildJsonObject {
                put("metadata.retailDemo", "TapToPlay")
            },
        )
    }
}

class SaleToAcquirerDataQrParser(
    private val json: Json = Json {
        ignoreUnknownKeys = false
        explicitNulls = false
    },
) {
    fun parse(payload: String): Result<SaleToAcquirerDataConfig> = runCatching {
        val config = json.decodeFromString<SaleToAcquirerDataConfig>(payload)
        validate(config)
        config
    }.recoverCatching { error ->
        throw IllegalArgumentException(error.message ?: "Invalid SaleToAcquirerData QR payload", error)
    }

    private fun validate(config: SaleToAcquirerDataConfig) {
        require(config.schema == SaleToAcquirerDataConfig.SCHEMA) { "Unsupported schema: ${config.schema}" }
        require(config.displayName.isNotBlank()) { "displayName is required" }
        require(config.properties.isNotEmpty()) { "properties must contain at least one entry" }
    }
}

object SaleToAcquirerDataEncoder {
    private val json = Json { explicitNulls = false }

    fun encodeBase64(config: SaleToAcquirerDataConfig): String {
        val merged = JsonObject(SaleToAcquirerDataConfig.default().properties + config.properties)
        val rawJson = json.encodeToString(JsonObject.serializer(), merged)
        return Base64.getEncoder().encodeToString(rawJson.toByteArray(Charsets.UTF_8))
    }

    fun decodeBase64ForTest(encoded: String): JsonObject {
        val rawJson = Base64.getDecoder().decode(encoded).toString(Charsets.UTF_8)
        return json.parseToJsonElement(rawJson) as JsonObject
    }
}
