package com.xenophont.taptoplay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.xenophont.taptoplay.R
import com.xenophont.taptoplay.adyen.PaymentReceipt
import com.xenophont.taptoplay.adyen.PaymentResult
import com.xenophont.taptoplay.adyen.TerminalApiRequestInsight
import com.xenophont.taptoplay.adyen.TerminalApiRequestInspector
import com.xenophont.taptoplay.adyen.TerminalApiResponseInsight
import com.xenophont.taptoplay.adyen.TerminalApiResponseInspector
import com.xenophont.taptoplay.adyen.TransactionRecord
import com.xenophont.taptoplay.adyen.TransactionStatus
import com.xenophont.taptoplay.adyen.pspReferenceOrNull
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

private enum class TransactionSection {
    Request,
    Response,
    Receipt,
}

private val transactionJson = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
}

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
                    Text(stringResource(R.string.screen_transactions), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (records.isEmpty()) stringResource(R.string.transactions_empty_title) else savedPaymentAttemptsLabel(records.size),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onClear, enabled = records.isNotEmpty()) { Text(stringResource(R.string.clear)) }
            }
            if (records.isEmpty()) {
                Text(
                    stringResource(R.string.transactions_empty_body),
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
    val pspReference = remember(record.pspReference, record.responseBody) { record.pspReferenceOrNull() }
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
                    "${itemCountLabel(record.itemCount)} | ${record.saleToAcquirerDataName}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                record.failureReason?.let {
                    Text(
                        stringResource(R.string.adyen_issue, it),
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                pspReference?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier.widthIn(max = 128.dp),
                    )
                }
                TextButton(onClick = onInspect) { Text(stringResource(R.string.inspect)) }
            }
        }
    }
}

@Composable
private fun TransactionStatusChip(status: TransactionStatus) {
    AssistChip(onClick = {}, label = { Text(status.localizedLabel()) })
}

@Composable
internal fun PaymentResultDialog(result: PaymentResult, isRefund: Boolean, onDismiss: () -> Unit) {
    val title = when (result) {
        is PaymentResult.BoardingStatus -> stringResource(R.string.boarding_returned)
        is PaymentResult.Success -> if (isRefund) stringResource(R.string.refund_approved) else stringResource(R.string.payment_approved)
        is PaymentResult.Refused -> stringResource(R.string.payment_refused)
        is PaymentResult.Failure -> stringResource(R.string.adyen_result)
    }
    val message = when (result) {
        is PaymentResult.BoardingStatus -> {
            val state = if (result.boarded) stringResource(R.string.boarded) else stringResource(R.string.not_boarded)
            val notSupplied = stringResource(R.string.not_supplied)
            val returnData = result.returnData
            val previousMerchant = returnData?.merchantAccountCode?.let { stringResource(R.string.previous_merchant, it) }
            val previousStore = returnData?.merchantStoreCode?.let { stringResource(R.string.previous_store, it) }
            val reboarding = returnData?.reboarding?.takeIf { it }?.let { stringResource(R.string.reboarding_flow_started) }
            val returnSummary = listOfNotNull(previousMerchant, previousStore, reboarding).joinToString(" | ").ifBlank { null }
            listOfNotNull(
                stringResource(R.string.adyen_app_is_state, state, result.installationId ?: notSupplied),
                returnSummary,
                result.errorAdvice,
            ).joinToString("\n")
        }
        is PaymentResult.Success -> stringResource(R.string.reference, result.pspReference ?: stringResource(R.string.not_supplied))
        is PaymentResult.Refused -> result.reason ?: stringResource(R.string.no_refusal_reason)
        is PaymentResult.Failure -> result.message
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
        title = { Text(title) },
        text = { Text(message) },
    )
}

