package com.example.taptoplay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.taptoplay.adyen.PaymentReceipt
import com.example.taptoplay.adyen.PaymentResult
import com.example.taptoplay.adyen.ReceiptLine
import com.example.taptoplay.adyen.TerminalApiRequestInsight
import com.example.taptoplay.adyen.TerminalApiRequestInspector
import com.example.taptoplay.adyen.TerminalApiResponseInsight
import com.example.taptoplay.adyen.TerminalApiResponseInspector
import com.example.taptoplay.adyen.TransactionRecord
import com.example.taptoplay.adyen.TransactionStatus

@Composable
internal fun TransactionHistoryPanel(
    records: List<TransactionRecord>,
    onInspect: (TransactionRecord) -> Unit,
    onClear: () -> Unit,
) {
    OutlinedCard(shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Transactions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (records.isEmpty()) "No payment attempts yet" else "${records.size} saved payment attempt${if (records.size == 1) "" else "s"}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onClear, enabled = records.isNotEmpty()) { Text("Clear") }
            }
            if (records.isEmpty()) {
                Text(
                    "Each checkout attempt will appear here with its Terminal API request and Adyen response.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                records.forEach { record ->
                    TransactionRow(record = record, onInspect = { onInspect(record) })
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(record: TransactionRecord, onInspect: () -> Unit) {
    OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(record.amountLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    TransactionStatusChip(record.status)
                }
                Text(
                    "${record.itemCount} item${if (record.itemCount == 1) "" else "s"} | ${record.saleToAcquirerDataName}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                record.failureReason?.let {
                    Text(
                        "Adyen issue: $it",
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            TextButton(onClick = onInspect) { Text("Inspect") }
        }
    }
}

@Composable
private fun TransactionStatusChip(status: TransactionStatus) {
    val label = when (status) {
        TransactionStatus.LAUNCHED -> "Pending"
        TransactionStatus.APPROVED -> "Approved"
        TransactionStatus.REFUSED -> "Refused"
        TransactionStatus.FAILED -> "Failed"
        TransactionStatus.REFUND_LAUNCHED -> "Refunding"
        TransactionStatus.REFUNDED -> "Refunded"
    }
    AssistChip(onClick = {}, label = { Text(label) })
}

@Composable
internal fun PaymentResultDialog(result: PaymentResult, isRefund: Boolean, onDismiss: () -> Unit) {
    val title = when (result) {
        is PaymentResult.BoardingStatus -> "Boarding returned"
        is PaymentResult.Success -> if (isRefund) "Refund approved" else "Payment approved"
        is PaymentResult.Refused -> "Payment refused"
        is PaymentResult.Failure -> "Adyen result"
    }
    val message = when (result) {
        is PaymentResult.BoardingStatus -> {
            val state = if (result.boarded) "boarded" else "not boarded"
            listOfNotNull(
                "Adyen app is $state. Installation ID: ${result.installationId ?: "not supplied"}",
                result.returnData?.let { data ->
                    listOfNotNull(
                        data.merchantAccountCode?.let { "Previous merchant: $it" },
                        data.merchantStoreCode?.let { "Previous store: $it" },
                        data.reboarding?.takeIf { it }?.let { "Reboarding flow started" },
                    ).joinToString(" | ").ifBlank { null }
                },
                result.errorAdvice,
            ).joinToString("\n")
        }
        is PaymentResult.Success -> "Reference: ${result.pspReference ?: "not supplied"}"
        is PaymentResult.Refused -> result.reason ?: "No refusal reason supplied."
        is PaymentResult.Failure -> result.message
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text(title) },
        text = { Text(message) },
    )
}

@Composable
internal fun TransactionDialog(
    record: TransactionRecord,
    onRefund: () -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedSection by remember { mutableStateOf("Request") }
    val requestInsight = remember(record.requestJson) { TerminalApiRequestInspector.inspect(record.requestJson) }
    val responseInsight = remember(record.responseBody) { TerminalApiResponseInspector.inspect(record.responseBody) }
    val highlights = remember(record.responseBody) { TerminalApiResponseInspector.compactSummary(record.responseBody) }
    val receipts = responseInsight?.receipts.orEmpty()
    val canRefund = record.status == TransactionStatus.APPROVED &&
        record.refundOfTransactionId == null &&
        (record.adyenTransactionId != null || responseInsight?.transactionId != null)
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize()
                .padding(vertical = 24.dp),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Transaction", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${record.amountLabel} | ${record.createdAt}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = onRefund, enabled = canRefund) { Text("Refund", maxLines = 1) }
                        TextButton(onClick = onDismiss) { Text("Close") }
                    }
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    FilterChip(
                        selected = selectedSection == "Request",
                        onClick = { selectedSection = "Request" },
                        label = { Text("Request") },
                    )
                    FilterChip(
                        selected = selectedSection == "Response",
                        onClick = { selectedSection = "Response" },
                        label = { Text("Response") },
                    )
                    FilterChip(
                        selected = selectedSection == "Receipt",
                        onClick = { selectedSection = "Receipt" },
                        enabled = record.responseBody != null,
                        label = { Text("Receipt") },
                    )
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                    item { TransactionStatusChip(record.status) }
                    when (selectedSection) {
                        "Request" -> {
                            item {
                                RequestSummary(record)
                            }
                            item {
                                Text("Terminal API request", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                            item {
                                DecodedSaleToAcquirerDataCard(requestInsight)
                            }
                            item { MonospaceBlock(record.requestJson) }
                        }
                        "Response" -> {
                            if (highlights.isNotEmpty()) {
                                item {
                                    OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Important response fields", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                            highlights.forEach { (label, value) ->
                                                KeyValueLine(label = label, value = value)
                                            }
                                        }
                                    }
                                }
                            }
                            record.failureReason?.let {
                                item {
                                    OutlinedCard(shape = RoundedCornerShape(8.dp)) {
                                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("Adyen failure detail", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                                            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                            item {
                                Text("Adyen response", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                            item {
                                Text(
                                    record.responseSummary ?: "No response has been received yet.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            responseInsight?.let { insight ->
                                item {
                                    ResponseFieldList(insight)
                                }
                            }
                            record.responseBody?.let { body ->
                                item {
                                    Text("Raw Terminal API response", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                }
                                item { MonospaceBlock(body) }
                            }
                            record.responseUri?.let { response ->
                                item {
                                    Text("Raw return URI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                }
                                item { MonospaceBlock(response) }
                            }
                        }
                        "Receipt" -> {
                            if (receipts.isEmpty()) {
                                item {
                                    Text(
                                        "No PaymentReceipt data was returned for this transaction.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            } else {
                                items(receipts) { receipt ->
                                    DigitalReceiptCard(receipt)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DigitalReceiptCard(receipt: PaymentReceipt) {
    val display = remember(receipt) { receipt.toDisplay() }
    OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(receipt.documentQualifier.receiptTitle(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Adyen-generated receipt data",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (receipt.requiredSignature) {
                    AssistChip(onClick = {}, label = { Text("Signature") })
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ReceiptHeader(display)
                display.total?.let { total ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Total", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(total, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                }
                display.status?.let { status ->
                    AssistChip(onClick = {}, label = { Text(status) })
                }
                if (display.details.isNotEmpty()) {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        display.details.forEach { (label, value) ->
                            ReceiptDetailLine(label, value)
                        }
                    }
                }
                if (display.footer.isNotEmpty()) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        display.footer.forEach { footer ->
                            Text(
                                footer,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptHeader(display: ReceiptDisplay) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            display.merchantName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        display.header.drop(1).forEach { header ->
            Text(
                header,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ReceiptDetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.42f))
        Text(value, textAlign = TextAlign.Right, fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.58f))
    }
}

private data class ReceiptDisplay(
    val merchantName: String,
    val header: List<String>,
    val status: String?,
    val total: String?,
    val details: List<Pair<String, String>>,
    val footer: List<String>,
)

private fun PaymentReceipt.displayLines(): List<ReceiptLine> {
    val rendered = mutableListOf<ReceiptLine>()
    var pending: ReceiptLine? = null
    lines.forEach { line ->
        val current = pending
        if (current == null) {
            pending = line
        } else {
            pending = current.copy(text = current.text + line.text)
        }
        if (line.endOfLine) {
            pending?.let(rendered::add)
            pending = null
        }
    }
    pending?.let(rendered::add)
    return rendered
}

private fun PaymentReceipt.toDisplay(): ReceiptDisplay {
    val lines = displayLines().map { it.text.trim() }.filter { it.isNotBlank() }
    val entries = lines.mapNotNull { it.receiptEntry() }
    val entryKeys = entries.map { it.first.normalizedReceiptKey() }.toSet()
    val headerEntries = entries.filter { it.first.normalizedReceiptKey().startsWith("header") }.map { it.second }
    val freeHeaders = lines
        .takeWhile { line -> line.receiptEntry()?.first?.normalizedReceiptKey()?.let { it in amountAndStatusKeys } != true }
        .filterNot { it.receiptEntry()?.first?.normalizedReceiptKey() in ignoredReceiptKeys }
        .filterNot { line -> entries.any { it.first.normalizedReceiptKey().startsWith("header") && it.second == line } }
        .take(3)
    val headers = (headerEntries + freeHeaders).distinct().ifEmpty { listOf("TapToPlay Boutique") }
    val total = entries.firstValue("totalAmount")
        ?: entries.firstValue("originalAmount")
        ?: entries.firstValue("shopperAmount")
    val status = when {
        entries.any { it.first.normalizedReceiptKey() == "approved" } -> "Approved"
        entries.any { it.first.normalizedReceiptKey() == "refused" } -> "Refused"
        entries.any { it.first.normalizedReceiptKey() == "void" } -> "Voided"
        else -> null
    }
    val details = receiptDetailOrder.mapNotNull { (key, label) ->
        entries.firstValue(key)?.let { label to it }
    }
    val footer = entries
        .filter { it.first.normalizedReceiptKey() in footerReceiptKeys }
        .map { it.second }
        .ifEmpty {
            lines.takeLast(2).filterNot { line ->
                line.receiptEntry()?.first?.normalizedReceiptKey() in entryKeys || line in headers
            }
        }
    return ReceiptDisplay(
        merchantName = headers.first(),
        header = headers,
        status = status,
        total = total,
        details = details,
        footer = footer,
    )
}

private fun String.receiptEntry(): Pair<String, String>? {
    val separators = listOf(": ", " : ", "=")
    val separator = separators.firstOrNull { contains(it) } ?: return null
    val parts = split(separator, limit = 2)
    val key = parts.getOrNull(0)?.trim().orEmpty()
    val value = parts.getOrNull(1)?.trim().orEmpty()
    return if (key.isBlank() || value.isBlank()) null else key to value
}

private fun List<Pair<String, String>>.firstValue(key: String): String? =
    firstOrNull { it.first.normalizedReceiptKey().equals(key, ignoreCase = true) }?.second

private fun String.normalizedReceiptKey(): String =
    filter { it.isLetterOrDigit() }.replaceFirstChar { it.lowercase() }

private val amountAndStatusKeys = setOf("totalAmount", "originalAmount", "shopperAmount", "approved", "refused", "void")

private val ignoredReceiptKeys = setOf("filler", "sigline", "signature", "merchantSigline")

private val footerReceiptKeys = setOf("thanks", "retain")

private val receiptDetailOrder = listOf(
    "txtype" to "Type",
    "paymentMethod" to "Payment method",
    "cardType" to "Card",
    "pan" to "Card number",
    "authCode" to "Authorisation",
    "txdate" to "Date",
    "txtime" to "Time",
    "mref" to "Reference",
    "txRef" to "Transaction reference",
    "tid" to "Terminal",
    "mid" to "Merchant ID",
    "rrn" to "RRN",
    "stan" to "STAN",
    "aid" to "AID",
)

private fun String.receiptTitle(): String = when (this) {
    "CustomerReceipt", "SaleReceipt" -> "Customer receipt"
    "CashierReceipt" -> "Merchant receipt"
    else -> this
}

@Composable
private fun RequestSummary(record: TransactionRecord) {
    OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Request summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            KeyValueLine("Amount", record.amountLabel)
            KeyValueLine("Items", record.itemCount.toString())
            record.messageCategory?.let { KeyValueLine("Message category", it) }
            record.serviceId?.let { KeyValueLine("ServiceID", it) }
            record.saleTransactionId?.let { KeyValueLine("Sale transaction", it) }
            record.adyenTransactionId?.let { KeyValueLine("Adyen transaction", it) }
            record.refundOfTransactionId?.let { KeyValueLine("Refund of", it) }
        }
    }
}

@Composable
private fun DecodedSaleToAcquirerDataCard(insight: TerminalApiRequestInsight) {
    var expanded by remember { mutableStateOf(false) }
    OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Decoded SaleToAcquirerData", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (insight.saleToAcquirerDataJson == null) {
                            "This request does not include PaymentRequest.SaleData.SaleToAcquirerData."
                        } else {
                            "Raw JSON sent inside PaymentRequest.SaleData.SaleToAcquirerData"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(
                    onClick = { expanded = !expanded },
                    enabled = insight.saleToAcquirerDataJson != null || insight.saleToAcquirerDataBase64 != null,
                ) {
                    Text(if (expanded) "Hide" else "Decode")
                }
            }
            if (expanded) {
                insight.saleToAcquirerDataJson?.let { decoded ->
                    MonospaceBlock(decoded)
                }
                if (insight.saleToAcquirerDataJson == null) {
                    insight.saleToAcquirerDataBase64?.let { encoded ->
                        Text("Base64 value could not be decoded as JSON.", color = MaterialTheme.colorScheme.error)
                        MonospaceBlock(encoded)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResponseFieldList(insight: TerminalApiResponseInsight) {
    OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Readable response", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            KeyValueLine("Category", insight.category)
            insight.result?.let { KeyValueLine("Result", it) }
            insight.transactionId?.let { KeyValueLine("Transaction ID", it) }
            insight.errorCondition?.let { KeyValueLine("Error condition", it) }
            if (insight.additionalResponseFields.isNotEmpty()) {
                Text("AdditionalResponse", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                insight.additionalResponseFields.forEach { field ->
                    ExpandableValueRow(
                        label = field.name,
                        value = field.decodedValue?.let { "${field.value}\n\nDecoded:\n$it" } ?: field.value,
                    )
                }
            } else if (!insight.additionalResponseRaw.isNullOrBlank()) {
                ExpandableValueRow("AdditionalResponse raw", insight.additionalResponseRaw)
            }
            insight.additionalResponseDecoded?.let { decoded ->
                Text("AdditionalResponse decoded JSON", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                JsonNodeRow(name = "additionalResponse", value = decoded, depth = 0)
            }
        }
    }
}
