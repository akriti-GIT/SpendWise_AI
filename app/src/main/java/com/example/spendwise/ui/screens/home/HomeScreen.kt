package com.example.spendwise.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spendwise.data.entity.TransactionEntity
import com.example.spendwise.ui.components.*
import com.example.spendwise.ui.theme.*
import com.example.spendwise.util.CurrencyUtils
import com.example.spendwise.viewmodel.SpendWiseViewModel

@Composable
fun HomeScreen(
    viewModel: SpendWiseViewModel,
    onSeeAllClicked: () -> Unit,
    onTransactionClicked: (TransactionEntity) -> Unit,
    onBudgetCardClicked: () -> Unit,
    onNavigateToScreenshotReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val balance by viewModel.totalBalance.collectAsState()
    val income by viewModel.totalIncome.collectAsState()
    val expenses by viewModel.totalExpenses.collectAsState()
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val monthlyBudget by viewModel.monthlyBudget.collectAsState()
    val selectedRange by viewModel.selectedTimeRange.collectAsState()
    val isOcrProcessing by viewModel.isOcrProcessing.collectAsState()
    val ocrErrorMessage by viewModel.ocrErrorMessage.collectAsState()
    val importSuccessMessage by viewModel.importSuccessMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(ocrErrorMessage) {
        ocrErrorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(importSuccessMessage) {
        importSuccessMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearImportSuccessMessage()
        }
    }

    var showImageSourceSheet by remember { mutableStateOf(false) }

    val permissionsToRequest = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        arrayOf(
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )
    } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    var pendingPickerLaunch by remember { mutableStateOf<(() -> Unit)?>(null) }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        pendingPickerLaunch?.invoke()
        pendingPickerLaunch = null
    }

    val requestPermissionAndLaunch = { onGrantedLaunch: () -> Unit ->
        val allGranted = permissionsToRequest.all { perm ->
            androidx.core.content.ContextCompat.checkSelfPermission(context, perm) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            onGrantedLaunch()
        } else {
            pendingPickerLaunch = onGrantedLaunch
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        viewModel.isImagePickerActive = false
        if (!uris.isNullOrEmpty()) {
            viewModel.processScreenshots(
                context = context,
                imageUris = uris,
                onParsed = { onNavigateToScreenshotReview() }
            )
        }
    }

    val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        viewModel.isImagePickerActive = false
        if (!uris.isNullOrEmpty()) {
            viewModel.processScreenshots(
                context = context,
                imageUris = uris,
                onParsed = { onNavigateToScreenshotReview() }
            )
        }
    }

    val budgetAmount = monthlyBudget?.amount ?: 10000.0
    val budgetRemaining = (budgetAmount - expenses).coerceAtLeast(0.0)
    val budgetRatio = if (budgetAmount > 0) (expenses / budgetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val budgetPercentage = (budgetRatio * 100).toInt()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
        // Top Bar Greeting
        item {
            SpendWiseTopBar(
                studentName = userSettings?.studentName ?: "Maya Chen"
            )
        }

        // Hero Balance Card
        item {
            BalanceCard(
                balance = balance,
                selectedRange = selectedRange,
                onRangeSelected = { viewModel.setTimeRange(it) }
            )
        }

        // Summary Cards: Income & Spent
        item {
            SummaryCardsRow(
                income = income,
                expenses = expenses,
                incomeSubtitle = "Job + allowance",
                expensesCount = recentTransactions.size
            )
        }

        // Monthly Budget Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
                    .clickable { onBudgetCardClicked() }
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "SEPTEMBER BUDGET",
                            fontFamily = SansFamily,
                            fontSize = 9.sp,
                            letterSpacing = 1.8.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${CurrencyUtils.formatAmount(budgetRemaining)} left",
                            fontFamily = SansFamily,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Budget Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(TrackBg)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(budgetRatio)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(99.dp))
                                .background(if (budgetPercentage > 85) WarmBrown else DarkPine)
                        )
                    }

                    Spacer(modifier = Modifier.height(7.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${CurrencyUtils.formatAmount(expenses)} of ${CurrencyUtils.formatAmount(budgetAmount)}",
                            fontSize = 10.5.sp,
                            fontFamily = SansFamily,
                            color = TextSecondary
                        )
                        Text(
                            text = "$budgetPercentage% used",
                            fontSize = 10.5.sp,
                            fontFamily = SansFamily,
                            fontWeight = FontWeight.Medium,
                            color = if (budgetPercentage > 85) WarmBrown else Terracotta
                        )
                    }
                }
            }
        }

        // Import from Screenshot Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(PillBg)
                    .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
                    .clickable {
                        if (!isOcrProcessing) {
                            showImageSourceSheet = true
                        }
                    }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(DarkPine),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = "Scan Screenshot",
                            tint = CardLight,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Import from Screenshot",
                            fontFamily = SansFamily,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Scan UPI payments, bank statements or receipts.",
                            fontFamily = SansFamily,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Open",
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Recent Transactions Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT",
                    fontFamily = SansFamily,
                    fontSize = 9.5.sp,
                    letterSpacing = 2.0.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "See all",
                    fontFamily = SansFamily,
                    fontSize = 11.5.sp,
                    color = Terracotta,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onSeeAllClicked() }
                )
            }
        }

        // Recent Transactions List
        if (recentTransactions.isEmpty()) {
            item {
                EmptyState(
                    title = "No Recent Transactions",
                    subtitle = "Add your first expense or income to see it here."
                )
            }
        } else {
            items(recentTransactions) { tx ->
                TransactionItem(
                    transaction = tx,
                    onClick = { onTransactionClicked(tx) }
                )
            }
        }
    }

    // Reading transactions loading dialog
    if (isOcrProcessing) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { /* Prevent dismiss while processing */ }
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = DarkPine,
                        modifier = Modifier.size(44.dp),
                        strokeWidth = 3.5.dp
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Reading your transactions...",
                        fontFamily = SansFamily,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Processing on-device with ML Kit OCR",
                        fontFamily = SansFamily,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }

    if (showImageSourceSheet) {
        ImageSourceBottomSheet(
            onDismissRequest = { showImageSourceSheet = false },
            onSelectFiles = {
                requestPermissionAndLaunch {
                    viewModel.isImagePickerActive = true
                    filePickerLauncher.launch(arrayOf("image/*"))
                }
            },
            onSelectGallery = {
                requestPermissionAndLaunch {
                    viewModel.isImagePickerActive = true
                    photoPickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                }
            }
        )
    }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}
