package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.ui.theme.CategoryBills
import com.example.ui.theme.CategoryBillsBg
import com.example.ui.theme.CategoryEntertainment
import com.example.ui.theme.CategoryEntertainmentBg
import com.example.ui.theme.CategoryFood
import com.example.ui.theme.CategoryFoodBg
import com.example.ui.theme.CategoryOther
import com.example.ui.theme.CategoryOtherBg
import com.example.ui.theme.CategoryTravel
import com.example.ui.theme.CategoryTravelBg
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class ExpenseCategory(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val backgroundColor: Color
) {
    Food("Food", Icons.Default.Fastfood, CategoryFood, CategoryFoodBg),
    Travel("Travel", Icons.Default.Flight, CategoryTravel, CategoryTravelBg),
    Bills("Bills", Icons.Default.Receipt, CategoryBills, CategoryBillsBg),
    Entertainment("Entertainment", Icons.Default.Movie, CategoryEntertainment, CategoryEntertainmentBg),
    Other("Other", Icons.Default.Category, CategoryOther, CategoryOtherBg);

    companion object {
        fun fromString(name: String): ExpenseCategory {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: Other
        }
    }
}

fun formatRupees(amount: Double, showDecimals: Boolean = false): String {
    return if (showDecimals || (amount % 1.0 != 0.0)) {
        val formatted = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }.format(amount)
        "₹$formatted"
    } else {
        val formatted = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 0
        }.format(amount)
        "₹$formatted"
    }
}

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val category: String, // 'Food' | 'Travel' | 'Bills' | 'Entertainment' | 'Other'
    val note: String,
    val date: String // ISO string format: "yyyy-MM-dd'T'HH:mm:ss" or "yyyy-MM-dd"
) {
    val categoryEnum: ExpenseCategory
        get() = ExpenseCategory.fromString(category)

    val formattedAmount: String
        get() = formatRupees(amount)

    val displayDate: String
        get() {
            return try {
                val inputFormat = if (date.contains("T")) {
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                } else {
                    SimpleDateFormat("yyyy-MM-dd", Locale.US)
                }
                val parsed = inputFormat.parse(date) ?: Date()
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val expenseDayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(parsed)

                if (todayStr == expenseDayStr) {
                    "Today"
                } else {
                    SimpleDateFormat("MMM d, yyyy", Locale.US).format(parsed)
                }
            } catch (e: Exception) {
                date
            }
        }

    val yearMonthKey: String
        get() {
            return try {
                if (date.length >= 7) date.substring(0, 7) else ""
            } catch (e: Exception) {
                ""
            }
        }
}
