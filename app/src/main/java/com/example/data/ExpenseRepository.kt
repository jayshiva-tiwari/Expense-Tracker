package com.example.data

import android.content.Context
import com.example.model.Expense
import com.example.model.MonthlyBudget
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val appContext: Context? = null
) {

    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()

    val recentExpenses: Flow<List<Expense>> = expenseDao.getRecentExpenses(5)

    val allMonthlyBudgets: Flow<List<MonthlyBudget>> = expenseDao.getAllMonthlyBudgets()

    suspend fun insert(expense: Expense): Long {
        // Mark initial seed done as soon as user interacts with the database
        appContext?.let { AppDatabase.markInitialSeedDone(it) }
        return expenseDao.insertExpense(expense)
    }

    suspend fun delete(expense: Expense) {
        appContext?.let { AppDatabase.markInitialSeedDone(it) }
        expenseDao.deleteExpense(expense)
    }

    suspend fun deleteById(id: String) {
        appContext?.let { AppDatabase.markInitialSeedDone(it) }
        expenseDao.deleteExpenseById(id)
    }

    suspend fun setMonthlyBudget(yearMonth: String, amount: Double) {
        appContext?.let { AppDatabase.markInitialSeedDone(it) }
        expenseDao.upsertMonthlyBudget(
            MonthlyBudget(
                yearMonth = yearMonth,
                budgetAmount = amount
            )
        )
    }

    suspend fun ensureDataPopulated() {
        val ctx = appContext
        if (ctx != null) {
            if (!AppDatabase.isInitialSeedDone(ctx)) {
                if (expenseDao.getCount() == 0) {
                    AppDatabase.populateInitialSampleData(expenseDao)
                }
                AppDatabase.markInitialSeedDone(ctx)
            }
        } else {
            // Fallback for unit tests without context
            if (expenseDao.getCount() == 0) {
                AppDatabase.populateInitialSampleData(expenseDao)
            }
        }
    }

    suspend fun clearAllExpenses() {
        appContext?.let { AppDatabase.markInitialSeedDone(it) }
        expenseDao.clearAll()
    }

    suspend fun resetToSampleData() {
        appContext?.let { AppDatabase.markInitialSeedDone(it) }
        expenseDao.clearAll()
        expenseDao.clearAllBudgets()
        AppDatabase.populateInitialSampleData(expenseDao)
    }
}
