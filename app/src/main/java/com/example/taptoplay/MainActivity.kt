package com.example.taptoplay

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import com.example.taptoplay.adyen.AdyenLinks
import com.example.taptoplay.adyen.AndroidBoardingStateStore
import com.example.taptoplay.adyen.AndroidTransactionStore
import com.example.taptoplay.adyen.NexoCrypto
import com.example.taptoplay.adyen.PaymentResult
import com.example.taptoplay.adyen.PaymentResultParser
import com.example.taptoplay.adyen.PaymentReceipt
import com.example.taptoplay.adyen.PaymentsAppApiClient
import com.example.taptoplay.adyen.PaymentsAppInstance
import com.example.taptoplay.adyen.PaymentsAppStatus
import com.example.taptoplay.adyen.ReceiptLine
import com.example.taptoplay.adyen.SaleToAcquirerDataConfig
import com.example.taptoplay.adyen.SaleToAcquirerDataEditor
import com.example.taptoplay.adyen.SaleToAcquirerDataFavoriteStore
import com.example.taptoplay.adyen.SaleToAcquirerDataQrParser
import com.example.taptoplay.adyen.TerminalApiRequestInspector
import com.example.taptoplay.adyen.TerminalApiResponseInspector
import com.example.taptoplay.adyen.TerminalPaymentRequestBuilder
import com.example.taptoplay.adyen.TransactionRecord
import com.example.taptoplay.adyen.TransactionStatus
import com.example.taptoplay.adyen.failureReasonOrNull
import com.example.taptoplay.adyen.responseJsonOrNull
import com.example.taptoplay.adyen.serviceIdOrNull
import com.example.taptoplay.adyen.toTransactionStatus
import com.example.taptoplay.adyen.toTransactionSummary
import com.example.taptoplay.adyen.transactionIdOrNull
import com.example.taptoplay.cart.Cart
import com.example.taptoplay.cart.CartLine
import com.example.taptoplay.catalog.Product
import com.example.taptoplay.catalog.ProductCatalog
import com.example.taptoplay.profiles.AndroidProfileStore
import com.example.taptoplay.profiles.AdyenProfile
import com.example.taptoplay.profiles.LocalProfileBootstrap
import com.example.taptoplay.profiles.PaymentEnvironment
import com.example.taptoplay.profiles.ProfileQrParser
import com.example.taptoplay.profiles.requiresLivePaymentConfirmation
import com.example.taptoplay.ui.theme.TapToPlayTheme
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import java.time.Instant
import java.util.UUID

class MainActivity : ComponentActivity() {
    private lateinit var profileStore: AndroidProfileStore
    private lateinit var transactionStore: AndroidTransactionStore
    private lateinit var boardingStateStore: AndroidBoardingStateStore
    private lateinit var saleToAcquirerDataFavoriteStore: SaleToAcquirerDataFavoriteStore
    private val qrParser = ProfileQrParser()
    private val saleToAcquirerDataQrParser = SaleToAcquirerDataQrParser()
    private val paymentsAppApiClient = PaymentsAppApiClient()
    private val nexoCrypto = NexoCrypto()

    private var profilesState by mutableStateOf(emptyList<AdyenProfile>())
    private var activeProfileIdState by mutableStateOf<String?>(null)
    private var paymentResultState by mutableStateOf<PaymentResult?>(null)
    private var paymentResultIsRefundState by mutableStateOf(false)
    private var statusState by mutableStateOf("Ready for boutique checkout")
    private var installationIdState by mutableStateOf<String?>(null)
    private var boardingRequestTokenState by mutableStateOf<String?>(null)
    private var saleToAcquirerDataConfigState by mutableStateOf(SaleToAcquirerDataConfig.default())
    private var saleToAcquirerDataFavoritesState by mutableStateOf(emptyList<SaleToAcquirerDataConfig>())
    private var transactionHistoryState by mutableStateOf(emptyList<TransactionRecord>())
    private var pendingTransactionIdState by mutableStateOf<String?>(null)
    private var paymentsAppInstancesState by mutableStateOf(emptyList<PaymentsAppInstance>())
    private var paymentsAppStatusState by mutableStateOf("Payments App instances not loaded")

    private val qrLauncher = registerForActivityResult(ScanContract()) { result ->
        val contents = result.contents ?: return@registerForActivityResult
        qrParser.parse(contents)
            .onSuccess { profile ->
                profileStore.save(profile)
                profileStore.setActive(profile.id)
                reloadProfiles()
                reloadBoardingState()
                statusState = "Scanned ${profile.displayName}. Active profile updated."
            }
            .onFailure { statusState = "QR rejected: ${it.message}" }
    }

