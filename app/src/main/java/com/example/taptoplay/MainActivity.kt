package com.example.taptoplay

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import com.example.taptoplay.adyen.AdyenLinks
import com.example.taptoplay.adyen.AndroidTransactionStore
import com.example.taptoplay.adyen.BoardingApiClient
import com.example.taptoplay.adyen.NexoCrypto
import com.example.taptoplay.adyen.PaymentResult
import com.example.taptoplay.adyen.PaymentResultParser
import com.example.taptoplay.adyen.SaleToAcquirerDataConfig
import com.example.taptoplay.adyen.SaleToAcquirerDataQrParser
import com.example.taptoplay.adyen.TerminalPaymentRequestBuilder
import com.example.taptoplay.adyen.TransactionRecord
import com.example.taptoplay.adyen.TransactionStatus
import com.example.taptoplay.adyen.failureReasonOrNull
import com.example.taptoplay.adyen.toTransactionStatus
import com.example.taptoplay.adyen.toTransactionSummary
import com.example.taptoplay.cart.Cart
import com.example.taptoplay.cart.CartLine
import com.example.taptoplay.catalog.Product
import com.example.taptoplay.catalog.ProductCatalog
import com.example.taptoplay.profiles.AndroidProfileStore
import com.example.taptoplay.profiles.AdyenProfile
import com.example.taptoplay.profiles.LocalProfileBootstrap
import com.example.taptoplay.profiles.PaymentEnvironment
import com.example.taptoplay.profiles.ProfileQrParser
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
    private val qrParser = ProfileQrParser()
    private val saleToAcquirerDataQrParser = SaleToAcquirerDataQrParser()
    private val boardingApiClient = BoardingApiClient()
    private val nexoCrypto = NexoCrypto()

    private var profilesState by mutableStateOf(emptyList<AdyenProfile>())
    private var activeProfileIdState by mutableStateOf<String?>(null)
    private var paymentResultState by mutableStateOf<PaymentResult?>(null)
    private var statusState by mutableStateOf("Ready for boutique checkout")
    private var installationIdState by mutableStateOf<String?>(null)
    private var boardingRequestTokenState by mutableStateOf<String?>(null)
    private var saleToAcquirerDataConfigState by mutableStateOf(SaleToAcquirerDataConfig.default())
    private var transactionHistoryState by mutableStateOf(emptyList<TransactionRecord>())
    private var pendingTransactionIdState by mutableStateOf<String?>(null)

    private val qrLauncher = registerForActivityResult(ScanContract()) { result ->
        val contents = result.contents ?: return@registerForActivityResult
        qrParser.parse(contents)
            .onSuccess { profile ->
                profileStore.save(profile)
                profileStore.setActive(profile.id)
                reloadProfiles()
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
        LocalProfileBootstrap.profileOrNull()?.let { bootstrap ->
            if (profileStore.profiles().none { it.id == bootstrap.id }) profileStore.save(bootstrap)
            if (profileStore.activeProfileId() == null) profileStore.setActive(bootstrap.id)
        }
        reloadProfiles()
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
                    transactionHistory = transactionHistoryState,
                    status = statusState,
                    paymentResult = paymentResultState,
                    onDismissResult = { paymentResultState = null },
                    onScanProfile = { scanQr() },
                    onScanSaleToAcquirerData = { scanSaleToAcquirerDataQr() },
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
                        installationIdState = null
                        boardingRequestTokenState = null
                        statusState = "Active profile switched deliberately."
                    },
                    onCheckBoarding = { profile -> launchLink(AdyenLinks.boarded(profile)) },
                    onBoard = { profile -> board(profile) },
                    onReboard = { profile -> launchLink(AdyenLinks.startReboard(profile)) },
                    onPay = { profile, lines, totalMinor -> pay(profile, lines, totalMinor) },
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

    private fun reloadTransactions() {
        transactionHistoryState = transactionStore.records()
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
            val tokenResult = withContext(Dispatchers.IO) { boardingApiClient.createBoardingToken(profile, requestToken) }
            tokenResult
                .onSuccess { response ->
                    installationIdState = response.installationId ?: installationIdState
                    statusState = "Opening Adyen to finish boarding..."
                    launchLink(AdyenLinks.board(profile, response.boardingToken))
                }
                .onFailure { statusState = "Boarding token failed: ${it.message}" }
        }
    }

    private fun pay(profile: AdyenProfile, lines: List<CartLine>, totalMinor: Long) {
        val installationId = installationIdState
        if (installationId.isNullOrBlank()) {
            statusState = "Check boarding first. Payments require an installation ID as POIID."
            launchLink(AdyenLinks.boarded(profile))
            return
        }
        val requestJson = TerminalPaymentRequestBuilder.buildDemoRequest(
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
            itemCount = lines.sumOf { it.quantity },
            saleToAcquirerDataName = saleToAcquirerDataConfigState.displayName,
            requestJson = requestJson,
        )
        transactionStore.save(record)
        pendingTransactionIdState = transactionId
        reloadTransactions()
        val encoded = nexoCrypto.encryptToBase64Url(profile, requestJson)
        statusState = "Opening Adyen payment app with encrypted Terminal API request..."
        launchLink(AdyenLinks.nexo(profile, encoded))
    }

    private fun launchLink(rawUrl: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(rawUrl)))
    }

    private fun handleReturnIntent(intent: Intent?) {
        val rawUri = intent?.data?.toString()
        val parsed = PaymentResultParser.parse(rawUri ?: return) ?: return
        paymentResultState = parsed
        if (parsed is PaymentResult.BoardingStatus) {
            installationIdState = parsed.installationId ?: installationIdState
            boardingRequestTokenState = parsed.boardingRequestToken ?: boardingRequestTokenState
            statusState = when {
                parsed.boarded -> "Adyen app is boarded and ready."
                parsed.boardingRequestToken != null -> "Boarding request token received. Tap Board to finish setup."
                parsed.error != null -> "Boarding error: ${parsed.error}"
                else -> "Adyen app is not boarded yet."
            }
        } else {
            val transactionId = pendingTransactionIdState ?: transactionHistoryState.firstOrNull { it.status == TransactionStatus.LAUNCHED }?.id
            if (transactionId != null) {
                transactionStore.update(transactionId) { record ->
                    record.copy(
                        status = parsed.toTransactionStatus(),
                        responseUri = rawUri,
                        responseSummary = parsed.toTransactionSummary(),
                        failureReason = parsed.failureReasonOrNull(),
                    )
                }
                pendingTransactionIdState = null
                reloadTransactions()
            }
        }
    }
}

