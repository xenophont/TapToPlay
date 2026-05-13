package com.xenophont.taptoplay.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xenophont.taptoplay.BuildConfig
import com.xenophont.taptoplay.R

@Composable
internal fun AboutPanel(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val aboutMessage = stringResource(R.string.about_message)
    val noEmailMessage = stringResource(R.string.status_no_email_beta_access)
    var showGameBetaDialog by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val imageShape = RoundedCornerShape(8.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .aspectRatio(3f / 4f)
                .clip(imageShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, imageShape),
        ) {
            Image(
                painter = painterResource(R.drawable.javier_portrait_3_4),
                contentDescription = aboutMessage,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            aboutMessage,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.about_build_info, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { showGameBetaDialog = true }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.about_game_eyebrow),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.about_game_action),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
    if (showGameBetaDialog) {
        ClosedBetaAccessDialog(
            onRequestAccess = {
                try {
                    context.startActivity(TransactionGame.betaEmailIntent())
                } catch (_: RuntimeException) {
                    Toast.makeText(context, noEmailMessage, Toast.LENGTH_LONG).show()
                }
                showGameBetaDialog = false
            },
            onDismiss = { showGameBetaDialog = false },
        )
    }
}

@Composable
private fun ClosedBetaAccessDialog(
    onRequestAccess: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onRequestAccess) {
                Text(stringResource(R.string.request_beta_access_here))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
        title = { Text(stringResource(R.string.game_beta_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.game_beta_body))
                Text(
                    stringResource(R.string.game_beta_coming_soon),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
    )
}

@Composable
internal fun PrivacyPolicyStickyButton(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val noBrowserMessage = stringResource(R.string.status_no_browser_privacy_policy)
    OutlinedButton(
        onClick = {
            try {
                context.startActivity(PrivacyPolicy.viewIntent())
            } catch (_: RuntimeException) {
                Toast.makeText(context, noBrowserMessage, Toast.LENGTH_LONG).show()
            }
        },
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.privacy_policy))
    }
}

private object TransactionGame {
    private const val EMAIL = "xenophont.dev@gmail.com"

    fun betaEmailIntent(): Intent =
        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$EMAIL")).apply {
            putExtra(Intent.EXTRA_SUBJECT, "Authorisation Engine closed beta access request")
            putExtra(Intent.EXTRA_TEXT, "Hi Javier,\n\nI would like access to the closed beta of Authorisation Engine on Google Play.\n\nThanks!")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
}
