package com.xenophont.taptoplay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xenophont.taptoplay.R
import com.xenophont.taptoplay.adyen.PaymentsAppInstance
import com.xenophont.taptoplay.adyen.PaymentsAppStatus
import com.xenophont.taptoplay.profiles.AdyenProfile

@Composable
internal fun ProfilePanel(
    profiles: List<AdyenProfile>,
    activeProfile: AdyenProfile?,
    installationId: String?,
    boardingRequestToken: String?,
    boardingTokenIssued: Boolean,
    showPaymentsAppDownloadPrompt: Boolean,
    onScanProfile: () -> Unit,
    onOpenCredentialQrDocs: () -> Unit,
    onDownloadPaymentsApp: (AdyenProfile) -> Unit,
    onSelectProfile: (String) -> Unit,
    onRemoveProfile: (AdyenProfile) -> Unit,
    onCheckBoarding: (AdyenProfile) -> Unit,
    onBoard: (AdyenProfile) -> Unit,
    onReboard: (AdyenProfile) -> Unit,
) {
    var expanded by remember { mutableStateOf(true) }
    var profilePendingRemoval by remember { mutableStateOf<AdyenProfile?>(null) }
    val boardingState = when {
        activeProfile == null -> stringResource(R.string.boarding_no_profile)
        installationId != null -> stringResource(R.string.boarding_boarded)
        boardingRequestToken != null -> stringResource(R.string.boarding_ready_to_board)
        else -> stringResource(R.string.boarding_collapsed_setup)
    }
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(stringResource(R.string.payment_profile), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        activeProfile?.let { "${it.profileName} | ${it.environment.localizedLabel()}" }
                            ?: stringResource(R.string.no_payment_profile_selected),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    AssistChip(onClick = { expanded = !expanded }, label = { Text(boardingState) })
                    Text(
                        if (expanded) stringResource(R.string.hide) else stringResource(R.string.setup),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            if (expanded) {
                Button(onClick = onScanProfile, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.scan_qr)) }
            }
            if (expanded && profiles.isEmpty()) {
                OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.no_credential_profile_loaded), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(R.string.credential_profile_empty_body),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = onOpenCredentialQrDocs, modifier = Modifier.align(Alignment.Start)) {
                            Text(stringResource(R.string.open_qr_documentation))
                        }
                    }
                }
            }
            if (expanded && activeProfile != null) {
                OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.secure_device_vault), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(R.string.secure_device_vault_body),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        KeyValueLine(stringResource(R.string.merchant), activeProfile.merchantId)
                        activeProfile.storeName?.let { KeyValueLine(stringResource(R.string.store_name), it) }
                        activeProfile.storeId?.let { KeyValueLine(stringResource(R.string.store_id), it) }
                        KeyValueLine(stringResource(R.string.environment), activeProfile.environment.localizedLabel())
                        KeyValueLine(stringResource(R.string.api_key), secretMask(activeProfile.apiKey))
                        KeyValueLine(stringResource(R.string.terminal_key), "${activeProfile.terminalKeyIdentifier} v${activeProfile.terminalKeyVersion}")
                        KeyValueLine(stringResource(R.string.passphrase), passphraseMask(activeProfile.terminalPassphrase))
                        KeyValueLine(stringResource(R.string.installation), installationId ?: stringResource(R.string.installation_not_returned_yet))
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
                    }
                }
                if (showPaymentsAppDownloadPrompt && installationId == null) {
                    PaymentsAppDownloadCard(
                        profile = activeProfile,
                        onDownload = { onDownloadPaymentsApp(activeProfile) },
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onCheckBoarding(activeProfile) }) { Text(stringResource(R.string.check)) }
                    Button(onClick = { onBoard(activeProfile) }) { Text(stringResource(R.string.board)) }
                    OutlinedButton(onClick = { onReboard(activeProfile) }) { Text(stringResource(R.string.reboard)) }
                    TextButton(onClick = { profilePendingRemoval = activeProfile }) { Text(stringResource(R.string.remove)) }
                }
            }
            if (expanded && profiles.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    profiles.forEach { profile ->
                        FilterChip(
                            selected = profile.id == activeProfile?.id,
                            onClick = { onSelectProfile(profile.id) },
                            label = {
                                Text(
                                    profile.profileName,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            modifier = Modifier.widthIn(max = 180.dp),
                        )
                    }
                }
            }
        }
    }
    profilePendingRemoval?.let { profile ->
        AlertDialog(
            onDismissRequest = { profilePendingRemoval = null },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveProfile(profile)
                        profilePendingRemoval = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { profilePendingRemoval = null }) { Text(stringResource(R.string.cancel)) }
            },
            title = { Text(stringResource(R.string.remove_payment_profile_title)) },
            text = {
                Text(
                    stringResource(R.string.remove_payment_profile_body, profile.profileName),
                )
            },
        )
    }
}

