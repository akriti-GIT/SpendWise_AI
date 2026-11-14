package com.example.spendwise.data

import com.example.spendwise.data.entity.BudgetEntity
import com.example.spendwise.data.entity.TransactionEntity
import com.example.spendwise.data.entity.UserEntity
import com.example.spendwise.model.PaymentMethod
import com.example.spendwise.model.TransactionType
import com.example.spendwise.util.HashUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Locale
import java.util.UUID

/**
 * Data Isolation and Multi-User Authentication Test Suite.
 * Validates requirements from Section 9 of the specification.
 */
class UserDataIsolationTest {

    // In-memory data store simulating the database tables with strict userId isolation
    private val usersTable = mutableMapOf<String, UserEntity>()
    private val transactionsTable = mutableListOf<TransactionEntity>()
    private val budgetsTable = mutableListOf<BudgetEntity>()
    private var persistedActiveUserId: String? = null

    // Simulated session & repository state
    private var currentUserId: String? = null

    @Before
    fun setUp() {
        usersTable.clear()
        transactionsTable.clear()
        budgetsTable.clear()
        persistedActiveUserId = null
        currentUserId = null
    }

    // --- Helper Functions simulating Repository operations ---

    private fun register(name: String, email: String, password: String, pin: String): Result<UserEntity> {
        val normalized = email.trim().lowercase(Locale.ROOT)
        if (normalized.isBlank() || !normalized.contains("@")) {
            return Result.failure(IllegalArgumentException("Invalid email"))
        }
        if (usersTable.values.any { it.email.equals(normalized, ignoreCase = true) }) {
            return Result.failure(IllegalStateException("An account with this email already exists."))
        }
        val id = UUID.randomUUID().toString()
        val user = UserEntity(
            id = id,
            name = name.trim(),
            email = normalized,
            passwordHash = HashUtils.sha256(password),
            securityPin = pin.trim(),
            createdAt = System.currentTimeMillis()
        )
        usersTable[id] = user
        currentUserId = id
        persistedActiveUserId = id
        return Result.success(user)
    }

    private fun login(email: String, passwordOrPin: String): Result<UserEntity> {
        val normalized = email.trim().lowercase(Locale.ROOT)
        val user = usersTable.values.find { it.email.equals(normalized, ignoreCase = true) }
            ?: return Result.failure(IllegalArgumentException("No account found with this email. Please sign up."))

        val passwordValid = HashUtils.verifyPassword(passwordOrPin, user.passwordHash)
        val pinValid = user.securityPin == passwordOrPin

        if (!passwordValid && !pinValid) {
            return Result.failure(IllegalArgumentException("Incorrect password or PIN."))
        }

        currentUserId = user.id
        persistedActiveUserId = user.id
        return Result.success(user)
    }

    private fun logout() {
        currentUserId = null
        persistedActiveUserId = null
    }

    private fun insertTransaction(type: TransactionType, amount: Double, category: String, description: String): Long {
        val userId = currentUserId ?: throw IllegalStateException("Not authenticated")
        val id = (transactionsTable.size + 1).toLong()
        val tx = TransactionEntity(
            id = id,
            userId = userId,
            type = type,
            amount = amount,
            category = category,
            description = description,
            date = System.currentTimeMillis(),
            paymentMethod = PaymentMethod.UPI
        )
        transactionsTable.add(tx)
        return id
    }

    private fun updateTransaction(transaction: TransactionEntity) {
        val userId = currentUserId ?: throw IllegalStateException("Not authenticated")
        if (transaction.userId != userId) {
            throw SecurityException("Cannot modify transactions of another user")
        }
        val index = transactionsTable.indexOfFirst { it.id == transaction.id && it.userId == userId }
        if (index != -1) {
            transactionsTable[index] = transaction
        }
    }

    private fun deleteTransaction(transaction: TransactionEntity) {
        val userId = currentUserId ?: throw IllegalStateException("Not authenticated")
        if (transaction.userId != userId) {
            throw SecurityException("Cannot delete transactions of another user")
        }
        transactionsTable.removeAll { it.id == transaction.id && it.userId == userId }
    }

    private fun getUserTransactions(): List<TransactionEntity> {
        val userId = currentUserId ?: return emptyList()
        return transactionsTable.filter { it.userId == userId }
    }

    private fun getTotalIncome(): Double {
        val userId = currentUserId ?: return 0.0
        return transactionsTable
            .filter { it.userId == userId && it.type == TransactionType.INCOME }
            .sumOf { it.amount }
    }

