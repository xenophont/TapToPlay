package com.xenophont.taptoplay.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.xenophont.taptoplay.R

@Composable
internal fun AboutPanel(
    modifier: Modifier = Modifier,
) {
    val aboutMessage = stringResource(R.string.about_message)
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
    }
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
