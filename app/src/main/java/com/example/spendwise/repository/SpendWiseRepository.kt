package com.example.spendwise.repository

import com.example.spendwise.data.entity.BudgetEntity
import com.example.spendwise.data.entity.CategoryEntity
import com.example.spendwise.data.entity.TransactionEntity
import com.example.spendwise.data.entity.UserEntity
import com.example.spendwise.data.local.AppDatabase
import com.example.spendwise.data.local.SessionManager
import com.example.spendwise.model.TransactionType
import com.example.spendwise.util.HashUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class SpendWiseRepository(
    private val database: AppDatabase,
    private val sessionManager: SessionManager
) {
    private val userDao = database.userDao()
    private val transactionDao = database.transactionDao()
    private val budgetDao = database.budgetDao()
    private val categoryDao = database.categoryDao()

    // 1. Reactive Session State
    private val _currentUserId = MutableStateFlow<String?>(sessionManager.getActiveUserId())
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    // 2. Active User Profile Flow
    val currentUser: Flow<UserEntity?> = _currentUserId.flatMapLatest { userId ->
        if (userId != null) {
            userDao.getUserByIdFlow(userId)
        } else {
            flowOf(null)
        }
    }

    // 3. User-Scoped Financial Flows (automatically empty when logged out)
    val allTransactions: Flow<List<TransactionEntity>> = _currentUserId.flatMapLatest { userId ->
        if (userId != null) {
            transactionDao.getAllTransactionsFlow(userId)
        } else {
            flowOf(emptyList())
        }
    }

    val recentTransactions: Flow<List<TransactionEntity>> = _currentUserId.flatMapLatest { userId ->
        if (userId != null) {
            transactionDao.getRecentTransactionsFlow(userId, 5)
        } else {
            flowOf(emptyList())
        }
    }

    val totalIncome: Flow<Double> = _currentUserId.flatMapLatest { userId ->
        if (userId != null) {
            transactionDao.getTotalAmountByTypeFlow(userId, TransactionType.INCOME)
        } else {
            flowOf(0.0)
        }
    }

    val totalExpenses: Flow<Double> = _currentUserId.flatMapLatest { userId ->
        if (userId != null) {
            transactionDao.getTotalAmountByTypeFlow(userId, TransactionType.EXPENSE)
        } else {
            flowOf(0.0)
        }
    }

    // 4. Budgets
    fun getBudgetForCurrentMonth(): Flow<BudgetEntity?> = _currentUserId.flatMapLatest { userId ->
        if (userId != null) {
            val calendar = Calendar.getInstance()
            budgetDao.getBudgetForMonthFlow(
                userId = userId,
                month = calendar.get(Calendar.MONTH) + 1,
                year = calendar.get(Calendar.YEAR)
            )
        } else {
            flowOf(null)
        }
    }

    suspend fun setMonthlyBudget(amount: Double, categoryBudgetsJson: String = "") = withContext(Dispatchers.IO) {
        val userId = _currentUserId.value ?: throw IllegalStateException("User not authenticated")
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val currentYear = calendar.get(Calendar.YEAR)

        val existing = budgetDao.getBudgetForMonth(userId, currentMonth, currentYear)
        val updated = existing?.copy(
            amount = amount,
            categoryBudgetsJson = if (categoryBudgetsJson.isNotEmpty()) categoryBudgetsJson else existing.categoryBudgetsJson
        ) ?: BudgetEntity(
            userId = userId,
            month = currentMonth,
            year = currentYear,
            amount = amount,
            categoryBudgetsJson = categoryBudgetsJson
        )
        budgetDao.insertOrUpdate(updated)

        // Also update monthlyBudget in user profile
        userDao.getUserById(userId)?.let { user ->
            userDao.update(user.copy(monthlyBudget = amount))
        }
    }

    // 5. Universal Categories
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategoriesFlow()

    // 6. User Mutations & Queries
    suspend fun getActiveUser(): UserEntity? = withContext(Dispatchers.IO) {
        _currentUserId.value?.let { userDao.getUserById(it) }
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long = withContext(Dispatchers.IO) {
        val userId = _currentUserId.value ?: throw IllegalStateException("User not authenticated")
        transactionDao.insert(transaction.copy(userId = userId))
    }

    suspend fun insertTransactions(transactions: List<TransactionEntity>): List<Long> = withContext(Dispatchers.IO) {
        val userId = _currentUserId.value ?: throw IllegalStateException("User not authenticated")
        val scoped = transactions.map { it.copy(userId = userId) }
        transactionDao.insertAll(scoped)
    }

    suspend fun updateTransaction(transaction: TransactionEntity): Int = withContext(Dispatchers.IO) {
        val userId = _currentUserId.value ?: throw IllegalStateException("User not authenticated")
        if (transaction.userId != userId) {
            throw SecurityException("Cannot modify transactions belonging to another user")
        }
        transactionDao.update(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity): Int = withContext(Dispatchers.IO) {
        val userId = _currentUserId.value ?: throw IllegalStateException("User not authenticated")
        if (transaction.userId != userId) {
            throw SecurityException("Cannot delete transactions belonging to another user")
        }
        transactionDao.delete(transaction)
    }

    suspend fun deleteTransactionById(id: Long): Int = withContext(Dispatchers.IO) {
        val userId = _currentUserId.value ?: throw IllegalStateException("User not authenticated")
        transactionDao.deleteById(id, userId)
    }

    suspend fun getTransactionById(id: Long): TransactionEntity? = withContext(Dispatchers.IO) {
        val userId = _currentUserId.value ?: return@withContext null
        transactionDao.getTransactionById(id, userId)
    }

    // 7. Authentication & Registration
    suspend fun register(
        name: String,
        email: String,
        password: String,
        pin: String,
        collegeId: String = ""
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        val normalizedEmail = email.trim().lowercase(Locale.ROOT)
        val cleanPassword = password.trim()
        val cleanPin = pin.trim()

        if (normalizedEmail.isBlank() || !normalizedEmail.contains("@")) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address"))
        }
        if (cleanPassword.length < 4) {
            return@withContext Result.failure(IllegalArgumentException("Password must be at least 4 characters"))
        }
        if (cleanPin.length != 4 || !cleanPin.all { it.isDigit() }) {
            return@withContext Result.failure(IllegalArgumentException("PIN must be exactly 4 digits"))
        }

        val existing = userDao.getUserByEmail(normalizedEmail)
        if (existing != null) {
            return@withContext Result.failure(IllegalStateException("An account with this email already exists. Please log in."))
        }

        val newUserId = UUID.randomUUID().toString()
        val newUser = UserEntity(
            id = newUserId,
            name = name.trim(),
            email = normalizedEmail,
            passwordHash = HashUtils.sha256(cleanPassword),
            securityPin = cleanPin,
            collegeId = collegeId.ifBlank { "MCA-2026-${(100..999).random()}" },
            currency = "₹",
            darkMode = false,
            monthlyBudget = 0.0,
            createdAt = System.currentTimeMillis()
        )

        userDao.insert(newUser)
        sessionManager.setActiveUserId(newUserId)
        _currentUserId.value = newUserId

        Result.success(newUser)
    }

    suspend fun login(email: String, passwordOrPin: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        val normalizedEmail = email.trim().lowercase(Locale.ROOT)
        val secret = passwordOrPin.trim()

        if (normalizedEmail.isBlank() || secret.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter both email and password/PIN"))
        }

        val user = userDao.getUserByEmail(normalizedEmail)
            ?: return@withContext Result.failure(IllegalArgumentException("No account found with this email. Please sign up."))

        val passwordValid = HashUtils.verifyPassword(secret, user.passwordHash) ||
                HashUtils.verifyPassword(passwordOrPin, user.passwordHash)
        val pinValid = user.securityPin == secret || user.securityPin == passwordOrPin

        if (!passwordValid && !pinValid) {
            return@withContext Result.failure(IllegalArgumentException("Incorrect password or PIN. Please try again."))
        }

        sessionManager.setActiveUserId(user.id)
        _currentUserId.value = user.id

        Result.success(user)
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        sessionManager.clearSession()
        _currentUserId.value = null
    }

    suspend fun quickDemoLogin(): Result<UserEntity> = withContext(Dispatchers.IO) {
        val demoId = "demo_maya_chen"
        var demoUser = userDao.getUserById(demoId)
        if (demoUser == null) {
            demoUser = UserEntity(
                id = demoId,
                name = "Maya Chen",
                email = "maya.chen@campus.edu",
                passwordHash = HashUtils.sha256("password123"),
                securityPin = "1234",
                collegeId = "MCA-2026-087",
                currency = "₹",
                monthlyBudget = 10000.0,
                createdAt = System.currentTimeMillis()
            )
            userDao.insertOrUpdate(demoUser)
            AppDatabase.seedSampleDataForUser(database, demoId)
        }

        sessionManager.setActiveUserId(demoId)
        _currentUserId.value = demoId
        Result.success(demoUser)
    }

    suspend fun loadSampleDataForCurrentUser() = withContext(Dispatchers.IO) {
        val userId = _currentUserId.value ?: throw IllegalStateException("User not authenticated")
        AppDatabase.seedSampleDataForUser(database, userId)
    }

    // 8. User Settings Updates
    suspend fun updateProfile(name: String, collegeId: String) = withContext(Dispatchers.IO) {
        val userId = _currentUserId.value ?: return@withContext
        userDao.getUserById(userId)?.let { user ->
            userDao.update(user.copy(name = name, collegeId = collegeId))
        }
    }

    suspend fun updateDarkMode(isDark: Boolean) = withContext(Dispatchers.IO) {
        val userId = _currentUserId.value ?: return@withContext
        userDao.getUserById(userId)?.let { user ->
            userDao.update(user.copy(darkMode = isDark))
        }
    }

    suspend fun updateSecurityPin(newPin: String) = withContext(Dispatchers.IO) {
        val userId = _currentUserId.value ?: return@withContext
        userDao.getUserById(userId)?.let { user ->
            userDao.update(user.copy(securityPin = newPin))
        }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        val userId = _currentUserId.value ?: return@withContext
        userDao.getUserById(userId)?.let { user ->
            userDao.update(user.copy(isAppLockEnabled = enabled))
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        val userId = _currentUserId.value ?: return@withContext
        userDao.getUserById(userId)?.let { user ->
            userDao.update(user.copy(isBiometricEnabled = enabled))
        }
    }

    suspend fun clearCurrentUserFinancialData() = withContext(Dispatchers.IO) {
        val userId = _currentUserId.value ?: return@withContext
        transactionDao.deleteAllForUser(userId)
        budgetDao.deleteAllForUser(userId)
    }
}
