package com.xenophont.taptoplay.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.xenophont.taptoplay.R
import com.xenophont.taptoplay.adyen.PaymentsAppStatus
import com.xenophont.taptoplay.adyen.TransactionStatus
import com.xenophont.taptoplay.catalog.Product
import com.xenophont.taptoplay.profiles.PaymentEnvironment

@Composable
internal fun Product.localizedName(): String =
    stringResource(TapToPlayProductNameResources[id] ?: R.string.app_name)

@Composable
internal fun Product.localizedDescription(): String =
    stringResource(TapToPlayProductDescriptionResources[id] ?: R.string.app_name)

@Composable
internal fun categoryLabel(category: String): String =
    TapToPlayCategoryResources[category]?.let { stringResource(it) } ?: category

@Composable
internal fun AppScreen.localizedLabel(): String = stringResource(
    when (this) {
        AppScreen.Catalog -> R.string.screen_catalog
        AppScreen.Checkout -> R.string.screen_checkout
        AppScreen.PaymentsApp -> R.string.screen_payments_app
        AppScreen.Transactions -> R.string.screen_transactions
        AppScreen.Diagnostics -> R.string.screen_diagnostics
        AppScreen.Language -> R.string.screen_language
        AppScreen.About -> R.string.screen_about
    },
)

@Composable
internal fun PaymentEnvironment.localizedLabel(): String = stringResource(environmentLabelRes())

@StringRes
internal fun PaymentEnvironment.environmentLabelRes(): Int = when (this) {
    PaymentEnvironment.TEST -> R.string.environment_test
    PaymentEnvironment.LIVE -> R.string.environment_live
}

@Composable
internal fun PaymentsAppStatus.localizedLabel(): String = stringResource(
    when (this) {
        PaymentsAppStatus.BOARDING -> R.string.payments_app_status_boarding
        PaymentsAppStatus.BOARDED -> R.string.payments_app_status_boarded
        PaymentsAppStatus.REVOKED -> R.string.payments_app_status_revoked
        PaymentsAppStatus.UNKNOWN -> R.string.payments_app_status_unknown
    },
)

@Composable
internal fun TransactionStatus.localizedLabel(): String = stringResource(
    when (this) {
        TransactionStatus.LAUNCHED -> R.string.transaction_status_pending
        TransactionStatus.APPROVED -> R.string.transaction_status_approved
        TransactionStatus.REFUSED -> R.string.transaction_status_refused
        TransactionStatus.FAILED -> R.string.transaction_status_failed
        TransactionStatus.REFUND_LAUNCHED -> R.string.transaction_status_refunding
        TransactionStatus.REFUNDED -> R.string.transaction_status_refunded
    },
)

@Composable
internal fun itemCountLabel(count: Int): String =
    pluralStringResource(R.plurals.item_count, count, count)

@Composable
internal fun savedPaymentAttemptsLabel(count: Int): String =
    pluralStringResource(R.plurals.saved_attempt, count, count)

@Composable
internal fun jsonFieldCountLabel(count: Int): String =
    pluralStringResource(R.plurals.json_field_count, count, count)

@Composable
internal fun saleToAcquirerDataSummary(name: String, fieldCount: Int): String = stringResource(
    R.string.sale_to_acquirer_data_summary,
    name,
    pluralStringResource(R.plurals.json_field_count, fieldCount, fieldCount),
)

@Composable
internal fun secretMask(value: String): String = when {
    value.isBlank() -> stringResource(R.string.not_set)
    value.length <= 8 -> "****"
    else -> value.take(4) + "..." + value.takeLast(4)
}

@Composable
internal fun passphraseMask(value: String): String =
    if (value.isBlank()) stringResource(R.string.not_set) else stringResource(R.string.secret_set_hidden)

@Composable
internal fun maskedIdentifier(value: String?): String = when {
    value.isNullOrBlank() -> stringResource(R.string.not_set)
    value.length <= 8 -> "****"
    else -> value.take(4) + "..." + value.takeLast(4)
}
