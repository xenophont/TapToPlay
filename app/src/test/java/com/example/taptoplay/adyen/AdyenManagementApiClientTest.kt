package com.example.taptoplay.adyen

import com.example.taptoplay.profiles.AdyenProfile
import com.example.taptoplay.profiles.PaymentEnvironment
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Timeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdyenManagementApiClientTest {
    @Test
    fun findsStoreNameFromManagementStoreList() {
        val factory = ManagementFakeCallFactory(
            200 to """
                {
                  "data": [
                    {
                      "id": "ST322LJ223223K5F",
                      "merchantId": "merchant",
                      "reference": "Boutique Centro",
                      "shopperStatement": "Statement Name",
                      "description": "Madrid Centro"
                    }
                  ],
                  "pagesTotal": 1
                }
            """.trimIndent(),
        )
        val client = AdyenManagementApiClient(factory, baseUrlOverride = "https://management-test.adyen.com")

        val store = client.findStoreForProfile(profile(storeId = "ST322LJ223223K5F")).getOrThrow()

        assertEquals("Boutique Centro", store?.profileName)
        assertEquals("/v3/stores", factory.requests.single().url.encodedPath)
        assertEquals("merchant", factory.requests.single().url.queryParameter("merchantId"))
        assertEquals("100", factory.requests.single().url.queryParameter("pageSize"))
        assertEquals("1", factory.requests.single().url.queryParameter("pageNumber"))
        assertEquals("api", factory.requests.single().header("X-API-Key"))
    }

    @Test
    fun usesReferenceAsStoreProfileName() {
        val factory = ManagementFakeCallFactory(
            200 to """
                {
                  "data": [
                    {
                      "id": "ST322LJ223223K5F",
                      "merchantId": "merchant",
                      "reference": "Real Store Name",
                      "shopperStatement": "Receipt Name",
                      "description": "Description Name"
                    }
                  ],
                  "pagesTotal": 1
                }
            """.trimIndent(),
        )
        val client = AdyenManagementApiClient(factory, baseUrlOverride = "https://management-test.adyen.com")

        val store = client.findStoreForProfile(profile(storeId = "ST322LJ223223K5F")).getOrThrow()

        assertEquals("Real Store Name", store?.profileName)
    }

    @Test
    fun paginatesUntilMatchingStoreIsFound() {
        val factory = ManagementFakeCallFactory(
            200 to """
                {
                  "data": [
                    {"id": "ST-other", "merchantId": "merchant", "shopperStatement": "Other"}
                  ],
                  "pagesTotal": 2
                }
            """.trimIndent(),
            200 to """
                {
                  "data": [
                    {"id": "ST-target", "merchantId": "merchant", "description": "Second Page Store"}
                  ],
                  "pagesTotal": 2
                }
            """.trimIndent(),
        )
        val client = AdyenManagementApiClient(factory, baseUrlOverride = "https://management-test.adyen.com")

        val store = client.findStoreForProfile(profile(storeId = "ST-target")).getOrThrow()

        assertEquals("Second Page Store", store?.profileName)
        assertEquals(2, factory.requests.size)
        assertEquals("1", factory.requests[0].url.queryParameter("pageNumber"))
        assertEquals("2", factory.requests[1].url.queryParameter("pageNumber"))
    }

    @Test
    fun skipsLookupForMerchantScopedProfile() {
        val factory = ManagementFakeCallFactory()
        val client = AdyenManagementApiClient(factory, baseUrlOverride = "https://management-test.adyen.com")

        val store = client.findStoreForProfile(profile(storeId = null)).getOrThrow()

        assertNull(store)
        assertTrue(factory.requests.isEmpty())
    }

    @Test
    fun returnsTypedAdyenApiErrors() {
        val factory = ManagementFakeCallFactory(
            403 to """{"title":"Forbidden","detail":"Missing Stores read role","errorCode":"901"}""",
        )
        val client = AdyenManagementApiClient(factory, baseUrlOverride = "https://management-test.adyen.com")

        val result = client.findStoreForProfile(profile(storeId = "ST-target"))

        assertTrue(result.isFailure)
        val error = (result.exceptionOrNull() as AdyenApiException).error
        assertEquals(403, error.statusCode)
        assertEquals("901", error.errorCode)
        assertTrue(error.safeMessage.contains("Missing Stores read role"))
    }

    private fun profile(storeId: String? = null) = AdyenProfile(
        displayName = "Demo",
        environment = PaymentEnvironment.TEST,
        merchantId = "merchant",
        storeId = storeId,
        apiKey = "api",
        clientKey = "client",
        terminalKeyIdentifier = "key",
        terminalKeyVersion = 1,
        terminalPassphrase = "passphrase",
        currency = "EUR",
        countryCode = "ES",
    )
}

private class ManagementFakeCallFactory(
    vararg responses: Pair<Int, String>,
) : Call.Factory {
    val requests = mutableListOf<Request>()
    private val responses = responses.toMutableList()

    override fun newCall(request: Request): Call {
        requests += request
        val (code, body) = responses.removeAt(0)
        return ManagementFakeCall(request, code, body)
    }
}

private class ManagementFakeCall(
    private val request: Request,
    private val code: Int,
    private val body: String,
) : Call {
    private var executed = false

    override fun request(): Request = request

    override fun execute(): Response {
        executed = true
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code in 200..299) "OK" else "Error")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
    }

    override fun enqueue(responseCallback: Callback) {
        responseCallback.onResponse(this, execute())
    }

    override fun cancel() = Unit
    override fun isExecuted(): Boolean = executed
    override fun isCanceled(): Boolean = false
    override fun clone(): Call = ManagementFakeCall(request, code, body)
    override fun timeout(): Timeout = Timeout.NONE
}
