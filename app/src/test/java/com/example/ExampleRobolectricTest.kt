package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.ExpenseRepository
import com.example.model.Expense
import com.example.model.ExpenseCategory
import com.example.model.MonthlyBudget
import com.example.model.formatRupees
import com.example.ui.AppTab
import com.example.ui.ExpenseViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ExpenseTracker", appName)
    }

    @Test
    fun `insert and retrieve expenses from Room in Rupees`() = runTest {
        val dao = database.expenseDao()
        val expense = Expense(
            amount = 450.50,
            category = "Food",
            note = "Team lunch",
            date = "2026-11-01T12:30:00"
        )
        dao.insertExpense(expense)

        val all = dao.getAllExpenses().first()
        assertEquals(1, all.size)
        assertEquals(450.50, all[0].amount, 0.001)
        assertEquals("Food", all[0].category)
        assertEquals(ExpenseCategory.Food, all[0].categoryEnum)
        assertEquals("₹450.50", all[0].formattedAmount)
    }

    @Test
    fun `monthly budget defaults to 25000 rupees and can be updated`() = runTest {
        val dao = database.expenseDao()
        assertEquals(25000.0, AppDatabase.DEFAULT_MONTHLY_BUDGET_INR, 0.001)
        assertEquals("₹25,000", formatRupees(25000.0))

        dao.upsertMonthlyBudget(MonthlyBudget("2026-11", 30000.0))
        val saved = dao.getBudgetForMonth("2026-11")
        assertNotNull(saved)
        assertEquals(30000.0, saved!!.budgetAmount, 0.001)
    }

    @Test
    fun `delete expense removes record`() = runTest {
        val dao = database.expenseDao()
        val expense = Expense(
            id = "test-123",
            amount = 150.00,
            category = "Travel",
            note = "Metro ride",
            date = "2026-11-01"
        )
        dao.insertExpense(expense)
        assertEquals(1, dao.getCount())

        dao.deleteExpenseById("test-123")
        assertEquals(0, dao.getCount())
    }

    @Test
    fun `adding expense updates monthTotal and remainingBalance reactively`() = runTest {
        val repository = ExpenseRepository(database.expenseDao())
        val viewModel = ExpenseViewModel(repository)

        assertNotNull(viewModel.formState.value)
        assertEquals(AppTab.DASHBOARD, viewModel.currentTab.value)
        assertEquals(25000.0, viewModel.monthlyBudget.value, 0.001)

        val initialTotal = viewModel.monthTotal.value
        viewModel.onAmountChanged("1500")
        viewModel.onCategoryChanged(ExpenseCategory.Food)
        viewModel.onNoteChanged("Dinner with family")

        var callbackCalled = false
        viewModel.saveExpense {
            callbackCalled = true
        }
        // Wait briefly for coroutine on viewModelScope
        Thread.sleep(150)

        assertEquals(true, callbackCalled)
        assertEquals(initialTotal + 1500.0, viewModel.monthTotal.value, 0.001)
        assertEquals(25000.0 - (initialTotal + 1500.0), viewModel.remainingBalance.value, 0.001)
    }
}
