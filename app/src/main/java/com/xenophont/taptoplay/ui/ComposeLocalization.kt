package com.xenophont.taptoplay.ui

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.xenophont.taptoplay.R
import com.xenophont.taptoplay.adyen.PaymentsAppStatus
import com.xenophont.taptoplay.adyen.TransactionRecord
import com.xenophont.taptoplay.adyen.TransactionStatus
import com.xenophont.taptoplay.adyen.pspReferenceOrNull
import com.xenophont.taptoplay.catalog.Product
import com.xenophont.taptoplay.profiles.PaymentEnvironment
import java.util.Locale

@Composable
internal fun ProvideLocalizedResources(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    val baseContext = LocalContext.current
    val baseConfiguration = LocalConfiguration.current
    val localizedConfiguration = remember(language, baseConfiguration) {
        Configuration(baseConfiguration).apply {
            setLocale(Locale.forLanguageTag(language.tag))
        }
    }
    val localizedContext = remember(baseContext, language, localizedConfiguration) {
        baseContext.createConfigurationContext(localizedConfiguration)
    }
    CompositionLocalProvider(
        LocalConfiguration provides localizedConfiguration,
        LocalContext provides localizedContext,
        content = content,
    )
}

@Composable
internal fun Product.localizedName(): String =
    TapToPlayProductNameResources[id]?.let { stringResource(it) } ?: name

@Composable
internal fun Product.localizedDescription(): String =
    TapToPlayProductDescriptionResources[id]?.let { stringResource(it) } ?: description

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
internal fun TransactionRecord.localizedSummary(): String {
    val pspSuffix = pspReferenceOrNull()?.let { " | PSP $it" }.orEmpty()
    return when (status) {
        TransactionStatus.LAUNCHED -> stringResource(R.string.transaction_status_pending)
        TransactionStatus.APPROVED -> {
            if (refundOfTransactionId == null) {
                stringResource(R.string.summary_approved, pspSuffix)
            } else {
                stringResource(R.string.refund_approved) + pspSuffix
            }
        }
        TransactionStatus.REFUSED -> stringResource(R.string.summary_refused, failureReason?.let { " | $it" }.orEmpty())
        TransactionStatus.FAILED -> failureReason?.let { stringResource(R.string.summary_failed, it) }
            ?: stringResource(R.string.transaction_status_failed)
        TransactionStatus.REFUND_LAUNCHED -> stringResource(R.string.transaction_status_refunding)
        TransactionStatus.REFUNDED -> stringResource(R.string.refund_approved) + pspSuffix
    }
}

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
