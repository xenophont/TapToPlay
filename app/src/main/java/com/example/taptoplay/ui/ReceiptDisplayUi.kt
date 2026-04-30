package com.example.taptoplay.ui

import com.example.taptoplay.adyen.PaymentReceipt
import com.example.taptoplay.adyen.ReceiptLine
import java.net.URLDecoder
import java.util.Locale

internal data class ReceiptDisplay(
    val title: String,
    val requiredSignature: Boolean,
    val items: List<ReceiptDisplayItem>,
)

internal sealed class ReceiptDisplayItem {
    data class TextLine(
        val text: String,
        val alignment: ReceiptTextAlignment,
        val bold: Boolean,
    ) : ReceiptDisplayItem()

    data class Row(
        val label: String,
        val value: String,
        val emphasis: ReceiptRowEmphasis = ReceiptRowEmphasis.Normal,
    ) : ReceiptDisplayItem()

    data class Status(val text: String) : ReceiptDisplayItem()
    data class Total(val label: String, val value: String) : ReceiptDisplayItem()
    data class Note(val text: String) : ReceiptDisplayItem()
    data class SignatureLine(val label: String) : ReceiptDisplayItem()
    data class QrCode(val value: String) : ReceiptDisplayItem()
    data object Separator : ReceiptDisplayItem()
}

internal enum class ReceiptTextAlignment {
    Start,
    Center,
    End,
}

internal enum class ReceiptRowEmphasis {
    Normal,
    Secondary,
    Technical,
}

private data class ReceiptEntry(
    val key: String,
    val value: String,
    val label: String? = null,
)

internal fun PaymentReceipt.toReceiptDisplay(strings: TapToPlayStrings): ReceiptDisplay {
    val items = displayLines()
        .mapNotNull { line -> line.toReceiptDisplayItem(strings) }
        .let { parsed ->
            if (requiredSignature && parsed.none { it is ReceiptDisplayItem.SignatureLine }) {
                parsed + ReceiptDisplayItem.Separator + ReceiptDisplayItem.SignatureLine(strings["signature"])
            } else {
                parsed
            }
        }
    return ReceiptDisplay(
        title = documentQualifier.receiptTitle(strings),
        requiredSignature = requiredSignature,
        items = items,
    )
}

private fun PaymentReceipt.displayLines(): List<ReceiptLine> {
    val rendered = mutableListOf<ReceiptLine>()
    var pending: ReceiptLine? = null
    lines.forEach { line ->
        val current = pending
        pending = if (current == null) {
            line
        } else {
            current.copy(
                text = current.text + line.text,
                alignment = current.alignment ?: line.alignment,
                characterStyle = current.characterStyle ?: line.characterStyle,
                endOfLine = line.endOfLine,
            )
        }
        if (line.endOfLine) {
            pending?.let(rendered::add)
            pending = null
        }
    }
    pending?.let(rendered::add)
    return rendered
}

private fun ReceiptLine.toReceiptDisplayItem(strings: TapToPlayStrings): ReceiptDisplayItem? {
    val text = text.trim()
    if (text.isBlank()) return null
    val entry = text.receiptEntry()
    if (entry == null) {
        return ReceiptDisplayItem.TextLine(
            text = text,
            alignment = alignment.receiptAlignment(),
            bold = characterStyle?.contains("bold", ignoreCase = true) == true,
        )
    }
    val (rawKey, rawValue, rawLabel) = entry
    val key = rawKey.canonicalReceiptKey()
    val value = rawValue.trim()
    if (key == "key" && value.canonicalReceiptKey() in receiptHiddenKeyValues) return null
    return when {
        key.startsWith("header") || key in receiptTitleKeys -> ReceiptDisplayItem.TextLine(
            text = value,
            alignment = ReceiptTextAlignment.Center,
            bold = key == "header1" || key in receiptTitleKeys,
        )
        key == "cardholderheader" -> ReceiptDisplayItem.TextLine(
            text = value.uppercase(strings.locale),
            alignment = ReceiptTextAlignment.Center,
            bold = true,
        )
        key in receiptStatusKeys -> ReceiptDisplayItem.Status(
            text = value.ifBlank { strings.receiptStatusLabel(key) },
        )
        key == "totalamount" -> ReceiptDisplayItem.Total(strings["total"], value)
        key == "filler" -> ReceiptDisplayItem.Separator
        key in receiptFooterKeys -> ReceiptDisplayItem.Note(value)
        key in receiptSignatureKeys -> ReceiptDisplayItem.SignatureLine(strings.receiptDisplayLabel(rawKey))
        key == "qrcode" -> ReceiptDisplayItem.QrCode(value)
        else -> ReceiptDisplayItem.Row(
            label = rawLabel?.takeIf { it.isNotBlank() } ?: strings.receiptDisplayLabel(rawKey),
            value = value,
            emphasis = key.receiptRowEmphasis(),
        )
    }
}

