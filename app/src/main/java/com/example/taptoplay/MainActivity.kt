package com.example.taptoplay

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.example.taptoplay.adyen.AdyenLinks
import com.example.taptoplay.adyen.AndroidBoardingStateStore
import com.example.taptoplay.adyen.AndroidTransactionStore
import com.example.taptoplay.adyen.NexoCrypto
import com.example.taptoplay.adyen.PaymentResult
import com.example.taptoplay.adyen.PaymentResultParser
import com.example.taptoplay.adyen.PaymentsAppApiClient
import com.example.taptoplay.adyen.PaymentsAppInstance
import com.example.taptoplay.adyen.PaymentsAppStatus
import com.example.taptoplay.adyen.SaleToAcquirerDataConfig
import com.example.taptoplay.adyen.SaleToAcquirerDataFavoriteStore
import com.example.taptoplay.adyen.SaleToAcquirerDataQrParser
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
import com.example.taptoplay.cart.CartLine
import com.example.taptoplay.profiles.AndroidProfileStore
import com.example.taptoplay.profiles.AdyenProfile
import com.example.taptoplay.profiles.LocalProfileBootstrap
import com.example.taptoplay.profiles.ProfileQrParser
import com.example.taptoplay.ui.AppScreen
import com.example.taptoplay.ui.TapToPlayApp
import com.example.taptoplay.ui.formatMoney
import com.example.taptoplay.ui.maskForDisplay
import com.example.taptoplay.ui.screenForAdyenReturn
import com.example.taptoplay.ui.theme.TapToPlayTheme
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private var selectedScreenState by mutableStateOf(AppScreen.Catalog)
    private var pendingReturnScreenState by mutableStateOf<AppScreen?>(null)
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
        selectedScreenState = AppScreen.PaymentsApp
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
        selectedScreenState = AppScreen.Checkout
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
        selectedScreenState = savedInstanceState?.screen(KEY_SELECTED_SCREEN) ?: AppScreen.Catalog
        pendingReturnScreenState = savedInstanceState?.screen(KEY_PENDING_RETURN_SCREEN)
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
                    selectedScreen = selectedScreenState,
                    onSelectScreen = { selectedScreenState = it },
                    onDismissResult = { paymentResultState = null },
                    onScanProfile = {
                        selectedScreenState = AppScreen.PaymentsApp
                        scanQr()
                    },
                    onScanSaleToAcquirerData = {
                        selectedScreenState = AppScreen.Checkout
                        scanSaleToAcquirerDataQr()
                    },
                    onUpdateSaleToAcquirerData = { config ->
                        selectedScreenState = AppScreen.Checkout
                        saleToAcquirerDataConfigState = config
                        statusState = "SaleToAcquirerData updated from the field editor."
                    },
                    onSaveSaleToAcquirerDataFavorite = { config ->
                        selectedScreenState = AppScreen.Checkout
                        saleToAcquirerDataFavoriteStore.save(config)
                        reloadSaleToAcquirerDataFavorites()
                        statusState = "Saved ${config.displayName} as a SaleToAcquirerData favorite."
                    },
                    onApplySaleToAcquirerDataFavorite = { config ->
                        selectedScreenState = AppScreen.Checkout
                        saleToAcquirerDataConfigState = config
                        statusState = "SaleToAcquirerData favorite applied: ${config.displayName}."
                    },
                    onRemoveSaleToAcquirerDataFavorite = { config ->
                        selectedScreenState = AppScreen.Checkout
                        saleToAcquirerDataFavoriteStore.remove(config.displayName)
                        reloadSaleToAcquirerDataFavorites()
                        statusState = "Removed SaleToAcquirerData favorite: ${config.displayName}."
                    },
                    onClearSaleToAcquirerData = {
                        selectedScreenState = AppScreen.Checkout
                        saleToAcquirerDataConfigState = SaleToAcquirerDataConfig.default()
                        statusState = "SaleToAcquirerData reset to retail demo defaults."
                    },
                    onClearTransactions = {
                        selectedScreenState = AppScreen.Transactions
                        transactionStore.clear()
                        pendingTransactionIdState = null
                        reloadTransactions()
                        statusState = "Transaction history cleared."
                    },
                    onSelectProfile = {
                        selectedScreenState = AppScreen.PaymentsApp
                        profileStore.setActive(it)
                        reloadProfiles()
                        reloadBoardingState()
                        paymentsAppInstancesState = emptyList()
                        paymentsAppStatusState = "Payments App instances not loaded"
                        statusState = "Active profile switched deliberately."
                    },
                    onRemoveProfile = { profile ->
                        selectedScreenState = AppScreen.PaymentsApp
                        removeProfile(profile)
                    },
                    onCheckBoarding = { profile ->
                        selectedScreenState = AppScreen.PaymentsApp
                        launchLink(AdyenLinks.boarded(profile), AppScreen.PaymentsApp)
                    },
                    onBoard = { profile ->
                        selectedScreenState = AppScreen.PaymentsApp
                        board(profile)
                    },
                    onReboard = { profile ->
                        selectedScreenState = AppScreen.PaymentsApp
                        launchLink(AdyenLinks.startReboard(profile), AppScreen.PaymentsApp)
                    },
                    onRefreshPaymentsApps = { profile ->
                        selectedScreenState = AppScreen.PaymentsApp
                        refreshPaymentsApps(profile)
                    },
                    onRevokePaymentsApp = { profile, instance ->
                        selectedScreenState = AppScreen.PaymentsApp
                        revokePaymentsApp(profile, instance)
                    },
                    onPay = { profile, lines, totalMinor ->
                        selectedScreenState = AppScreen.Checkout
                        pay(profile, lines, totalMinor)
                    },
                    onRefund = { record ->
                        selectedScreenState = AppScreen.Transactions
                        refund(record)
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleReturnIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_SELECTED_SCREEN, selectedScreenState.name)
        pendingReturnScreenState?.let { outState.putString(KEY_PENDING_RETURN_SCREEN, it.name) }
        super.onSaveInstanceState(outState)
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
        selectedScreenState = AppScreen.PaymentsApp
        qrLauncher.launch(
            ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt("Scan a TapToPlay Adyen profile QR")
                .setBeepEnabled(false),
        )
    }

    private fun scanSaleToAcquirerDataQr() {
        selectedScreenState = AppScreen.Checkout
        saleToAcquirerDataQrLauncher.launch(
            ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt("Scan a SaleToAcquirerData properties QR")
                .setBeepEnabled(false),
        )
    }

    private fun board(profile: AdyenProfile) {
        selectedScreenState = AppScreen.PaymentsApp
        val requestToken = boardingRequestTokenState
        if (requestToken.isNullOrBlank()) {
            statusState = "Check boarding first so Adyen can return a boarding request token."
            launchLink(AdyenLinks.boarded(profile), AppScreen.PaymentsApp)
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
                    launchLink(AdyenLinks.board(profile, response.boardingToken), AppScreen.PaymentsApp)
                }
                .onFailure { statusState = "Boarding token failed: ${it.message}" }
        }
    }

    private fun removeProfile(profile: AdyenProfile) {
        selectedScreenState = AppScreen.PaymentsApp
        boardingStateStore.clear(profile.id)
        profileStore.remove(profile.id)
        reloadProfiles()
        reloadBoardingState()
        paymentsAppInstancesState = emptyList()
        paymentsAppStatusState = "Payments App instances not loaded"
        statusState = "Removed ${profile.displayName} and cleared its local boarding state."
    }

    private fun refreshPaymentsApps(profile: AdyenProfile) {
        selectedScreenState = AppScreen.PaymentsApp
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
        selectedScreenState = AppScreen.PaymentsApp
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
            selectedScreenState = AppScreen.PaymentsApp
            statusState = "Check boarding first. Payments require an installation ID as POIID."
            launchLink(AdyenLinks.boarded(profile), AppScreen.PaymentsApp)
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
        launchLink(AdyenLinks.nexo(profile, encoded), AppScreen.Transactions)
    }

    private fun refund(record: TransactionRecord) {
        selectedScreenState = AppScreen.Transactions
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
        launchLink(AdyenLinks.nexo(profile, encoded), AppScreen.Transactions)
    }

    private fun launchLink(rawUrl: String, returnScreen: AppScreen) {
        pendingReturnScreenState = returnScreen
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(rawUrl)))
    }

    private fun handleReturnIntent(intent: Intent?) {
        val rawUri = intent?.data?.toString()
        val activeProfile = profilesState.firstOrNull { it.id == activeProfileIdState }
        val parsed = PaymentResultParser.parse(rawUri ?: return, activeProfile, nexoCrypto) ?: return
        selectedScreenState = pendingReturnScreenState ?: screenForAdyenReturn(parsed)
        pendingReturnScreenState = null
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

private const val KEY_SELECTED_SCREEN = "selectedScreen"
private const val KEY_PENDING_RETURN_SCREEN = "pendingReturnScreen"

private fun Bundle.screen(key: String): AppScreen? =
    getString(key)?.let { name ->
        AppScreen.entries.firstOrNull { it.name == name }
    }