@Composable
private fun TapToPlayApp(
    profiles: List<AdyenProfile>,
    activeProfileId: String?,
    installationId: String?,
    boardingRequestToken: String?,
    saleToAcquirerDataConfig: SaleToAcquirerDataConfig,
    transactionHistory: List<TransactionRecord>,
    status: String,
    paymentResult: PaymentResult?,
    onDismissResult: () -> Unit,
    onScanProfile: () -> Unit,
    onScanSaleToAcquirerData: () -> Unit,
    onClearSaleToAcquirerData: () -> Unit,
    onClearTransactions: () -> Unit,
    onSelectProfile: (String) -> Unit,
    onCheckBoarding: (AdyenProfile) -> Unit,
    onBoard: (AdyenProfile) -> Unit,
    onReboard: (AdyenProfile) -> Unit,
    onPay: (AdyenProfile, List<CartLine>, Long) -> Unit,
) {
    val cart = remember { Cart() }
    var cartVersion by remember { mutableStateOf(0) }
    var selectedCategory by remember { mutableStateOf("All") }
    var showSaleToAcquirerData by remember { mutableStateOf(false) }
    var inspectedTransaction by remember { mutableStateOf<TransactionRecord?>(null) }
    val activeProfile = profiles.firstOrNull { it.id == activeProfileId }
    val lines = remember(cartVersion) { cart.lines() }
    val products = ProductCatalog.products.filter {
        selectedCategory == "All" || it.category == selectedCategory
    }

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Header(status = status)
            }
            item {
                ProfilePanel(
                    profiles = profiles,
                    activeProfile = activeProfile,
                    installationId = installationId,
                    boardingRequestToken = boardingRequestToken,
                    onScanProfile = onScanProfile,
                    onSelectProfile = onSelectProfile,
                    onCheckBoarding = onCheckBoarding,
                    onBoard = onBoard,
                    onReboard = onReboard,
                )
            }
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
                CartPanel(
                    lines = lines,
                    totalMinor = cart.totalMinor(),
                    activeProfile = activeProfile,
                    saleToAcquirerDataConfig = saleToAcquirerDataConfig,
                    onRemove = {
                        cart.removeOne(it)
                        cartVersion++
                    },
                    onClear = {
                        cart.clear()
                        cartVersion++
                    },
                    onScanSaleToAcquirerData = onScanSaleToAcquirerData,
                    onClearSaleToAcquirerData = onClearSaleToAcquirerData,
                    onInspectSaleToAcquirerData = { showSaleToAcquirerData = true },
                    onPay = { profile -> onPay(profile, lines, cart.totalMinor()) },
                )
            }
            item {
                TransactionHistoryPanel(
                    records = transactionHistory,
                    onInspect = { inspectedTransaction = it },
                    onClear = onClearTransactions,
                )
            }
            item { Spacer(Modifier.height(18.dp)) }
        }
    }

    paymentResult?.let {
        PaymentResultDialog(result = it, onDismiss = onDismissResult)
    }

    if (showSaleToAcquirerData) {
        SaleToAcquirerDataDialog(
            config = saleToAcquirerDataConfig,
            onDismiss = { showSaleToAcquirerData = false },
        )
    }

    inspectedTransaction?.let { record ->
        TransactionDialog(
            record = record,
            onDismiss = { inspectedTransaction = null },
        )
    }
}

