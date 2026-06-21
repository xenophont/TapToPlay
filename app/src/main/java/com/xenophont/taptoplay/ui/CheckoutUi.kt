package com.xenophont.taptoplay.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xenophont.taptoplay.R
import com.xenophont.taptoplay.adyen.SaleToAcquirerDataConfig
import com.xenophont.taptoplay.cart.CartLine
import com.xenophont.taptoplay.profiles.AdyenProfile
import com.xenophont.taptoplay.profiles.PaymentEnvironment

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
    installationId: String?,
    selectedLanguage: AppLanguage,
    saleToAcquirerDataConfig: SaleToAcquirerDataConfig,
    saleToAcquirerDataFavorites: List<SaleToAcquirerDataConfig>,
    saleToAcquirerDataDefaults: List<SaleToAcquirerDataConfig>,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    onScanSaleToAcquirerData: () -> Unit,
    onImportSaleToAcquirerDataJson: () -> Unit,
    onImportSaleToAcquirerDataImage: () -> Unit,
    onSaveSaleToAcquirerDataFavorite: () -> Unit,
    onApplySaleToAcquirerDataFavorite: (SaleToAcquirerDataConfig) -> Unit,
    onApplySaleToAcquirerDataDefault: (SaleToAcquirerDataConfig) -> Unit,
    onRemoveSaleToAcquirerDataFavorite: (SaleToAcquirerDataConfig) -> Unit,
    onClearSaleToAcquirerData: () -> Unit,
    onInspectSaleToAcquirerData: () -> Unit,
    onPay: (AdyenProfile) -> Unit,
    onOpenPaymentsApp: () -> Unit,
) {
    val needsPaymentsAppSetup = installationId.isNullOrBlank()
    var showModifyOptions by remember { mutableStateOf(false) }
    var showDefaults by remember { mutableStateOf(false) }
    OutlinedCard(shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.screen_checkout), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onClear, enabled = lines.isNotEmpty()) { Text(stringResource(R.string.clear)) }
            }
            if (lines.isEmpty()) {
                Text(stringResource(R.string.checkout_empty_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                lines.forEach { line ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(line.product.localizedName(), fontWeight = FontWeight.Medium)
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
                Text(stringResource(R.string.total), style = MaterialTheme.typography.titleLarge)
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
                            Text(stringResource(R.string.reset), maxLines = 1)
                        }
                    }
                    Text(
                        saleToAcquirerDataSummary(saleToAcquirerDataConfig.displayName, saleToAcquirerDataConfig.fieldCount),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { showModifyOptions = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.modify), maxLines = 1)
                    }
                    if (saleToAcquirerDataFavorites.isNotEmpty()) {
                        Text(stringResource(R.string.favorites), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
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
                                            jsonFieldCountLabel(favorite.fieldCount),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            TextButton(onClick = { onApplySaleToAcquirerDataFavorite(favorite) }) { Text(stringResource(R.string.use)) }
                                            TextButton(onClick = { onRemoveSaleToAcquirerDataFavorite(favorite) }) { Text(stringResource(R.string.remove)) }
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
                Text(if (activeProfile?.environment == PaymentEnvironment.LIVE) stringResource(R.string.charge_live_payment) else stringResource(R.string.charge_test_payment))
            }
            AnimatedVisibility(
                visible = needsPaymentsAppSetup,
                enter = fadeIn(tween(180)) + expandVertically() + scaleIn(initialScale = 0.96f),
                exit = fadeOut(tween(140)) + shrinkVertically() + scaleOut(targetScale = 0.96f),
            ) {
                PaymentsAppStatusPrompt(onClick = onOpenPaymentsApp)
            }
        }
    }
    if (showModifyOptions) {
        SaleToAcquirerDataModifyDialog(
            hasDefaults = saleToAcquirerDataDefaults.isNotEmpty(),
            selectedLanguage = selectedLanguage,
            onScanQr = {
                showModifyOptions = false
                onScanSaleToAcquirerData()
            },
            onImportJson = {
                showModifyOptions = false
                onImportSaleToAcquirerDataJson()
            },
            onImportImage = {
                showModifyOptions = false
                onImportSaleToAcquirerDataImage()
            },
            onDefaults = {
                showModifyOptions = false
                showDefaults = true
            },
            onInspect = {
                showModifyOptions = false
                onInspectSaleToAcquirerData()
            },
            onSave = {
                showModifyOptions = false
                onSaveSaleToAcquirerDataFavorite()
            },
            onDismiss = { showModifyOptions = false },
        )
    }
    if (showDefaults) {
        SaleToAcquirerDataDefaultsDialog(
            defaults = saleToAcquirerDataDefaults,
            selectedLanguage = selectedLanguage,
            onApply = {
                onApplySaleToAcquirerDataDefault(it)
                showDefaults = false
            },
            onDismiss = { showDefaults = false },
        )
    }
}

