package com.walletscholer.app.data.repository

import com.walletscholer.app.data.local.AppDatabase
import com.walletscholer.app.data.model.BudgetEntity
import com.walletscholer.app.data.model.GoalEntity
import com.walletscholer.app.data.model.TransactionEntity
import com.walletscholer.app.data.model.UserSettingsEntity
import com.walletscholer.app.domain.result.AppResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class WalletScholarRepository(private val database: AppDatabase) : IWalletScholarRepository {

    private val transactionDao = database.transactionDao()
    private val budgetDao = database.budgetDao()
    private val goalDao = database.goalDao()
    private val userSettingsDao = database.userSettingsDao()

    override val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    override val allGoals: Flow<List<GoalEntity>> = goalDao.getAllGoals()
    override val userSettings: Flow<UserSettingsEntity?> = userSettingsDao.getSettings()

    override fun getBudgetForMonth(monthKey: String): Flow<BudgetEntity?> {
        return budgetDao.getBudgetForMonth(monthKey)
    }

    override suspend fun getSettingsDirect(): UserSettingsEntity? = withContext(Dispatchers.IO) {
        userSettingsDao.getSettingsDirect()
    }

    override suspend fun getBudgetForMonthDirect(monthKey: String): BudgetEntity? = withContext(Dispatchers.IO) {
        budgetDao.getBudgetForMonthDirect(monthKey)
    }

    override suspend fun initializeSeedDataIfNeeded(): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            if (userSettingsDao.getSettingsDirect() == null) {
                userSettingsDao.insertSettings(UserSettingsEntity())
            }

            val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
            if (budgetDao.getBudgetForMonthDirect(currentMonth) == null) {
                val emptyAllocations = JSONObject().apply {
                    put("rent", 0.0)
                    put("food", 0.0)
                    put("transport", 0.0)
                    put("shopping", 0.0)
                    put("entertainment", 0.0)
                    put("savings", 0.0)
                    put("emergency", 0.0)
                    put("medicine", 0.0)
                    put("emi", 0.0)
                    put("investment", 0.0)
                    put("other", 0.0)
                }
                budgetDao.insertBudget(
                    BudgetEntity(
                        monthKey = currentMonth,
                        income = 0.0,
                        allocationsJson = emptyAllocations.toString(),
                        customCategoriesJson = "[]"
                    )
                )
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error("Failed to initialize seed data: ${e.message}", e)
        }
    }

    override suspend fun resetDataWithPreset(preset: String, currentMonthKey: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            transactionDao.deleteAll()
            goalDao.deleteAll()

            val initialAllocations = JSONObject().apply {
                put("rent", 0.0)
                put("food", 0.0)
                put("transport", 0.0)
                put("shopping", 0.0)
                put("entertainment", 0.0)
                put("savings", 0.0)
                put("emergency", 0.0)
                put("medicine", 0.0)
                put("emi", 0.0)
                put("investment", 0.0)
                put("other", 0.0)
            }
            val budget = BudgetEntity(
                monthKey = currentMonthKey,
                income = 0.0,
                allocationsJson = initialAllocations.toString(),
                customCategoriesJson = "[]"
            )
            budgetDao.insertBudget(budget)
            val existingClean = userSettingsDao.getSettingsDirect() ?: UserSettingsEntity()
            userSettingsDao.insertSettings(existingClean.copy(salaryAmount = 0.0))
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error("Failed to reset data: ${e.message}", e)
        }
    }

    override suspend fun saveTransaction(transaction: TransactionEntity): AppResult<Unit> = withContext(Dispatchers.IO) {
        if (transaction.amount <= 0.0 || transaction.amount.isNaN() || transaction.amount.isInfinite()) {
            return@withContext AppResult.ValidationError("amount", "Amount must be a positive number greater than 0")
        }
        if (transaction.description.isBlank()) {
            return@withContext AppResult.ValidationError("description", "Description cannot be empty")
        }
        if (transaction.categoryId.isBlank()) {
            return@withContext AppResult.ValidationError("categoryId", "Category must be selected")
        }
        if (transaction.date.isBlank() || !transaction.date.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
            return@withContext AppResult.ValidationError("date", "Date must be formatted as YYYY-MM-DD")
        }
        if (transaction.type != "INCOME" && transaction.type != "EXPENSE") {
            return@withContext AppResult.ValidationError("type", "Type must be INCOME or EXPENSE")
        }

        try {
            transactionDao.insertTransaction(transaction)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error("Failed to save transaction: ${e.message}", e)
        }
    }

    override suspend fun deleteTransaction(transaction: TransactionEntity): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            transactionDao.deleteTransaction(transaction)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error("Failed to delete transaction: ${e.message}", e)
        }
    }

    override suspend fun updateTransaction(transaction: TransactionEntity): AppResult<Unit> = withContext(Dispatchers.IO) {
        if (transaction.amount <= 0.0) {
            return@withContext AppResult.ValidationError("amount", "Amount must be a positive number")
        }
        try {
            transactionDao.updateTransaction(transaction)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error("Failed to update transaction: ${e.message}", e)
        }
    }

    override suspend fun saveGoal(goal: GoalEntity): AppResult<Unit> = withContext(Dispatchers.IO) {
        if (goal.name.isBlank()) {
            return@withContext AppResult.ValidationError("name", "Goal name cannot be empty")
        }
        if (goal.targetAmount <= 0.0) {
            return@withContext AppResult.ValidationError("targetAmount", "Target amount must be greater than 0")
        }
        if (goal.currentAmount < 0.0) {
            return@withContext AppResult.ValidationError("currentAmount", "Current amount cannot be negative")
        }

        try {
            goalDao.insertGoal(goal)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error("Failed to save goal: ${e.message}", e)
        }
    }

    override suspend fun deleteGoal(id: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            goalDao.deleteById(id)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error("Failed to delete goal: ${e.message}", e)
        }
    }

    override suspend fun saveBudget(budget: BudgetEntity): AppResult<Unit> = withContext(Dispatchers.IO) {
        if (budget.monthKey.isBlank() || !budget.monthKey.matches(Regex("^\\d{4}-\\d{2}$"))) {
            return@withContext AppResult.ValidationError("monthKey", "Month key must be formatted as YYYY-MM")
        }
        if (budget.income < 0.0) {
            return@withContext AppResult.ValidationError("income", "Income cannot be negative")
        }

        try {
            budgetDao.insertBudget(budget)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error("Failed to save budget: ${e.message}", e)
        }
    }

    override suspend fun updateSettings(settings: UserSettingsEntity): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            userSettingsDao.insertSettings(settings)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error("Failed to update settings: ${e.message}", e)
        }
    }
}
