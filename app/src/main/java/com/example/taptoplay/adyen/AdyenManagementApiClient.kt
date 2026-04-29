package com.example.taptoplay.adyen

import com.example.taptoplay.profiles.AdyenProfile
import com.example.taptoplay.profiles.PaymentEnvironment
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import okhttp3.Call
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

class AdyenManagementApiClient(
    private val callFactory: Call.Factory = OkHttpClient(),
    private val baseUrlOverride: String? = null,
) {
    fun findStoreForProfile(profile: AdyenProfile): Result<AdyenStore?> = runCatching {
        val storeId = profile.storeId?.takeIf { it.isNotBlank() } ?: return@runCatching null
        var pageNumber = 1
        var pagesTotal = 1
        do {
            val root = json.parseToJsonElement(execute(storesRequest(profile, pageNumber))).jsonObject
            stores(root).firstOrNull { store ->
                store.id == storeId && (store.merchantId == null || store.merchantId == profile.merchantId)
            }?.let { return@runCatching it }
            pagesTotal = root.int("pagesTotal") ?: pagesTotal
            pageNumber += 1
        } while (pageNumber <= pagesTotal)
        null
    }

    private fun storesRequest(profile: AdyenProfile, pageNumber: Int): Request {
        val url = managementBase(profile.environment)
            .toHttpUrl()
            .newBuilder()
            .addPathSegment("v3")
            .addPathSegment("stores")
            .addQueryParameter("merchantId", profile.merchantId)
            .addQueryParameter("pageSize", MAX_PAGE_SIZE.toString())
            .addQueryParameter("pageNumber", pageNumber.toString())
            .build()
        return Request.Builder()
            .url(url)
            .addHeader("X-API-Key", profile.apiKey)
            .get()
            .build()
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

    private fun stores(root: JsonObject): List<AdyenStore> =
        (root["data"] as? JsonArray)
            ?.mapNotNull { item -> item as? JsonObject }
            ?.mapNotNull { store ->
                val id = store.string("id") ?: return@mapNotNull null
                AdyenStore(
                    id = id,
                    merchantId = store.string("merchantId"),
                    reference = store.string("reference"),
                    shopperStatement = store.string("shopperStatement"),
                    description = store.string("description"),
                    status = store.string("status"),
                )
            }
            .orEmpty()

    private fun managementBase(environment: PaymentEnvironment): String =
        baseUrlOverride ?: when (environment) {
            PaymentEnvironment.TEST -> "https://management-test.adyen.com"
            PaymentEnvironment.LIVE -> "https://management-live.adyen.com"
        }

    private fun JsonObject.string(name: String): String? =
        (get(name) as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.int(name: String): Int? =
        string(name)?.toIntOrNull()

    companion object {
        private const val MAX_PAGE_SIZE = 100
        private val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
}

data class AdyenStore(
    val id: String,
    val merchantId: String?,
    val reference: String?,
    val shopperStatement: String?,
    val description: String?,
    val status: String?,
) {
    val profileName: String
        get() = listOf(reference, shopperStatement, description, id)
            .firstOrNull { !it.isNullOrBlank() }
            .orEmpty()
}
