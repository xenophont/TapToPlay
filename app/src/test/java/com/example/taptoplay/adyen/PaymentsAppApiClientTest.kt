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
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentsAppApiClientTest {
    @Test
    fun createsStoreScopedBoardingToken() {
        val factory = FakeCallFactory(
            code = 200,
            body = """{"installationId":"install-1","boardingToken":"token-1"}""",
        )
        val client = PaymentsAppApiClient(factory, baseUrlOverride = "https://management-test.adyen.com")

        val response = client.createBoardingToken(profile(storeId = "store-1"), "request-token").getOrThrow()

        assertEquals("install-1", response.installationId)
        assertEquals("token-1", response.boardingToken)
        assertEquals("/v1/merchants/merchant/stores/store-1/generatePaymentsAppBoardingToken", factory.request.url.encodedPath)
        assertEquals("api", factory.request.header("X-API-Key"))
    }

    @Test
    fun listsPaymentsAppInstances() {
        val factory = FakeCallFactory(
            code = 200,
            body = """
                {
                  "paymentsApps": [
                    {
                      "installationId": "install-1",
                      "merchantAccountCode": "merchant",
                      "merchantStoreCode": "store",
                      "status": "BOARDED"
                    }
                  ]
                }
            """.trimIndent(),
        )
        val client = PaymentsAppApiClient(factory, baseUrlOverride = "https://management-test.adyen.com")

        val instances = client.listPaymentsApps(profile(storeId = "store")).getOrThrow()

        assertEquals(1, instances.size)
        assertEquals(PaymentsAppStatus.BOARDED, instances.first().status)
        assertEquals("/v1/merchants/merchant/stores/store/paymentsApps", factory.request.url.encodedPath)
        assertEquals("100", factory.request.url.queryParameter("limit"))
    }

    @Test
    fun revokesMerchantScopedInstallation() {
        val factory = FakeCallFactory(code = 200, body = "{}")
        val client = PaymentsAppApiClient(factory, baseUrlOverride = "https://management-test.adyen.com")

        client.revokePaymentsApp(profile(storeId = "store"), "install-1").getOrThrow()

        assertEquals("/v1/merchants/merchant/paymentsApps/install-1/revoke", factory.request.url.encodedPath)
        assertEquals("POST", factory.request.method)
    }

    @Test
    fun returnsTypedAdyenApiErrors() {
        val factory = FakeCallFactory(
            code = 403,
            body = """{"title":"Forbidden","detail":"Missing Payments App role","errorCode":"901"}""",
        )
        val client = PaymentsAppApiClient(factory, baseUrlOverride = "https://management-test.adyen.com")

        val result = client.listPaymentsApps(profile())

        assertTrue(result.isFailure)
        val error = (result.exceptionOrNull() as AdyenApiException).error
        assertEquals(403, error.statusCode)
        assertEquals("901", error.errorCode)
        assertTrue(error.safeMessage.contains("Missing Payments App role"))
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

private class FakeCallFactory(
    private val code: Int,
    private val body: String,
) : Call.Factory {
    lateinit var request: Request

    override fun newCall(request: Request): Call {
        this.request = request
        return FakeCall(request, code, body)
    }
}

private class FakeCall(
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
    override fun clone(): Call = FakeCall(request, code, body)
    override fun timeout(): Timeout = Timeout.NONE
}
