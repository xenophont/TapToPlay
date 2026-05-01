package com.xenophont.taptoplay.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xenophont.taptoplay.R
import com.xenophont.taptoplay.cart.CartLine
import com.xenophont.taptoplay.catalog.Product
import com.xenophont.taptoplay.profiles.AdyenProfile
import kotlinx.coroutines.delay

@Composable
internal fun CartSummaryCard(
    lines: List<CartLine>,
    totalMinor: Long,
    activeProfile: AdyenProfile?,
    onCheckout: () -> Unit,
) {
    val itemCount = lines.sumOf { it.quantity }
    OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.cart), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (lines.isEmpty()) {
                            stringResource(R.string.cart_empty_hint)
                        } else {
                            pluralStringResource(R.plurals.cart_item_ready, itemCount, itemCount)
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(formatMoney(totalMinor), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Text(
                activeProfile?.let {
                    stringResource(R.string.charging_profile, it.profileName, stringResource(it.environment.environmentLabelRes()))
                } ?: stringResource(R.string.no_payment_profile_selected),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Button(onClick = onCheckout, enabled = lines.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.go_to_checkout))
            }
        }
    }
}

@Composable
internal fun ProductCard(product: Product, onAdd: () -> Unit) {
    val productName = product.localizedName()
    val productDescription = product.localizedDescription()
    val productCategory = categoryLabel(product.category)
    val locale = LocalConfiguration.current.locales[0]
    var feedbackTick by remember { mutableStateOf(0) }
    var addedFeedback by remember { mutableStateOf(false) }
    LaunchedEffect(feedbackTick) {
        if (feedbackTick > 0) {
            addedFeedback = true
            delay(850)
            addedFeedback = false
        }
    }
    val buttonScale by animateFloatAsState(
        targetValue = if (addedFeedback) 1.04f else 1f,
        animationSpec = spring(stiffness = 460f, dampingRatio = 0.55f),
        label = "addButtonScale",
    )
    val buttonElevation by animateDpAsState(
        targetValue = if (addedFeedback) 8.dp else 2.dp,
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.7f),
        label = "addButtonElevation",
    )
    val buttonColor by animateColorAsState(
        targetValue = if (addedFeedback) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        animationSpec = spring(stiffness = 360f, dampingRatio = 0.75f),
        label = "addButtonColor",
    )
    val buttonContentColor by animateColorAsState(
        targetValue = if (addedFeedback) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimary,
        animationSpec = spring(stiffness = 360f, dampingRatio = 0.75f),
        label = "addButtonContentColor",
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.linearGradient(listOf(product.color, product.accentColor))),
            ) {
                if (product.imageResId != 0) {
                    Image(
                        painter = painterResource(product.imageResId),
                        contentDescription = productName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Transparent,
                                    0.72f to Color.Transparent,
                                    1f to Color.Black.copy(alpha = 0.42f),
                                )
                            ),
                    )
                }
                Text(
                    productCategory.uppercase(locale),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                productName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                productDescription,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    formatMoney(product.priceMinor),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Button(
                    onClick = {
                        onAdd()
                        feedbackTick++
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .scale(buttonScale),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = ButtonDefaults.ContentPadding,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor,
                        contentColor = buttonContentColor,
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = buttonElevation,
                        pressedElevation = 10.dp,
                    ),
                ) {
                    Text(
                        if (addedFeedback) stringResource(R.string.added) else stringResource(R.string.add_to_cart),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
