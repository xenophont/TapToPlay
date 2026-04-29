package com.example.taptoplay.ui

import com.example.taptoplay.adyen.PaymentResult

internal enum class AppScreen(val label: String) {
    Catalog("Catalog"),
    Checkout("Checkout"),
    PaymentsApp("Payments App"),
    Transactions("Transactions"),
    Diagnostics("Diagnostics"),
}

internal fun screenForAdyenReturn(result: PaymentResult): AppScreen = when (result) {
    is PaymentResult.BoardingStatus -> AppScreen.PaymentsApp
    is PaymentResult.Success,
    is PaymentResult.Refused,
    is PaymentResult.Failure -> AppScreen.Transactions
}

internal const val PAYMENTS_APP_REFRESH_INTERVAL_MS = 5 * 60 * 1000L

internal fun shouldRefreshPaymentsAppInstances(lastEnteredAtMillis: Long?, nowMillis: Long): Boolean =
    lastEnteredAtMillis == null || nowMillis - lastEnteredAtMillis > PAYMENTS_APP_REFRESH_INTERVAL_MS
