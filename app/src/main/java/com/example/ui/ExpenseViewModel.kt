package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ExpenseRepository
import com.example.model.Expense
import com.example.model.ExpenseCategory
import com.example.model.MonthlyBudget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class AppTab(val title: String) {
    DASHBOARD("Dashboard"),
    ADD_EXPENSE("Add Expense"),
    SUMMARY("Monthly Analysis")
}

data class AddExpenseUiState(
    val amount: String = "",
    val category: ExpenseCategory = ExpenseCategory.Food,
    val note: String = "",
    val dateIso: String = "",
    val dateDisplay: String = "Today",
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

class ExpenseViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    companion object {
        private fun getIsoFormat() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        private fun getMonthFormat() = SimpleDateFormat("yyyy-MM", Locale.US)
        private fun getMonthDisplayFormat() = SimpleDateFormat("MMMM yyyy", Locale.US)
        private fun getDisplayDateFormat() = SimpleDateFormat("MMM d, yyyy", Locale.US)
    }

    private val _currentTab = MutableStateFlow(AppTab.DASHBOARD)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    // Current selected calendar month key (e.g. "2026-11")
    private val _selectedMonthKey = MutableStateFlow(getMonthFormat().format(Date()))
    val selectedMonthKey: StateFlow<String> = _selectedMonthKey.asStateFlow()

    // Display string for selected month (e.g. "November 2026")
    val selectedMonthDisplay: StateFlow<String> = _selectedMonthKey.map { key ->
        try {
            val parsed = getMonthFormat().parse(key) ?: Date()
            getMonthDisplayFormat().format(parsed)
        } catch (e: Exception) {
            key
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = getMonthDisplayFormat().format(Date())
    )

    val allExpenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val recentExpenses: StateFlow<List<Expense>> = repository.recentExpenses
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val allMonthlyBudgets: StateFlow<List<MonthlyBudget>> = repository.allMonthlyBudgets
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // Form state for adding an expense
    private val _formState = MutableStateFlow(createInitialFormState())
    val formState: StateFlow<AddExpenseUiState> = _formState.asStateFlow()

    // Monthly Account Balance / Budget for the selected month (defaults to ₹25,000)
    val monthlyBudget: StateFlow<Double> = combine(allMonthlyBudgets, selectedMonthKey) { budgets, monthKey ->
        budgets.firstOrNull { it.yearMonth == monthKey }?.budgetAmount
            ?: AppDatabase.DEFAULT_MONTHLY_BUDGET_INR
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppDatabase.DEFAULT_MONTHLY_BUDGET_INR
    )

    // Filtered expenses for the currently selected month
    val monthExpenses: StateFlow<List<Expense>> = combine(allExpenses, selectedMonthKey) { list, monthKey ->
        list.filter { it.yearMonthKey == monthKey }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    // Total spent for the selected month
    val monthTotal: StateFlow<Double> = monthExpenses.map { expenses ->
        expenses.sumOf { it.amount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 0.0
    )

    // Remaining Balance in account for the selected month (Budget - Spent)
    val remainingBalance: StateFlow<Double> = combine(monthlyBudget, monthTotal) { budget, spent ->
        budget - spent
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppDatabase.DEFAULT_MONTHLY_BUDGET_INR
    )

    // Percentage of monthly account balance spent
    val budgetUsedPercent: StateFlow<Float> = combine(monthlyBudget, monthTotal) { budget, spent ->
        if (budget > 0.0) ((spent / budget) * 100.0).toFloat() else 0f
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 0f
    )

    // Category breakdown totals
    val categoryTotals: StateFlow<Map<ExpenseCategory, Double>> = monthExpenses.map { expenses ->
        val map = mutableMapOf<ExpenseCategory, Double>()
        for (category in ExpenseCategory.entries) {
            map[category] = 0.0
        }
        for (expense in expenses) {
            val cat = expense.categoryEnum
            map[cat] = (map[cat] ?: 0.0) + expense.amount
        }
        map
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ExpenseCategory.entries.associateWith { 0.0 }
    )

    // Top spending category
    val topCategory: StateFlow<ExpenseCategory?> = categoryTotals.map { totals ->
        totals.maxByOrNull { it.value }?.takeIf { it.value > 0.0 }?.key
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    init {
        viewModelScope.launch {
            repository.ensureDataPopulated()
        }
    }

    private fun createInitialFormState(): AddExpenseUiState {
        val now = Date()
        return AddExpenseUiState(
            amount = "",
            category = ExpenseCategory.Food,
            note = "",
            dateIso = getIsoFormat().format(now),
            dateDisplay = "Today",
            errorMessage = null,
            isSuccess = false
        )
    }

    fun navigateTo(tab: AppTab) {
        if (tab == AppTab.ADD_EXPENSE) {
            // Ensure error message is cleared when entering Add tab
            _formState.value = _formState.value.copy(errorMessage = null)
        }
        _currentTab.value = tab
    }

    fun nextMonth() {
        val cal = parseMonthToCalendar(_selectedMonthKey.value)
        cal.add(Calendar.MONTH, 1)
        _selectedMonthKey.value = getMonthFormat().format(cal.time)
    }

    fun previousMonth() {
        val cal = parseMonthToCalendar(_selectedMonthKey.value)
        cal.add(Calendar.MONTH, -1)
        _selectedMonthKey.value = getMonthFormat().format(cal.time)
    }

    fun resetToCurrentMonth() {
        _selectedMonthKey.value = getMonthFormat().format(Date())
    }

    private fun parseMonthToCalendar(monthKey: String): Calendar {
        val cal = Calendar.getInstance()
        try {
            val parsed = getMonthFormat().parse(monthKey)
            if (parsed != null) {
                cal.time = parsed
            }
        } catch (_: Exception) {
        }
        return cal
    }

    fun updateMonthlyBudget(newAmount: Double) {
        if (newAmount > 0.0) {
            val monthKey = _selectedMonthKey.value
            viewModelScope.launch {
                repository.setMonthlyBudget(monthKey, newAmount)
            }
        }
    }

    fun onAmountChanged(newAmount: String) {
        // Normalize commas or spaces and allow numbers + up to 1 decimal point
        val normalized = newAmount.replace(",", "")
        val filtered = normalized.filter { it.isDigit() || it == '.' }
        if (filtered.count { it == '.' } <= 1) {
            val dotIndex = filtered.indexOf('.')
            if (dotIndex == -1 || filtered.length - dotIndex <= 3) {
                _formState.value = _formState.value.copy(
                    amount = filtered,
                    errorMessage = null
                )
            }
        }
    }

    fun addQuickAmount(increment: Double) {
        val current = _formState.value.amount.replace(",", "").toDoubleOrNull() ?: 0.0
        val updated = current + increment
        val formatted = if (updated % 1.0 == 0.0) {
            updated.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", updated)
        }
        _formState.value = _formState.value.copy(
            amount = formatted,
            errorMessage = null
        )
    }

    fun onCategoryChanged(category: ExpenseCategory) {
        _formState.value = _formState.value.copy(category = category)
    }

    fun onNoteChanged(note: String) {
        _formState.value = _formState.value.copy(note = note)
    }

    fun setDateToday() {
        val now = Date()
        _formState.value = _formState.value.copy(
            dateIso = getIsoFormat().format(now),
            dateDisplay = "Today"
        )
    }

    fun setDateYesterday() {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        _formState.value = _formState.value.copy(
            dateIso = getIsoFormat().format(cal.time),
            dateDisplay = "Yesterday"
        )
    }

    fun setCustomDate(year: Int, monthZeroIndexed: Int, dayOfMonth: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroIndexed)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
        }
        _formState.value = _formState.value.copy(
            dateIso = getIsoFormat().format(cal.time),
            dateDisplay = getDisplayDateFormat().format(cal.time)
        )
    }

    fun saveExpense(onSuccess: () -> Unit) {
        val rawAmount = _formState.value.amount.replace(",", "").trim()
        val amountVal = rawAmount.toDoubleOrNull()
        if (amountVal == null || amountVal <= 0.0) {
            _formState.value = _formState.value.copy(
                errorMessage = "Please enter a valid amount greater than ₹0"
            )
            return
        }

        val noteText = _formState.value.note.trim().ifEmpty {
            "${_formState.value.category.title} expense"
        }

        val expenseDateIso = _formState.value.dateIso.ifEmpty { getIsoFormat().format(Date()) }
        val newExpense = Expense(
            id = UUID.randomUUID().toString(),
            amount = amountVal,
            category = _formState.value.category.title,
            note = noteText,
            date = expenseDateIso
        )

        viewModelScope.launch {
            repository.insert(newExpense)
            // Automatically switch the selected analysis month to the saved expense's month
            // so the user immediately sees the new expense reflected in the monthly calculations!
            if (newExpense.yearMonthKey.length == 7) {
                _selectedMonthKey.value = newExpense.yearMonthKey
            }
            // Reset form for the next entry
            _formState.value = createInitialFormState()
            // Navigate back to Dashboard
            _currentTab.value = AppTab.DASHBOARD
            onSuccess()
        }
    }

    fun clearAllExpensesThisMonth() {
        val currentList = monthExpenses.value
        viewModelScope.launch {
            currentList.forEach { expense ->
                repository.deleteById(expense.id)
            }
        }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun resetToSampleData() {
        viewModelScope.launch {
            repository.resetToSampleData()
        }
    }
}

class ExpenseViewModelFactory(
    private val repository: ExpenseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            return ExpenseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
