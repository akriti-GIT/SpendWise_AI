package com.example.spendwise.data.local

import androidx.room.*
import com.example.spendwise.data.entity.TransactionEntity
import com.example.spendwise.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC, createdAt DESC")
    fun getAllTransactionsFlow(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC, createdAt DESC LIMIT :limit")
    fun getRecentTransactionsFlow(userId: String, limit: Int = 5): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id AND userId = :userId")
    fun getTransactionById(id: Long, userId: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE userId = :userId AND date BETWEEN :startTime AND :endTime ORDER BY date DESC")
    fun getTransactionsBetween(userId: String, startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE userId = :userId AND type = :type")
    fun getTotalAmountByTypeFlow(userId: String, type: TransactionType): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE userId = :userId AND type = 'EXPENSE' AND date BETWEEN :startTime AND :endTime")
    fun getExpensesBetweenFlow(userId: String, startTime: Long, endTime: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE userId = :userId AND type = 'INCOME' AND date BETWEEN :startTime AND :endTime")
    fun getIncomeBetweenFlow(userId: String, startTime: Long, endTime: Long): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(transactions: List<TransactionEntity>): List<Long>

    @Update
    fun update(transaction: TransactionEntity): Int

    @Delete
    fun delete(transaction: TransactionEntity): Int

    @Query("DELETE FROM transactions WHERE id = :id AND userId = :userId")
    fun deleteById(id: Long, userId: String): Int

    @Query("DELETE FROM transactions WHERE userId = :userId")
    fun deleteAllForUser(userId: String): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE userId = :userId")
    fun getCountForUser(userId: String): Int
}
