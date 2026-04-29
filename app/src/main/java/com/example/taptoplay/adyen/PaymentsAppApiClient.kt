package com.example.taptoplay.adyen

import com.example.taptoplay.profiles.AdyenProfile
import com.example.taptoplay.profiles.PaymentEnvironment
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class PaymentsAppApiClient(
    private val callFactory: Call.Factory = OkHttpClient(),
    private val baseUrlOverride: String? = null,
) {
    fun createBoardingToken(profile: AdyenProfile, boardingRequestToken: String): Result<BoardingTokenResponse> =
        runCatching {
            val body = json.encodeToString(
                JsonObject.serializer(),
                buildJsonObject {
                    put("boardingRequestToken", boardingRequestToken)
                },
            )
            val responseBody = execute(
                Request.Builder()
                    .url(scopedPaymentsAppUrl(profile, "generatePaymentsAppBoardingToken"))
                    .addHeader("X-API-Key", profile.apiKey)
                    .post(body.toRequestBody(JSON_MEDIA_TYPE))
                    .build(),
            )
            val root = json.parseToJsonElement(responseBody).jsonObject
            BoardingTokenResponse(
                installationId = root.string("installationId"),
                boardingToken = root.string("boardingToken")
                    ?: error("Adyen response did not contain boardingToken"),
            )
        }

    fun listPaymentsApps(profile: AdyenProfile): Result<List<PaymentsAppInstance>> = runCatching {
        val responseBody = execute(
            Request.Builder()
                .url(
                    scopedPaymentsAppUrl(profile, "paymentsApps")
                        .newBuilder()
                        .addQueryParameter("limit", "100")
                        .build(),
                )
                .addHeader("X-API-Key", profile.apiKey)
                .get()
                .build(),
        )
        val root = json.parseToJsonElement(responseBody).jsonObject
        (root["paymentsApps"] as? JsonArray)
            ?.mapNotNull { item -> item as? JsonObject }
            ?.map { app ->
                PaymentsAppInstance(
                    installationId = app.string("installationId").orEmpty(),
                    merchantAccountCode = app.string("merchantAccountCode"),
                    merchantStoreCode = app.string("merchantStoreCode"),
                    status = PaymentsAppStatus.fromWireValue(app.string("status")),
                )
            }
            ?.filter { it.installationId.isNotBlank() }
            .orEmpty()
    }

    fun revokePaymentsApp(profile: AdyenProfile, installationId: String): Result<Unit> = runCatching {
        execute(
            Request.Builder()
                .url(merchantPaymentsAppUrl(profile, "paymentsApps", installationId, "revoke"))
                .addHeader("X-API-Key", profile.apiKey)
                .post(ByteArray(0).toRequestBody(null))
                .build(),
        )
        Unit
    }

    private fun execute(request: Request): String {
        callFactory.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw AdyenApiException(AdyenApiError.from(response.code, responseBody))
            }
            return responseBody
        }
    }

    private fun scopedPaymentsAppUrl(profile: AdyenProfile, vararg pathSegments: String): HttpUrl {
        val builder = merchantUrlBuilder(profile)
        if (!profile.storeId.isNullOrBlank()) {
            builder.addPathSegment("stores").addPathSegment(profile.storeId)
        }
        pathSegments.forEach(builder::addPathSegment)
        return builder.build()
    }

    private fun merchantPaymentsAppUrl(profile: AdyenProfile, vararg pathSegments: String): HttpUrl {
        val builder = merchantUrlBuilder(profile)
        pathSegments.forEach(builder::addPathSegment)
        return builder.build()
    }

    private fun merchantUrlBuilder(profile: AdyenProfile): HttpUrl.Builder =
        managementBase(profile.environment)
            .toHttpUrl()
            .newBuilder()
            .addPathSegment("v1")
            .addPathSegment("merchants")
            .addPathSegment(profile.merchantId)

    private fun managementBase(environment: PaymentEnvironment): String =
        baseUrlOverride ?: when (environment) {
            PaymentEnvironment.TEST -> "https://management-test.adyen.com"
            PaymentEnvironment.LIVE -> "https://management-live.adyen.com"
        }

    private fun JsonObject.string(name: String): String? =
        (get(name) as? JsonPrimitive)?.contentOrNull

    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
        private val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
}

data class BoardingTokenResponse(
    val installationId: String?,
    val boardingToken: String,
)

data class PaymentsAppInstance(
    val installationId: String,
    val merchantAccountCode: String?,
    val merchantStoreCode: String?,
    val status: PaymentsAppStatus,
)

enum class PaymentsAppStatus {
    BOARDING,
    BOARDED,
    REVOKED,
    UNKNOWN;

    companion object {
        fun fromWireValue(value: String?): PaymentsAppStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
    }
}

data class AdyenApiError(
    val statusCode: Int,
    val errorCode: String?,
    val title: String?,
    val detail: String?,
    val requestId: String?,
    val rawBody: String,
) {
    val safeMessage: String
        get() = listOfNotNull(
            title,
            detail,
            errorCode?.let { "code $it" },
            requestId?.let { "request $it" },
        ).joinToString(" | ").ifBlank { "HTTP $statusCode" }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun from(statusCode: Int, rawBody: String): AdyenApiError {
            val root = runCatching { json.parseToJsonElement(rawBody).jsonObject }.getOrNull()
            return AdyenApiError(
                statusCode = statusCode,
                errorCode = root?.string("errorCode"),
                title = root?.string("title"),
                detail = root?.string("detail"),
                requestId = root?.string("requestId"),
                rawBody = rawBody,
            )
        }

        private fun JsonObject.string(name: String): String? =
            (get(name) as? JsonPrimitive)?.contentOrNull
    }
}

class AdyenApiException(
    val error: AdyenApiError,
) : RuntimeException("Adyen API failed (${error.statusCode}): ${error.safeMessage}")
