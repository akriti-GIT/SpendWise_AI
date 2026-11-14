package com.example.spendwise.ui.screens.ocr

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spendwise.model.ParsedTransaction
import com.example.spendwise.model.TransactionType
import com.example.spendwise.ui.components.EmptyState
import com.example.spendwise.ui.theme.*
import com.example.spendwise.util.CurrencyUtils
import com.example.spendwise.util.DateUtils
import com.example.spendwise.util.TransactionParserEngine
import com.example.spendwise.viewmodel.SpendWiseViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenshotReviewScreen(
    viewModel: SpendWiseViewModel,
    onNavigateBack: () -> Unit,
    onImportSuccess: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val parsedTransactions by viewModel.parsedTransactions.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            snackbarMessage = null
        }
    }

    val selectedCount = parsedTransactions.count { it.isSelected }
    val selectedTotal = parsedTransactions.filter { it.isSelected }.sumOf { it.amount }
    val allSelected = parsedTransactions.isNotEmpty() && parsedTransactions.all { it.isSelected }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (parsedTransactions.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.clearOcrState()
                                onNavigateBack()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(99.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                        ) {
                            Text(
                                text = "Cancel",
                                fontFamily = SansFamily,
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.importSelectedTransactions(
                                    onSuccess = { count ->
                                        onImportSuccess(count)
                                    },
                                    onError = { err ->
                                        snackbarMessage = err
                                    }
                                )
                            },
                            enabled = selectedCount > 0,
                            modifier = Modifier
                                .weight(1.6f)
                                .height(48.dp),
                            shape = RoundedCornerShape(99.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkPine,
                                disabledContainerColor = CardBorder
                            )
                        ) {
                            Text(
                                text = "Add $selectedCount ${if (selectedCount == 1) "Expense" else "Expenses"} (${CurrencyUtils.formatAmount(selectedTotal)})",
                                fontFamily = SansFamily,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = CardLight
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cancel",
                    fontFamily = SansFamily,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.clickable {
                        viewModel.clearOcrState()
                        onNavigateBack()
                    }
                )
                Text(
                    text = "REVIEW IMPORT (${parsedTransactions.size})",
                    fontFamily = SansFamily,
                    fontSize = 10.sp,
                    letterSpacing = 1.8.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                if (parsedTransactions.isNotEmpty()) {
                    Text(
                        text = if (allSelected) "Deselect All" else "Select All",
                        fontFamily = SansFamily,
                        fontSize = 12.sp,
                        color = Terracotta,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable {
                            viewModel.setAllTransactionsSelected(!allSelected)
                        }
                    )
                } else {
                    Spacer(modifier = Modifier.width(40.dp))
                }
            }

            if (parsedTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        title = "No Transactions Found",
                        subtitle = "We couldn't recognize any transactions from the selected screenshot. Please try a clearer screenshot or add the transaction manually."
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(parsedTransactions, key = { it.id }) { item ->
                        EditableTransactionCard(
                            item = item,
                            onToggleSelection = { viewModel.toggleTransactionSelection(item.id) },
                            onUpdate = { updated -> viewModel.updateParsedTransaction(updated) },
                            onDelete = { viewModel.removeParsedTransaction(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditableTransactionCard(
    item: ParsedTransaction,
    onToggleSelection: () -> Unit,
    onUpdate: (ParsedTransaction) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

    // Date picker dialog
    val calendar = Calendar.getInstance().apply { timeInMillis = item.date }
    val datePickerDialog = remember(item.date) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                onUpdate(item.copy(date = cal.timeInMillis))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                if (item.isLikelyDuplicate) WarmBrown.copy(alpha = 0.5f) else CardBorder,
                RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row: Checkbox, Merchant/Description, Amount, and Delete icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = item.isSelected,
                    onCheckedChange = { onToggleSelection() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = DarkPine,
                        uncheckedColor = CardBorder
                    )
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                ) {
                    Text(
                        text = item.description.ifBlank { "Untitled Transaction" },
                        fontFamily = SansFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "${DateUtils.formatDate(item.date)} • ${item.category}",
                        fontFamily = SansFamily,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Text(
                    text = "${if (item.type == TransactionType.INCOME) "+" else "-"} ₹${String.format("%.2f", item.amount)}",
                    fontFamily = SerifFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (item.type == TransactionType.INCOME) SageGreen else Terracotta
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Remove",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Duplicate warning badge if detected
            if (item.isLikelyDuplicate) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(WarmBrown.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = "Duplicate warning",
                            tint = WarmBrown,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Possible duplicate detected with an existing transaction.",
                            fontFamily = SansFamily,
                            fontSize = 10.5.sp,
                            color = WarmBrown,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Reference info if present
            item.referenceNumber?.let { ref ->
                Text(
                    text = "Ref: $ref",
                    fontFamily = SansFamily,
                    fontSize = 9.5.sp,
                    color = TextSecondary
                )
            }

            // Expand / Collapse Details for editing
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Hide details ▲" else "Edit details ▼",
                    fontFamily = SansFamily,
                    fontSize = 11.sp,
                    color = Terracotta,
                    fontWeight = FontWeight.Medium
                )
            }

            // Expanded Edit Section
            if (isExpanded) {
                Divider(color = CardBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

                // 1. Amount and Type Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = if (item.amount > 0) item.amount.toString() else "",
                        onValueChange = { input ->
                            val parsed = input.toDoubleOrNull() ?: 0.0
                            onUpdate(item.copy(amount = parsed))
                        },
                        label = { Text("Amount (₹)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkPine,
                            unfocusedBorderColor = CardBorder
                        )
                    )

                    // Type Toggle (Expense / Income)
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(PillBg)
                            .padding(2.dp)
                    ) {
                        listOf(TransactionType.EXPENSE, TransactionType.INCOME).forEach { t ->
                            val isSel = item.type == t
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isSel) DarkPine else Color.Transparent)
                                    .clickable { onUpdate(item.copy(type = t)) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = t.title,
                                    fontFamily = SansFamily,
                                    fontSize = 10.5.sp,
                                    color = if (isSel) CardLight else TextSecondary,
                                    fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // 2. Description
                OutlinedTextField(
                    value = item.description,
                    onValueChange = { onUpdate(item.copy(description = it)) },
                    label = { Text("Description / Merchant", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkPine,
                        unfocusedBorderColor = CardBorder
                    )
                )

                // 3. Category Selector Chips
                Text(
                    text = "CATEGORY",
                    fontFamily = SansFamily,
                    fontSize = 9.sp,
                    letterSpacing = 1.5.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TransactionParserEngine.CATEGORIES.chunked(4).forEach { chunk ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            chunk.forEach { cat ->
                                val isSelected = item.category.equals(cat, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) DarkPine else PillBg)
                                        .clickable { onUpdate(item.copy(category = cat)) }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cat,
                                        fontFamily = SansFamily,
                                        fontSize = 10.sp,
                                        color = if (isSelected) CardLight else TextSecondary,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Date Picker Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { datePickerDialog.show() }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transaction Date",
                        fontFamily = SansFamily,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = "Date",
                            tint = Terracotta,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = DateUtils.formatDate(item.date),
                            fontFamily = SansFamily,
                            fontSize = 12.sp,
                            color = Terracotta,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
