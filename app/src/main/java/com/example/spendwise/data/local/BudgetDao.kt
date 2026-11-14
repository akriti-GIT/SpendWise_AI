package com.example.spendwise.data.local

import androidx.room.*
import com.example.spendwise.data.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets WHERE userId = :userId AND month = :month AND year = :year LIMIT 1")
    fun getBudgetForMonthFlow(userId: String, month: Int, year: Int): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE userId = :userId AND month = :month AND year = :year LIMIT 1")
    fun getBudgetForMonth(userId: String, month: Int, year: Int): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(budget: BudgetEntity): Long

    @Query("DELETE FROM budgets WHERE userId = :userId")
    fun deleteAllForUser(userId: String): Int
}
