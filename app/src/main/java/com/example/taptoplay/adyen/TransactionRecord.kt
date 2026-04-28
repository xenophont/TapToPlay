package com.example.taptoplay.adyen

import kotlinx.serialization.Serializable

@Serializable
enum class TransactionStatus {
    LAUNCHED,
    APPROVED,
    REFUSED,
    FAILED,
}

@Serializable
data class TransactionRecord(
    val id: String,
    val createdAt: String,
    val amountLabel: String,
    val itemCount: Int,
    val saleToAcquirerDataName: String,
    val requestJson: String,
    val status: TransactionStatus = TransactionStatus.LAUNCHED,
    val responseUri: String? = null,
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

fun PaymentResult.failureReasonOrNull(): String? = when (this) {
    is PaymentResult.Refused -> reason ?: "Adyen refused the payment without a reason."
    is PaymentResult.Failure -> message
    else -> null
}
