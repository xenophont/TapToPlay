package com.example.taptoplay.adyen

import kotlinx.serialization.Serializable

@Serializable
enum class TransactionStatus {
    LAUNCHED,
    APPROVED,
    REFUSED,
    FAILED,
    REFUND_LAUNCHED,
    REFUNDED,
}

@Serializable
data class TransactionRecord(
    val id: String,
    val createdAt: String,
    val amountLabel: String,
    val amountMinor: Long? = null,
    val itemCount: Int,
    val saleToAcquirerDataName: String,
    val requestJson: String,
    val serviceId: String? = null,
    val saleTransactionId: String? = null,
    val messageCategory: String? = null,
    val profileId: String? = null,
    val installationId: String? = null,
    val pspReference: String? = null,
    val adyenTransactionId: String? = null,
    val refundOfTransactionId: String? = null,
    val status: TransactionStatus = TransactionStatus.LAUNCHED,
    val responseUri: String? = null,
    val responseBody: String? = null,
    val responseSummary: String? = null,
    val failureReason: String? = null,
)

fun PaymentResult.toTransactionStatus(): TransactionStatus = when (this) {
    is PaymentResult.Success -> TransactionStatus.APPROVED
    is PaymentResult.Refused -> TransactionStatus.REFUSED
    is PaymentResult.Failure -> TransactionStatus.FAILED
    is PaymentResult.BoardingStatus -> TransactionStatus.LAUNCHED
}

fun PaymentResult.toTransactionSummary(): String = when (this) {
    is PaymentResult.Success -> "Approved${pspReference?.let { " | PSP $it" }.orEmpty()}"
    is PaymentResult.Refused -> "Refused${reason?.let { " | $it" }.orEmpty()}"
    is PaymentResult.Failure -> "Failed | $message"
    is PaymentResult.BoardingStatus -> "Boarding response"
}

fun PaymentResult.transactionIdOrNull(): String? = when (this) {
    is PaymentResult.Success -> terminalTransactionId ?: pspReference
    else -> null
}

fun PaymentResult.pspReferenceOrNull(): String? = when (this) {
    is PaymentResult.Success -> pspReference
    else -> null
}

fun TransactionRecord.pspReferenceOrNull(): String? =
    pspReference
        ?: TerminalApiResponseInspector.inspect(responseBody)
            ?.let { TerminalApiResponseInspector.importantAdditional("pspReference", it) }

fun PaymentResult.serviceIdOrNull(): String? = when (this) {
    is PaymentResult.Success -> serviceId
    is PaymentResult.Refused -> serviceId
    is PaymentResult.Failure -> serviceId
    is PaymentResult.BoardingStatus -> null
}

fun PaymentResult.failureReasonOrNull(): String? = when (this) {
    is PaymentResult.Refused -> reason ?: "Adyen refused the payment without a reason."
    is PaymentResult.Failure -> message
    else -> null
}

fun PaymentResult.responseJsonOrNull(): String? = when (this) {
    is PaymentResult.Success -> responseJson
    is PaymentResult.Refused -> responseJson
    is PaymentResult.Failure -> responseJson
    is PaymentResult.BoardingStatus -> null
}