private fun String.receiptEntry(): ReceiptEntry? {
    val fields = parseReceiptFields()
    val hasStructuredReceiptKeys = fields.keys.any { it.equals("key", ignoreCase = true) } ||
        fields.keys.any { it.equals("name", ignoreCase = true) } ||
        fields.keys.any { it.equals("value", ignoreCase = true) }
    return if (hasStructuredReceiptKeys) {
        fields.structuredReceiptEntry()
    } else {
        simpleReceiptEntry()
    }
}

private fun Map<String, String>.structuredReceiptEntry(): ReceiptEntry? {
    val rawKey = valueFor("key") ?: return null
    val rawName = valueFor("name")
    val rawValue = valueFor("value")
    if (rawKey.canonicalReceiptKey() == "filler") return ReceiptEntry(rawKey, "")
    val value = rawValue?.takeIf { it.isNotBlank() }
        ?: rawName?.takeIf { it.isNotBlank() }
        ?: return null
    val label = rawName?.takeIf { rawValue?.isNotBlank() == true }
    return ReceiptEntry(rawKey, value, label)
}

private fun String.simpleReceiptEntry(): ReceiptEntry? {
    val separator = receiptSeparators.firstOrNull { contains(it) } ?: return null
    val parts = split(separator, limit = 2)
    val key = parts.getOrNull(0)?.trim().orEmpty()
    val value = parts.getOrNull(1)?.trim().orEmpty()
    return if (key.isBlank() || value.isBlank()) null else ReceiptEntry(key, value)
}

private fun String.parseReceiptFields(): Map<String, String> =
    split("&")
        .mapNotNull { field ->
            val parts = field.split("=", limit = 2)
            val key = parts.getOrNull(0)?.trim()?.urlDecodeOrSelf().orEmpty()
            val value = parts.getOrNull(1)?.trim()?.urlDecodeOrSelf().orEmpty()
            if (key.isBlank() || parts.size < 2) null else key to value
        }
        .toMap()

private fun Map<String, String>.valueFor(name: String): String? =
    entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value

private fun String.urlDecodeOrSelf(): String =
    runCatching { URLDecoder.decode(this, Charsets.UTF_8.name()) }.getOrElse { this }

private fun String?.receiptAlignment(): ReceiptTextAlignment = when {
    this == null -> ReceiptTextAlignment.Start
    contains("cent", ignoreCase = true) -> ReceiptTextAlignment.Center
    contains("right", ignoreCase = true) -> ReceiptTextAlignment.End
    else -> ReceiptTextAlignment.Start
}

private fun String.canonicalReceiptKey(): String =
    filter { it.isLetterOrDigit() }.lowercase(Locale.ROOT)

private fun String.receiptRowEmphasis(): ReceiptRowEmphasis = when (this) {
    in receiptTechnicalKeys -> ReceiptRowEmphasis.Technical
    in receiptSecondaryKeys -> ReceiptRowEmphasis.Secondary
    else -> ReceiptRowEmphasis.Normal
}

private fun String.receiptTitle(strings: TapToPlayStrings): String = when (this) {
    "CustomerReceipt", "SaleReceipt" -> strings["customer_receipt"]
    "CashierReceipt" -> strings["merchant_receipt"]
    else -> this
}

private fun TapToPlayStrings.receiptStatusLabel(key: String): String = when (key) {
    "approved" -> this["transaction_status_approved"]
    "refused" -> this["transaction_status_refused"]
    "void" -> this["receipt_status_voided"]
    else -> key.humanizeReceiptKey(locale)
}

