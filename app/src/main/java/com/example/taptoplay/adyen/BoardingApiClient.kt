package com.example.taptoplay.adyen

import com.example.taptoplay.profiles.AdyenProfile
import com.example.taptoplay.profiles.PaymentEnvironment
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class BoardingApiClient(
    private val client: OkHttpClient = OkHttpClient(),
) {
    fun createBoardingToken(profile: AdyenProfile, boardingRequestToken: String): Result<BoardingTokenResponse> = runCatching {
        val body = """{"boardingRequestToken":"${boardingRequestToken.escapeJson()}"}"""
        val request = Request.Builder()
            .url(boardingUrl(profile))
            .addHeader("X-API-Key", profile.apiKey)
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error("Adyen boarding token request failed (${response.code}): $responseBody")
            }
            Regex("\"boardingToken\"\\s*:\\s*\"([^\"]+)\"")
                .find(responseBody)
                ?.groupValues
                ?.get(1)
                ?.let { token ->
                    BoardingTokenResponse(
                        installationId = Regex("\"installationId\"\\s*:\\s*\"([^\"]+)\"")
                            .find(responseBody)
                            ?.groupValues
                            ?.get(1),
                        boardingToken = token,
                    )
                }
                ?: error("Adyen response did not contain boardingToken")
        }
    }

    private fun boardingUrl(profile: AdyenProfile): String {
        val merchantBase = "${managementBase(profile.environment)}/v1/merchants/${profile.merchantId}"
        return if (profile.storeId.isNullOrBlank()) {
            "$merchantBase/generatePaymentsAppBoardingToken"
        } else {
            "$merchantBase/stores/${profile.storeId}/generatePaymentsAppBoardingToken"
        }
    }

    private fun managementBase(environment: PaymentEnvironment): String = when (environment) {
        PaymentEnvironment.TEST -> "https://management-test.adyen.com"
        PaymentEnvironment.LIVE -> "https://management-live.adyen.com"
    }

    private fun String.escapeJson(): String = replace("\\", "\\\\").replace("\"", "\\\"")
}

data class BoardingTokenResponse(
    val installationId: String?,
    val boardingToken: String,
)