    private val saleToAcquirerDataQrLauncher = registerForActivityResult(ScanContract()) { result ->
        val contents = result.contents ?: return@registerForActivityResult
        saleToAcquirerDataQrParser.parse(contents)
            .onSuccess { config ->
                saleToAcquirerDataConfigState = config
                statusState = "SaleToAcquirerData QR loaded: ${config.displayName}."
            }
            .onFailure { statusState = "SaleToAcquirerData QR rejected: ${it.message}" }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        profileStore = AndroidProfileStore(this)
        transactionStore = AndroidTransactionStore(this)
        boardingStateStore = AndroidBoardingStateStore(this)
        saleToAcquirerDataFavoriteStore = SaleToAcquirerDataFavoriteStore(this)
        LocalProfileBootstrap.profileOrNull()?.let { bootstrap ->
            if (profileStore.profiles().none { it.id == bootstrap.id }) profileStore.save(bootstrap)
            if (profileStore.activeProfileId() == null) profileStore.setActive(bootstrap.id)
        }
        reloadProfiles()
        reloadBoardingState()
        reloadSaleToAcquirerDataFavorites()
        reloadTransactions()
        handleReturnIntent(intent)

        setContent {
            TapToPlayTheme {
                TapToPlayApp(
                    profiles = profilesState,
                    activeProfileId = activeProfileIdState,
                    installationId = installationIdState,
                    boardingRequestToken = boardingRequestTokenState,
                    saleToAcquirerDataConfig = saleToAcquirerDataConfigState,
                    saleToAcquirerDataFavorites = saleToAcquirerDataFavoritesState,
                    transactionHistory = transactionHistoryState,
                    paymentsAppInstances = paymentsAppInstancesState,
                    paymentsAppStatus = paymentsAppStatusState,
                    status = statusState,
                    paymentResult = paymentResultState,
                    paymentResultIsRefund = paymentResultIsRefundState,
                    onDismissResult = { paymentResultState = null },
                    onScanProfile = { scanQr() },
                    onScanSaleToAcquirerData = { scanSaleToAcquirerDataQr() },
                    onUpdateSaleToAcquirerData = { config ->
                        saleToAcquirerDataConfigState = config
                        statusState = "SaleToAcquirerData updated from the field editor."
                    },
                    onSaveSaleToAcquirerDataFavorite = { config ->
                        saleToAcquirerDataFavoriteStore.save(config)
                        reloadSaleToAcquirerDataFavorites()
                        statusState = "Saved ${config.displayName} as a SaleToAcquirerData favorite."
                    },
                    onApplySaleToAcquirerDataFavorite = { config ->
                        saleToAcquirerDataConfigState = config
                        statusState = "SaleToAcquirerData favorite applied: ${config.displayName}."
                    },
                    onRemoveSaleToAcquirerDataFavorite = { config ->
                        saleToAcquirerDataFavoriteStore.remove(config.displayName)
                        reloadSaleToAcquirerDataFavorites()
                        statusState = "Removed SaleToAcquirerData favorite: ${config.displayName}."
                    },
                    onClearSaleToAcquirerData = {
                        saleToAcquirerDataConfigState = SaleToAcquirerDataConfig.default()
                        statusState = "SaleToAcquirerData reset to retail demo defaults."
                    },
                    onClearTransactions = {
                        transactionStore.clear()
                        pendingTransactionIdState = null
                        reloadTransactions()
                        statusState = "Transaction history cleared."
                    },
                    onSelectProfile = {
                        profileStore.setActive(it)
                        reloadProfiles()
                        reloadBoardingState()
                        paymentsAppInstancesState = emptyList()
                        paymentsAppStatusState = "Payments App instances not loaded"
                        statusState = "Active profile switched deliberately."
                    },
                    onRemoveProfile = { profile -> removeProfile(profile) },
                    onCheckBoarding = { profile -> launchLink(AdyenLinks.boarded(profile)) },
                    onBoard = { profile -> board(profile) },
                    onReboard = { profile -> launchLink(AdyenLinks.startReboard(profile)) },
                    onRefreshPaymentsApps = { profile -> refreshPaymentsApps(profile) },
                    onRevokePaymentsApp = { profile, instance -> revokePaymentsApp(profile, instance) },
                    onPay = { profile, lines, totalMinor -> pay(profile, lines, totalMinor) },
                    onRefund = { record -> refund(record) },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleReturnIntent(intent)
    }

    private fun reloadProfiles() {
        profilesState = profileStore.profiles()
        activeProfileIdState = profileStore.activeProfileId()
    }

    private fun reloadBoardingState() {
        val activeProfileId = activeProfileIdState
        installationIdState = activeProfileId?.let { boardingStateStore.installationId(it) }
        boardingRequestTokenState = activeProfileId?.let { boardingStateStore.boardingRequestToken(it) }
    }

    private fun reloadTransactions() {
        transactionHistoryState = transactionStore.records()
    }

    private fun reloadSaleToAcquirerDataFavorites() {
        saleToAcquirerDataFavoritesState = saleToAcquirerDataFavoriteStore.favorites()
    }

    private fun scanQr() {
        qrLauncher.launch(
            ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt("Scan a TapToPlay Adyen profile QR")
                .setBeepEnabled(false),
        )
    }

    private fun scanSaleToAcquirerDataQr() {
        saleToAcquirerDataQrLauncher.launch(
            ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt("Scan a SaleToAcquirerData properties QR")
                .setBeepEnabled(false),
        )
    }

    private fun board(profile: AdyenProfile) {
        val requestToken = boardingRequestTokenState
        if (requestToken.isNullOrBlank()) {
            statusState = "Check boarding first so Adyen can return a boarding request token."
            launchLink(AdyenLinks.boarded(profile))
            return
        }
        statusState = "Requesting Adyen boarding token..."
        lifecycleScope.launch {
            val tokenResult = withContext(Dispatchers.IO) { paymentsAppApiClient.createBoardingToken(profile, requestToken) }
            tokenResult
                .onSuccess { response ->
                    installationIdState = response.installationId ?: installationIdState
                    response.installationId?.let { boardingStateStore.saveInstallationId(profile.id, it) }
                    statusState = "Opening Adyen to finish boarding..."
                    launchLink(AdyenLinks.board(profile, response.boardingToken))
                }
                .onFailure { statusState = "Boarding token failed: ${it.message}" }
        }
    }

    private fun removeProfile(profile: AdyenProfile) {
        boardingStateStore.clear(profile.id)
        profileStore.remove(profile.id)
        reloadProfiles()
        reloadBoardingState()
        paymentsAppInstancesState = emptyList()
        paymentsAppStatusState = "Payments App instances not loaded"
        statusState = "Removed ${profile.displayName} and cleared its local boarding state."
    }

    private fun refreshPaymentsApps(profile: AdyenProfile) {
        paymentsAppStatusState = "Refreshing Payments App instances..."
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { paymentsAppApiClient.listPaymentsApps(profile) }
            result
                .onSuccess { instances ->
                    paymentsAppInstancesState = instances
                    paymentsAppStatusState = if (instances.isEmpty()) {
                        "No Payments App instances returned for this profile."
                    } else {
                        "Loaded ${instances.size} Payments App instance${if (instances.size == 1) "" else "s"}."
                    }
                }
                .onFailure {
                    paymentsAppStatusState = "Payments App lookup failed: ${it.message}"
                }
        }
    }

    private fun revokePaymentsApp(profile: AdyenProfile, instance: PaymentsAppInstance) {
        paymentsAppStatusState = "Revoking Payments App instance ${instance.installationId.maskForDisplay()}..."
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                paymentsAppApiClient.revokePaymentsApp(profile, instance.installationId)
            }
            result
                .onSuccess {
                    if (installationIdState == instance.installationId) {
                        boardingStateStore.clear(profile.id)
                        reloadBoardingState()
                    }
                    paymentsAppInstancesState = paymentsAppInstancesState.map { current ->
                        if (current.installationId == instance.installationId) {
                            current.copy(status = PaymentsAppStatus.REVOKED)
                        } else {
                            current
                        }
                    }
                    paymentsAppStatusState = "Revoked Payments App instance ${instance.installationId.maskForDisplay()}."
                    statusState = "Payments App instance revoked. Reboard before charging with that device."
                }
                .onFailure {
                    paymentsAppStatusState = "Revoke failed: ${it.message}"
                }
        }
    }

