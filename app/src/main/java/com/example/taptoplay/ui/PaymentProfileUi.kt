package com.example.taptoplay.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.taptoplay.adyen.PaymentsAppInstance
import com.example.taptoplay.adyen.PaymentsAppStatus
import com.example.taptoplay.profiles.AdyenProfile

@Composable
internal fun ProfilePanel(
    profiles: List<AdyenProfile>,
    activeProfile: AdyenProfile?,
    installationId: String?,
    boardingRequestToken: String?,
    onScanProfile: () -> Unit,
    onOpenCredentialQrDocs: () -> Unit,
    onSelectProfile: (String) -> Unit,
    onRemoveProfile: (AdyenProfile) -> Unit,
    onCheckBoarding: (AdyenProfile) -> Unit,
    onBoard: (AdyenProfile) -> Unit,
    onReboard: (AdyenProfile) -> Unit,
) {
    var expanded by remember { mutableStateOf(true) }
    var profilePendingRemoval by remember { mutableStateOf<AdyenProfile?>(null) }
    val boardingState = when {
        activeProfile == null -> "No profile"
        installationId != null -> "Boarded"
        boardingRequestToken != null -> "Ready to board"
        else -> "Collapsed setup"
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
                    Text("Payment profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        activeProfile?.let { "${it.displayName} | ${it.environment.name.lowercase()}" } ?: "No Adyen profile selected",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    AssistChip(onClick = { expanded = !expanded }, label = { Text(boardingState) })
                    Text(
                        if (expanded) "Hide" else "Setup",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            if (expanded) {
                Button(onClick = onScanProfile, modifier = Modifier.fillMaxWidth()) { Text("Scan QR") }
            }
            if (expanded && profiles.isEmpty()) {
                OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("No credential profile loaded", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Create a credential QR from the TapToPlay schema before boarding the Adyen Payments app.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = onOpenCredentialQrDocs, modifier = Modifier.align(Alignment.Start)) {
                            Text("Open QR documentation")
                        }
                    }
                }
            }
            if (expanded && activeProfile != null) {
                OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Secure device vault", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Scanned QR profiles are stored with Android encrypted preferences. Secrets stay masked in the app.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        KeyValueLine("Merchant", activeProfile.merchantId)
                        activeProfile.storeId?.let { KeyValueLine("Store ID", it) }
                        KeyValueLine("Environment", activeProfile.environment.name.lowercase())
                        KeyValueLine("API key", activeProfile.maskedApiKey())
                        KeyValueLine("Terminal key", "${activeProfile.terminalKeyIdentifier} v${activeProfile.terminalKeyVersion}")
                        KeyValueLine("Passphrase", activeProfile.maskedPassphrase())
                        KeyValueLine("Installation", installationId ?: "not returned yet")
                        KeyValueLine("Boarding request token", boardingRequestToken?.let { "received" } ?: "not received")
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onCheckBoarding(activeProfile) }) { Text("Check") }
                    Button(onClick = { onBoard(activeProfile) }) { Text("Board") }
                    OutlinedButton(onClick = { onReboard(activeProfile) }) { Text("Reboard") }
                    TextButton(onClick = { profilePendingRemoval = activeProfile }) { Text("Remove") }
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
                                    profile.displayName,
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
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { profilePendingRemoval = null }) { Text("Cancel") }
            },
            title = { Text("Remove payment profile?") },
            text = {
                Text(
                    "This removes ${profile.displayName} from the local encrypted vault and clears its saved boarding state. Adyen app authentication is not revoked unless you revoke the instance separately.",
                )
            },
        )
    }
}

@Composable
internal fun PaymentsAppOperationsPanel(
    activeProfile: AdyenProfile?,
    installationId: String?,
    instances: List<PaymentsAppInstance>,
    status: String,
    onRefresh: (AdyenProfile) -> Unit,
    onRevoke: (AdyenProfile, PaymentsAppInstance) -> Unit,
) {
    var revokeTarget by remember { mutableStateOf<PaymentsAppInstance?>(null) }
    OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Payments App instances", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Button(onClick = { activeProfile?.let(onRefresh) }, enabled = activeProfile != null) {
                    Text("Refresh")
                }
            }
            if (activeProfile == null) {
                Text("Scan or select an Adyen profile to inspect app instances.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (instances.isEmpty()) {
                Text("No instances loaded yet. Refresh uses the scanned profile API key.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                instances.forEach { instance ->
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
                    Text("Revoke")
                }
            },
            dismissButton = {
                TextButton(onClick = { revokeTarget = null }) { Text("Cancel") }
            },
            title = { Text("Revoke Payments App instance?") },
            text = {
                Text(
                    "This revokes installation ${instance.installationId.maskForDisplay()} through Adyen. Payments on that app/device need reboarding afterward.",
                )
            },
        )
    }
}

@Composable
private fun PaymentsAppInstanceRow(
    instance: PaymentsAppInstance,
    isCurrentInstallation: Boolean,
    onRevoke: () -> Unit,
) {
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
                    listOfNotNull(instance.merchantAccountCode, instance.merchantStoreCode).joinToString(" | ").ifBlank { "No merchant/store returned" },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AssistChip(onClick = {}, label = { Text(if (isCurrentInstallation) "Current" else instance.status.name.lowercase()) })
        }
        TextButton(onClick = onRevoke, enabled = instance.status != PaymentsAppStatus.REVOKED) {
            Text("Revoke instance")
        }
    }
}