@Composable
private fun SaleToAcquirerDataModifyDialog(
    hasDefaults: Boolean,
    selectedLanguage: AppLanguage,
    onScanQr: () -> Unit,
    onImportJson: () -> Unit,
    onImportImage: () -> Unit,
    onDefaults: () -> Unit,
    onInspect: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            ProvideLocalizedResources(selectedLanguage) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
            }
        },
        title = {
            ProvideLocalizedResources(selectedLanguage) {
                Text(stringResource(R.string.sale_to_acquirer_modify_title))
            }
        },
        text = {
            ProvideLocalizedResources(selectedLanguage) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.sale_to_acquirer_modify_body),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = onInspect, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.view))
                    }
                    OutlinedButton(onClick = onScanQr, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.scan_data_qr))
                    }
                    OutlinedButton(onClick = onImportJson, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.import_json))
                    }
                    OutlinedButton(onClick = onImportImage, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.ocr_image))
                    }
                    OutlinedButton(onClick = onDefaults, enabled = hasDefaults, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.defaults))
                    }
                    OutlinedButton(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        },
    )
}

@Composable
private fun SaleToAcquirerDataDefaultsDialog(
    defaults: List<SaleToAcquirerDataConfig>,
    selectedLanguage: AppLanguage,
    onApply: (SaleToAcquirerDataConfig) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            ProvideLocalizedResources(selectedLanguage) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
            }
        },
        title = {
            ProvideLocalizedResources(selectedLanguage) {
                Text(stringResource(R.string.defaults))
            }
        },
        text = {
            ProvideLocalizedResources(selectedLanguage) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    defaults.forEach { preset ->
                        OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(preset.displayName, fontWeight = FontWeight.SemiBold)
                                    Text(jsonFieldCountLabel(preset.fieldCount), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                TextButton(onClick = { onApply(preset) }) {
                                    Text(stringResource(R.string.use))
                                }
                            }
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun PaymentsAppStatusPrompt(onClick: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "boardingPromptPulse")
    val scale by pulse.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "boardingPromptScale",
    )
    val elevationDp by pulse.animateFloat(
        initialValue = 4f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "boardingPromptElevation",
    )
    OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.payments_app_not_boarded),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                stringResource(R.string.payments_app_not_boarded_body),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .scale(scale),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = elevationDp.dp,
                    pressedElevation = 12.dp,
                ),
            ) {
                Text(
                    stringResource(R.string.open_payments_app_status),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                )
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
                Text(stringResource(R.string.charge_live))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
        title = { Text(stringResource(R.string.confirm_live_payment)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.confirm_live_payment_body))
                KeyValueLine(stringResource(R.string.amount), formatMoney(confirmation.totalMinor))
                KeyValueLine(stringResource(R.string.items), confirmation.lines.sumOf { it.quantity }.toString())
                KeyValueLine(stringResource(R.string.profile), confirmation.profile.profileName)
                KeyValueLine(stringResource(R.string.environment), confirmation.profile.environment.localizedLabel())
                KeyValueLine(stringResource(R.string.merchant), confirmation.profile.merchantId)
                confirmation.profile.storeName?.let { KeyValueLine(stringResource(R.string.store_name), it) }
                confirmation.profile.storeId?.let { KeyValueLine(stringResource(R.string.store), it) }
            }
        },
    )
}
