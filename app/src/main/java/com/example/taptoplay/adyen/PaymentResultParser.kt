package com.example.taptoplay.adyen

import android.net.Uri
import com.example.taptoplay.profiles.AdyenProfile
import java.net.URLDecoder
import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

sealed interface PaymentResult {
    data class BoardingStatus(
        val boarded: Boolean,
        val installationId: String?,
        val boardingRequestToken: String?,
        val error: String?,
        val data: String?,
    ) : PaymentResult

    data class Success(
        val pspReference: String?,
        val rawResult: String?,
        val responseJson: String? = null,
    ) : PaymentResult

    data class Refused(
        val reason: String?,
        val responseJson: String? = null,
    ) : PaymentResult

    data class Failure(
        val message: String,
        val responseJson: String? = null,
    ) : PaymentResult
}

object PaymentResultParser {
    fun parse(uri: Uri?): PaymentResult? {
        if (uri == null || uri.scheme != "taptoplay" || uri.host != "adyen-return") return null
        return parse(uri.toString())
    }

    fun parse(rawUri: String): PaymentResult? {
        val parsed = runCatching { java.net.URI(rawUri) }.getOrNull() ?: return null
        if (parsed.scheme != "taptoplay" || parsed.host != "adyen-return") return null
        val params = parseQuery(parsed.rawQuery.orEmpty())
        return parseParams(params, profile = null, crypto = null)
    }

    fun parse(rawUri: String, profile: AdyenProfile?, crypto: NexoCrypto): PaymentResult? {
        val parsed = runCatching { java.net.URI(rawUri) }.getOrNull() ?: return null
        if (parsed.scheme != "taptoplay" || parsed.host != "adyen-return") return null
        val params = parseQuery(parsed.rawQuery.orEmpty())
        return parseParams(params, profile, crypto)
    }

    private fun parseParams(
        params: Map<String, String>,
        profile: AdyenProfile?,
        crypto: NexoCrypto?,
    ): PaymentResult {
        val boarded = params["boarded"]
        val installationId = params["installationId"]
        if (boarded != null || params["boardingRequestToken"] != null) {
            return PaymentResult.BoardingStatus(
                boarded = boarded.equals("true", ignoreCase = true),
                installationId = installationId,
                boardingRequestToken = params["boardingRequestToken"],
                error = params["error"],
                data = params["data"],
            )
        }

        terminalApiResponse(params, profile, crypto)?.let { return it }

        val result = params["result"] ?: params["Result"] ?: params["event"]
        return when (result?.lowercase()) {
            "success", "approved", "authorised", "authorized" -> PaymentResult.Success(
                pspReference = params["pspReference"],
                rawResult = result,
            )
            "refused", "declined" -> PaymentResult.Refused(params["reason"])
            null -> PaymentResult.Failure("Adyen returned without a recognizable result.")
            else -> PaymentResult.Failure(params["message"] ?: result)
        }
    }

    private fun terminalApiResponse(
        params: Map<String, String>,
        profile: AdyenProfile?,
        crypto: NexoCrypto?,
    ): PaymentResult? {
        val preferredKeys = listOf("response", "nexoResponse", "terminalApiResponse", "payload", "data")
        val candidates = buildList {
            preferredKeys.forEach { key -> params[key]?.let(::add) }
            params
                .filterKeys { it !in preferredKeys && it !in setOf("result", "Result", "event", "pspReference", "reason", "message") }
                .values
                .forEach(::add)
        }.distinct()

        candidates.forEach { candidate ->
            val decoded = decodeBase64(candidate) ?: return@forEach
            val responseJson = when {
                decoded.contains("\"NexoBlob\"") && profile != null && crypto != null ->
                    runCatching { crypto.decrypt(profile, decoded) }.getOrNull() ?: decoded
                else -> decoded
            }
            parseTerminalApiResponse(responseJson)?.let { return it }
        }
        return null
    }

    private fun parseTerminalApiResponse(responseJson: String): PaymentResult? {
        val paymentResponse = runCatching {
            json.parseToJsonElement(responseJson)
                .jsonObject["SaleToPOIResponse"]
                ?.jsonObject
                ?.get("PaymentResponse")
                ?.jsonObject
        }.getOrNull() ?: return null

        val response = paymentResponse["Response"]?.jsonObject
        val result = response?.string("Result")
        val errorCondition = response?.string("ErrorCondition")
        val additionalResponse = response?.string("AdditionalResponse")
        val transactionId = paymentResponse["POIData"]
            ?.jsonObject
            ?.get("POITransactionID")
            ?.jsonObject
            ?.string("TransactionID")

        return when (result?.lowercase()) {
            "success" -> PaymentResult.Success(
                pspReference = transactionId,
                rawResult = result,
                responseJson = responseJson,
            )
            "failure" -> {
                val reason = listOfNotNull(errorCondition, additionalResponse).joinToString(" | ").ifBlank { null }
                if (errorCondition.equals("Refusal", ignoreCase = true) || errorCondition.equals("Refused", ignoreCase = true)) {
                    PaymentResult.Refused(reason, responseJson)
                } else {
                    PaymentResult.Failure(reason ?: "Terminal API payment failed.", responseJson)
                }
            }
            null -> PaymentResult.Failure("Terminal API response did not include Response.Result.", responseJson)
            else -> PaymentResult.Failure("Terminal API returned $result.", responseJson)
        }
    }

    private fun decodeBase64(value: String): String? {
        val normalized = value.trim().replace(' ', '+')
        val padded = normalized.padEnd(normalized.length + ((4 - normalized.length % 4) % 4), '=')
        return listOf(
            runCatching { Base64.getUrlDecoder().decode(padded) },
            runCatching { Base64.getDecoder().decode(padded) },
        ).firstNotNullOfOrNull { result ->
            result.getOrNull()
                ?.toString(Charsets.UTF_8)
                ?.takeIf { it.trimStart().startsWith("{") }
        }
    }

    private fun parseQuery(query: String): Map<String, String> =
        query.split("&")
            .filter { it.isNotBlank() }
            .mapNotNull {
                val parts = it.split("=", limit = 2)
                if (parts.isEmpty()) null else parts[0].decode() to parts.getOrElse(1) { "" }.decode()
            }
            .toMap()

    private fun String.decode(): String = URLDecoder.decode(this, Charsets.UTF_8.name())

    private fun JsonObject.string(name: String): String? =
        get(name)?.stringOrNull()

    private fun JsonElement.stringOrNull(): String? =
        runCatching { jsonPrimitive.content }.getOrNull()

    private val json = Json { ignoreUnknownKeys = true }
}
