package com.example.taptoplay.adyen

import android.net.Uri
import java.net.URLDecoder

sealed interface PaymentResult {
    data class BoardingStatus(
        val boarded: Boolean,
        val installationId: String?,
        val boardingRequestToken: String?,
        val error: String?,
        val data: String?,
    ) : PaymentResult

    data class Success(val pspReference: String?, val rawResult: String?) : PaymentResult
    data class Refused(val reason: String?) : PaymentResult
    data class Failure(val message: String) : PaymentResult
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

    private fun parseQuery(query: String): Map<String, String> =
        query.split("&")
            .filter { it.isNotBlank() }
            .mapNotNull {
                val parts = it.split("=", limit = 2)
                if (parts.isEmpty()) null else parts[0].decode() to parts.getOrElse(1) { "" }.decode()
            }
            .toMap()

    private fun String.decode(): String = URLDecoder.decode(this, Charsets.UTF_8.name())
}