@Composable
internal fun TransactionDialog(
    record: TransactionRecord,
    selectedLanguage: AppLanguage,
    onRefund: () -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedSection by remember { mutableStateOf(TransactionSection.Request) }
    val requestInsight = remember(record.requestJson) { TerminalApiRequestInspector.inspect(record.requestJson) }
    val responseInsight = remember(record.responseBody) { TerminalApiResponseInspector.inspect(record.responseBody) }
    val highlights = responseInsight?.localizedCompactSummary().orEmpty()
    val receipts = responseInsight?.receipts.orEmpty()
    val pspReference = remember(record.pspReference, record.responseBody) { record.pspReferenceOrNull() }
    val createdAtLabel = remember(record.createdAt) { formatTransactionTimestamp(record.createdAt) }
    val canRefund = record.status == TransactionStatus.APPROVED &&
        record.refundOfTransactionId == null &&
        (record.adyenTransactionId != null || responseInsight?.transactionId != null)
    Dialog(onDismissRequest = onDismiss) {
        ProvideLocalizedResources(selectedLanguage) {
            TransactionDialogContent(
                record = record,
                selectedSection = selectedSection,
                onSelectedSectionChange = { selectedSection = it },
                requestInsight = requestInsight,
                responseInsight = responseInsight,
                highlights = highlights,
                receipts = receipts,
                pspReference = pspReference,
                createdAtLabel = createdAtLabel,
                canRefund = canRefund,
                onRefund = onRefund,
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun TransactionDialogContent(
    record: TransactionRecord,
    selectedSection: TransactionSection,
    onSelectedSectionChange: (TransactionSection) -> Unit,
    requestInsight: TerminalApiRequestInsight,
    responseInsight: TerminalApiResponseInsight?,
    highlights: List<Pair<String, String>>,
    receipts: List<PaymentReceipt>,
    pspReference: String?,
    createdAtLabel: String,
    canRefund: Boolean,
    onRefund: () -> Unit,
    onDismiss: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxSize()
            .padding(vertical = 24.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.transaction),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    pspReference ?: stringResource(R.string.not_supplied),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    createdAtLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    OutlinedButton(onClick = onRefund, enabled = canRefund) { Text(stringResource(R.string.refund), maxLines = 1) }
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
                }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FilterChip(
                    selected = selectedSection == TransactionSection.Request,
                    onClick = { onSelectedSectionChange(TransactionSection.Request) },
                    label = { Text(stringResource(R.string.request)) },
                )
                FilterChip(
                    selected = selectedSection == TransactionSection.Response,
                    onClick = { onSelectedSectionChange(TransactionSection.Response) },
                    label = { Text(stringResource(R.string.response)) },
                )
                FilterChip(
                    selected = selectedSection == TransactionSection.Receipt,
                    onClick = { onSelectedSectionChange(TransactionSection.Receipt) },
                    enabled = record.responseBody != null,
                    label = { Text(stringResource(R.string.receipt)) },
                )
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                item { TransactionStatusChip(record.status) }
                when (selectedSection) {
                    TransactionSection.Request -> {
                        item { RequestSummary(record) }
                        item {
                            StructuredJsonSection(
                                title = stringResource(R.string.terminal_api_request),
                                rawLabel = stringResource(R.string.raw_terminal_api_request),
                                raw = record.requestJson,
                                rootName = stringResource(R.string.request),
                            )
                        }
                        item { DecodedSaleToAcquirerDataSection(requestInsight) }
                    }
                    TransactionSection.Response -> {
                        if (highlights.isNotEmpty()) {
                            item {
                                OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(stringResource(R.string.important_response_fields), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
                                        Text(stringResource(R.string.adyen_failure_detail), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                                        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        item {
                            Text(stringResource(R.string.adyen_response), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        item {
                            Text(
                                if (record.responseBody == null) {
                                    stringResource(R.string.no_response_received)
                                } else {
                                    record.localizedSummary()
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        responseInsight?.let { insight -> item { ResponseFieldList(insight) } }
                        record.responseBody?.let { body ->
                            item {
                                Text(stringResource(R.string.raw_terminal_api_response), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                            item { MonospaceBlock(body) }
                        }
                        record.responseUri?.let { response ->
                            item {
                                Text(stringResource(R.string.raw_return_uri), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                            item { MonospaceBlock(response) }
                        }
                    }
                    TransactionSection.Receipt -> {
                        if (receipts.isEmpty()) {
                            item {
                                Text(
                                    stringResource(R.string.no_receipt_data),
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

@Composable
private fun DigitalReceiptCard(receipt: PaymentReceipt) {
    val strings = currentTapToPlayStrings()
    val display = remember(receipt, strings.language) { receipt.toReceiptDisplay(strings) }
    OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(display.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.adyen_generated_receipt_data),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (display.requiredSignature) {
                    AssistChip(onClick = {}, label = { Text(stringResource(R.string.signature)) })
                }
            }
            ReceiptPaper(display)
        }
    }
}

@Composable
private fun ReceiptPaper(display: ReceiptDisplay) {
    val paperColor = Color(0xFFFFFCF4)
    val inkColor = Color(0xFF24211D)
    val mutedInk = inkColor.copy(alpha = 0.64f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 390.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(paperColor)
                .border(1.dp, inkColor.copy(alpha = 0.10f), RoundedCornerShape(6.dp))
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            display.items.forEach { item ->
                when (item) {
                    is ReceiptDisplayItem.TextLine -> ReceiptTextLine(item, inkColor, mutedInk)
                    is ReceiptDisplayItem.Row -> ReceiptTicketRow(item, inkColor, mutedInk)
                    is ReceiptDisplayItem.Status -> ReceiptStatusLine(item.text, inkColor)
                    is ReceiptDisplayItem.Total -> ReceiptTotalLine(item, inkColor)
                    is ReceiptDisplayItem.Note -> ReceiptNoteLine(item.text, mutedInk)
                    is ReceiptDisplayItem.SignatureLine -> ReceiptSignatureLine(item.label, mutedInk)
                    is ReceiptDisplayItem.QrCode -> ReceiptQrLine(item.value, inkColor, mutedInk)
                    ReceiptDisplayItem.Separator -> ReceiptSeparator(inkColor)
                }
            }
        }
    }
}

@Composable
private fun ReceiptTextLine(item: ReceiptDisplayItem.TextLine, inkColor: Color, mutedInk: Color) {
    Text(
        text = item.text,
        modifier = Modifier.fillMaxWidth(),
        color = if (item.bold) inkColor else mutedInk,
        style = if (item.bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodySmall,
        fontWeight = if (item.bold) FontWeight.Bold else FontWeight.Normal,
        fontFamily = FontFamily.Monospace,
        textAlign = item.alignment.textAlign(),
    )
}

@Composable
private fun ReceiptTicketRow(item: ReceiptDisplayItem.Row, inkColor: Color, mutedInk: Color) {
    val valueColor = when (item.emphasis) {
        ReceiptRowEmphasis.Normal -> inkColor
        ReceiptRowEmphasis.Secondary -> mutedInk
        ReceiptRowEmphasis.Technical -> mutedInk.copy(alpha = 0.82f)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = item.label.uppercase(),
            modifier = Modifier.weight(0.44f),
            color = mutedInk,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            text = item.value,
            modifier = Modifier.weight(0.56f),
            color = valueColor,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (item.emphasis == ReceiptRowEmphasis.Normal) FontWeight.Medium else FontWeight.Normal,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Right,
        )
    }
}

@Composable
private fun ReceiptStatusLine(text: String, inkColor: Color) {
    ReceiptSeparator(inkColor)
    Text(
        text = text.uppercase(),
        modifier = Modifier.fillMaxWidth(),
        color = inkColor,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        textAlign = TextAlign.Center,
    )
    ReceiptSeparator(inkColor)
}

@Composable
private fun ReceiptTotalLine(item: ReceiptDisplayItem.Total, inkColor: Color) {
    ReceiptSeparator(inkColor)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = item.label.uppercase(),
            color = inkColor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            text = item.value,
            color = inkColor,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Right,
        )
    }
    ReceiptSeparator(inkColor)
}

@Composable
private fun ReceiptNoteLine(text: String, mutedInk: Color) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        color = mutedInk,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ReceiptSignatureLine(label: String, mutedInk: Color) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HorizontalDivider(color = mutedInk.copy(alpha = 0.45f))
        Text(
            text = label.uppercase(),
            modifier = Modifier.fillMaxWidth(),
            color = mutedInk,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ReceiptQrLine(value: String, inkColor: Color, mutedInk: Color) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .border(1.dp, inkColor.copy(alpha = 0.28f), RoundedCornerShape(4.dp))
                .background(Color.White)
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Text(
                text = "QR",
                color = inkColor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
        }
        Text(
            text = value,
            modifier = Modifier.fillMaxWidth(),
            color = mutedInk,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ReceiptSeparator(inkColor: Color) {
    HorizontalDivider(color = inkColor.copy(alpha = 0.20f))
}

private fun ReceiptTextAlignment.textAlign(): TextAlign = when (this) {
    ReceiptTextAlignment.Start -> TextAlign.Start
    ReceiptTextAlignment.Center -> TextAlign.Center
    ReceiptTextAlignment.End -> TextAlign.End
}

internal fun formatTransactionTimestamp(value: String, zoneId: ZoneId = ZoneId.systemDefault()): String =
    runCatching {
        transactionTimestampFormatter.format(Instant.parse(value).atZone(zoneId))
    }.getOrDefault(value)

private val transactionTimestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

@Composable
private fun RequestSummary(record: TransactionRecord) {
    OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.request_summary), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            KeyValueLine(stringResource(R.string.amount), record.amountLabel)
            KeyValueLine(stringResource(R.string.items), record.itemCount.toString())
            record.messageCategory?.let { KeyValueLine(stringResource(R.string.message_category), it) }
            record.serviceId?.let { KeyValueLine(stringResource(R.string.service_id), it) }
            record.saleTransactionId?.let { KeyValueLine(stringResource(R.string.merchant_reference), it) }
            record.adyenTransactionId?.let { KeyValueLine(stringResource(R.string.adyen_transaction), it) }
            record.refundOfTransactionId?.let { KeyValueLine(stringResource(R.string.refund_of), it) }
        }
    }
}

@Composable
private fun StructuredJsonSection(
    title: String,
    rawLabel: String,
    raw: String,
    rootName: String,
) {
    var showRaw by remember(raw) { mutableStateOf(false) }
    val parsed = remember(raw) { parseJsonElement(raw) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = { showRaw = !showRaw }) {
                Text(if (showRaw) stringResource(R.string.hide_raw_json) else stringResource(R.string.show_raw_json), maxLines = 1)
            }
        }
        if (parsed == null) {
            Text(stringResource(R.string.json_parse_failed), color = MaterialTheme.colorScheme.error)
        } else {
            JsonElementTree(rootName = rootName, value = parsed)
        }
        if (showRaw || parsed == null) {
            Text(rawLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            MonospaceBlock(raw)
        }
    }
}

@Composable
private fun DecodedSaleToAcquirerDataSection(insight: TerminalApiRequestInsight) {
    var showRaw by remember(insight.saleToAcquirerDataJson, insight.saleToAcquirerDataBase64) { mutableStateOf(false) }
    val decodedJson = insight.saleToAcquirerDataJson
    val parsed = remember(decodedJson) { decodedJson?.let(::parseJsonElement) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.decoded_sale_to_acquirer_data), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                if (decodedJson == null) {
                    stringResource(R.string.no_sale_to_acquirer_data)
                } else {
                    stringResource(R.string.structured_sale_to_acquirer_data)
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(
                onClick = { showRaw = !showRaw },
                enabled = decodedJson != null || insight.saleToAcquirerDataBase64 != null,
            ) {
                Text(if (showRaw) stringResource(R.string.hide_raw_json) else stringResource(R.string.show_raw_json), maxLines = 1)
            }
        }
        when {
            parsed != null -> JsonElementTree(rootName = "SaleToAcquirerData", value = parsed)
            decodedJson != null -> Text(stringResource(R.string.json_parse_failed), color = MaterialTheme.colorScheme.error)
            insight.saleToAcquirerDataBase64 != null -> Text(stringResource(R.string.base64_decode_failed), color = MaterialTheme.colorScheme.error)
            else -> OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.no_sale_to_acquirer_data),
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (showRaw) {
            Text(stringResource(R.string.raw_sale_to_acquirer_data), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            MonospaceBlock(decodedJson ?: insight.saleToAcquirerDataBase64.orEmpty())
        }
    }
}

@Composable
private fun ResponseFieldList(insight: TerminalApiResponseInsight) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.readable_response), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                KeyValueLine(stringResource(R.string.category), insight.category)
                insight.result?.let { KeyValueLine(stringResource(R.string.result), it) }
                insight.transactionId?.let { KeyValueLine(stringResource(R.string.transaction_id), it) }
                insight.errorCondition?.let { KeyValueLine(stringResource(R.string.error_condition), it) }
            }
        }
        if (insight.additionalResponseFields.isNotEmpty()) {
            Text(stringResource(R.string.additional_response), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            insight.additionalResponseFields.forEach { field ->
                AdditionalResponseFieldCard(field)
            }
        } else if (!insight.additionalResponseRaw.isNullOrBlank()) {
            AdditionalResponseFieldCard(
                label = stringResource(R.string.additional_response_raw),
                value = insight.additionalResponseRaw,
            )
        }
    }
}

@Composable
private fun TerminalApiResponseInsight.localizedCompactSummary(): List<Pair<String, String>> {
    val fields = mutableListOf<Pair<String, String>>()
    result?.let { fields += stringResource(R.string.result) to it }
    transactionId?.let { fields += stringResource(R.string.transaction_id) to it }
    errorCondition?.let { fields += stringResource(R.string.error_condition) to it }
    TerminalApiResponseInspector.importantAdditional("pspReference", this)
        ?.let { fields += stringResource(R.string.psp_reference) to it }
    val reason = TerminalApiResponseInspector.importantAdditional("refusalReason", this)
        ?: TerminalApiResponseInspector.importantAdditional("refusalReasonRaw", this)
        ?: TerminalApiResponseInspector.importantAdditional("message", this)
    reason?.let { fields += stringResource(R.string.reason) to it }
    TerminalApiResponseInspector.importantAdditional("transactionType", this)
        ?.let { fields += stringResource(R.string.transaction_type) to it }
    return fields
}

@Composable
private fun AdditionalResponseFieldCard(field: com.xenophont.taptoplay.adyen.AdditionalResponseField) {
    AdditionalResponseFieldCard(
        label = field.name,
        value = field.value,
        decodedValue = field.decodedValue,
    )
}

@Composable
private fun AdditionalResponseFieldCard(
    label: String,
    value: String,
    decodedValue: String? = null,
) {
    OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            SelectableValueText(value)
            decodedValue?.let {
                Text(stringResource(R.string.decoded), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                SelectableValueText(it)
            }
        }
    }
}

@Composable
private fun SelectableValueText(value: String) {
    SelectionContainer {
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = if (value.contains('\n')) FontFamily.Monospace else null,
        )
    }
}

@Composable
private fun JsonElementTree(rootName: String, value: JsonElement) {
    if (value is JsonObject && value.isNotEmpty()) {
        value.entries.forEach { (name, child) ->
            JsonNodeRow(name = name, value = child, depth = 0)
        }
    } else {
        JsonNodeRow(name = rootName, value = value, depth = 0)
    }
}

@Composable
private fun currentTapToPlayStrings(): TapToPlayStrings {
    val context = LocalContext.current
    val language = AppLanguage.fromTag(LocalConfiguration.current.locales[0].toLanguageTag())
    return remember(context, language) { stringsFor(context, language) }
}

private fun parseJsonElement(raw: String): JsonElement? =
    runCatching { transactionJson.parseToJsonElement(raw) }.getOrNull()