    private fun pay(profile: AdyenProfile, lines: List<CartLine>, totalMinor: Long) {
        val installationId = installationIdState
        if (installationId.isNullOrBlank()) {
            statusState = "Check boarding first. Payments require an installation ID as POIID."
            launchLink(AdyenLinks.boarded(profile))
            return
        }
        val request = TerminalPaymentRequestBuilder.buildDemoPaymentRequest(
            profile = profile,
            installationId = installationId,
            lines = lines,
            totalMinor = totalMinor,
            saleToAcquirerDataConfig = saleToAcquirerDataConfigState,
        )
        val transactionId = UUID.randomUUID().toString()
        val record = TransactionRecord(
            id = transactionId,
            createdAt = Instant.now().toString(),
            amountLabel = formatMoney(totalMinor),
            amountMinor = totalMinor,
            itemCount = lines.sumOf { it.quantity },
            saleToAcquirerDataName = saleToAcquirerDataConfigState.displayName,
            requestJson = request.json,
            serviceId = request.serviceId,
            saleTransactionId = request.saleTransactionId,
            messageCategory = request.messageCategory,
            profileId = profile.id,
            installationId = installationId,
        )
        transactionStore.save(record)
        pendingTransactionIdState = transactionId
        reloadTransactions()
        val encoded = nexoCrypto.encryptToBase64Url(profile, request.json)
        statusState = "Opening Adyen payment app with encrypted Terminal API request..."
        launchLink(AdyenLinks.nexo(profile, encoded))
    }

