package com.example.taptoplay.adyen

import java.net.URLDecoder
import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class TerminalApiResponseInsight(
    val category: String,
    val result: String?,
    val transactionId: String?,
    val errorCondition: String?,
    val additionalResponseRaw: String?,
    val additionalResponseDecoded: JsonElement?,
    val additionalResponseFields: List<AdditionalResponseField>,
    val root: JsonObject,
)

data class AdditionalResponseField(
    val name: String,
    val value: String,
    val decodedValue: String? = null,
)

object TerminalApiResponseInspector {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun inspect(responseJson: String?): TerminalApiResponseInsight? {
        if (responseJson.isNullOrBlank()) return null
        val root = runCatching { json.parseToJsonElement(responseJson).jsonObject }.getOrNull() ?: return null
        val saleToPoi = root["SaleToPOIResponse"]?.jsonObject ?: return null
        val category = saleToPoi.keys.firstOrNull { it.endsWith("Response") && it != "MessageHeader" } ?: return null
        val body = saleToPoi[category]?.jsonObject ?: return null
        val response = body["Response"]?.jsonObject
        val additionalResponse = response?.string("AdditionalResponse")
        val decodedAdditional = additionalResponse?.let(::decodeJson)
        val fields = additionalResponseFields(additionalResponse, decodedAdditional)
        val transactionId = body["POIData"]
            ?.jsonObject
            ?.get("POITransactionID")
            ?.jsonObject
            ?.string("TransactionID")

        return TerminalApiResponseInsight(
            category = category.removeSuffix("Response"),
            result = response?.string("Result"),
            transactionId = transactionId,
            errorCondition = response?.string("ErrorCondition"),
            additionalResponseRaw = additionalResponse,
            additionalResponseDecoded = decodedAdditional,
            additionalResponseFields = fields,
            root = root,
        )
    }

    fun compactSummary(responseJson: String?): Map<String, String> {
        val insight = inspect(responseJson) ?: return emptyMap()
        val fields = linkedMapOf<String, String>()
        insight.result?.let { fields["Result"] = it }
        insight.transactionId?.let { fields["Transaction ID"] = it }
        insight.errorCondition?.let { fields["Error"] = it }
        importantAdditional("pspReference", insight)?.let { fields["PSP reference"] = it }
        val reason = importantAdditional("refusalReason", insight)
            ?: importantAdditional("refusalReasonRaw", insight)
            ?: importantAdditional("message", insight)
        reason?.let { fields["Reason"] = it }
        importantAdditional("transactionType", insight)?.let { fields["Transaction type"] = it }
        return fields
    }

    fun importantAdditional(name: String, insight: TerminalApiResponseInsight): String? {
        insight.additionalResponseFields
            .firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?.let { return it.decodedValue ?: it.value }
        val decoded = insight.additionalResponseDecoded
        if (decoded is JsonObject) {
            decoded.findString(name)?.let { return it }
        }
        return null
    }

    private fun additionalResponseFields(raw: String?, decoded: JsonElement?): List<AdditionalResponseField> {
        if (decoded is JsonObject) return decoded.flatten()
        if (raw.isNullOrBlank()) return emptyList()
        return parseFormFields(raw).map { (key, value) ->
            AdditionalResponseField(
                name = key,
                value = value,
                decodedValue = decodePrintable(value),
            )
        }
    }

    private fun parseFormFields(raw: String): List<Pair<String, String>> {
        if (!raw.contains("=")) return emptyList()
        return raw.split("&")
            .filter { it.isNotBlank() }
            .mapNotNull { part ->
                val pieces = part.split("=", limit = 2)
                if (pieces.isEmpty()) {
                    null
                } else {
                    pieces[0].urlDecode() to pieces.getOrElse(1) { "" }.urlDecode()
                }
            }
    }

    private fun decodeJson(value: String): JsonElement? {
        val decoded = decodePrintable(value) ?: return null
        return runCatching { json.parseToJsonElement(decoded) }.getOrNull()
    }

    private fun decodePrintable(value: String): String? {
        val normalized = value.trim().replace(' ', '+')
        if (value.any { it.isWhitespace() }) return null
        if (!normalized.all { it.isLetterOrDigit() || it == '+' || it == '/' || it == '_' || it == '-' || it == '=' }) return null
        if (normalized.length < 8) return null
        val padded = normalized.padEnd(normalized.length + ((4 - normalized.length % 4) % 4), '=')
        return listOf(
            runCatching { Base64.getUrlDecoder().decode(padded) },
            runCatching { Base64.getDecoder().decode(padded) },
        ).firstNotNullOfOrNull { result ->
            result.getOrNull()
                ?.toString(Charsets.UTF_8)
                ?.takeIf { decoded -> decoded.isMostlyReadable() }
        }
    }

    private fun String.isMostlyReadable(): Boolean =
        isNotBlank() && count { !it.isISOControl() || it == '\n' || it == '\r' || it == '\t' } >= length * 9 / 10

    private fun JsonObject.flatten(prefix: String = ""): List<AdditionalResponseField> =
        entries.flatMap { (key, value) ->
            val name = if (prefix.isBlank()) key else "$prefix.$key"
            when (value) {
                is JsonObject -> value.flatten(name)
                is JsonPrimitive -> listOf(AdditionalResponseField(name, value.displayValue()))
                else -> listOf(AdditionalResponseField(name, value.toString()))
            }
        }

    private fun JsonObject.findString(name: String): String? {
        entries.forEach { (key, value) ->
            if (key.equals(name, ignoreCase = true) && value is JsonPrimitive) return value.displayValue()
            if (value is JsonObject) value.findString(name)?.let { return it }
        }
        return null
    }

    private fun JsonPrimitive.displayValue(): String = contentOrNull ?: toString()

    private fun JsonObject.string(name: String): String? =
        get(name)?.let { runCatching { it.jsonPrimitive.content }.getOrNull() }

    private fun String.urlDecode(): String = URLDecoder.decode(this, Charsets.UTF_8.name())
}
