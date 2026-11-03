package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_budgets")
data class MonthlyBudget(
    @PrimaryKey
    val yearMonth: String, // e.g., "2026-11"
    val budgetAmount: Double // e.g., 25000.0
)
