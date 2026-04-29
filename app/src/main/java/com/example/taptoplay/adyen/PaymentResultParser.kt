package com.example.taptoplay.adyen

import android.net.Uri
import com.example.taptoplay.profiles.AdyenProfile
import java.net.URLDecoder
import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

sealed interface PaymentResult {
    data class BoardingStatus(
        val boarded: Boolean,
        val installationId: String?,
        val boardingRequestToken: String?,
        val error: String?,
        val data: String?,
        val returnData: BoardingReturnData? = null,
        val errorAdvice: String? = adyenAppLinkAdvice(error),
    ) : PaymentResult

    data class Success(
        val pspReference: String?,
        val rawResult: String?,
        val responseJson: String? = null,
        val serviceId: String? = null,
        val terminalTransactionId: String? = null,
    ) : PaymentResult

    data class Refused(
        val reason: String?,
        val responseJson: String? = null,
        val serviceId: String? = null,
    ) : PaymentResult

    data class Failure(
        val message: String,
        val responseJson: String? = null,
        val serviceId: String? = null,
    ) : PaymentResult
}

data class BoardingReturnData(
    val boarded: Boolean?,
    val installationId: String?,
    val date: String?,
    val reboarding: Boolean?,
    val boardingRequestToken: String?,
    val merchantAccountCode: String?,
    val merchantStoreCode: String?,
)

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
        val rawData = params["data"]
        val returnData = rawData?.let(::parseBoardingReturnData)
        val installationId = params["installationId"] ?: returnData?.installationId
        val boardingRequestToken = params["boardingRequestToken"] ?: returnData?.boardingRequestToken
        val isBoardingReturn = boarded != null ||
            boardingRequestToken != null ||
            returnData?.boarded != null ||
            returnData?.installationId != null
        if (isBoardingReturn) {
            return PaymentResult.BoardingStatus(
                boarded = boarded?.equals("true", ignoreCase = true) ?: (returnData?.boarded == true),
                installationId = installationId,
                boardingRequestToken = boardingRequestToken,
                error = params["error"],
                data = rawData,
                returnData = returnData,
            )
        }

        terminalApiResponse(params, profile, crypto)?.let { return it }

        params["error"]?.let { error ->
            val advice = adyenAppLinkAdvice(error)
            return PaymentResult.Failure(
                listOfNotNull("Adyen App Link error: $error", advice).joinToString(" | "),
            )
        }

        val result = params["result"] ?: params["Result"] ?: params["event"]
        return if (result == null) {
            PaymentResult.Failure("Adyen returned without a Terminal API response.")
        } else {
            PaymentResult.Failure(
                "Adyen returned short result '$result' without a Terminal API response. " +
                    "TapToPlay only records approved/refused states from full Terminal API payloads.",
            )
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
                .filterKeys { it !in preferredKeys && it !in setOf("result", "Result", "event", "pspReference", "reason", "message", "error") }
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
        val saleToPoi = runCatching {
            json.parseToJsonElement(responseJson).jsonObject["SaleToPOIResponse"]?.jsonObject
        }.getOrNull() ?: return null
        val serviceId = saleToPoi["MessageHeader"]?.jsonObject?.string("ServiceID")
        val responseKey = saleToPoi.keys.firstOrNull { it.endsWith("Response") && it != "MessageHeader" } ?: return null
        val terminalResponse = saleToPoi[responseKey]?.jsonObject ?: return null

        val response = terminalResponse["Response"]?.jsonObject
        val result = response?.string("Result")
        val errorCondition = response?.string("ErrorCondition")
        val additionalResponse = response?.string("AdditionalResponse")
        val insight = TerminalApiResponseInspector.inspect(responseJson)
        val transactionId = terminalResponse["POIData"]
            ?.jsonObject
            ?.get("POITransactionID")
            ?.jsonObject
            ?.string("TransactionID")
        val pspReference = insight?.let { TerminalApiResponseInspector.importantAdditional("pspReference", it) }
            ?: transactionId
        val refusalReason = insight?.let {
            TerminalApiResponseInspector.importantAdditional("refusalReason", it)
                ?: TerminalApiResponseInspector.importantAdditional("refusalReasonRaw", it)
                ?: TerminalApiResponseInspector.importantAdditional("message", it)
        }

        return when (result?.lowercase()) {
            "success" -> PaymentResult.Success(
                pspReference = pspReference,
                rawResult = result,
                responseJson = responseJson,
                serviceId = serviceId,
                terminalTransactionId = transactionId,
            )
            "failure" -> {
                val reason = listOfNotNull(errorCondition, refusalReason ?: additionalResponse).joinToString(" | ").ifBlank { null }
                if (errorCondition.equals("Refusal", ignoreCase = true) || errorCondition.equals("Refused", ignoreCase = true)) {
                    PaymentResult.Refused(reason, responseJson, serviceId)
                } else {
                    PaymentResult.Failure(reason ?: "Terminal API payment failed.", responseJson, serviceId)
                }
            }
            null -> PaymentResult.Failure("Terminal API response did not include Response.Result.", responseJson, serviceId)
            else -> PaymentResult.Failure("Terminal API returned $result.", responseJson, serviceId)
        }
    }

    private fun parseBoardingReturnData(encoded: String): BoardingReturnData? {
        val decoded = decodeBase64(encoded) ?: return null
        val root = runCatching { json.parseToJsonElement(decoded).jsonObject }.getOrNull() ?: return null
        return BoardingReturnData(
            boarded = root.boolean("boarded"),
            installationId = root.string("installationId"),
            date = root.string("date"),
            reboarding = root.boolean("reboarding"),
            boardingRequestToken = root.string("boardingRequestToken"),
            merchantAccountCode = root.string("merchantAccountCode"),
            merchantStoreCode = root.string("merchantStoreCode"),
        )
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

    private fun JsonObject.boolean(name: String): Boolean? =
        runCatching {
            val primitive = get(name)?.jsonPrimitive ?: return@runCatching null
            primitive.booleanOrNull ?: primitive.contentOrNull?.toBooleanStrictOrNull()
        }.getOrNull()

    private fun JsonElement.stringOrNull(): String? =
        runCatching { jsonPrimitive.contentOrNull }.getOrNull()

    private val json = Json { ignoreUnknownKeys = true }
}

fun adyenAppLinkAdvice(error: String?): String? = when {
    error.isNullOrBlank() -> null
    error.contains("03_013") || error.contains("03_014") ->
        "Nexo request security validation failed. Check Terminal API encryption, key version, key identifier, and required request fields."
    error.contains("03_015") ->
        "The Payments app could not decrypt the request. Recheck the terminal shared key and profile environment."
    error.contains("03_010") ->
        "The Payments app is missing the installation token. Check boarding status before charging."
    error.contains("03_006") || error.contains("03_007") || error.contains("03_008") ->
        "The Payments app could not resolve the requested App Link configuration. Check the environment and path."
    error.contains("03_012") ->
        "The Payments app could not verify the request. Restart the boarding flow."
    else -> null
}
