package com.example.spendwise.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.spendwise.data.entity.BudgetEntity
import com.example.spendwise.data.entity.CategoryEntity
import com.example.spendwise.data.entity.TransactionEntity
import com.example.spendwise.data.entity.UserEntity
import com.example.spendwise.data.entity.UserSettingsEntity
import com.example.spendwise.model.PaymentMethod
import com.example.spendwise.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Database(
    entities = [
        UserEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        CategoryEntity::class,
        UserSettingsEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun categoryDao(): CategoryDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create users table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `users` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `passwordHash` TEXT NOT NULL,
                        `securityPin` TEXT NOT NULL,
                        `collegeId` TEXT NOT NULL,
                        `currency` TEXT NOT NULL,
                        `darkMode` INTEGER NOT NULL,
                        `notificationsEnabled` INTEGER NOT NULL,
                        `monthlyBudget` REAL NOT NULL,
                        `isAppLockEnabled` INTEGER NOT NULL,
                        `isBiometricEnabled` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_email` ON `users` (`email`)")

                // 2. Quarantine legacy demo user if user_settings had Maya Chen
                db.execSQL("""
                    INSERT OR IGNORE INTO `users` (
                        `id`, `name`, `email`, `passwordHash`, `securityPin`, `collegeId`,
                        `currency`, `darkMode`, `notificationsEnabled`, `monthlyBudget`,
                        `isAppLockEnabled`, `isBiometricEnabled`, `createdAt`
                    ) VALUES (
                        'demo_maya_chen', 'Maya Chen', 'maya.chen@campus.edu',
                        '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8',
                        '1234', 'MCA-2026-087', '₹', 0, 1, 10000.0, 1, 1, 0
                    )
                """.trimIndent())

                // 3. Add userId column to transactions with indices
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `userId` TEXT NOT NULL DEFAULT 'demo_maya_chen'")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_userId` ON `transactions` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_userId_date` ON `transactions` (`userId`, `date`)")

                // 4. Add userId column to budgets with index
                db.execSQL("ALTER TABLE `budgets` ADD COLUMN `userId` TEXT NOT NULL DEFAULT 'demo_maya_chen'")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_budgets_userId_month_year` ON `budgets` (`userId`, `month`, `year`)")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "spendwise_database"
                )
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }
        }

        /**
         * Populates ONLY universal master categories on new database creation.
         * NEVER automatically inserts sample transactions or demo accounts for new users.
         */
        suspend fun populateInitialData(database: AppDatabase) {
            val categoryDao = database.categoryDao()

            val defaultCategories = listOf(
                CategoryEntity(name = "Food", iconName = "restaurant", colorHex = "#C1866B", type = TransactionType.EXPENSE),
                CategoryEntity(name = "Transport", iconName = "directions_bus", colorHex = "#A8B79B", type = TransactionType.EXPENSE),
                CategoryEntity(name = "Education", iconName = "menu_book", colorHex = "#2F3B2F", type = TransactionType.EXPENSE),
                CategoryEntity(name = "Shopping", iconName = "shopping_bag", colorHex = "#E0BBA8", type = TransactionType.EXPENSE),
                CategoryEntity(name = "Entertainment", iconName = "theater_comedy", colorHex = "#C1866B", type = TransactionType.EXPENSE),
                CategoryEntity(name = "Health", iconName = "favorite", colorHex = "#A96B4E", type = TransactionType.EXPENSE),
                CategoryEntity(name = "Bills", iconName = "receipt_long", colorHex = "#A8B79B", type = TransactionType.EXPENSE),
                CategoryEntity(name = "Other", iconName = "more_horiz", colorHex = "#D6D3C6", type = TransactionType.EXPENSE),
                CategoryEntity(name = "Salary", iconName = "payments", colorHex = "#4A6B4A", type = TransactionType.INCOME),
                CategoryEntity(name = "Scholarship", iconName = "school", colorHex = "#2F3B2F", type = TransactionType.INCOME),
                CategoryEntity(name = "Allowance", iconName = "savings", colorHex = "#C6D4BC", type = TransactionType.INCOME)
            )
            categoryDao.insertAll(defaultCategories)
        }

        /**
         * Explicitly populates demo transactions strictly scoped to the specified user.
         * Never called automatically for new accounts.
         */
        suspend fun seedSampleDataForUser(database: AppDatabase, userId: String) {
            val transactionDao = database.transactionDao()
            val budgetDao = database.budgetDao()

            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH) + 1
            val currentYear = calendar.get(Calendar.YEAR)

            budgetDao.insertOrUpdate(
                BudgetEntity(
                    userId = userId,
                    month = currentMonth,
                    year = currentYear,
                    amount = 10000.0,
                    categoryBudgetsJson = "{\"Food\":3000.0,\"Transport\":2000.0,\"Education\":2000.0,\"Shopping\":1500.0,\"Entertainment\":1000.0,\"Other\":500.0}"
                )
            )

            val now = System.currentTimeMillis()
            val dayMs = 86400000L

            val sampleTransactions = listOf(
                TransactionEntity(
                    userId = userId,
                    type = TransactionType.INCOME,
                    amount = 10000.0,
                    category = "Scholarship",
                    description = "Semester Merit Scholarship",
                    date = now - (dayMs * 5),
                    paymentMethod = PaymentMethod.UPI
                ),
                TransactionEntity(
                    userId = userId,
                    type = TransactionType.INCOME,
                    amount = 10000.0,
                    category = "Salary",
                    description = "Part-time Campus Assistant shift",
                    date = now - (dayMs * 2),
                    paymentMethod = PaymentMethod.UPI
                ),
                TransactionEntity(
                    userId = userId,
                    type = TransactionType.EXPENSE,
                    amount = 250.0,
                    category = "Food",
                    description = "Campus Cafeteria Lunch",
                    date = now - (3600000L * 2),
                    paymentMethod = PaymentMethod.UPI
                ),
                TransactionEntity(
                    userId = userId,
                    type = TransactionType.EXPENSE,
                    amount = 80.0,
                    category = "Transport",
                    description = "Metro Recharge",
                    date = now - (3600000L * 4),
                    paymentMethod = PaymentMethod.CARD
                ),
                TransactionEntity(
                    userId = userId,
                    type = TransactionType.EXPENSE,
                    amount = 500.0,
                    category = "Education",
                    description = "Algorithms & Data Structures Textbook",
                    date = now - dayMs,
                    paymentMethod = PaymentMethod.CARD
                ),
                TransactionEntity(
                    userId = userId,
                    type = TransactionType.EXPENSE,
                    amount = 2400.0,
                    category = "Food",
                    description = "Monthly Mess Subscription",
                    date = now - (dayMs * 3),
                    paymentMethod = PaymentMethod.UPI
                ),
                TransactionEntity(
                    userId = userId,
                    type = TransactionType.EXPENSE,
                    amount = 1420.0,
                    category = "Transport",
                    description = "Monthly Bus Pass",
                    date = now - (dayMs * 4),
                    paymentMethod = PaymentMethod.UPI
                ),
                TransactionEntity(
                    userId = userId,
                    type = TransactionType.EXPENSE,
                    amount = 700.0,
                    category = "Education",
                    description = "Lab Notebooks & Stationery",
                    date = now - (dayMs * 6),
                    paymentMethod = PaymentMethod.CASH
                ),
                TransactionEntity(
                    userId = userId,
                    type = TransactionType.EXPENSE,
                    amount = 1350.0,
                    category = "Shopping",
                    description = "College Backpack & Umbrella",
                    date = now - (dayMs * 7),
                    paymentMethod = PaymentMethod.CARD
                ),
                TransactionEntity(
                    userId = userId,
                    type = TransactionType.EXPENSE,
                    amount = 500.0,
                    category = "Entertainment",
                    description = "Weekend Movie & Snacks",
                    date = now - (dayMs * 8),
                    paymentMethod = PaymentMethod.UPI
                ),
                TransactionEntity(
                    userId = userId,
                    type = TransactionType.EXPENSE,
                    amount = 350.0,
                    category = "Other",
                    description = "Mobile Data Recharge",
                    date = now - (dayMs * 9),
                    paymentMethod = PaymentMethod.UPI
                )
            )
            transactionDao.insertAll(sampleTransactions)
        }
    }
}
