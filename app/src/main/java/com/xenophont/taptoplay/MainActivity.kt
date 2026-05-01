package com.xenophont.taptoplay

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.xenophont.taptoplay.adyen.AdyenLinks
import com.xenophont.taptoplay.adyen.AdyenManagementApiClient
import com.xenophont.taptoplay.adyen.AndroidBoardingStateStore
import com.xenophont.taptoplay.adyen.AndroidTransactionStore
import com.xenophont.taptoplay.adyen.NexoCrypto
import com.xenophont.taptoplay.adyen.PaymentResult
import com.xenophont.taptoplay.adyen.PaymentResultParser
import com.xenophont.taptoplay.adyen.PaymentsAppApiClient
import com.xenophont.taptoplay.adyen.PaymentsAppInstance
import com.xenophont.taptoplay.adyen.PaymentsAppStatus
import com.xenophont.taptoplay.adyen.SaleToAcquirerDataConfig
import com.xenophont.taptoplay.adyen.SaleToAcquirerDataFavoriteStore
import com.xenophont.taptoplay.adyen.SaleToAcquirerDataQrParser
import com.xenophont.taptoplay.adyen.TerminalApiResponseInspector
import com.xenophont.taptoplay.adyen.TerminalPaymentRequestBuilder
import com.xenophont.taptoplay.adyen.TransactionRecord
import com.xenophont.taptoplay.adyen.TransactionStatus
import com.xenophont.taptoplay.adyen.pspReferenceOrNull
import com.xenophont.taptoplay.adyen.responseJsonOrNull
import com.xenophont.taptoplay.adyen.serviceIdOrNull
import com.xenophont.taptoplay.adyen.toTransactionStatus
import com.xenophont.taptoplay.adyen.transactionIdOrNull
import com.xenophont.taptoplay.cart.CartLine
import com.xenophont.taptoplay.profiles.AndroidProfileStore
import com.xenophont.taptoplay.profiles.AdyenProfile
import com.xenophont.taptoplay.profiles.CredentialQrDocumentation
import com.xenophont.taptoplay.profiles.LocalProfileBootstrap
import com.xenophont.taptoplay.profiles.ProfileQrParser
import com.xenophont.taptoplay.ui.AppScreen
import com.xenophont.taptoplay.ui.AppLanguage
import com.xenophont.taptoplay.ui.AppLanguageStore
import com.xenophont.taptoplay.ui.LocalTapToPlayStrings
import com.xenophont.taptoplay.ui.TapToPlayApp
import com.xenophont.taptoplay.ui.formatMoney
import com.xenophont.taptoplay.ui.maskForDisplay
import com.xenophont.taptoplay.ui.screenForAdyenReturn
import com.xenophont.taptoplay.ui.stringsFor
import com.xenophont.taptoplay.ui.theme.TapToPlayTheme
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
    private lateinit var languageStore: AppLanguageStore
    private val qrParser = ProfileQrParser()
    private val saleToAcquirerDataQrParser = SaleToAcquirerDataQrParser()
    private val managementApiClient = AdyenManagementApiClient()
    private val paymentsAppApiClient = PaymentsAppApiClient()
    private val nexoCrypto = NexoCrypto()

    private var profilesState by mutableStateOf(emptyList<AdyenProfile>())
    private var activeProfileIdState by mutableStateOf<String?>(null)
    private var paymentResultState by mutableStateOf<PaymentResult?>(null)
    private var paymentResultIsRefundState by mutableStateOf(false)
    private var selectedScreenState by mutableStateOf(AppScreen.Catalog)
    private var selectedLanguageState by mutableStateOf(AppLanguage.English)
    private var pendingReturnScreenState by mutableStateOf<AppScreen?>(null)
    private var showDrawerHintState by mutableStateOf(false)
    private var statusState by mutableStateOf(stringsFor(AppLanguage.English)["status_ready"])
    private var installationIdState by mutableStateOf<String?>(null)
    private var boardingRequestTokenState by mutableStateOf<String?>(null)
    private var boardingTokenIssuedState by mutableStateOf(false)
    private var showPaymentsAppDownloadPromptState by mutableStateOf(false)
    private var saleToAcquirerDataConfigState by mutableStateOf(SaleToAcquirerDataConfig.default())
    private var saleToAcquirerDataFavoritesState by mutableStateOf(emptyList<SaleToAcquirerDataConfig>())
    private var transactionHistoryState by mutableStateOf(emptyList<TransactionRecord>())
    private var pendingTransactionIdState by mutableStateOf<String?>(null)
    private var paymentsAppInstancesState by mutableStateOf(emptyList<PaymentsAppInstance>())
    private var paymentsAppStatusState by mutableStateOf(stringsFor(AppLanguage.English)["status_payments_instances_not_loaded"])

    private val strings
        get() = stringsFor(selectedLanguageState)

    private val qrLauncher = registerForActivityResult(ScanContract()) { result ->
        selectScreen(AppScreen.PaymentsApp)
        val contents = result.contents ?: return@registerForActivityResult
        qrParser.parse(contents)
            .onSuccess { profile -> importScannedProfile(profile) }
            .onFailure { statusState = strings.format("status_qr_rejected", it.message.orEmpty()) }
    }

    private val saleToAcquirerDataQrLauncher = registerForActivityResult(ScanContract()) { result ->
        selectScreen(AppScreen.Checkout)
        val contents = result.contents ?: return@registerForActivityResult
        saleToAcquirerDataQrParser.parse(contents)
            .onSuccess { config ->
                saleToAcquirerDataConfigState = config
                statusState = strings.format("status_sale_to_acquirer_loaded", config.displayName)
            }
            .onFailure { statusState = strings.format("status_sale_to_acquirer_rejected", it.message.orEmpty()) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        selectedScreenState = savedInstanceState?.screen(KEY_SELECTED_SCREEN) ?: AppScreen.Catalog
        pendingReturnScreenState = savedInstanceState?.screen(KEY_PENDING_RETURN_SCREEN)
        showDrawerHintState = savedInstanceState == null
        profileStore = AndroidProfileStore(this)
        transactionStore = AndroidTransactionStore(this)
        boardingStateStore = AndroidBoardingStateStore(this)
        saleToAcquirerDataFavoriteStore = SaleToAcquirerDataFavoriteStore(this)
        languageStore = AppLanguageStore(this)
        selectedLanguageState = languageStore.selected()
        statusState = strings["status_ready"]
        paymentsAppStatusState = strings["status_payments_instances_not_loaded"]
        LocalProfileBootstrap.profileOrNull()?.let { bootstrap ->
            if (profileStore.profiles().none { it.id == bootstrap.id }) profileStore.save(bootstrap)
            if (profileStore.activeProfileId() == null) profileStore.setActive(bootstrap.id)
        }
        reloadProfiles()
        reloadBoardingState()
        reloadSaleToAcquirerDataFavorites()
        reloadTransactions()
        if (selectedScreenState == AppScreen.PaymentsApp) {
            handlePaymentsAppEntered()
        }
        handleReturnIntent(intent)

        setContent {
            CompositionLocalProvider(LocalTapToPlayStrings provides stringsFor(selectedLanguageState)) {
                TapToPlayTheme {
                    TapToPlayApp(
                        profiles = profilesState,
                        activeProfileId = activeProfileIdState,
                        installationId = installationIdState,
                        boardingRequestToken = boardingRequestTokenState,
                        boardingTokenIssued = boardingTokenIssuedState,
                        showPaymentsAppDownloadPrompt = showPaymentsAppDownloadPromptState,
                        saleToAcquirerDataConfig = saleToAcquirerDataConfigState,
                        saleToAcquirerDataFavorites = saleToAcquirerDataFavoritesState,
                        transactionHistory = transactionHistoryState,
                        paymentsAppInstances = paymentsAppInstancesState,
                        paymentsAppStatus = paymentsAppStatusState,
                        status = statusState,
                        paymentResult = paymentResultState,
                        paymentResultIsRefund = paymentResultIsRefundState,
                        selectedScreen = selectedScreenState,
                        selectedLanguage = selectedLanguageState,
                        onSelectScreen = { selectScreen(it) },
                        onSelectLanguage = { selectLanguage(it) },
                        showDrawerHint = showDrawerHintState,
                        onDrawerHintShown = { markDrawerHintShown() },
                        onDismissResult = { paymentResultState = null },
                        onScanProfile = {
                            selectScreen(AppScreen.PaymentsApp)
                            scanQr()
                        },
                        onOpenCredentialQrDocs = {
                            selectScreen(AppScreen.PaymentsApp)
                            openCredentialQrDocs()
                        },
                        onDownloadPaymentsApp = { profile ->
                            selectScreen(AppScreen.PaymentsApp)
                            openPaymentsAppDownload(profile)
                        },
                        onScanSaleToAcquirerData = {
                            selectScreen(AppScreen.Checkout)
                            scanSaleToAcquirerDataQr()
                        },
                        onUpdateSaleToAcquirerData = { config ->
                            selectScreen(AppScreen.Checkout)
                            saleToAcquirerDataConfigState = config
                            statusState = strings["status_sale_to_acquirer_updated"]
                        },
                        onSaveSaleToAcquirerDataFavorite = { config ->
                            selectScreen(AppScreen.Checkout)
                            saleToAcquirerDataFavoriteStore.save(config)
                            reloadSaleToAcquirerDataFavorites()
                            statusState = strings.format("status_favorite_saved", config.displayName)
                        },
                        onApplySaleToAcquirerDataFavorite = { config ->
                            selectScreen(AppScreen.Checkout)
                            saleToAcquirerDataConfigState = config
                            statusState = strings.format("status_favorite_applied", config.displayName)
                        },
                        onRemoveSaleToAcquirerDataFavorite = { config ->
                            selectScreen(AppScreen.Checkout)
                            saleToAcquirerDataFavoriteStore.remove(config.displayName)
                            reloadSaleToAcquirerDataFavorites()
                            statusState = strings.format("status_favorite_removed", config.displayName)
                        },
                        onClearSaleToAcquirerData = {
                            selectScreen(AppScreen.Checkout)
                            saleToAcquirerDataConfigState = SaleToAcquirerDataConfig.default()
                            statusState = strings["status_sale_to_acquirer_reset"]
                        },
                        onClearTransactions = {
                            selectScreen(AppScreen.Transactions)
                            transactionStore.clear()
                            pendingTransactionIdState = null
                            reloadTransactions()
                            statusState = strings["status_transactions_cleared"]
                        },
                        onSelectProfile = {
                            selectScreen(AppScreen.PaymentsApp)
                            profileStore.setActive(it)
                            reloadProfiles()
                            reloadBoardingState()
                            paymentsAppInstancesState = emptyList()
                            paymentsAppStatusState = strings["status_payments_instances_not_loaded"]
                            boardingTokenIssuedState = false
                            showPaymentsAppDownloadPromptState = false
                            statusState = strings["status_active_profile_switched"]
                        },
                        onRemoveProfile = { profile ->
                            selectScreen(AppScreen.PaymentsApp)
                            removeProfile(profile)
                        },
                        onCheckBoarding = { profile ->
                            selectScreen(AppScreen.PaymentsApp)
                            checkBoarding(profile)
                        },
                        onBoard = { profile ->
                            selectScreen(AppScreen.PaymentsApp)
                            board(profile)
                        },
                        onReboard = { profile ->
                            selectScreen(AppScreen.PaymentsApp)
                            launchPaymentsAppLink(profile, AdyenLinks.startReboard(profile), AppScreen.PaymentsApp)
                        },
                        onRefreshPaymentsApps = { profile ->
                            selectScreen(AppScreen.PaymentsApp)
                            refreshPaymentsApps(profile)
                        },
                        onRevokePaymentsApp = { profile, instance ->
                            selectScreen(AppScreen.PaymentsApp)
                            revokePaymentsApp(profile, instance)
                        },
                        onPay = { profile, lines, totalMinor ->
                            selectScreen(AppScreen.Checkout)
                            pay(profile, lines, totalMinor)
                        },
                        onRefund = { record ->
                            selectScreen(AppScreen.Transactions)
                            refund(record)
                        },
                    )
                }
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

    private fun selectScreen(screen: AppScreen) {
        val previousScreen = selectedScreenState
        selectedScreenState = screen
        if (screen == AppScreen.PaymentsApp && previousScreen != AppScreen.PaymentsApp) {
            handlePaymentsAppEntered()
        }
    }

    private fun handlePaymentsAppEntered() {
        profilesState.firstOrNull { it.id == activeProfileIdState }?.let { profile ->
            refreshPaymentsApps(profile)
        }
    }

    private fun markDrawerHintShown() {
        showDrawerHintState = false
    }

    private fun scanQr() {
        selectScreen(AppScreen.PaymentsApp)
        qrLauncher.launch(
            ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt(strings["scan_profile_prompt"])
                .setBeepEnabled(false),
        )
    }

    private fun scanSaleToAcquirerDataQr() {
        selectScreen(AppScreen.Checkout)
        saleToAcquirerDataQrLauncher.launch(
            ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt(strings["scan_sale_to_acquirer_prompt"])
                .setBeepEnabled(false),
        )
    }

    private fun importScannedProfile(profile: AdyenProfile) {
        if (profile.storeId.isNullOrBlank()) {
            saveImportedProfile(profile, strings.format("status_profile_active_updated", profile.profileName))
            return
        }
        statusState = strings.format("status_resolving_store", profile.storeId.orEmpty())
        lifecycleScope.launch {
            val storeResult = withContext(Dispatchers.IO) { managementApiClient.findStoreForProfile(profile) }
            val resolvedProfile = storeResult.getOrNull()
                ?.takeIf { it.profileName.isNotBlank() }
                ?.let { profile.copy(storeName = it.profileName) }
                ?: profile
            val statusMessage = storeResult.fold(
                onSuccess = { store ->
                    when (store) {
                        null -> strings.format("status_store_not_found", profile.profileName, profile.storeId.orEmpty(), profile.merchantId)
                        else -> strings.format("status_store_resolved", resolvedProfile.profileName)
                    }
                },
                onFailure = { error ->
                    strings.format("status_store_lookup_failed", profile.profileName, error.message.orEmpty())
                },
            )
            saveImportedProfile(resolvedProfile, statusMessage)
        }
    }

    private fun saveImportedProfile(profile: AdyenProfile, statusMessage: String) {
        profileStore.save(profile)
        profileStore.setActive(profile.id)
        reloadProfiles()
        reloadBoardingState()
        paymentsAppInstancesState = emptyList()
        paymentsAppStatusState = strings["status_payments_instances_not_loaded"]
        boardingTokenIssuedState = false
        showPaymentsAppDownloadPromptState = false
        statusState = statusMessage
        if (selectedScreenState == AppScreen.PaymentsApp) {
            refreshPaymentsApps(profile)
        }
    }

    private fun checkBoarding(profile: AdyenProfile) {
        selectScreen(AppScreen.PaymentsApp)
        launchPaymentsAppLink(profile, AdyenLinks.boarded(profile), AppScreen.PaymentsApp)
    }

    private fun board(profile: AdyenProfile) {
        selectScreen(AppScreen.PaymentsApp)
        val requestToken = boardingRequestTokenState
        if (requestToken.isNullOrBlank()) {
            statusState = strings["status_check_boarding_first"]
            checkBoarding(profile)
            return
        }
        statusState = strings["status_requesting_boarding_token"]
        boardingTokenIssuedState = false
        lifecycleScope.launch {
            val tokenResult = withContext(Dispatchers.IO) { paymentsAppApiClient.createBoardingToken(profile, requestToken) }
            tokenResult
                .onSuccess { response ->
                    installationIdState = response.installationId ?: installationIdState
                    response.installationId?.let { boardingStateStore.saveInstallationId(profile.id, it) }
                    boardingRequestTokenState = null
                    boardingStateStore.clearBoardingRequestToken(profile.id)
                    boardingTokenIssuedState = true
                    statusState = strings["status_opening_finish_boarding"]
                    launchPaymentsAppLink(profile, AdyenLinks.board(profile, response.boardingToken), AppScreen.PaymentsApp)
                }
                .onFailure { statusState = strings.format("status_boarding_token_failed", it.message.orEmpty()) }
        }
    }

    private fun removeProfile(profile: AdyenProfile) {
        selectScreen(AppScreen.PaymentsApp)
        boardingStateStore.clear(profile.id)
        profileStore.remove(profile.id)
        reloadProfiles()
        reloadBoardingState()
        paymentsAppInstancesState = emptyList()
        paymentsAppStatusState = strings["status_payments_instances_not_loaded"]
        boardingTokenIssuedState = false
        showPaymentsAppDownloadPromptState = false
        statusState = strings.format("status_profile_removed", profile.profileName)
    }

    private fun refreshPaymentsApps(profile: AdyenProfile) {
        paymentsAppStatusState = strings["status_refreshing_payments_instances"]
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { paymentsAppApiClient.listPaymentsApps(profile) }
            result
                .onSuccess { instances ->
                    paymentsAppInstancesState = instances
                    paymentsAppStatusState = if (instances.isEmpty()) {
                        strings["status_no_payments_instances"]
                    } else {
                        strings.loadedPaymentInstances(instances.size)
                    }
                }
                .onFailure {
                    paymentsAppStatusState = strings.format("status_payments_lookup_failed", it.message.orEmpty())
                }
        }
    }

    private fun revokePaymentsApp(profile: AdyenProfile, instance: PaymentsAppInstance) {
        selectScreen(AppScreen.PaymentsApp)
        setPaymentsAppStatus(strings.format("status_revoking_instance", instance.installationId.maskForDisplay()))
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
                    setPaymentsAppStatus(strings.format("status_revoked_instance", instance.installationId.maskForDisplay()))
                }
                .onFailure {
                    setPaymentsAppStatus(strings.format("status_revoke_failed", it.message.orEmpty()))
                }
        }
    }

    private fun setPaymentsAppStatus(message: String) {
        paymentsAppStatusState = message
        statusState = message
    }

    private fun pay(profile: AdyenProfile, lines: List<CartLine>, totalMinor: Long) {
        val installationId = installationIdState
        if (installationId.isNullOrBlank()) {
            selectScreen(AppScreen.PaymentsApp)
            statusState = strings["status_need_installation_id"]
            checkBoarding(profile)
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
        statusState = strings["status_opening_payment_app"]
        launchLink(AdyenLinks.nexo(profile, encoded), AppScreen.Transactions)
    }

    private fun launchPaymentsAppLink(profile: AdyenProfile, rawUrl: String, returnScreen: AppScreen) {
        if (!isPaymentsAppInstalled(profile)) {
            showPaymentsAppDownloadPromptState = true
            val environment = strings.environmentLabel(profile.environment)
            statusState = strings.format("status_payments_app_missing", environment)
            return
        }
        showPaymentsAppDownloadPromptState = false
        launchLink(rawUrl, returnScreen)
    }

    @Suppress("DEPRECATION")
    private fun isPaymentsAppInstalled(profile: AdyenProfile): Boolean =
        runCatching {
            packageManager.getPackageInfo(AdyenLinks.paymentsAppPackageName(profile), 0)
        }.isSuccess

    private fun openPaymentsAppDownload(profile: AdyenProfile) {
        showPaymentsAppDownloadPromptState = true
        val environment = strings.environmentLabel(profile.environment)
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AdyenLinks.paymentsAppPlayStore(profile))))
            statusState = strings.format("status_opening_google_play", environment)
        } catch (_: ActivityNotFoundException) {
            statusState = strings.format("status_no_browser_google_play", environment)
        }
    }

    private fun refund(record: TransactionRecord) {
        selectScreen(AppScreen.Transactions)
        val originalTransactionId = record.adyenTransactionId
            ?: TerminalApiResponseInspector.inspect(record.responseBody)?.transactionId
        if (originalTransactionId.isNullOrBlank()) {
            statusState = strings["status_refund_missing_transaction"]
            return
        }
        val profile = record.profileId?.let { profileId -> profilesState.firstOrNull { it.id == profileId } }
            ?: profilesState.firstOrNull { it.id == activeProfileIdState }
        if (profile == null) {
            statusState = strings["status_refund_missing_profile"]
            return
        }
        val installationId = record.installationId ?: installationIdState
        if (installationId.isNullOrBlank()) {
            statusState = strings["status_refund_missing_installation"]
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
                saleToAcquirerDataName = strings["refund"],
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
        statusState = strings["status_opening_refund"]
        launchLink(AdyenLinks.nexo(profile, encoded), AppScreen.Transactions)
    }

    private fun launchLink(rawUrl: String, returnScreen: AppScreen) {
        pendingReturnScreenState = returnScreen
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(rawUrl)))
    }

    private fun openCredentialQrDocs() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(CredentialQrDocumentation.URL)))
        } catch (_: ActivityNotFoundException) {
            statusState = strings["status_no_browser_qr_docs"]
        }
    }

    private fun handleReturnIntent(intent: Intent?) {
        val rawUri = intent?.data?.toString()
        val activeProfile = profilesState.firstOrNull { it.id == activeProfileIdState }
        val parsed = PaymentResultParser.parse(rawUri ?: return, activeProfile, nexoCrypto) ?: return
        selectScreen(pendingReturnScreenState ?: screenForAdyenReturn(parsed))
        pendingReturnScreenState = null
        if (parsed is PaymentResult.BoardingStatus) {
            showPaymentsAppDownloadPromptState = false
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
            if (parsed.boardingRequestToken == null && parsed.boarded) {
                boardingRequestTokenState = null
                activeProfileId?.let { boardingStateStore.clearBoardingRequestToken(it) }
            }
            if (parsed.boardingRequestToken != null) {
                boardingTokenIssuedState = false
            }
            statusState = when {
                parsed.boarded -> strings["status_adyen_boarded_ready"]
                parsed.boardingRequestToken != null -> strings["status_boarding_request_received"]
                parsed.error != null -> listOfNotNull(
                    strings.format("status_boarding_error", parsed.error),
                    parsed.errorAdvice,
                ).joinToString(" ")
                else -> strings["status_adyen_not_boarded"]
            }
            if (parsed.boarded) {
                activeProfile?.let { refreshPaymentsApps(it) }
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
                        responseSummary = parsed.localizedTransactionSummary(),
                        failureReason = parsed.localizedFailureReasonOrNull(),
                        pspReference = parsed.pspReferenceOrNull() ?: record.pspReference,
                        adyenTransactionId = parsed.transactionIdOrNull() ?: record.adyenTransactionId,
                    )
                }
                pendingTransactionIdState = null
                reloadTransactions()
                statusState = parsed.localizedTransactionSummary()
            }
        }
    }

    private fun selectLanguage(language: AppLanguage) {
        selectedLanguageState = language
        languageStore.save(language)
        statusState = strings.languageChanged(language)
        val defaultPaymentsStatuses = AppLanguage.entries.map { stringsFor(it)["status_payments_instances_not_loaded"] }
        if (paymentsAppStatusState in defaultPaymentsStatuses) {
            paymentsAppStatusState = strings["status_payments_instances_not_loaded"]
        }
    }

    private fun PaymentResult.localizedTransactionSummary(): String = when (this) {
        is PaymentResult.Success -> strings.format("summary_approved", pspReference?.let { " | PSP $it" }.orEmpty())
        is PaymentResult.Refused -> strings.format("summary_refused", reason?.let { " | $it" }.orEmpty())
        is PaymentResult.Failure -> strings.format("summary_failed", message)
        is PaymentResult.BoardingStatus -> strings["summary_boarding_response"]
    }

    private fun PaymentResult.localizedFailureReasonOrNull(): String? = when (this) {
        is PaymentResult.Refused -> reason ?: strings["failure_refused_without_reason"]
        is PaymentResult.Failure -> message
        else -> null
    }
}

private const val KEY_SELECTED_SCREEN = "selectedScreen"
private const val KEY_PENDING_RETURN_SCREEN = "pendingReturnScreen"

private fun Bundle.screen(key: String): AppScreen? =
    getString(key)?.let { name ->
        AppScreen.entries.firstOrNull { it.name == name }
    }