@Composable
private fun PaymentsAppDownloadCard(
    profile: AdyenProfile,
    onDownload: () -> Unit,
) {
    OutlinedCard(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.payments_app_not_installed), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.payments_app_not_installed_body, profile.environment.localizedLabel()),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onDownload, modifier = Modifier.align(Alignment.Start)) {
                Text(stringResource(R.string.open_google_play))
            }
        }
    }
}

@Composable
internal fun PaymentsAppOperationsPanel(
    activeProfile: AdyenProfile?,
    installationId: String?,
    instances: List<PaymentsAppInstance>,
    onRefresh: (AdyenProfile) -> Unit,
    onRevoke: (AdyenProfile, PaymentsAppInstance) -> Unit,
) {
    var revokeTarget by remember { mutableStateOf<PaymentsAppInstance?>(null) }
    var showRevoked by remember { mutableStateOf(false) }
    val revokedCount = instances.count { it.status == PaymentsAppStatus.REVOKED }
    val displayedInstances = displayedPaymentsAppInstances(
        instances = instances,
        currentInstallationId = installationId,
        showRevoked = showRevoked,
    )
    OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.payments_app_instances), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = { activeProfile?.let(onRefresh) }, enabled = activeProfile != null) {
                    Text(stringResource(R.string.refresh))
                }
                if (revokedCount > 0) {
                    OutlinedButton(onClick = { showRevoked = !showRevoked }) {
                        Text(if (showRevoked) stringResource(R.string.hide_revoked) else stringResource(R.string.show_revoked))
                    }
                }
            }
            if (activeProfile == null) {
                Text(stringResource(R.string.instances_no_profile), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (instances.isEmpty()) {
                Text(stringResource(R.string.instances_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (displayedInstances.isEmpty()) {
                Text(stringResource(R.string.instances_only_revoked_hidden), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                displayedInstances.forEach { instance ->
                    PaymentsAppInstanceRow(
                        instance = instance,
                        isCurrentInstallation = instance.installationId == installationId,
                        onRevoke = { revokeTarget = instance },
                    )
                }
            }
        }
    }
    revokeTarget?.let { instance ->
        AlertDialog(
            onDismissRequest = { revokeTarget = null },
            confirmButton = {
                Button(
                    onClick = {
                        activeProfile?.let { profile -> onRevoke(profile, instance) }
                        revokeTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.revoke))
                }
            },
            dismissButton = {
                TextButton(onClick = { revokeTarget = null }) { Text(stringResource(R.string.cancel)) }
            },
            title = { Text(stringResource(R.string.revoke_instance_title)) },
            text = {
                Text(
                    stringResource(R.string.revoke_instance_body, instance.installationId.maskForDisplay()),
                )
            },
        )
    }
}

internal fun displayedPaymentsAppInstances(
    instances: List<PaymentsAppInstance>,
    currentInstallationId: String?,
    showRevoked: Boolean,
): List<PaymentsAppInstance> {
    val current = instances.filter {
        it.installationId == currentInstallationId && (showRevoked || it.status != PaymentsAppStatus.REVOKED)
    }
    val active = instances.filter {
        it.status != PaymentsAppStatus.REVOKED && it.installationId != currentInstallationId
    }
    val revoked = if (showRevoked) {
        instances.filter { it.status == PaymentsAppStatus.REVOKED && it.installationId != currentInstallationId }
    } else {
        emptyList()
    }
    return current + active + revoked
}

@Composable
private fun PaymentsAppInstanceRow(
    instance: PaymentsAppInstance,
    isCurrentInstallation: Boolean,
    onRevoke: () -> Unit,
) {
    val noMerchantStore = stringResource(R.string.no_merchant_store_returned)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(instance.installationId.maskForDisplay(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    listOfNotNull(instance.merchantAccountCode, instance.merchantStoreCode).joinToString(" | ").ifBlank { noMerchantStore },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AssistChip(onClick = {}, label = { Text(if (isCurrentInstallation) stringResource(R.string.current) else instance.status.localizedLabel()) })
        }
        TextButton(onClick = onRevoke, enabled = instance.status != PaymentsAppStatus.REVOKED) {
            Text(stringResource(R.string.revoke_instance))
        }
    }
}
