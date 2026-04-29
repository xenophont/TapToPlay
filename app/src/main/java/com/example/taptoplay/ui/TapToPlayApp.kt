package com.example.taptoplay.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.taptoplay.adyen.PaymentResult
import com.example.taptoplay.adyen.PaymentsAppInstance
import com.example.taptoplay.adyen.SaleToAcquirerDataConfig
import com.example.taptoplay.adyen.SaleToAcquirerDataEditor
import com.example.taptoplay.adyen.TransactionRecord
import com.example.taptoplay.cart.Cart
import com.example.taptoplay.cart.CartLine
import com.example.taptoplay.catalog.ProductCatalog
import com.example.taptoplay.profiles.AdyenProfile
import com.example.taptoplay.profiles.requiresLivePaymentConfirmation
import kotlinx.coroutines.launch

private enum class OpsTab(val label: String) {
    Catalog("Catalog"),
    Checkout("Checkout"),
    PaymentsApp("Payments App"),
    Transactions("Transactions"),
    Diagnostics("Diagnostics"),
}

@Composable
internal fun TapToPlayApp(
    profiles: List<AdyenProfile>,
    activeProfileId: String?,
    installationId: String?,
    boardingRequestToken: String?,
    saleToAcquirerDataConfig: SaleToAcquirerDataConfig,
    saleToAcquirerDataFavorites: List<SaleToAcquirerDataConfig>,
    transactionHistory: List<TransactionRecord>,
    paymentsAppInstances: List<PaymentsAppInstance>,
    paymentsAppStatus: String,
    status: String,
    paymentResult: PaymentResult?,
    paymentResultIsRefund: Boolean,
    onDismissResult: () -> Unit,
    onScanProfile: () -> Unit,
    onScanSaleToAcquirerData: () -> Unit,
    onUpdateSaleToAcquirerData: (SaleToAcquirerDataConfig) -> Unit,
    onSaveSaleToAcquirerDataFavorite: (SaleToAcquirerDataConfig) -> Unit,
    onApplySaleToAcquirerDataFavorite: (SaleToAcquirerDataConfig) -> Unit,
    onRemoveSaleToAcquirerDataFavorite: (SaleToAcquirerDataConfig) -> Unit,
    onClearSaleToAcquirerData: () -> Unit,
    onClearTransactions: () -> Unit,
    onSelectProfile: (String) -> Unit,
    onRemoveProfile: (AdyenProfile) -> Unit,
    onCheckBoarding: (AdyenProfile) -> Unit,
    onBoard: (AdyenProfile) -> Unit,
    onReboard: (AdyenProfile) -> Unit,
    onRefreshPaymentsApps: (AdyenProfile) -> Unit,
    onRevokePaymentsApp: (AdyenProfile, PaymentsAppInstance) -> Unit,
    onPay: (AdyenProfile, List<CartLine>, Long) -> Unit,
    onRefund: (TransactionRecord) -> Unit,
) {
    val cart = remember { Cart() }
    var cartVersion by remember { mutableStateOf(0) }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedTab by remember { mutableStateOf(OpsTab.Catalog) }
    var showSaleToAcquirerData by remember { mutableStateOf(false) }
    var editableSaleToAcquirerData by remember(saleToAcquirerDataConfig) { mutableStateOf(saleToAcquirerDataConfig) }
    var inspectedTransaction by remember { mutableStateOf<TransactionRecord?>(null) }
    var liveChargeConfirmation by remember { mutableStateOf<LiveChargeConfirmation?>(null) }
    val activeProfile = profiles.firstOrNull { it.id == activeProfileId }
    val lines = remember(cartVersion) { cart.lines() }
    val products = ProductCatalog.products.filter {
        selectedCategory == "All" || it.category == selectedCategory
    }
    val tabs = OpsTab.entries.toList()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showLatestAction by remember { mutableStateOf(false) }
    var lastToastedStatus by remember { mutableStateOf(status) }

    LaunchedEffect(status, showLatestAction) {
        if (showLatestAction) {
            lastToastedStatus = status
        } else if (status.isNotBlank() && status != lastToastedStatus) {
            Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
            lastToastedStatus = status
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppNavigationDrawer(
                tabs = tabs,
                selectedTab = selectedTab,
                onSelectTab = { tab ->
                    selectedTab = tab
                    scope.launch { drawerState.close() }
                },
            )
        },
    ) {
        Scaffold { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    CompactNavigationBar(
                        selectedTab = selectedTab,
                        onOpenMenu = { scope.launch { drawerState.open() } },
                    )
                }
                if (showLatestAction) {
                    item {
                        LatestActionBanner(status = status)
                    }
                }
                when (selectedTab) {
                    OpsTab.Catalog -> {
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                items(ProductCatalog.categories) { category ->
                                    FilterChip(
                                        selected = selectedCategory == category,
                                        onClick = { selectedCategory = category },
                                        label = { Text(category, maxLines = 1) },
                                    )
                                }
                            }
                        }
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                products.chunked(2).forEach { rowProducts ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        rowProducts.forEach { product ->
                                            Box(Modifier.weight(1f)) {
                                                ProductCard(product = product) {
                                                    cart.add(product)
                                                    cartVersion++
                                                }
                                            }
                                        }
                                        if (rowProducts.size == 1) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            CartSummaryCard(
                                lines = lines,
                                totalMinor = cart.totalMinor(),
                                activeProfile = activeProfile,
                                onCheckout = { selectedTab = OpsTab.Checkout },
                            )
                        }
                    }
                    OpsTab.Checkout -> {
                        item {
                            CartPanel(
                                lines = lines,
                                totalMinor = cart.totalMinor(),
                                activeProfile = activeProfile,
                                saleToAcquirerDataConfig = saleToAcquirerDataConfig,
                                saleToAcquirerDataFavorites = saleToAcquirerDataFavorites,
                                onRemove = {
                                    cart.removeOne(it)
                                    cartVersion++
                                },
                                onClear = {
                                    cart.clear()
                                    cartVersion++
                                },
                                onScanSaleToAcquirerData = onScanSaleToAcquirerData,
                                onSaveSaleToAcquirerDataFavorite = { onSaveSaleToAcquirerDataFavorite(saleToAcquirerDataConfig) },
                                onApplySaleToAcquirerDataFavorite = onApplySaleToAcquirerDataFavorite,
                                onRemoveSaleToAcquirerDataFavorite = onRemoveSaleToAcquirerDataFavorite,
                                onClearSaleToAcquirerData = onClearSaleToAcquirerData,
                                onInspectSaleToAcquirerData = { showSaleToAcquirerData = true },
                                onPay = { profile ->
                                    if (profile.requiresLivePaymentConfirmation()) {
                                        liveChargeConfirmation = LiveChargeConfirmation(profile, lines, cart.totalMinor())
                                    } else {
                                        onPay(profile, lines, cart.totalMinor())
                                    }
                                },
                            )
                        }
                    }
                    OpsTab.PaymentsApp -> {
                        item {
                            ProfilePanel(
                                profiles = profiles,
                                activeProfile = activeProfile,
                                installationId = installationId,
                                boardingRequestToken = boardingRequestToken,
                                onScanProfile = onScanProfile,
                                onSelectProfile = onSelectProfile,
                                onRemoveProfile = onRemoveProfile,
                                onCheckBoarding = onCheckBoarding,
                                onBoard = onBoard,
                                onReboard = onReboard,
                            )
                        }
                        item {
                            PaymentsAppOperationsPanel(
                                activeProfile = activeProfile,
                                installationId = installationId,
                                instances = paymentsAppInstances,
                                status = paymentsAppStatus,
                                onRefresh = onRefreshPaymentsApps,
                                onRevoke = onRevokePaymentsApp,
                            )
                        }
                    }
                    OpsTab.Transactions -> {
                        item {
                            TransactionHistoryPanel(
                                records = transactionHistory,
                                onInspect = { inspectedTransaction = it },
                                onClear = onClearTransactions,
                            )
                        }
                    }
                    OpsTab.Diagnostics -> {
                        item {
                            DiagnosticsPanel(
                                activeProfile = activeProfile,
                                installationId = installationId,
                                boardingRequestToken = boardingRequestToken,
                                saleToAcquirerDataConfig = saleToAcquirerDataConfig,
                                transactionHistory = transactionHistory,
                                paymentsAppInstances = paymentsAppInstances,
                                paymentsAppStatus = paymentsAppStatus,
                                status = status,
                                showLatestAction = showLatestAction,
                                onShowLatestActionChange = { showLatestAction = it },
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(18.dp)) }
            }
        }
    }

    paymentResult?.let {
        PaymentResultDialog(result = it, isRefund = paymentResultIsRefund, onDismiss = onDismissResult)
    }

    liveChargeConfirmation?.let { confirmation ->
        LivePaymentConfirmationDialog(
            confirmation = confirmation,
            onConfirm = {
                onPay(confirmation.profile, confirmation.lines, confirmation.totalMinor)
                liveChargeConfirmation = null
            },
            onDismiss = { liveChargeConfirmation = null },
        )
    }

    if (showSaleToAcquirerData) {
        SaleToAcquirerDataDialog(
            config = editableSaleToAcquirerData,
            onEdit = { path, value ->
                editableSaleToAcquirerData = SaleToAcquirerDataEditor.update(editableSaleToAcquirerData, path, value)
            },
            onRemove = { path ->
                editableSaleToAcquirerData = SaleToAcquirerDataEditor.remove(editableSaleToAcquirerData, path)
            },
            onApply = {
                onUpdateSaleToAcquirerData(editableSaleToAcquirerData)
                showSaleToAcquirerData = false
            },
            onSaveFavorite = {
                onSaveSaleToAcquirerDataFavorite(editableSaleToAcquirerData)
            },
            onDismiss = { showSaleToAcquirerData = false },
        )
    }

    inspectedTransaction?.let { record ->
        TransactionDialog(
            record = record,
            onRefund = { onRefund(record) },
            onDismiss = { inspectedTransaction = null },
        )
    }
}

@Composable
private fun AppNavigationDrawer(
    tabs: List<OpsTab>,
    selectedTab: OpsTab,
    onSelectTab: (OpsTab) -> Unit,
) {
    ModalDrawerSheet {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("TapToPlay", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "Boutique POS demo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        tabs.forEach { tab ->
            NavigationDrawerItem(
                selected = selectedTab == tab,
                onClick = { onSelectTab(tab) },
                label = {
                    Text(
                        tab.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}

@Composable
private fun CompactNavigationBar(
    selectedTab: OpsTab,
    onOpenMenu: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(onClick = onOpenMenu, modifier = Modifier.height(40.dp)) {
            Text("Menu")
        }
        Text(
            selectedTab.label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LatestActionBanner(status: String) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Latest action",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                status,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
