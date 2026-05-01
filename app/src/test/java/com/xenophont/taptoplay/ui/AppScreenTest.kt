package com.xenophont.taptoplay.ui

import com.xenophont.taptoplay.adyen.PaymentResult
import com.xenophont.taptoplay.adyen.PaymentsAppInstance
import com.xenophont.taptoplay.adyen.PaymentsAppStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class AppScreenTest {
    @Test
    fun boardingReturnsStayOnPaymentsApp() {
        val result = PaymentResult.BoardingStatus(
            boarded = false,
            installationId = null,
            boardingRequestToken = "request-token",
            error = null,
            data = null,
        )

        assertEquals(AppScreen.PaymentsApp, screenForAdyenReturn(result))
    }

    @Test
    fun paymentReturnsOpenTransactions() {
        val result = PaymentResult.Success(
            pspReference = "psp-1",
            rawResult = "Success",
            serviceId = "svc-1",
        )

        assertEquals(AppScreen.Transactions, screenForAdyenReturn(result))
    }

    @Test
    fun failedPaymentReturnsOpenTransactions() {
        val result = PaymentResult.Failure(
            message = "Terminal API payment failed.",
            serviceId = "svc-1",
        )

        assertEquals(AppScreen.Transactions, screenForAdyenReturn(result))
    }

    @Test
    fun paymentsAppInstancesHideRevokedAndKeepCurrentFirst() {
        val instances = listOf(
            instance("active-1", PaymentsAppStatus.BOARDED),
            instance("revoked-1", PaymentsAppStatus.REVOKED),
            instance("current", PaymentsAppStatus.BOARDED),
            instance("revoked-2", PaymentsAppStatus.REVOKED),
        )

        val visible = displayedPaymentsAppInstances(instances, currentInstallationId = "current", showRevoked = false)
        val withRevoked = displayedPaymentsAppInstances(instances, currentInstallationId = "current", showRevoked = true)

        assertEquals(listOf("current", "active-1"), visible.map { it.installationId })
        assertEquals(listOf("current", "active-1", "revoked-1", "revoked-2"), withRevoked.map { it.installationId })
    }

    private fun instance(id: String, status: PaymentsAppStatus) = PaymentsAppInstance(
        installationId = id,
        merchantAccountCode = "merchant",
        merchantStoreCode = "store",
        status = status,
    )
}