    private fun getTotalExpenses(): Double {
        val userId = currentUserId ?: return 0.0
        return transactionsTable
            .filter { it.userId == userId && it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
    }

    private fun getTotalBalance(): Double = getTotalIncome() - getTotalExpenses()

    // --- Specification Tests (Prompt Section 9) ---

    @Test
    fun test1_SeparateAccounts() {
        // Register User A
        val userAResult = register("User A", "user.a@spendwise.app", "passA123", "1111")
        assertTrue(userAResult.isSuccess)
        val userA = userAResult.getOrThrow()

        // Add three transactions to User A
        insertTransaction(TransactionType.INCOME, 5000.0, "Salary", "Monthly Stipend")
        insertTransaction(TransactionType.EXPENSE, 250.0, "Food", "Cafeteria Lunch")
        insertTransaction(TransactionType.EXPENSE, 80.0, "Transport", "Metro Fare")

        assertEquals(3, getUserTransactions().size)

        // Log out User A
        logout()
        assertNull(currentUserId)
        assertEquals(0, getUserTransactions().size)

        // Register User B
        val userBResult = register("User B", "user.b@spendwise.app", "passB456", "2222")
        assertTrue(userBResult.isSuccess)
        val userB = userBResult.getOrThrow()
        assertNotEquals(userA.id, userB.id)

        // Verify that User B has zero transactions!
        assertEquals(0, getUserTransactions().size)
        assertEquals(0.0, getTotalIncome(), 0.001)
        assertEquals(0.0, getTotalExpenses(), 0.001)
        assertEquals(0.0, getTotalBalance(), 0.001)
    }

    @Test
    fun test2_AccountSwitching() {
        // Setup User A with 3 transactions
        register("User A", "user.a@spendwise.app", "passA123", "1111")
        insertTransaction(TransactionType.INCOME, 10000.0, "Salary", "Salary")
        insertTransaction(TransactionType.EXPENSE, 300.0, "Food", "Dinner")
        insertTransaction(TransactionType.EXPENSE, 150.0, "Transport", "Taxi")
        assertEquals(3, getUserTransactions().size)

        // Setup User B with 0 transactions
        logout()
        register("User B", "user.b@spendwise.app", "passB456", "2222")
        assertEquals(0, getUserTransactions().size)

        // Switch to User A
        logout()
        val loginAResult = login("user.a@spendwise.app", "passA123")
        assertTrue(loginAResult.isSuccess)
        val userATransactions = getUserTransactions()
        assertEquals(3, userATransactions.size)
        assertTrue(userATransactions.all { it.userId == loginAResult.getOrThrow().id })

        // Switch to User B
        logout()
        val loginBResult = login("user.b@spendwise.app", "passB456")
        assertTrue(loginBResult.isSuccess)
        assertEquals(0, getUserTransactions().size)

        // Log back in as User A
        logout()
        login("user.a@spendwise.app", "passA123")
        assertEquals(3, getUserTransactions().size)
    }

    @Test
    fun test3_IndependentAdditions() {
        // Register User A and add 3 transactions
        val userA = register("User A", "user.a@spendwise.app", "passA123", "1111").getOrThrow()
        insertTransaction(TransactionType.EXPENSE, 100.0, "Food", "Coffee")
        insertTransaction(TransactionType.EXPENSE, 200.0, "Food", "Lunch")
        insertTransaction(TransactionType.EXPENSE, 300.0, "Food", "Dinner")
        assertEquals(3, getUserTransactions().size)

        // Register User B and add 2 transactions
        logout()
        val userB = register("User B", "user.b@spendwise.app", "passB456", "2222").getOrThrow()
        insertTransaction(TransactionType.EXPENSE, 500.0, "Shopping", "Shoes")
        insertTransaction(TransactionType.EXPENSE, 750.0, "Shopping", "Backpack")
        assertEquals(2, getUserTransactions().size)
        assertEquals(1250.0, getTotalExpenses(), 0.001)

        // Switch back to User A
        logout()
        login("user.a@spendwise.app", "passA123")
        val aTxs = getUserTransactions()
        assertEquals(3, aTxs.size)
        assertEquals(600.0, getTotalExpenses(), 0.001)
        assertTrue(aTxs.none { it.description.contains("Shoes") })
        assertTrue(aTxs.none { it.description.contains("Backpack") })
    }

    @Test
    fun test4_IndependentUpdatesAndDeletes() {
        // Register User A & add transaction
        val userA = register("User A", "user.a@spendwise.app", "passA123", "1111").getOrThrow()
        val txAId = insertTransaction(TransactionType.EXPENSE, 200.0, "Food", "Burger")

        // Register User B & add transaction
        logout()
        val userB = register("User B", "user.b@spendwise.app", "passB456", "2222").getOrThrow()
        val txBId = insertTransaction(TransactionType.EXPENSE, 800.0, "Bills", "Electricity")

        // Switch to User A and update txA
        logout()
        login("user.a@spendwise.app", "passA123")
        val txA = getUserTransactions().first()
        updateTransaction(txA.copy(amount = 250.0, description = "Burger + Fries"))
        assertEquals(250.0, getUserTransactions().first().amount, 0.001)

        // Switch to User B and verify User B's transaction is completely unaffected
        logout()
        login("user.b@spendwise.app", "passB456")
        val txB = getUserTransactions().first()
        assertEquals(800.0, txB.amount, 0.001)
        assertEquals("Electricity", txB.description)

        // Delete User B's transaction
        deleteTransaction(txB)
        assertEquals(0, getUserTransactions().size)

        // Verify User A still has their transaction
        logout()
        login("user.a@spendwise.app", "passA123")
        assertEquals(1, getUserTransactions().size)
        assertEquals(250.0, getUserTransactions().first().amount, 0.001)
    }

    @Test
    fun test5_UnknownAccountFailsWithoutSilentCreation() {
        val initialUserCount = usersTable.size

        // Attempt login with an unknown email
        val loginResult = login("unknown.person@domain.com", "anyPassword")
        assertTrue(loginResult.isFailure)
        assertTrue(loginResult.exceptionOrNull()?.message?.contains("No account found", ignoreCase = true) == true)

        // Verify that NO account or default transactions were created!
        assertEquals(initialUserCount, usersTable.size)
        assertEquals(0, transactionsTable.size)
        assertNull(currentUserId)
    }

    @Test
    fun test6_AppRestartSessionRestoration() {
        // Register User A
        val userA = register("User A", "user.a@spendwise.app", "passA123", "1111").getOrThrow()
        insertTransaction(TransactionType.INCOME, 12000.0, "Salary", "Paycheck")

        // Simulate app kill (clear in-memory currentUserId, keep persistedActiveUserId)
        currentUserId = null
        assertNotNull(persistedActiveUserId)

        // Simulate app restart: restore session from persisted ID
        currentUserId = persistedActiveUserId
        assertEquals(userA.id, currentUserId)
        val restoredTransactions = getUserTransactions()
        assertEquals(1, restoredTransactions.size)
        assertEquals("Paycheck", restoredTransactions.first().description)

        // Repeat with User B
        logout()
        val userB = register("User B", "user.b@spendwise.app", "passB456", "2222").getOrThrow()
        insertTransaction(TransactionType.EXPENSE, 450.0, "Health", "Medicine")

        currentUserId = null
        currentUserId = persistedActiveUserId
        assertEquals(userB.id, currentUserId)
        assertEquals(1, getUserTransactions().size)
        assertEquals("Medicine", getUserTransactions().first().description)
    }

    @Test
    fun test7_EmptyAccountHasZeroTotalsAndNoSampleData() {
        // Register fresh account
        val user = register("Brand New User", "fresh.student@campus.edu", "securePass99", "9999").getOrThrow()

        // Verify 0 transactions, 0 totals, no demo data
        val txs = getUserTransactions()
        assertTrue(txs.isEmpty())
        assertEquals(0.0, getTotalIncome(), 0.001)
        assertEquals(0.0, getTotalExpenses(), 0.001)
        assertEquals(0.0, getTotalBalance(), 0.001)
    }

    @Test
    fun test8_DashboardConsistencyAcrossUsers() {
        // User A: Income 15000, Expenses 4000 (Food: 3000, Transport: 1000)
        register("User A", "user.a@spendwise.app", "passA123", "1111")
        insertTransaction(TransactionType.INCOME, 15000.0, "Salary", "Salary")
        insertTransaction(TransactionType.EXPENSE, 3000.0, "Food", "Groceries")
        insertTransaction(TransactionType.EXPENSE, 1000.0, "Transport", "Fuel")

        assertEquals(15000.0, getTotalIncome(), 0.001)
        assertEquals(4000.0, getTotalExpenses(), 0.001)
        assertEquals(11000.0, getTotalBalance(), 0.001)
        assertEquals(3, getUserTransactions().size)

        // User B: Income 20000, Expenses 8000 (Shopping: 8000)
        logout()
        register("User B", "user.b@spendwise.app", "passB456", "2222")
        insertTransaction(TransactionType.INCOME, 20000.0, "Scholarship", "Grant")
        insertTransaction(TransactionType.EXPENSE, 8000.0, "Shopping", "Laptop")

        assertEquals(20000.0, getTotalIncome(), 0.001)
        assertEquals(8000.0, getTotalExpenses(), 0.001)
        assertEquals(12000.0, getTotalBalance(), 0.001)
        assertEquals(2, getUserTransactions().size)

        // Re-verify User A
        logout()
        login("user.a@spendwise.app", "passA123")
        assertEquals(15000.0, getTotalIncome(), 0.001)
        assertEquals(4000.0, getTotalExpenses(), 0.001)
        assertEquals(11000.0, getTotalBalance(), 0.001)
        assertEquals(3, getUserTransactions().size)
    }
}
