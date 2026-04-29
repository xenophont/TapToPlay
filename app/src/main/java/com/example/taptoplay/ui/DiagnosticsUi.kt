package com.example.taptoplay.ui

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
import com.example.taptoplay.adyen.PaymentsAppInstance
import com.example.taptoplay.adyen.SaleToAcquirerDataConfig
import com.example.taptoplay.adyen.TransactionRecord
import com.example.taptoplay.profiles.AdyenProfile

@Composable
internal fun DiagnosticsPanel(
    activeProfile: AdyenProfile?,
    installationId: String?,
    boardingRequestToken: String?,
    saleToAcquirerDataConfig: SaleToAcquirerDataConfig,
    transactionHistory: List<TransactionRecord>,
    paymentsAppInstances: List<PaymentsAppInstance>,
    paymentsAppStatus: String,
    status: String,
    showLatestAction: Boolean,
    onShowLatestActionChange: (Boolean) -> Unit,
) {
    OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Diagnostics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("Redacted operational state for the selected profile.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        "Show latest action on top",
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
            KeyValueLine("Latest action", status)
            KeyValueLine("Profile", activeProfile?.displayName ?: "none")
            KeyValueLine("Environment", activeProfile?.environment?.name?.lowercase() ?: "none")
            activeProfile?.let {
                KeyValueLine("Merchant", it.merchantId)
                it.storeId?.let { store -> KeyValueLine("Store", store) }
                KeyValueLine("API key", it.maskedApiKey())
                KeyValueLine("Terminal key", "${it.terminalKeyIdentifier} v${it.terminalKeyVersion}")
            }
            KeyValueLine("Installation", installationId?.maskForDisplay() ?: "not returned yet")
            KeyValueLine("Boarding token", boardingRequestToken?.let { "received" } ?: "not received")
            KeyValueLine(
                "SaleToAcquirerData",
                "${saleToAcquirerDataConfig.displayName} | ${saleToAcquirerDataConfig.fieldCount} fields",
            )
            KeyValueLine("Payments App API", paymentsAppStatus)
            KeyValueLine(
                "Loaded instances",
                paymentsAppInstances
                    .groupingBy { it.status }
                    .eachCount()
                    .entries
                    .joinToString { "${it.key.name.lowercase()}: ${it.value}" }
                    .ifBlank { "none" },
            )
            KeyValueLine(
                "Transaction history",
                transactionHistory
                    .groupingBy { it.status }
                    .eachCount()
                    .entries
                    .joinToString { "${it.key.name.lowercase()}: ${it.value}" }
                    .ifBlank { "none" },
            )
            transactionHistory.firstOrNull()?.let { latest ->
                KeyValueLine("Latest ServiceID", latest.serviceId ?: "not recorded")
                KeyValueLine("Latest summary", latest.responseSummary ?: latest.status.name.lowercase())
            }
        }
    }
}
