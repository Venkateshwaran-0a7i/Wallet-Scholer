package com.walletscholer.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val monthKey: String, // "YYYY-MM"
    val income: Double = 0.0,
    val allocationsJson: String = "{}", // JSON map of categoryId -> Double
    val customCategoriesJson: String = "[]" // JSON array of custom CategoryItem
)
