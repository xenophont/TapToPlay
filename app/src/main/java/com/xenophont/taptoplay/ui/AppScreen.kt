package com.xenophont.taptoplay.ui

import com.xenophont.taptoplay.adyen.PaymentResult

internal enum class AppScreen {
    Catalog,
    Checkout,
    PaymentsApp,
    Transactions,
    Diagnostics,
    Language,
    About,
}

internal fun screenForAdyenReturn(result: PaymentResult): AppScreen = when (result) {
    is PaymentResult.BoardingStatus -> AppScreen.PaymentsApp
    is PaymentResult.Success,
    is PaymentResult.Refused,
    is PaymentResult.Failure -> AppScreen.Transactions
}
