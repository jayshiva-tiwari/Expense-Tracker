package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.Expense
import com.example.model.MonthlyBudget
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Database(
    entities = [Expense::class, MonthlyBudget::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao

    companion object {
        const val DEFAULT_MONTHLY_BUDGET_INR = 25000.0
        private const val PREFS_NAME = "expense_tracker_prefs"
        private const val KEY_INITIAL_SEED_DONE = "initial_seed_done"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `monthly_budgets` (`yearMonth` TEXT NOT NULL, `budgetAmount` REAL NOT NULL, PRIMARY KEY(`yearMonth`))"
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_tracker_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun isInitialSeedDone(context: Context): Boolean {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_INITIAL_SEED_DONE, false)
        }

        fun markInitialSeedDone(context: Context) {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_INITIAL_SEED_DONE, true).apply()
        }

        suspend fun populateInitialSampleData(dao: ExpenseDao) {
            val cal = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val monthFormat = SimpleDateFormat("yyyy-MM", Locale.US)
            val currentYearMonth = monthFormat.format(cal.time)

            // Seed ₹25,000 monthly account balance for the current month if not already set
            if (dao.getBudgetForMonth(currentYearMonth) == null) {
                dao.upsertMonthlyBudget(
                    MonthlyBudget(
                        yearMonth = currentYearMonth,
                        budgetAmount = DEFAULT_MONTHLY_BUDGET_INR
                    )
                )
            }

            // Today
            val todayStr = dateFormat.format(cal.time)

            // Yesterday
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = dateFormat.format(cal.time)

            // 2 days ago
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val twoDaysAgoStr = dateFormat.format(cal.time)

            // 4 days ago
            cal.add(Calendar.DAY_OF_YEAR, -2)
            val fourDaysAgoStr = dateFormat.format(cal.time)

            // 6 days ago
            cal.add(Calendar.DAY_OF_YEAR, -2)
            val sixDaysAgoStr = dateFormat.format(cal.time)

            val sampleExpenses = listOf(
                Expense(
                    amount = 380.0,
                    category = "Food",
                    note = "Morning chai, snacks & breakfast",
                    date = todayStr
                ),
                Expense(
                    amount = 650.0,
                    category = "Travel",
                    note = "Metro pass & cab ride to office",
                    date = todayStr
                ),
                Expense(
                    amount = 2450.0,
                    category = "Food",
                    note = "Weekly grocery & vegetables",
                    date = yesterdayStr
                ),
                Expense(
                    amount = 3200.0,
                    category = "Bills",
                    note = "Electricity & broadband Wi-Fi bill",
                    date = twoDaysAgoStr
                ),
                Expense(
                    amount = 950.0,
                    category = "Entertainment",
                    note = "Weekend movie tickets & popcorn",
                    date = fourDaysAgoStr
                ),
                Expense(
                    amount = 1499.0,
                    category = "Bills",
                    note = "Mobile recharge & OTT subscription",
                    date = sixDaysAgoStr
                ),
                Expense(
                    amount = 850.0,
                    category = "Other",
                    note = "Household supplies & stationery",
                    date = sixDaysAgoStr
                )
            )
            dao.insertExpenses(sampleExpenses)
        }
    }
}
