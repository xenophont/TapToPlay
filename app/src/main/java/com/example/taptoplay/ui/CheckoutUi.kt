package com.example.taptoplay.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.taptoplay.adyen.SaleToAcquirerDataConfig
import com.example.taptoplay.cart.CartLine
import com.example.taptoplay.profiles.AdyenProfile
import com.example.taptoplay.profiles.PaymentEnvironment

internal data class LiveChargeConfirmation(
    val profile: AdyenProfile,
    val lines: List<CartLine>,
    val totalMinor: Long,
)

@Composable
internal fun CartPanel(
    lines: List<CartLine>,
    totalMinor: Long,
    activeProfile: AdyenProfile?,
    saleToAcquirerDataConfig: SaleToAcquirerDataConfig,
    saleToAcquirerDataFavorites: List<SaleToAcquirerDataConfig>,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    onScanSaleToAcquirerData: () -> Unit,
    onSaveSaleToAcquirerDataFavorite: () -> Unit,
    onApplySaleToAcquirerDataFavorite: (SaleToAcquirerDataConfig) -> Unit,
    onRemoveSaleToAcquirerDataFavorite: (SaleToAcquirerDataConfig) -> Unit,
    onClearSaleToAcquirerData: () -> Unit,
    onInspectSaleToAcquirerData: () -> Unit,
    onPay: (AdyenProfile) -> Unit,
) {
    OutlinedCard(shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Checkout", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onClear, enabled = lines.isNotEmpty()) { Text("Clear") }
            }
            if (lines.isEmpty()) {
                Text("Add garments to start a Tap to Pay checkout.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                lines.forEach { line ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(line.product.name, fontWeight = FontWeight.Medium)
                            Text("${line.quantity} x ${formatMoney(line.product.priceMinor)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(formatMoney(line.lineTotalMinor), fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { onRemove(line.product.id) },
                            modifier = Modifier.size(width = 44.dp, height = 36.dp),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Text("X", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Total", style = MaterialTheme.typography.titleLarge)
                Text(formatMoney(totalMinor), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            OutlinedCard(shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "SaleToAcquirerData",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        OutlinedButton(
                            onClick = onClearSaleToAcquirerData,
                            modifier = Modifier
                                .width(96.dp)
                                .height(40.dp),
                        ) {
                            Text("Reset", maxLines = 1)
                        }
                    }
                    Text(
                        "${saleToAcquirerDataConfig.displayName} | ${saleToAcquirerDataConfig.fieldCount} JSON field${if (saleToAcquirerDataConfig.fieldCount == 1) "" else "s"}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        item {
                            OutlinedButton(onClick = onScanSaleToAcquirerData) { Text("Scan data QR", maxLines = 1) }
                        }
                        item {
                            OutlinedButton(onClick = onInspectSaleToAcquirerData) { Text("View", maxLines = 1) }
                        }
                        item {
                            OutlinedButton(onClick = onSaveSaleToAcquirerDataFavorite) { Text("Save", maxLines = 1) }
                        }
                    }
                    if (saleToAcquirerDataFavorites.isNotEmpty()) {
                        Text("Favorites", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            items(saleToAcquirerDataFavorites) { favorite ->
                                OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.width(220.dp)) {
                                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            favorite.displayName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            "${favorite.fieldCount} JSON field${if (favorite.fieldCount == 1) "" else "s"}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            TextButton(onClick = { onApplySaleToAcquirerDataFavorite(favorite) }) { Text("Use") }
                                            TextButton(onClick = { onRemoveSaleToAcquirerDataFavorite(favorite) }) { Text("Remove") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Button(
                onClick = { activeProfile?.let(onPay) },
                enabled = lines.isNotEmpty() && activeProfile != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (activeProfile?.environment == PaymentEnvironment.LIVE) "Charge live payment" else "Charge test payment")
            }
        }
    }
}

@Composable
internal fun LivePaymentConfirmationDialog(
    confirmation: LiveChargeConfirmation,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) {
                Text("Charge live")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Confirm live payment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("This will launch a live Adyen Tap to Pay charge.")
                KeyValueLine("Amount", formatMoney(confirmation.totalMinor))
                KeyValueLine("Items", confirmation.lines.sumOf { it.quantity }.toString())
                KeyValueLine("Profile", confirmation.profile.displayName)
                KeyValueLine("Environment", confirmation.profile.environment.name.lowercase())
                KeyValueLine("Merchant", confirmation.profile.merchantId)
                confirmation.profile.storeId?.let { KeyValueLine("Store", it) }
            }
        },
    )
}