@Composable
private fun Header(status: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 18.dp)) {
        Text("TapToPlay", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
        Text(
            "A premium clothing store demo for Adyen Tap to Pay",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AssistChip(onClick = {}, label = { Text(status, maxLines = 2, overflow = TextOverflow.Ellipsis) })
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
    onCheckBoarding: (AdyenProfile) -> Unit,
    onBoard: (AdyenProfile) -> Unit,
    onReboard: (AdyenProfile) -> Unit,
) {
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Payment profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        activeProfile?.let { "${it.displayName} | ${it.environment.name.lowercase()}" } ?: "No Adyen profile selected",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(onClick = onScanProfile) { Text("Scan QR") }
            }
            if (activeProfile != null) {
                Text("API key ${activeProfile.maskedApiKey()} | passphrase ${activeProfile.maskedPassphrase()}")
                Text("Installation ${installationId ?: "not returned yet"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Boarding request token ${boardingRequestToken?.let { "received" } ?: "not received"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onCheckBoarding(activeProfile) }) { Text("Check") }
                    Button(onClick = { onBoard(activeProfile) }) { Text("Board") }
                    OutlinedButton(onClick = { onReboard(activeProfile) }) { Text("Reboard") }
                }
            }
            if (profiles.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    profiles.forEach { profile ->
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

@Composable
private fun ProductCard(product: Product, onAdd: () -> Unit) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.linearGradient(listOf(product.color, product.accentColor))),
            ) {
                Text(
                    product.category.uppercase(),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(product.description, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    onScanSaleToAcquirerData: () -> Unit,
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
                        OutlinedButton(onClick = { onRemove(line.product.id) }, modifier = Modifier.size(width = 44.dp, height = 36.dp)) {
                            Text("-")
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
                    Text("SaleToAcquirerData", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${saleToAcquirerDataConfig.displayName} | ${saleToAcquirerDataConfig.fieldCount} JSON field${if (saleToAcquirerDataConfig.fieldCount == 1) "" else "s"}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onScanSaleToAcquirerData) { Text("Scan data QR") }
                        OutlinedButton(onClick = onInspectSaleToAcquirerData) { Text("View") }
                        TextButton(onClick = onClearSaleToAcquirerData) { Text("Reset") }
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
    }
    AssistChip(onClick = {}, label = { Text(label) })
}

@Composable
private fun PaymentResultDialog(result: PaymentResult, onDismiss: () -> Unit) {
    val title = when (result) {
        is PaymentResult.BoardingStatus -> "Boarding returned"
        is PaymentResult.Success -> "Payment approved"
        is PaymentResult.Refused -> "Payment refused"
        is PaymentResult.Failure -> "Adyen result"
    }
    val message = when (result) {
        is PaymentResult.BoardingStatus -> {
            val state = if (result.boarded) "boarded" else "not boarded"
            "Adyen app is $state. Installation ID: ${result.installationId ?: "not supplied"}"
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
    onDismiss: () -> Unit,
) {
    var selectedSection by remember { mutableStateOf("Request") }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .height(660.dp),
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
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
                TransactionStatusChip(record.status)
                record.failureReason?.let {
                    OutlinedCard(shape = RoundedCornerShape(8.dp)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Adyen failure detail", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                    when (selectedSection) {
                        "Request" -> {
                            item {
                                Text("Terminal API request", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                            item { MonospaceBlock(record.requestJson) }
                        }
                        else -> {
                            item {
                                Text("Adyen response", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                            item {
                                Text(
                                    record.responseSummary ?: "No response has been received yet.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            record.responseUri?.let { response ->
                                item { MonospaceBlock(response) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonospaceBlock(text: String) {
    OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SaleToAcquirerDataDialog(
    config: SaleToAcquirerDataConfig,
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("SaleToAcquirerData", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${config.displayName} | ${config.fieldCount} fields",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                    items(config.data.entries.toList()) { (key, value) ->
                        JsonNodeRow(name = key, value = value, depth = 0)
                    }
                }
            }
        }
    }
}

@Composable
private fun JsonNodeRow(name: String, value: JsonElement, depth: Int) {
    var expanded by remember { mutableStateOf(depth == 0) }
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
                }
            }
            if (expanded && value is JsonObject) {
                value.entries.forEach { (childKey, childValue) ->
                    JsonNodeRow(name = childKey, value = childValue, depth = depth + 1)
                }
            }
            if (expanded && value is JsonArray) {
                value.forEachIndexed { index, childValue ->
                    JsonNodeRow(name = "[$index]", value = childValue, depth = depth + 1)
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

private fun formatMoney(minor: Long): String = "EUR %.2f".format(minor / 100.0)
