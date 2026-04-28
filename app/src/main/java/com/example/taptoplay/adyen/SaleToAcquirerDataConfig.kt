package com.example.taptoplay.adyen

import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class SaleToAcquirerDataConfig(
    val schema: String = SCHEMA,
    val displayName: String,
    val data: JsonObject,
) {
    val fieldCount: Int
        get() = data.countLeaves()

    companion object {
        const val SCHEMA = "taptoplay.adyen.saleToAcquirerData.v1"

        fun default(): SaleToAcquirerDataConfig = SaleToAcquirerDataConfig(
            displayName = "Retail demo defaults",
            data = buildJsonObject {
                put("applicationInfo", buildJsonObject {
                    put("externalPlatform", buildJsonObject {
                        put("name", "TapToPlay Demo")
                        put("version", "1.0")
                        put("integrator", "TapToPlay")
                    })
                    put("merchantApplication", buildJsonObject {
                        put("name", "TapToPlay")
                        put("version", "1.0")
                    })
                    put("merchantDevice", buildJsonObject {
                        put("os", "Android")
                    })
                })
                put("metadata", buildJsonObject {
                    put("retailDemo", "TapToPlay")
                })
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
        val root = json.parseToJsonElement(payload).jsonObject
        val config = SaleToAcquirerDataConfig(
            schema = root["schema"]?.jsonPrimitive?.content ?: "",
            displayName = root["displayName"]?.jsonPrimitive?.content ?: "",
            data = (root["saleToAcquirerData"] ?: root["properties"])
                ?.jsonObject
                ?: error("saleToAcquirerData is required"),
        )
        validate(config)
        config
    }.recoverCatching { error ->
        throw IllegalArgumentException(error.message ?: "Invalid SaleToAcquirerData QR payload", error)
    }

    private fun validate(config: SaleToAcquirerDataConfig) {
        require(config.schema == SaleToAcquirerDataConfig.SCHEMA) { "Unsupported schema: ${config.schema}" }
        require(config.displayName.isNotBlank()) { "displayName is required" }
        require(config.data.isNotEmpty()) { "saleToAcquirerData must contain at least one entry" }
    }
}

object SaleToAcquirerDataEncoder {
    private val json = Json { explicitNulls = false }

    fun encodeBase64(config: SaleToAcquirerDataConfig): String {
        val merged = SaleToAcquirerDataConfig.default().data.deepMerge(config.data)
        val rawJson = json.encodeToString(JsonObject.serializer(), merged)
        return Base64.getEncoder().encodeToString(rawJson.toByteArray(Charsets.UTF_8))
    }

    fun decodeBase64ForTest(encoded: String): JsonObject {
        val rawJson = Base64.getDecoder().decode(encoded).toString(Charsets.UTF_8)
        return json.parseToJsonElement(rawJson) as JsonObject
    }
}

private fun JsonObject.deepMerge(override: JsonObject): JsonObject = JsonObject(
    toMutableMap().apply {
        override.forEach { (key, overrideValue) ->
            val baseValue = this[key]
            this[key] = if (baseValue is JsonObject && overrideValue is JsonObject) {
                baseValue.deepMerge(overrideValue)
            } else {
                overrideValue
            }
        }
    },
)

private fun JsonElement.countLeaves(): Int = when (this) {
    is JsonObject -> values.sumOf { it.countLeaves() }
    else -> 1
}
