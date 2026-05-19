package com.xenophont.taptoplay.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xenophont.taptoplay.R
import com.xenophont.taptoplay.adyen.PaymentResult
import com.xenophont.taptoplay.adyen.PaymentsAppInstance
import com.xenophont.taptoplay.adyen.SaleToAcquirerDataConfig
import com.xenophont.taptoplay.adyen.SaleToAcquirerDataEditor
import com.xenophont.taptoplay.adyen.TransactionRecord
import com.xenophont.taptoplay.cart.Cart
import com.xenophont.taptoplay.cart.CartLine
import com.xenophont.taptoplay.catalog.Product
import com.xenophont.taptoplay.catalog.ProductCatalog
import com.xenophont.taptoplay.profiles.AdyenProfile
import com.xenophont.taptoplay.profiles.requiresLivePaymentConfirmation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun TapToPlayApp(
    profiles: List<AdyenProfile>,
    activeProfileId: String?,
    installationId: String?,
    boardingRequestToken: String?,
    boardingTokenIssued: Boolean,
    showPaymentsAppDownloadPrompt: Boolean,
    saleToAcquirerDataConfig: SaleToAcquirerDataConfig,
    saleToAcquirerDataFavorites: List<SaleToAcquirerDataConfig>,
    saleToAcquirerDataDefaults: List<SaleToAcquirerDataConfig>,
    transactionHistory: List<TransactionRecord>,
    paymentsAppInstances: List<PaymentsAppInstance>,
    paymentsAppStatus: String,
    status: String,
    paymentResult: PaymentResult?,
    paymentResultIsRefund: Boolean,
    selectedScreen: AppScreen,
    selectedLanguage: AppLanguage,
    onSelectScreen: (AppScreen) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    showDrawerHint: Boolean,
    onDrawerHintShown: () -> Unit,
    onDismissResult: () -> Unit,
    onScanProfile: () -> Unit,
    onImportProfileJson: () -> Unit,
    onImportProfileImage: () -> Unit,
    onOpenCredentialQrDocs: () -> Unit,
    onDownloadPaymentsApp: (AdyenProfile) -> Unit,
    onScanSaleToAcquirerData: () -> Unit,
    onImportSaleToAcquirerDataJson: () -> Unit,
    onImportSaleToAcquirerDataImage: () -> Unit,
    onUpdateSaleToAcquirerData: (SaleToAcquirerDataConfig) -> Unit,
    onSaveSaleToAcquirerDataFavorite: (SaleToAcquirerDataConfig) -> Unit,
    onApplySaleToAcquirerDataFavorite: (SaleToAcquirerDataConfig) -> Unit,
    onApplySaleToAcquirerDataDefault: (SaleToAcquirerDataConfig) -> Unit,
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
    var showSaleToAcquirerData by remember { mutableStateOf(false) }
    var editableSaleToAcquirerData by remember(saleToAcquirerDataConfig) { mutableStateOf(saleToAcquirerDataConfig) }
    var inspectedTransaction by remember { mutableStateOf<TransactionRecord?>(null) }
    var inspectedProduct by remember { mutableStateOf<Product?>(null) }
    var liveChargeConfirmation by remember { mutableStateOf<LiveChargeConfirmation?>(null) }
    val activeProfile = profiles.firstOrNull { it.id == activeProfileId }
    val lines = remember(cartVersion) { cart.lines() }
    val products = ProductCatalog.products.filter {
        selectedCategory == "All" || it.category == selectedCategory
    }
    val primaryTabs = listOf(
        AppScreen.Catalog,
        AppScreen.Checkout,
        AppScreen.PaymentsApp,
        AppScreen.Transactions,
        AppScreen.Diagnostics,
    )
    val supportTabs = listOf(AppScreen.Language, AppScreen.About)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showLatestAction by remember { mutableStateOf(false) }
    var lastToastedStatus by remember { mutableStateOf(status) }
    var showDrawerPeek by remember { mutableStateOf(false) }
    val catalogCartSummaryIndex = if (showLatestAction) 4 else 3
    val shouldShowCartFab by remember(listState, selectedScreen, lines.isNotEmpty(), catalogCartSummaryIndex) {
        derivedStateOf {
            selectedScreen == AppScreen.Catalog &&
                lines.isNotEmpty() &&
                listState.layoutInfo.visibleItemsInfo.none { it.index == catalogCartSummaryIndex }
        }
    }

    LaunchedEffect(status, showLatestAction) {
        if (showLatestAction) {
            lastToastedStatus = status
        } else if (status.isNotBlank() && status != lastToastedStatus) {
            Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
            lastToastedStatus = status
        }
    }

    LaunchedEffect(showDrawerHint) {
        if (showDrawerHint) {
            delay(450)
            showDrawerPeek = true
            delay(1250)
            showDrawerPeek = false
            delay(240)
            onDrawerHintShown()
        }
    }

    LaunchedEffect(selectedScreen) {
        listState.scrollToItem(0)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppNavigationDrawer(
                primaryTabs = primaryTabs,
                supportTabs = supportTabs,
                selectedScreen = selectedScreen,
                onSelectScreen = { screen ->
                    onSelectScreen(screen)
                    scope.launch { drawerState.close() }
                },
            )
        },
    ) {
        Scaffold { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item {
                        CompactNavigationBar(
                            selectedScreen = selectedScreen,
                            onOpenMenu = { scope.launch { drawerState.open() } },
                        )
                    }
                    if (showLatestAction) {
                        item {
                            LatestActionBanner(status = status)
                        }
                    }
                    when (selectedScreen) {
                        AppScreen.Catalog -> {
                            item {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    items(ProductCatalog.categories) { category ->
                                        FilterChip(
                                            selected = selectedCategory == category,
                                            onClick = { selectedCategory = category },
                                            label = { Text(categoryLabel(category), maxLines = 1) },
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
                                                    ProductCard(
                                                        product = product,
                                                        onInspect = { inspectedProduct = it },
                                                    ) { productToAdd ->
                                                        cart.add(productToAdd)
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
                                    onCheckout = { onSelectScreen(AppScreen.Checkout) },
                                )
                            }
                        }
                        AppScreen.Checkout -> {
                            item {
                                CartPanel(
                                    lines = lines,
                                    totalMinor = cart.totalMinor(),
                                    activeProfile = activeProfile,
                                    installationId = installationId,
                                    saleToAcquirerDataConfig = saleToAcquirerDataConfig,
                                    saleToAcquirerDataFavorites = saleToAcquirerDataFavorites,
                                    saleToAcquirerDataDefaults = saleToAcquirerDataDefaults,
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
                                    onImportSaleToAcquirerDataJson = onImportSaleToAcquirerDataJson,
                                    onImportSaleToAcquirerDataImage = onImportSaleToAcquirerDataImage,
                                    onApplySaleToAcquirerDataDefault = onApplySaleToAcquirerDataDefault,
                                    onInspectSaleToAcquirerData = { showSaleToAcquirerData = true },
                                    onPay = { profile ->
                                        if (profile.requiresLivePaymentConfirmation()) {
                                            liveChargeConfirmation = LiveChargeConfirmation(profile, lines, cart.totalMinor())
                                        } else {
                                            onPay(profile, lines, cart.totalMinor())
                                        }
                                    },
                                    onOpenPaymentsApp = { onSelectScreen(AppScreen.PaymentsApp) },
                                )
                            }
                        }
                        AppScreen.PaymentsApp -> {
                            item {
                                ProfilePanel(
                                    profiles = profiles,
                                    activeProfile = activeProfile,
                                    installationId = installationId,
                                    boardingRequestToken = boardingRequestToken,
                                    boardingTokenIssued = boardingTokenIssued,
                                    showPaymentsAppDownloadPrompt = showPaymentsAppDownloadPrompt,
                                    onScanProfile = onScanProfile,
                                    onImportProfileJson = onImportProfileJson,
                                    onImportProfileImage = onImportProfileImage,
                                    onOpenCredentialQrDocs = onOpenCredentialQrDocs,
                                    onDownloadPaymentsApp = onDownloadPaymentsApp,
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
                                    onRefresh = onRefreshPaymentsApps,
                                    onRevoke = onRevokePaymentsApp,
                                )
                            }
                        }
                        AppScreen.Transactions -> {
                            item {
                                TransactionHistoryPanel(
                                    records = transactionHistory,
                                    onInspect = { inspectedTransaction = it },
                                    onClear = onClearTransactions,
                                )
                            }
                        }
                        AppScreen.Diagnostics -> {
                            item {
                                DiagnosticsPanel(
                                    activeProfile = activeProfile,
                                    installationId = installationId,
                                    boardingRequestToken = boardingRequestToken,
                                    boardingTokenIssued = boardingTokenIssued,
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
                        AppScreen.Language -> {
                            item {
                                LanguagePanel(
                                    selectedLanguage = selectedLanguage,
                                    onSelectLanguage = onSelectLanguage,
                                )
                            }
                        }
                        AppScreen.About -> {
                            item {
                                AboutPanel()
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.height(if (selectedScreen == AppScreen.About) 88.dp else 18.dp))
                    }
                }
                if (selectedScreen == AppScreen.About) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f))
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                    ) {
                        PrivacyPolicyStickyButton(Modifier.fillMaxWidth())
                    }
                }
                AnimatedVisibility(
                    visible = shouldShowCartFab,
                    enter = slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(durationMillis = 260, easing = FastOutLinearInEasing),
                    ) + fadeIn(animationSpec = tween(durationMillis = 220, easing = FastOutLinearInEasing)),
                    exit = slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                    ) + fadeOut(animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp),
                ) {
                    ScrollToCartFab(
                        onClick = {
                            scope.launch {
                                listState.animateScrollBy(
                                    value = 10_000f,
                                    animationSpec = tween(
                                        durationMillis = 900,
                                        easing = FastOutSlowInEasing,
                                    ),
                                )
                            }
                        },
                    )
                }
                AnimatedVisibility(
                    visible = showDrawerPeek && drawerState.currentValue == DrawerValue.Closed,
                    enter = slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    ) + fadeIn(animationSpec = tween(durationMillis = 150)),
                    exit = slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                    ) + fadeOut(animationSpec = tween(durationMillis = 140)),
                    modifier = Modifier.align(Alignment.CenterStart),
                ) {
                    DrawerPeekHint(
                        onClick = {
                            scope.launch {
                                showDrawerPeek = false
                                drawerState.open()
                            }
                        },
                    )
                }
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
            onAdd = { path, value ->
                editableSaleToAcquirerData = SaleToAcquirerDataEditor.add(editableSaleToAcquirerData, path, value)
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

    inspectedProduct?.let { product ->
        ProductDetailDialog(
            product = product,
            onAdd = {
                cart.add(it)
                cartVersion++
                inspectedProduct = null
            },
            onDismiss = { inspectedProduct = null },
        )
    }
}

