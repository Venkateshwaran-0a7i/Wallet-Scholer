package com.walletscholer.app.data.model

data class CategoryItem(
    val id: String,
    val name: String,
    val group: String, // "NEEDS", "WANTS", "SAVINGS", "OTHER", "INCOME"
    val iconKey: String,
    val isExpense: Boolean = true
)

object DefaultCategories {
    val EXPENSE_CATEGORIES = listOf(
        CategoryItem("rent", "Rent", "NEEDS", "building"),
        CategoryItem("food", "Food", "NEEDS", "utensils"),
        CategoryItem("transport", "Transport", "NEEDS", "car"),
        CategoryItem("shopping", "Shopping", "WANTS", "shopping_bag"),
        CategoryItem("entertainment", "Entertainment", "WANTS", "film"),
        CategoryItem("savings", "Savings", "SAVINGS", "piggy_bank"),
        CategoryItem("emergency", "Emergency", "SAVINGS", "alert"),
        CategoryItem("medicine", "Medicine", "NEEDS", "pill"),
        CategoryItem("emi", "EMI", "NEEDS", "credit_card"),
        CategoryItem("investment", "Investment", "SAVINGS", "trending_up"),
        CategoryItem("other", "Other", "OTHER", "more_horizontal")
    )

    val INCOME_CATEGORIES = listOf(
        CategoryItem("salary", "Salary", "INCOME", "briefcase", isExpense = false),
        CategoryItem("freelance", "Freelance", "INCOME", "laptop", isExpense = false),
        CategoryItem("bonus", "Bonus", "INCOME", "gift", isExpense = false),
        CategoryItem("other_income", "Other", "INCOME", "more_horizontal", isExpense = false)
    )

    val ALL_CATEGORIES = EXPENSE_CATEGORIES + INCOME_CATEGORIES

    fun findCategory(id: String): CategoryItem {
        return ALL_CATEGORIES.find { it.id == id }
            ?: CategoryItem(id, id.replaceFirstChar { it.uppercase() }, "OTHER", "more_horizontal")
    }
}