    private fun refund(record: TransactionRecord) {
        val originalTransactionId = record.adyenTransactionId
            ?: TerminalApiResponseInspector.inspect(record.responseBody)?.transactionId
        if (originalTransactionId.isNullOrBlank()) {
            statusState = "Cannot refund yet: the Adyen transaction identifier is missing."
            return
        }
        val profile = record.profileId?.let { profileId -> profilesState.firstOrNull { it.id == profileId } }
            ?: profilesState.firstOrNull { it.id == activeProfileIdState }
        if (profile == null) {
            statusState = "Cannot refund: select the Adyen profile used for the original payment."
            return
        }
        val installationId = record.installationId ?: installationIdState
        if (installationId.isNullOrBlank()) {
            statusState = "Cannot refund: no installation ID is available for the Payments app."
            return
        }
        val request = TerminalPaymentRequestBuilder.buildReferencedRefundPaymentRequest(
            installationId = installationId,
            originalTransactionId = originalTransactionId,
            originalTimestamp = record.createdAt,
        )
        val refundRecordId = UUID.randomUUID().toString()
        transactionStore.save(
            TransactionRecord(
                id = refundRecordId,
                createdAt = Instant.now().toString(),
                amountLabel = record.amountLabel,
                amountMinor = record.amountMinor,
                itemCount = record.itemCount,
                saleToAcquirerDataName = "Referenced refund",
                requestJson = request.json,
                serviceId = request.serviceId,
                saleTransactionId = request.saleTransactionId,
                messageCategory = request.messageCategory,
                profileId = profile.id,
                installationId = installationId,
                adyenTransactionId = originalTransactionId,
                refundOfTransactionId = record.id,
                status = TransactionStatus.REFUND_LAUNCHED,
            ),
        )
        pendingTransactionIdState = refundRecordId
        reloadTransactions()
        val encoded = nexoCrypto.encryptToBase64Url(profile, request.json)
        statusState = "Opening Adyen with a referenced refund request..."
        launchLink(AdyenLinks.nexo(profile, encoded))
    }

    private fun launchLink(rawUrl: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(rawUrl)))
    }

    private fun handleReturnIntent(intent: Intent?) {
        val rawUri = intent?.data?.toString()
        val activeProfile = profilesState.firstOrNull { it.id == activeProfileIdState }
        val parsed = PaymentResultParser.parse(rawUri ?: return, activeProfile, nexoCrypto) ?: return
        if (parsed is PaymentResult.BoardingStatus) {
            paymentResultIsRefundState = false
            paymentResultState = parsed
            val activeProfileId = activeProfile?.id
            parsed.installationId?.let { installationId ->
                installationIdState = installationId
                activeProfileId?.let { boardingStateStore.saveInstallationId(it, installationId) }
            }
            parsed.boardingRequestToken?.let { token ->
                boardingRequestTokenState = token
                activeProfileId?.let { boardingStateStore.saveBoardingRequestToken(it, token) }
            }
            statusState = when {
                parsed.boarded -> "Adyen app is boarded and ready."
                parsed.boardingRequestToken != null -> "Boarding request token received. Tap Board to finish setup."
                parsed.error != null -> listOfNotNull("Boarding error: ${parsed.error}", parsed.errorAdvice).joinToString(" ")
                else -> "Adyen app is not boarded yet."
            }
        } else {
            val transactionRecords = transactionStore.records()
            val parsedServiceId = parsed.serviceIdOrNull()
            val transactionId = parsedServiceId?.let { serviceId ->
                transactionRecords.firstOrNull { record ->
                    record.serviceId == serviceId &&
                        (record.status == TransactionStatus.LAUNCHED || record.status == TransactionStatus.REFUND_LAUNCHED)
                }?.id
            } ?: pendingTransactionIdState ?: transactionRecords.firstOrNull {
                it.status == TransactionStatus.LAUNCHED || it.status == TransactionStatus.REFUND_LAUNCHED
            }?.id
            val transactionRecord = transactionId?.let { id -> transactionRecords.firstOrNull { it.id == id } }
            paymentResultIsRefundState = transactionRecord?.refundOfTransactionId != null
            paymentResultState = parsed
            if (transactionId != null) {
                transactionStore.update(transactionId) { record ->
                    record.copy(
                        status = if (record.refundOfTransactionId != null && parsed is PaymentResult.Success) {
                            TransactionStatus.REFUNDED
                        } else {
                            parsed.toTransactionStatus()
                        },
                        responseUri = rawUri,
                        responseBody = parsed.responseJsonOrNull(),
                        responseSummary = parsed.toTransactionSummary(),
                        failureReason = parsed.failureReasonOrNull(),
                        adyenTransactionId = parsed.transactionIdOrNull() ?: record.adyenTransactionId,
                    )
                }
                pendingTransactionIdState = null
                reloadTransactions()
                statusState = parsed.toTransactionSummary()
            }
        }
    }
}

private enum class OpsTab(val label: String) {
    Catalog("Catalog"),
    Checkout("Checkout"),
    PaymentsApp("Payments App"),
    Transactions("Transactions"),
    Diagnostics("Diagnostics"),
}

private data class LiveChargeConfirmation(
    val profile: AdyenProfile,
    val lines: List<CartLine>,
    val totalMinor: Long,
)

