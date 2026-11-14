package com.example.spendwise.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.spendwise.data.entity.BudgetEntity
import com.example.spendwise.data.entity.CategoryEntity
import com.example.spendwise.data.entity.TransactionEntity
import com.example.spendwise.data.entity.UserEntity
import com.example.spendwise.data.entity.UserSettingsEntity
import com.example.spendwise.model.*
import com.example.spendwise.repository.SpendWiseRepository
import com.example.spendwise.util.DateUtils
import com.example.spendwise.util.OcrManager
import com.example.spendwise.util.TransactionParserEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class SpendWiseViewModel(
    private val repository: SpendWiseRepository
) : ViewModel() {

    // 0. Active User & Authentication Flows
    val currentUserId: StateFlow<String?> = repository.currentUserId

    val currentUser: StateFlow<UserEntity?> = repository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Maps currentUser to UserSettingsEntity so existing UI components remain 100% compatible
    val userSettings: StateFlow<UserSettingsEntity?> = currentUser.map { user ->
        user?.let {
            UserSettingsEntity(
                id = 1,
                currency = it.currency,
                darkMode = it.darkMode,
                notificationsEnabled = it.notificationsEnabled,
                studentName = it.name,
                collegeId = it.collegeId,
                monthlyBudget = it.monthlyBudget,
                email = it.email,
                password = "", // Plaintext password is never exposed
                securityPin = it.securityPin,
                isAppLockEnabled = it.isAppLockEnabled,
                isBiometricEnabled = it.isBiometricEnabled,
                isLoggedIn = true
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 1. Core Financial Flows (Automatically scoped to current user by repository)
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTransactions: StateFlow<List<TransactionEntity>> = repository.recentTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalIncome: StateFlow<Double> = repository.totalIncome
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpenses: StateFlow<Double> = repository.totalExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlyBudget: StateFlow<BudgetEntity?> = repository.getBudgetForCurrentMonth()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2. Computed Total Balance (Income - Expenses)
    val totalBalance: StateFlow<Double> = combine(totalIncome, totalExpenses) { inc, exp ->
        inc - exp
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // 3. Home Screen Time Range (1W, 1M, 3M, All)
    val selectedTimeRange = MutableStateFlow(TimeRange.ONE_MONTH)
    fun setTimeRange(range: TimeRange) {
        selectedTimeRange.value = range
    }

    // 4. Search & Filter State in Activity / Transactions Screen
    val searchQuery = MutableStateFlow("")
    val selectedTypeFilter = MutableStateFlow<TransactionType?>(null) // null = ALL
    val selectedCategoryFilter = MutableStateFlow<String?>(null)    // null = ALL
    val selectedSortOrder = MutableStateFlow(SortOrder.NEWEST)

    fun setSearchQuery(query: String) { searchQuery.value = query }
    fun setTypeFilter(type: TransactionType?) { selectedTypeFilter.value = type }
    fun setCategoryFilter(category: String?) { selectedCategoryFilter.value = category }
    fun setSortOrder(order: SortOrder) { selectedSortOrder.value = order }

    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        searchQuery,
        selectedTypeFilter,
        selectedCategoryFilter,
        selectedSortOrder
    ) { list, query, type, cat, sort ->
        list.filter { tx ->
            val matchesQuery = query.isBlank() ||
                    tx.description.contains(query, ignoreCase = true) ||
                    tx.category.contains(query, ignoreCase = true) ||
                    tx.paymentMethod.label.contains(query, ignoreCase = true)
            val matchesType = type == null || tx.type == type
            val matchesCategory = cat == null || tx.category.equals(cat, ignoreCase = true)
            matchesQuery && matchesType && matchesCategory
        }.let { filtered ->
            when (sort) {
                SortOrder.NEWEST -> filtered.sortedWith(compareByDescending<TransactionEntity> { it.date }.thenByDescending { it.createdAt })
                SortOrder.OLDEST -> filtered.sortedWith(compareBy<TransactionEntity> { it.date }.thenBy { it.createdAt })
                SortOrder.HIGHEST -> filtered.sortedByDescending { it.amount }
                SortOrder.LOWEST -> filtered.sortedBy { it.amount }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 5. Analytics Screen State (Period: Week, Month, Year)
    val analyticsPeriod = MutableStateFlow(PeriodType.MONTH)
    fun setAnalyticsPeriod(period: PeriodType) { analyticsPeriod.value = period }

    private val analyticsTransactions: Flow<List<TransactionEntity>> = combine(
        allTransactions,
        analyticsPeriod
    ) { txList, period ->
        val calendar = Calendar.getInstance()
        val startTime = when (period) {
            PeriodType.WEEK -> DateUtils.getStartOfWeek(calendar)
            PeriodType.MONTH -> DateUtils.getStartOfMonth(calendar)
            PeriodType.YEAR -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
        }
        txList.filter { it.date >= startTime && it.type == TransactionType.EXPENSE }
    }

    val periodExpensesTotal: StateFlow<Double> = analyticsTransactions.map { list ->
        list.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val categorySpending: StateFlow<List<CategorySpending>> = analyticsTransactions.map { list ->
        val total = list.sumOf { it.amount }
        if (total <= 0.0) return@map emptyList()

        val grouped = list.groupBy { it.category }
        val categoryColorMap = mapOf(
            "Food" to "#C1866B",
            "Transport" to "#2F3B2F",
            "Education" to "#A8B79B",
            "Shopping" to "#E0BBA8",
            "Entertainment" to "#C1866B",
            "Health" to "#A96B4E",
            "Bills" to "#A8B79B",
            "Other" to "#D6D3C6"
        )

        grouped.map { (category, txs) ->
            val sum = txs.sumOf { it.amount }
            val percentage = ((sum / total) * 100).toFloat()
            val colorHex = categoryColorMap[category] ?: "#A8B79B"
            CategorySpending(
                categoryName = category,
                totalAmount = sum,
                percentage = percentage,
                colorHex = colorHex,
                count = txs.size
            )
        }.sortedByDescending { it.totalAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topSpendingCategory: StateFlow<String> = categorySpending.map { list ->
        val top = list.firstOrNull()
        if (top != null) {
            "${top.categoryName} is your highest spending category (${String.format(Locale.ENGLISH, "%.0f", top.percentage)}% of total)."
        } else {
            "No spending recorded for this period yet."
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "No expenses yet.")

    val weeklySpendingDays: StateFlow<List<DailySpending>> = allTransactions.map { list ->
        val calendar = Calendar.getInstance()
        val currentWeekStart = DateUtils.getStartOfWeek(calendar)
        val dayMs = 86400000L
        val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

        val days = (0 until 7).map { dayIndex ->
            val startOfDay = currentWeekStart + (dayIndex * dayMs)
            val endOfDay = startOfDay + dayMs - 1
            val daySum = list.filter {
                it.type == TransactionType.EXPENSE && it.date in startOfDay..endOfDay
            }.sumOf { it.amount }

            DailySpending(
                dayLabel = dayLabels[dayIndex],
                dateEpochDay = startOfDay,
                amount = daySum
            )
        }

        val maxVal = days.maxOfOrNull { it.amount } ?: 0.0
        days.map { it.copy(isPeak = it.amount == maxVal && maxVal > 0.0) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 6. User Actions (Mutations)
    fun addTransaction(
        type: TransactionType,
        amount: Double,
        category: String,
        description: String,
        date: Long,
        paymentMethod: PaymentMethod,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (amount <= 0.0) {
            onError("Amount must be greater than zero")
            return
        }
        if (category.isBlank()) {
            onError("Please select a category")
            return
        }
        val userId = currentUserId.value
        if (userId.isNullOrBlank()) {
            onError("User not authenticated")
            return
        }
        viewModelScope.launch {
            try {
                repository.insertTransaction(
                    TransactionEntity(
                        userId = userId,
                        type = type,
                        amount = amount,
                        category = category,
                        description = description.trim(),
                        date = date,
                        paymentMethod = paymentMethod
                    )
                )
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to save transaction")
            }
        }
    }

    fun updateTransaction(
        transaction: TransactionEntity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (transaction.amount <= 0.0) {
            onError("Amount must be greater than zero")
            return
        }
        viewModelScope.launch {
            try {
                repository.updateTransaction(transaction)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to update transaction")
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            onComplete()
        }
    }

    fun setMonthlyBudget(amount: Double, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.setMonthlyBudget(amount)
            onComplete()
        }
    }

    fun toggleDarkMode(isDark: Boolean) {
        viewModelScope.launch {
            repository.updateDarkMode(isDark)
        }
    }

    fun updateProfile(name: String, collegeId: String) {
        viewModelScope.launch {
            repository.updateProfile(name, collegeId)
        }
    }

    fun clearAllData(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.clearCurrentUserFinancialData()
            onComplete()
        }
    }

    // 7. Screenshot OCR Import State
    val isOcrProcessing = MutableStateFlow(false)
    val parsedTransactions = MutableStateFlow<List<ParsedTransaction>>(emptyList())
    val ocrErrorMessage = MutableStateFlow<String?>(null)
    val importSuccessMessage = MutableStateFlow<String?>(null)

    fun clearImportSuccessMessage() {
        importSuccessMessage.value = null
    }

    fun processScreenshot(context: android.content.Context, imageUri: android.net.Uri, onParsed: () -> Unit) {
        processScreenshots(context, listOf(imageUri), onParsed)
    }

    fun processScreenshots(context: android.content.Context, imageUris: List<android.net.Uri>, onParsed: () -> Unit) {
        if (imageUris.isEmpty()) return
        viewModelScope.launch {
            isOcrProcessing.value = true
            ocrErrorMessage.value = null
            try {
                val allParsed = mutableListOf<ParsedTransaction>()
                val currentTxs = allTransactions.value

                for (uri in imageUris) {
                    try {
                        val parsed = com.example.spendwise.util.GeminiVisionManager.analyzeReceipt(context, uri)
                        if (parsed.isNotEmpty()) {
                            allParsed.addAll(parsed)
                        }
                    } catch (_: Exception) {
                        // Continue parsing other images in batch
                    }
                }

                if (allParsed.isEmpty()) {
                    ocrErrorMessage.value = "Could not detect transaction or bill amounts in the uploaded screenshot(s). Please ensure receipts or payment screens are clearly visible."
                    isOcrProcessing.value = false
                    return@launch
                }

                val flaggedList = allParsed.map { item ->
                    val duplicate = TransactionParserEngine.isDuplicate(item, currentTxs)
                    item.copy(isLikelyDuplicate = duplicate)
                }

                parsedTransactions.value = flaggedList
                isOcrProcessing.value = false
                onParsed()
            } catch (e: Exception) {
                ocrErrorMessage.value = "Error reading screenshots: ${e.localizedMessage ?: "Unknown error"}"
                isOcrProcessing.value = false
            }
        }
    }

    fun toggleTransactionSelection(id: String) {
        parsedTransactions.value = parsedTransactions.value.map {
            if (it.id == id) it.copy(isSelected = !it.isSelected) else it
        }
    }

    fun setAllTransactionsSelected(selected: Boolean) {
        parsedTransactions.value = parsedTransactions.value.map {
            it.copy(isSelected = selected)
        }
    }

    fun updateParsedTransaction(updated: ParsedTransaction) {
        parsedTransactions.value = parsedTransactions.value.map {
            if (it.id == updated.id) updated else it
        }
    }

    fun removeParsedTransaction(id: String) {
        parsedTransactions.value = parsedTransactions.value.filter { it.id != id }
    }

    fun importSelectedTransactions(onSuccess: (Int) -> Unit, onError: (String) -> Unit) {
        val selected = parsedTransactions.value.filter { it.isSelected }
        if (selected.isEmpty()) {
            onError("No transactions selected to import")
            return
        }

        val userId = currentUserId.value
        if (userId.isNullOrBlank()) {
            onError("User not authenticated")
            return
        }

        viewModelScope.launch {
            try {
                val entities = selected.map { item ->
                    TransactionEntity(
                        userId = userId,
                        type = item.type,
                        amount = item.amount,
                        category = item.category,
                        description = item.description.ifBlank { item.category },
                        date = item.date,
                        paymentMethod = item.paymentMethod
                    )
                }
                repository.insertTransactions(entities)
                val count = entities.size
                val totalAmount = entities.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                parsedTransactions.value = emptyList()
                importSuccessMessage.value = "Added $count expense${if (count > 1) "s" else ""} (₹${String.format(Locale.ENGLISH, "%.2f", totalAmount)}) to your dashboard!"
                onSuccess(count)
            } catch (e: Exception) {
                onError("Failed to save imported transactions: ${e.message}")
            }
        }
    }

    fun clearOcrState() {
        parsedTransactions.value = emptyList()
        ocrErrorMessage.value = null
        isOcrProcessing.value = false
    }

    fun resetToSampleData(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.loadSampleDataForCurrentUser()
            onComplete()
        }
    }

    // AI & Cloud Vision Configuration
    val isCloudAiEnabled = MutableStateFlow(true)
    val geminiApiKey = MutableStateFlow("")

    fun initAiConfig(context: android.content.Context) {
        isCloudAiEnabled.value = com.example.spendwise.util.AiConfigManager.isCloudAiEnabled(context)
        geminiApiKey.value = com.example.spendwise.util.AiConfigManager.getGeminiApiKey(context)
    }

    fun setCloudAiEnabled(context: android.content.Context, enabled: Boolean) {
        com.example.spendwise.util.AiConfigManager.setCloudAiEnabled(context, enabled)
        isCloudAiEnabled.value = enabled
    }

    fun setGeminiApiKey(context: android.content.Context, apiKey: String) {
        com.example.spendwise.util.AiConfigManager.setGeminiApiKey(context, apiKey)
        geminiApiKey.value = apiKey
    }

    // 8. Auth & Security State
    val isAppLocked = MutableStateFlow(true)
    var isImagePickerActive = false

    fun unlockWithPin(enteredPin: String): Boolean {
        val currentPin = currentUser.value?.securityPin ?: "1234"
        return if (enteredPin == currentPin) {
            isAppLocked.value = false
            true
        } else {
            false
        }
    }

    fun unlockWithBiometric(): Boolean {
        isAppLocked.value = false
        return true
    }

    fun lockApp() {
        if (currentUser.value?.isAppLockEnabled != false) {
            isAppLocked.value = true
        }
    }

    fun login(email: String, passwordOrPin: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.login(email, passwordOrPin)
            result.onSuccess {
                isAppLocked.value = false
                searchQuery.value = ""
                selectedTypeFilter.value = null
                selectedCategoryFilter.value = null
                parsedTransactions.value = emptyList()
                onSuccess()
            }.onFailure { err ->
                onError(err.message ?: "Authentication failed")
            }
        }
    }

    fun quickDemoLogin(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = repository.quickDemoLogin()
            result.onSuccess {
                isAppLocked.value = false
                searchQuery.value = ""
                selectedTypeFilter.value = null
                selectedCategoryFilter.value = null
                parsedTransactions.value = emptyList()
                onSuccess()
            }
        }
    }

    fun register(
        name: String,
        email: String,
        password: String,
        pin: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.register(
                name = name,
                email = email,
                password = password,
                pin = pin
            )
            result.onSuccess {
                isAppLocked.value = false
                searchQuery.value = ""
                selectedTypeFilter.value = null
                selectedCategoryFilter.value = null
                parsedTransactions.value = emptyList()
                onSuccess()
            }.onFailure { err ->
                onError(err.message ?: "Registration failed")
            }
        }
    }

    fun logout(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.logout()
            // Reset all in-memory user-specific state
            searchQuery.value = ""
            selectedTypeFilter.value = null
            selectedCategoryFilter.value = null
            selectedSortOrder.value = SortOrder.NEWEST
            parsedTransactions.value = emptyList()
            ocrErrorMessage.value = null
            importSuccessMessage.value = null
            isAppLocked.value = true
            onComplete()
        }
    }

    fun changePin(oldPin: String, newPin: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val currentPin = currentUser.value?.securityPin ?: "1234"
        if (oldPin != currentPin) {
            onError("Current PIN is incorrect")
            return
        }
        if (newPin.length != 4 || !newPin.all { it.isDigit() }) {
            onError("New PIN must be 4 digits")
            return
        }
        viewModelScope.launch {
            repository.updateSecurityPin(newPin)
            onSuccess()
        }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAppLockEnabled(enabled)
            if (!enabled) {
                isAppLocked.value = false
            }
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setBiometricEnabled(enabled)
        }
    }

    fun exportDataSummary(): String {
        val txs = allTransactions.value
        val sb = StringBuilder()
        sb.append("SPENDWISE EXPENSE REPORT\n")
        sb.append("Generated on: ${DateUtils.formatDate(System.currentTimeMillis())}\n")
        sb.append("Total Income: ₹${totalIncome.value}\n")
        sb.append("Total Expenses: ₹${totalExpenses.value}\n")
        sb.append("Current Balance: ₹${totalBalance.value}\n\n")
        sb.append("TRANSACTIONS (${txs.size}):\n")
        sb.append("Date, Type, Category, Description, Method, Amount\n")
        txs.forEach { tx ->
            sb.append("${DateUtils.formatDate(tx.date)}, ${tx.type.name}, ${tx.category}, \"${tx.description}\", ${tx.paymentMethod.name}, ₹${tx.amount}\n")
        }
        return sb.toString()
    }
}

class SpendWiseViewModelFactory(
    private val repository: SpendWiseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SpendWiseViewModel::class.java)) {
            return SpendWiseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
