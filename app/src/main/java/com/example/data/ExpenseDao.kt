package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.Expense
import com.example.model.MonthlyBudget
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC LIMIT :limit")
    fun getRecentExpenses(limit: Int = 5): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<Expense>)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: String)

    @Query("SELECT COUNT(*) FROM expenses")
    suspend fun getCount(): Int

    @Query("DELETE FROM expenses")
    suspend fun clearAll()

    // Monthly Budgets / Account Balance
    @Query("SELECT * FROM monthly_budgets")
    fun getAllMonthlyBudgets(): Flow<List<MonthlyBudget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMonthlyBudget(budget: MonthlyBudget)

    @Query("SELECT * FROM monthly_budgets WHERE yearMonth = :yearMonth LIMIT 1")
    suspend fun getBudgetForMonth(yearMonth: String): MonthlyBudget?

    @Query("DELETE FROM monthly_budgets")
    suspend fun clearAllBudgets()
}