@Composable
private fun TapToPlayApp(
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

@Composable
private fun CartSummaryCard(
    lines: List<CartLine>,
    totalMinor: Long,
    activeProfile: AdyenProfile?,
    onCheckout: () -> Unit,
) {
    OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Cart", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (lines.isEmpty()) {
                            "Add garments to build a checkout."
                        } else {
                            "${lines.sumOf { it.quantity }} item${if (lines.sumOf { it.quantity } == 1) "" else "s"} ready"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(formatMoney(totalMinor), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Text(
                activeProfile?.let { "Charging profile: ${it.displayName} (${it.environment.name.lowercase()})" }
                    ?: "No payment profile selected",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Button(onClick = onCheckout, enabled = lines.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Text("Go to checkout")
            }
        }
    }
}

@Composable
private fun ProfilePanel(
    profiles: List<AdyenProfile>,
    activeProfile: AdyenProfile?,
    installationId: String?,
    boardingRequestToken: String?,
    onScanProfile: () -> Unit,
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
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    profiles.forEach { profile ->
                        item {
                            FilterChip(
                                selected = profile.id == activeProfile?.id,
                                onClick = { onSelectProfile(profile.id) },
                                label = { Text(profile.displayName, maxLines = 1) },
                            )
                        }
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
private fun PaymentsAppOperationsPanel(
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

@Composable
private fun DiagnosticsPanel(
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

@Composable
private fun ProductCard(product: Product, onAdd: () -> Unit) {
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
                        contentDescription = product.name,
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
                    product.category.uppercase(),
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
                product.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                product.description,
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
                    onClick = onAdd,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = ButtonDefaults.ContentPadding,
                ) {
                    Text(
                        "Add to cart",
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

@Composable
private fun CartPanel(
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
private fun TransactionHistoryPanel(
    records: List<TransactionRecord>,
    onInspect: (TransactionRecord) -> Unit,
    onClear: () -> Unit,
) {
    OutlinedCard(shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Transactions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (records.isEmpty()) "No payment attempts yet" else "${records.size} saved payment attempt${if (records.size == 1) "" else "s"}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onClear, enabled = records.isNotEmpty()) { Text("Clear") }
            }
            if (records.isEmpty()) {
                Text(
                    "Each checkout attempt will appear here with its Terminal API request and Adyen response.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                records.forEach { record ->
                    TransactionRow(record = record, onInspect = { onInspect(record) })
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(record: TransactionRecord, onInspect: () -> Unit) {
    OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(record.amountLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    TransactionStatusChip(record.status)
                }
                Text(
                    "${record.itemCount} item${if (record.itemCount == 1) "" else "s"} | ${record.saleToAcquirerDataName}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                record.failureReason?.let {
                    Text(
                        "Adyen issue: $it",
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            TextButton(onClick = onInspect) { Text("Inspect") }
        }
    }
}

@Composable
private fun TransactionStatusChip(status: TransactionStatus) {
    val label = when (status) {
        TransactionStatus.LAUNCHED -> "Pending"
        TransactionStatus.APPROVED -> "Approved"
        TransactionStatus.REFUSED -> "Refused"
        TransactionStatus.FAILED -> "Failed"
        TransactionStatus.REFUND_LAUNCHED -> "Refunding"
        TransactionStatus.REFUNDED -> "Refunded"
    }
    AssistChip(onClick = {}, label = { Text(label) })
}

@Composable
private fun LivePaymentConfirmationDialog(
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

@Composable
private fun PaymentResultDialog(result: PaymentResult, isRefund: Boolean, onDismiss: () -> Unit) {
    val title = when (result) {
        is PaymentResult.BoardingStatus -> "Boarding returned"
        is PaymentResult.Success -> if (isRefund) "Refund approved" else "Payment approved"
        is PaymentResult.Refused -> "Payment refused"
        is PaymentResult.Failure -> "Adyen result"
    }
    val message = when (result) {
        is PaymentResult.BoardingStatus -> {
            val state = if (result.boarded) "boarded" else "not boarded"
            listOfNotNull(
                "Adyen app is $state. Installation ID: ${result.installationId ?: "not supplied"}",
                result.returnData?.let { data ->
                    listOfNotNull(
                        data.merchantAccountCode?.let { "Previous merchant: $it" },
                        data.merchantStoreCode?.let { "Previous store: $it" },
                        data.reboarding?.takeIf { it }?.let { "Reboarding flow started" },
                    ).joinToString(" | ").ifBlank { null }
                },
                result.errorAdvice,
            ).joinToString("\n")
        }
        is PaymentResult.Success -> "Reference: ${result.pspReference ?: "not supplied"}"
        is PaymentResult.Refused -> result.reason ?: "No refusal reason supplied."
        is PaymentResult.Failure -> result.message
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text(title) },
        text = { Text(message) },
    )
}

@Composable
private fun TransactionDialog(
    record: TransactionRecord,
    onRefund: () -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedSection by remember { mutableStateOf("Request") }
    val requestInsight = remember(record.requestJson) { TerminalApiRequestInspector.inspect(record.requestJson) }
    val responseInsight = remember(record.responseBody) { TerminalApiResponseInspector.inspect(record.responseBody) }
    val highlights = remember(record.responseBody) { TerminalApiResponseInspector.compactSummary(record.responseBody) }
    val receipts = responseInsight?.receipts.orEmpty()
    val canRefund = record.status == TransactionStatus.APPROVED &&
        record.refundOfTransactionId == null &&
        (record.adyenTransactionId != null || responseInsight?.transactionId != null)
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize()
                .padding(vertical = 24.dp),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Transaction", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${record.amountLabel} | ${record.createdAt}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = onRefund, enabled = canRefund) { Text("Refund", maxLines = 1) }
                        TextButton(onClick = onDismiss) { Text("Close") }
                    }
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    FilterChip(
                        selected = selectedSection == "Request",
                        onClick = { selectedSection = "Request" },
                        label = { Text("Request") },
                    )
                    FilterChip(
                        selected = selectedSection == "Response",
                        onClick = { selectedSection = "Response" },
                        label = { Text("Response") },
                    )
                    FilterChip(
                        selected = selectedSection == "Receipt",
                        onClick = { selectedSection = "Receipt" },
                        enabled = record.responseBody != null,
                        label = { Text("Receipt") },
                    )
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                    item { TransactionStatusChip(record.status) }
                    when (selectedSection) {
                        "Request" -> {
                            item {
                                RequestSummary(record)
                            }
                            item {
                                Text("Terminal API request", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                            item {
                                DecodedSaleToAcquirerDataCard(requestInsight)
                            }
                            item { MonospaceBlock(record.requestJson) }
                        }
                        "Response" -> {
                            if (highlights.isNotEmpty()) {
                                item {
                                    OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Important response fields", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                            highlights.forEach { (label, value) ->
                                                KeyValueLine(label = label, value = value)
                                            }
                                        }
                                    }
                                }
                            }
                            record.failureReason?.let {
                                item {
                                    OutlinedCard(shape = RoundedCornerShape(8.dp)) {
                                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("Adyen failure detail", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                                            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                            item {
                                Text("Adyen response", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                            item {
                                Text(
                                    record.responseSummary ?: "No response has been received yet.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            responseInsight?.let { insight ->
                                item {
                                    ResponseFieldList(insight)
                                }
                            }
                            record.responseBody?.let { body ->
                                item {
                                    Text("Raw Terminal API response", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                }
                                item { MonospaceBlock(body) }
                            }
                            record.responseUri?.let { response ->
                                item {
                                    Text("Raw return URI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                }
                                item { MonospaceBlock(response) }
                            }
                        }
                        "Receipt" -> {
                            if (receipts.isEmpty()) {
                                item {
                                    Text(
                                        "No PaymentReceipt data was returned for this transaction.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            } else {
                                items(receipts) { receipt ->
                                    DigitalReceiptCard(receipt)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DigitalReceiptCard(receipt: PaymentReceipt) {
    val display = remember(receipt) { receipt.toDisplay() }
    OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(receipt.documentQualifier.receiptTitle(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Adyen-generated receipt data",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (receipt.requiredSignature) {
                    AssistChip(onClick = {}, label = { Text("Signature") })
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ReceiptHeader(display)
                display.total?.let { total ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Total", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(total, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                }
                display.status?.let { status ->
                    AssistChip(onClick = {}, label = { Text(status) })
                }
                if (display.details.isNotEmpty()) {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        display.details.forEach { (label, value) ->
                            ReceiptDetailLine(label, value)
                        }
                    }
                }
                if (display.footer.isNotEmpty()) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        display.footer.forEach { footer ->
                            Text(
                                footer,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptHeader(display: ReceiptDisplay) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            display.merchantName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        display.header.drop(1).forEach { header ->
            Text(
                header,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ReceiptDetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.42f))
        Text(value, textAlign = TextAlign.Right, fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.58f))
    }
}

private data class ReceiptDisplay(
    val merchantName: String,
    val header: List<String>,
    val status: String?,
    val total: String?,
    val details: List<Pair<String, String>>,
    val footer: List<String>,
)

private fun PaymentReceipt.displayLines(): List<ReceiptLine> {
    val rendered = mutableListOf<ReceiptLine>()
    var pending: ReceiptLine? = null
    lines.forEach { line ->
        val current = pending
        if (current == null) {
            pending = line
        } else {
            pending = current.copy(text = current.text + line.text)
        }
        if (line.endOfLine) {
            pending?.let(rendered::add)
            pending = null
        }
    }
    pending?.let(rendered::add)
    return rendered
}

private fun PaymentReceipt.toDisplay(): ReceiptDisplay {
    val lines = displayLines().map { it.text.trim() }.filter { it.isNotBlank() }
    val entries = lines.mapNotNull { it.receiptEntry() }
    val entryKeys = entries.map { it.first.normalizedReceiptKey() }.toSet()
    val headerEntries = entries.filter { it.first.normalizedReceiptKey().startsWith("header") }.map { it.second }
    val freeHeaders = lines
        .takeWhile { line -> line.receiptEntry()?.first?.normalizedReceiptKey()?.let { it in amountAndStatusKeys } != true }
        .filterNot { it.receiptEntry()?.first?.normalizedReceiptKey() in ignoredReceiptKeys }
        .filterNot { line -> entries.any { it.first.normalizedReceiptKey().startsWith("header") && it.second == line } }
        .take(3)
    val headers = (headerEntries + freeHeaders).distinct().ifEmpty { listOf("TapToPlay Boutique") }
    val total = entries.firstValue("totalAmount")
        ?: entries.firstValue("originalAmount")
        ?: entries.firstValue("shopperAmount")
    val status = when {
        entries.any { it.first.normalizedReceiptKey() == "approved" } -> "Approved"
        entries.any { it.first.normalizedReceiptKey() == "refused" } -> "Refused"
        entries.any { it.first.normalizedReceiptKey() == "void" } -> "Voided"
        else -> null
    }
    val details = receiptDetailOrder.mapNotNull { (key, label) ->
        entries.firstValue(key)?.let { label to it }
    }
    val footer = entries
        .filter { it.first.normalizedReceiptKey() in footerReceiptKeys }
        .map { it.second }
        .ifEmpty {
            lines.takeLast(2).filterNot { line ->
                line.receiptEntry()?.first?.normalizedReceiptKey() in entryKeys || line in headers
            }
        }
    return ReceiptDisplay(
        merchantName = headers.first(),
        header = headers,
        status = status,
        total = total,
        details = details,
        footer = footer,
    )
}

private fun String.receiptEntry(): Pair<String, String>? {
    val separators = listOf(": ", " : ", "=")
    val separator = separators.firstOrNull { contains(it) } ?: return null
    val parts = split(separator, limit = 2)
    val key = parts.getOrNull(0)?.trim().orEmpty()
    val value = parts.getOrNull(1)?.trim().orEmpty()
    return if (key.isBlank() || value.isBlank()) null else key to value
}

private fun List<Pair<String, String>>.firstValue(key: String): String? =
    firstOrNull { it.first.normalizedReceiptKey().equals(key, ignoreCase = true) }?.second

private fun String.normalizedReceiptKey(): String =
    filter { it.isLetterOrDigit() }.replaceFirstChar { it.lowercase() }

private val amountAndStatusKeys = setOf("totalAmount", "originalAmount", "shopperAmount", "approved", "refused", "void")

private val ignoredReceiptKeys = setOf("filler", "sigline", "signature", "merchantSigline")

private val footerReceiptKeys = setOf("thanks", "retain")

private val receiptDetailOrder = listOf(
    "txtype" to "Type",
    "paymentMethod" to "Payment method",
    "cardType" to "Card",
    "pan" to "Card number",
    "authCode" to "Authorisation",
    "txdate" to "Date",
    "txtime" to "Time",
    "mref" to "Reference",
    "txRef" to "Transaction reference",
    "tid" to "Terminal",
    "mid" to "Merchant ID",
    "rrn" to "RRN",
    "stan" to "STAN",
    "aid" to "AID",
)

private fun String.receiptTitle(): String = when (this) {
    "CustomerReceipt", "SaleReceipt" -> "Customer receipt"
    "CashierReceipt" -> "Merchant receipt"
    else -> this
}

private fun String?.receiptTextAlign(): TextAlign = when {
    equals("Centred", ignoreCase = true) || equals("Center", ignoreCase = true) -> TextAlign.Center
    equals("Right", ignoreCase = true) -> TextAlign.Right
    else -> TextAlign.Left
}

@Composable
private fun RequestSummary(record: TransactionRecord) {
    OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Request summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            KeyValueLine("Amount", record.amountLabel)
            KeyValueLine("Items", record.itemCount.toString())
            record.messageCategory?.let { KeyValueLine("Message category", it) }
            record.serviceId?.let { KeyValueLine("ServiceID", it) }
            record.saleTransactionId?.let { KeyValueLine("Sale transaction", it) }
            record.adyenTransactionId?.let { KeyValueLine("Adyen transaction", it) }
            record.refundOfTransactionId?.let { KeyValueLine("Refund of", it) }
        }
    }
}

@Composable
private fun DecodedSaleToAcquirerDataCard(insight: com.example.taptoplay.adyen.TerminalApiRequestInsight) {
    var expanded by remember { mutableStateOf(false) }
    OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Decoded SaleToAcquirerData", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (insight.saleToAcquirerDataJson == null) {
                            "This request does not include PaymentRequest.SaleData.SaleToAcquirerData."
                        } else {
                            "Raw JSON sent inside PaymentRequest.SaleData.SaleToAcquirerData"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(
                    onClick = { expanded = !expanded },
                    enabled = insight.saleToAcquirerDataJson != null || insight.saleToAcquirerDataBase64 != null,
                ) {
                    Text(if (expanded) "Hide" else "Decode")
                }
            }
            if (expanded) {
                insight.saleToAcquirerDataJson?.let { decoded ->
                    MonospaceBlock(decoded)
                }
                if (insight.saleToAcquirerDataJson == null) {
                    insight.saleToAcquirerDataBase64?.let { encoded ->
                        Text("Base64 value could not be decoded as JSON.", color = MaterialTheme.colorScheme.error)
                        MonospaceBlock(encoded)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResponseFieldList(insight: com.example.taptoplay.adyen.TerminalApiResponseInsight) {
    OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Readable response", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            KeyValueLine("Category", insight.category)
            insight.result?.let { KeyValueLine("Result", it) }
            insight.transactionId?.let { KeyValueLine("Transaction ID", it) }
            insight.errorCondition?.let { KeyValueLine("Error condition", it) }
            if (insight.additionalResponseFields.isNotEmpty()) {
                Text("AdditionalResponse", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                insight.additionalResponseFields.forEach { field ->
                    ExpandableValueRow(
                        label = field.name,
                        value = field.decodedValue?.let { "${field.value}\n\nDecoded:\n$it" } ?: field.value,
                    )
                }
            } else if (!insight.additionalResponseRaw.isNullOrBlank()) {
                ExpandableValueRow("AdditionalResponse raw", insight.additionalResponseRaw)
            }
            insight.additionalResponseDecoded?.let { decoded ->
                Text("AdditionalResponse decoded JSON", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                JsonNodeRow(name = "additionalResponse", value = decoded, depth = 0)
            }
        }
    }
}

@Composable
private fun KeyValueLine(label: String, value: String) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Text(
            value,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ExpandableValueRow(label: String, value: String) {
    var expanded by remember { mutableStateOf(false) }
    OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        value,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (expanded) Int.MAX_VALUE else 2,
                        overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                    )
                }
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide" else "View")
                }
            }
        }
    }
}

@Composable
private fun MonospaceBlock(text: String) {
    OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        SelectionContainer {
            Text(
                text = text,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SaleToAcquirerDataDialog(
    config: SaleToAcquirerDataConfig,
    onEdit: (List<String>, String) -> Unit,
    onRemove: (List<String>) -> Unit,
    onApply: () -> Unit,
    onSaveFavorite: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .height(620.dp),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.fillMaxWidth()) {
                        Text("SaleToAcquirerData", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${config.displayName} | ${config.fieldCount} fields",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        item {
                            OutlinedButton(onClick = onSaveFavorite) { Text("Save", maxLines = 1) }
                        }
                        item {
                            OutlinedButton(onClick = onApply) { Text("Apply", maxLines = 1) }
                        }
                        item {
                            TextButton(onClick = onDismiss) { Text("Close", maxLines = 1) }
                        }
                    }
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                    items(config.data.entries.toList()) { (key, value) ->
                        JsonNodeRow(
                            name = key,
                            value = value,
                            depth = 0,
                            path = listOf(key),
                            editable = true,
                            onEdit = onEdit,
                            onRemove = onRemove,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JsonNodeRow(name: String, value: JsonElement, depth: Int) {
    JsonNodeRow(
        name = name,
        value = value,
        depth = depth,
        path = emptyList(),
        editable = false,
        onEdit = { _, _ -> },
        onRemove = {},
    )
}

@Composable
private fun JsonNodeRow(
    name: String,
    value: JsonElement,
    depth: Int,
    path: List<String>,
    editable: Boolean,
    onEdit: (List<String>, String) -> Unit,
    onRemove: (List<String>) -> Unit,
) {
    var expanded by remember { mutableStateOf(depth == 0) }
    var editing by remember { mutableStateOf(false) }
    var editedValue by remember(value) { mutableStateOf(value.editableText()) }
    val isExpandable = value is JsonObject || value is JsonArray
    val summary = value.summary()
    OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (12 + depth * 12).dp, top = 10.dp, end = 12.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(summary, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (isExpandable) {
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(if (expanded) "Hide" else "View")
                    }
                } else if (editable) {
                    TextButton(onClick = { editing = !editing }) {
                        Text(if (editing) "Cancel" else "Edit")
                    }
                }
            }
            if (editing && editable && !isExpandable) {
                OutlinedTextField(
                    value = editedValue,
                    onValueChange = { editedValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Value") },
                    singleLine = false,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onEdit(path, editedValue)
                            editing = false
                        },
                    ) {
                        Text("Save")
                    }
                    TextButton(onClick = { onRemove(path) }) {
                        Text("Remove")
                    }
                }
            }
            if (editable && isExpandable && depth > 0) {
                TextButton(onClick = { onRemove(path) }) {
                    Text("Remove group")
                }
            }
            if (expanded && value is JsonObject) {
                value.entries.forEach { (childKey, childValue) ->
                    JsonNodeRow(
                        name = childKey,
                        value = childValue,
                        depth = depth + 1,
                        path = path + childKey,
                        editable = editable,
                        onEdit = onEdit,
                        onRemove = onRemove,
                    )
                }
            }
            if (expanded && value is JsonArray) {
                value.forEachIndexed { index, childValue ->
                    JsonNodeRow(
                        name = "[$index]",
                        value = childValue,
                        depth = depth + 1,
                        path = path + index.toString(),
                        editable = false,
                        onEdit = onEdit,
                        onRemove = onRemove,
                    )
                }
            }
        }
    }
}

private fun JsonElement.summary(): String = when (this) {
    is JsonObject -> "${size} field${if (size == 1) "" else "s"}"
    is JsonArray -> "${size} item${if (size == 1) "" else "s"}"
    is JsonPrimitive -> when {
        isString -> contentOrNull.orEmpty()
        booleanOrNull != null -> booleanOrNull.toString()
        longOrNull != null -> longOrNull.toString()
        doubleOrNull != null -> doubleOrNull.toString()
        else -> toString()
    }
}

private fun JsonElement.editableText(): String = when (this) {
    is JsonPrimitive -> contentOrNull ?: toString()
    else -> toString()
}

private fun String.maskForDisplay(): String = when {
    isBlank() -> "not set"
    length <= 8 -> "****"
    else -> take(4) + "..." + takeLast(4)
}

private fun formatMoney(minor: Long): String = "EUR %.2f".format(minor / 100.0)