@Composable
private fun DrawerPeekHint(onClick: () -> Unit) {
    val shape = RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp)
    val lineColor = MaterialTheme.colorScheme.primary
    val openMenuDescription = stringResource(R.string.open_menu)
    Box(
        modifier = Modifier
            .size(width = 52.dp, height = 132.dp)
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = openMenuDescription },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(width = 26.dp, height = 70.dp)) {
            val strokeWidth = 2.25.dp.toPx()
            val startX = size.width * 0.18f
            val endX = size.width * 0.82f
            listOf(0.24f, 0.42f, 0.6f).forEach { yFactor ->
                drawLine(
                    color = lineColor,
                    start = Offset(startX, size.height * yFactor),
                    end = Offset(endX, size.height * yFactor),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }
            drawLine(
                color = lineColor,
                start = Offset(size.width * 0.46f, size.height * 0.78f),
                end = Offset(size.width * 0.68f, size.height * 0.68f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = lineColor,
                start = Offset(size.width * 0.46f, size.height * 0.78f),
                end = Offset(size.width * 0.68f, size.height * 0.88f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun ScrollToCartFab(onClick: () -> Unit) {
    val scrollToCartDescription = stringResource(R.string.scroll_to_cart)
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier
            .size(56.dp)
            .semantics { contentDescription = scrollToCartDescription },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        CartGlyph(Modifier.size(28.dp))
    }
}

@Composable
private fun CartGlyph(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onPrimaryContainer
    Canvas(modifier = modifier) {
        val strokeWidth = 2.5.dp.toPx()
        val wheelRadius = size.minDimension * 0.07f
        drawLine(
            color = color,
            start = Offset(size.width * 0.12f, size.height * 0.2f),
            end = Offset(size.width * 0.26f, size.height * 0.2f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.26f, size.height * 0.2f),
            end = Offset(size.width * 0.34f, size.height * 0.36f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.32f, size.height * 0.34f),
            size = Size(size.width * 0.5f, size.height * 0.26f),
            cornerRadius = CornerRadius(size.width * 0.04f, size.height * 0.04f),
            style = Stroke(width = strokeWidth),
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.39f, size.height * 0.46f),
            end = Offset(size.width * 0.76f, size.height * 0.46f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawCircle(color = color, radius = wheelRadius, center = Offset(size.width * 0.44f, size.height * 0.73f))
        drawCircle(color = color, radius = wheelRadius, center = Offset(size.width * 0.72f, size.height * 0.73f))
    }
}

@Composable
private fun AppNavigationDrawer(
    primaryTabs: List<AppScreen>,
    supportTabs: List<AppScreen>,
    selectedScreen: AppScreen,
    onSelectScreen: (AppScreen) -> Unit,
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.app_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            primaryTabs.forEach { tab ->
                DrawerItem(tab = tab, selectedScreen = selectedScreen, onSelectScreen = onSelectScreen)
            }
            Spacer(Modifier.weight(1f))
            supportTabs.forEach { tab ->
                DrawerItem(tab = tab, selectedScreen = selectedScreen, onSelectScreen = onSelectScreen)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DrawerItem(
    tab: AppScreen,
    selectedScreen: AppScreen,
    onSelectScreen: (AppScreen) -> Unit,
) {
    NavigationDrawerItem(
        selected = selectedScreen == tab,
        onClick = { onSelectScreen(tab) },
        label = {
            Text(
                tab.localizedLabel(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}

@Composable
private fun CompactNavigationBar(
    selectedScreen: AppScreen,
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
            Text(stringResource(R.string.menu))
        }
        Text(
            selectedScreen.localizedLabel(),
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
                stringResource(R.string.latest_action),
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
