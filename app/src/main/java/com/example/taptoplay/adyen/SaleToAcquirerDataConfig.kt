package com.example.taptoplay.adyen

import java.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

@Serializable
data class SaleToAcquirerDataConfig(
    val schema: String = SCHEMA,
    val displayName: String,
    val data: JsonObject,
    val mergeWithDefaults: Boolean = true,
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
        require(payload.length <= MAX_PAYLOAD_CHARS) { "SaleToAcquirerData QR payload is too large" }
        val root = json.parseToJsonElement(payload).jsonObject
        // Retired TapToPlay wrapper QR formats are rejected before anything reaches Terminal API encoding.
        require(root.keys.none { it in LEGACY_WRAPPER_KEYS }) {
            "Legacy TapToPlay SaleToAcquirerData QR wrappers are no longer supported. Scan the plain SaleToAcquirerData JSON object."
        }
        val config = SaleToAcquirerDataConfig(
            displayName = "Scanned SaleToAcquirerData",
            data = root,
            mergeWithDefaults = false,
        )
        validate(config)
        config
    }.recoverCatching { error ->
        throw IllegalArgumentException(error.message ?: "Invalid SaleToAcquirerData QR payload", error)
    }

    private fun validate(config: SaleToAcquirerDataConfig) {
        require(config.schema == SaleToAcquirerDataConfig.SCHEMA) { "Unsupported schema: ${config.schema}" }
        require(config.displayName.isNotBlank()) { "displayName is required" }
        require(config.displayName.length <= MAX_DISPLAY_NAME_CHARS) { "displayName is too long" }
        require(config.data.isNotEmpty()) { "saleToAcquirerData must contain at least one entry" }
        require(config.fieldCount <= MAX_FIELD_COUNT) { "saleToAcquirerData contains too many fields" }
    }

    private companion object {
        const val MAX_PAYLOAD_CHARS = 12_288
        const val MAX_DISPLAY_NAME_CHARS = 80
        const val MAX_FIELD_COUNT = 80
        val LEGACY_WRAPPER_KEYS = setOf("schema", "displayName", "saleToAcquirerData", "properties")
    }
}

object SaleToAcquirerDataEncoder {
    private val json = Json {
        explicitNulls = false
        prettyPrint = true
    }

    fun encodeBase64(config: SaleToAcquirerDataConfig): String {
        val payload = payload(config)
        val rawJson = json.encodeToString(JsonObject.serializer(), payload)
        return Base64.getEncoder().encodeToString(rawJson.toByteArray(Charsets.UTF_8))
    }

    fun payload(config: SaleToAcquirerDataConfig): JsonObject =
        if (config.mergeWithDefaults) {
            SaleToAcquirerDataConfig.default().data.deepMerge(config.data)
        } else {
            config.data
        }

    fun decodeBase64(encoded: String): Result<JsonObject> = runCatching {
        val rawJson = Base64.getDecoder().decode(encoded).toString(Charsets.UTF_8)
        json.parseToJsonElement(rawJson) as JsonObject
    }

    fun prettyPrint(payload: JsonObject): String =
        json.encodeToString(JsonObject.serializer(), payload)

    fun decodeBase64ForTest(encoded: String): JsonObject {
        return decodeBase64(encoded).getOrThrow()
    }
}

object SaleToAcquirerDataEditor {
    fun update(config: SaleToAcquirerDataConfig, path: List<String>, rawValue: String): SaleToAcquirerDataConfig {
        require(path.isNotEmpty()) { "Path is required" }
        return config.copy(
            data = config.data.setAt(path, rawValue.toJsonPrimitive()),
            displayName = config.displayName.asEditedName(),
        )
    }

    fun remove(config: SaleToAcquirerDataConfig, path: List<String>): SaleToAcquirerDataConfig {
        require(path.isNotEmpty()) { "Path is required" }
        return config.copy(
            data = config.data.removeAt(path),
            displayName = config.displayName.asEditedName(),
        )
    }

    private fun JsonObject.setAt(path: List<String>, value: JsonElement): JsonObject {
        val key = path.first()
        return JsonObject(
            toMutableMap().apply {
                this[key] = if (path.size == 1) {
                    value
                } else {
                    ((this[key] as? JsonObject) ?: JsonObject(emptyMap())).setAt(path.drop(1), value)
                }
            },
        )
    }

    private fun JsonObject.removeAt(path: List<String>): JsonObject {
        val key = path.first()
        return JsonObject(
            toMutableMap().apply {
                if (path.size == 1) {
                    remove(key)
                } else {
                    val child = this[key] as? JsonObject
                    if (child != null) {
                        val updatedChild = child.removeAt(path.drop(1))
                        if (updatedChild.isEmpty()) remove(key) else this[key] = updatedChild
                    }
                }
            },
        )
    }

    private fun String.toJsonPrimitive(): JsonPrimitive {
        val trimmed = trim()
        return when {
            trimmed.equals("true", ignoreCase = true) -> JsonPrimitive(true)
            trimmed.equals("false", ignoreCase = true) -> JsonPrimitive(false)
            trimmed.toLongOrNull() != null -> JsonPrimitive(trimmed.toLong())
            trimmed.toDoubleOrNull() != null -> JsonPrimitive(trimmed.toDouble())
            else -> JsonPrimitive(this)
        }
    }

    private fun String.asEditedName(): String =
        if (endsWith(" (edited)")) this else "$this (edited)"
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