private fun TapToPlayStrings.receiptDisplayLabel(key: String): String = when (key.canonicalReceiptKey()) {
    "originalamount" -> this["receipt_original_amount"]
    "shopperamount" -> this["receipt_shopper_amount"]
    "gratuityamount" -> this["receipt_tip_amount"]
    "surcharge" -> this["receipt_surcharge"]
    "cashbackamount" -> this["receipt_cashback_amount"]
    "charityamount" -> this["receipt_charity_amount"]
    "discountamount" -> this["receipt_discount_amount"]
    "txtype" -> this["receipt_type"]
    "paymentmethod" -> this["receipt_payment_method"]
    "paymentmethodvariant" -> this["receipt_payment_method_variant"]
    "cardtype" -> this["receipt_card"]
    "fundingsource" -> this["receipt_funding_source"]
    "producttype" -> this["receipt_product_type"]
    "posentrymode" -> this["receipt_pos_entry_mode"]
    "preferredname" -> this["receipt_application"]
    "pan" -> this["receipt_card_number"]
    "expirydate" -> this["receipt_expiry_date"]
    "cardholdername" -> this["receipt_cardholder_name"]
    "accounttype" -> this["receipt_account_type"]
    "authcode" -> this["receipt_authorisation"]
    "authorizationtype" -> this["receipt_authorization_type"]
    "authresponsecode" -> this["receipt_authorization_response"]
    "rrn" -> "RRN"
    "stan" -> "STAN"
    "mref" -> this["reference"].removeSuffix(": %s")
    "txref" -> this["receipt_transaction_reference"]
    "txdate" -> this["receipt_date"]
    "txtime" -> this["receipt_time"]
    "tid" -> this["receipt_terminal"]
    "ptid" -> "PTID"
    "mid" -> this["receipt_merchant_id"]
    "aid" -> "AID"
    "atc" -> "ATC"
    "aac" -> "AAC"
    "cid" -> "CID"
    "cvmres" -> this["receipt_cvm_result"]
    "additionalemvdata" -> this["receipt_additional_emv_data"]
    "panseq" -> this["receipt_pan_sequence"]
    "currentbalanceamount" -> this["receipt_current_balance"]
    "dccexpl" -> this["receipt_dcc_explanation"]
    "dccmarkup" -> this["receipt_dcc_markup"]
    "dccrate" -> this["receipt_dcc_rate"]
    "dccshopperamount" -> this["receipt_dcc_shopper_amount"]
    "dccsource" -> this["receipt_dcc_source"]
    "walletdccamount" -> this["receipt_wallet_dcc_amount"]
    "walletdccrate" -> this["receipt_wallet_dcc_rate"]
    "walletoperationtype" -> this["receipt_wallet_operation"]
    "wallettransactionreference" -> this["receipt_wallet_reference"]
    "tokentxvariant" -> this["receipt_token_variant"]
    "buyerid" -> this["receipt_buyer_id"]
    "cnpj" -> "CNPJ"
    "qrcode" -> this["receipt_qr_code"]
    "sigline", "signature", "merchantsigline" -> this["signature"]
    else -> key.humanizeReceiptKey(locale)
}

private fun String.humanizeReceiptKey(locale: Locale): String =
    replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .replace('_', ' ')
        .replace('-', ' ')
        .lowercase(locale)
        .replaceFirstChar { it.titlecase(locale) }

private val receiptSeparators = listOf(": ", " : ", "=")

private val receiptStatusKeys = setOf("approved", "refused", "void")

private val receiptFooterKeys = setOf("thanks", "retain")

private val receiptSignatureKeys = setOf("sigline", "signature", "merchantsigline")

private val receiptHiddenKeyValues = setOf("header1", "header2")

private val receiptTitleKeys = setOf(
    "merchanttitle",
    "shoppertitle",
    "customertitle",
)

private val receiptTechnicalKeys = setOf(
    "aac",
    "aid",
    "atc",
    "authresponsecode",
    "cid",
    "cvmres",
    "additionalemvdata",
    "panseq",
    "ptid",
)

private val receiptSecondaryKeys = setOf(
    "authorizationtype",
    "buyerid",
    "cnpj",
    "currentbalanceamount",
    "dccexpl",
    "dccmarkup",
    "dccrate",
    "dccshopperamount",
    "dccsource",
    "walletdccamount",
    "walletdccrate",
    "walletoperationtype",
    "wallettransactionreference",
    "tokentxvariant",
)
