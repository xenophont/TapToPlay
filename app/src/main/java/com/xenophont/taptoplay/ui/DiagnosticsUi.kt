package com.xenophont.taptoplay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xenophont.taptoplay.adyen.PaymentsAppInstance
import com.xenophont.taptoplay.adyen.SaleToAcquirerDataConfig
import com.xenophont.taptoplay.adyen.TransactionRecord
import com.xenophont.taptoplay.profiles.AdyenProfile

@Composable
internal fun DiagnosticsPanel(
    activeProfile: AdyenProfile?,
    installationId: String?,
    boardingRequestToken: String?,
    boardingTokenIssued: Boolean,
    saleToAcquirerDataConfig: SaleToAcquirerDataConfig,
    transactionHistory: List<TransactionRecord>,
    paymentsAppInstances: List<PaymentsAppInstance>,
    paymentsAppStatus: String,
    status: String,
    showLatestAction: Boolean,
    onShowLatestActionChange: (Boolean) -> Unit,
) {
    val strings = LocalTapToPlayStrings.current
    OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(strings["screen_diagnostics"], style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(strings["diagnostics_body"], color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = showLatestAction, onCheckedChange = onShowLatestActionChange)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        strings["show_latest_action_on_top"],
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        status,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            KeyValueLine(strings["latest_action"], status)
            KeyValueLine(strings["profile"], activeProfile?.profileName ?: strings["none"])
            KeyValueLine(strings["environment"], activeProfile?.environment?.let { strings.environmentLabel(it) } ?: strings["none"])
            activeProfile?.let {
                KeyValueLine(strings["merchant"], it.merchantId)
                it.storeName?.let { storeName -> KeyValueLine(strings["store_name"], storeName) }
                it.storeId?.let { store -> KeyValueLine(strings["store"], store) }
                KeyValueLine(strings["api_key"], strings.secretMask(it.apiKey))
                KeyValueLine(strings["terminal_key"], "${it.terminalKeyIdentifier} v${it.terminalKeyVersion}")
            }
            KeyValueLine(strings["installation"], installationId?.maskForDisplay() ?: strings["installation_not_returned_yet"])
            KeyValueLine(
                strings["boarding_request_token"],
                when {
                    boardingTokenIssued -> strings["boarding_token_exchanged"]
                    boardingRequestToken != null -> strings["boarding_token_received"]
                    installationId != null -> strings["boarding_token_not_needed"]
                    else -> strings["boarding_token_not_received"]
                },
            )
            KeyValueLine(
                strings["boarding_token"],
                when {
                    boardingTokenIssued -> strings["boarding_token_generated"]
                    boardingRequestToken != null -> strings["boarding_token_not_generated"]
                    else -> strings["boarding_token_not_requested"]
                },
            )
            KeyValueLine(
                "SaleToAcquirerData",
                strings.format(
                    "sale_to_acquirer_data_summary",
                    saleToAcquirerDataConfig.displayName,
                    strings.format(
                        if (saleToAcquirerDataConfig.fieldCount == 1) "field_count_one" else "field_count_many",
                        saleToAcquirerDataConfig.fieldCount,
                    ),
                ),
            )
            KeyValueLine(strings["payments_app_api"], paymentsAppStatus)
            KeyValueLine(
                strings["loaded_instances"],
                paymentsAppInstances
                    .groupingBy { it.status }
                    .eachCount()
                    .entries
                    .joinToString { "${it.key.localizedLabel(strings)}: ${it.value}" }
                    .ifBlank { strings["none"] },
            )
            KeyValueLine(
                strings["transaction_history"],
                transactionHistory
                    .groupingBy { it.status }
                    .eachCount()
                    .entries
                    .joinToString { "${it.key.localizedLabel(strings)}: ${it.value}" }
                    .ifBlank { strings["none"] },
            )
            transactionHistory.firstOrNull()?.let { latest ->
                KeyValueLine(strings["latest_service_id"], latest.serviceId ?: strings["not_set"])
                KeyValueLine(strings["latest_summary"], latest.responseSummary ?: latest.status.localizedLabel(strings))
            }
        }
    }
}
