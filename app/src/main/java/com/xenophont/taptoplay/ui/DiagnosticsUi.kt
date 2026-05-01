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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xenophont.taptoplay.R
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
    val paymentsAppStatusLabels = mapOf(
        com.xenophont.taptoplay.adyen.PaymentsAppStatus.BOARDING to stringResource(R.string.payments_app_status_boarding),
        com.xenophont.taptoplay.adyen.PaymentsAppStatus.BOARDED to stringResource(R.string.payments_app_status_boarded),
        com.xenophont.taptoplay.adyen.PaymentsAppStatus.REVOKED to stringResource(R.string.payments_app_status_revoked),
        com.xenophont.taptoplay.adyen.PaymentsAppStatus.UNKNOWN to stringResource(R.string.payments_app_status_unknown),
    )
    val transactionStatusLabels = mapOf(
        com.xenophont.taptoplay.adyen.TransactionStatus.LAUNCHED to stringResource(R.string.transaction_status_pending),
        com.xenophont.taptoplay.adyen.TransactionStatus.APPROVED to stringResource(R.string.transaction_status_approved),
        com.xenophont.taptoplay.adyen.TransactionStatus.REFUSED to stringResource(R.string.transaction_status_refused),
        com.xenophont.taptoplay.adyen.TransactionStatus.FAILED to stringResource(R.string.transaction_status_failed),
        com.xenophont.taptoplay.adyen.TransactionStatus.REFUND_LAUNCHED to stringResource(R.string.transaction_status_refunding),
        com.xenophont.taptoplay.adyen.TransactionStatus.REFUNDED to stringResource(R.string.transaction_status_refunded),
    )
    OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.screen_diagnostics), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.diagnostics_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        stringResource(R.string.show_latest_action_on_top),
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
            KeyValueLine(stringResource(R.string.latest_action), status)
            KeyValueLine(stringResource(R.string.profile), activeProfile?.profileName ?: stringResource(R.string.none))
            KeyValueLine(stringResource(R.string.environment), activeProfile?.environment?.localizedLabel() ?: stringResource(R.string.none))
            activeProfile?.let {
                KeyValueLine(stringResource(R.string.merchant), it.merchantId)
                it.storeName?.let { storeName -> KeyValueLine(stringResource(R.string.store_name), storeName) }
                it.storeId?.let { store -> KeyValueLine(stringResource(R.string.store), store) }
                KeyValueLine(stringResource(R.string.api_key), secretMask(it.apiKey))
                KeyValueLine(stringResource(R.string.terminal_key), "${it.terminalKeyIdentifier} v${it.terminalKeyVersion}")
            }
            KeyValueLine(stringResource(R.string.installation), installationId?.maskForDisplay() ?: stringResource(R.string.installation_not_returned_yet))
            KeyValueLine(
                stringResource(R.string.boarding_request_token),
                when {
                    boardingTokenIssued -> stringResource(R.string.boarding_token_exchanged)
                    boardingRequestToken != null -> stringResource(R.string.boarding_token_received)
                    installationId != null -> stringResource(R.string.boarding_token_not_needed)
                    else -> stringResource(R.string.boarding_token_not_received)
                },
            )
            KeyValueLine(
                stringResource(R.string.boarding_token),
                when {
                    boardingTokenIssued -> stringResource(R.string.boarding_token_generated)
                    boardingRequestToken != null -> stringResource(R.string.boarding_token_not_generated)
                    else -> stringResource(R.string.boarding_token_not_requested)
                },
            )
            KeyValueLine(
                "SaleToAcquirerData",
                saleToAcquirerDataSummary(saleToAcquirerDataConfig.displayName, saleToAcquirerDataConfig.fieldCount),
            )
            KeyValueLine(stringResource(R.string.payments_app_api), paymentsAppStatus)
            KeyValueLine(
                stringResource(R.string.loaded_instances),
                paymentsAppInstances
                    .groupingBy { it.status }
                    .eachCount()
                    .entries
                    .joinToString { "${paymentsAppStatusLabels.getValue(it.key)}: ${it.value}" }
                    .ifBlank { stringResource(R.string.none) },
            )
            KeyValueLine(
                stringResource(R.string.transaction_history),
                transactionHistory
                    .groupingBy { it.status }
                    .eachCount()
                    .entries
                    .joinToString { "${transactionStatusLabels.getValue(it.key)}: ${it.value}" }
                    .ifBlank { stringResource(R.string.none) },
            )
            transactionHistory.firstOrNull()?.let { latest ->
                KeyValueLine(stringResource(R.string.latest_service_id), latest.serviceId ?: stringResource(R.string.not_set))
                KeyValueLine(stringResource(R.string.latest_summary), latest.responseSummary ?: transactionStatusLabels.getValue(latest.status))
            }
        }
    }
}
