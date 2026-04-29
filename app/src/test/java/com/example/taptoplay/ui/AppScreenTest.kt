package com.example.taptoplay.ui

import com.example.taptoplay.adyen.PaymentResult
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
    fun paymentsAppInstancesRefreshOnFirstEntry() {
        assertEquals(true, shouldRefreshPaymentsAppInstances(lastEnteredAtMillis = null, nowMillis = 1_000L))
    }

    @Test
    fun paymentsAppInstancesDoNotRefreshWithinFiveMinutes() {
        assertEquals(false, shouldRefreshPaymentsAppInstances(lastEnteredAtMillis = 1_000L, nowMillis = 301_000L))
    }

    @Test
    fun paymentsAppInstancesRefreshAfterMoreThanFiveMinutes() {
        assertEquals(true, shouldRefreshPaymentsAppInstances(lastEnteredAtMillis = 1_000L, nowMillis = 301_001L))
    }
}
