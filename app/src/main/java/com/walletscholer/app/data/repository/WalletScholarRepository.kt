package com.walletscholer.app.data.repository

import com.walletscholer.app.data.local.AppDatabase
import com.walletscholer.app.data.model.BudgetEntity
import com.walletscholer.app.data.model.CategoryItem
import com.walletscholer.app.data.model.GoalEntity
import com.walletscholer.app.data.model.TransactionEntity
import com.walletscholer.app.data.model.UserSettingsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class WalletScholarRepository(private val database: AppDatabase) {

    private val transactionDao = database.transactionDao()
    private val budgetDao = database.budgetDao()
    private val goalDao = database.goalDao()
    private val userSettingsDao = database.userSettingsDao()

    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allGoals: Flow<List<GoalEntity>> = goalDao.getAllGoals()
    val userSettings: Flow<UserSettingsEntity?> = userSettingsDao.getSettings()

    fun getBudgetForMonth(monthKey: String): Flow<BudgetEntity?> {
        return budgetDao.getBudgetForMonth(monthKey)
    }

    /**
     * Ensures the app has the minimal rows it needs to function (a settings row and a
     * budget row for the current month) WITHOUT inserting any fake/demo transactions,
     * goals, or a fake "already logged in" identity. Real accounts only get populated
     * once the person actually signs in with Google, and real transactions only appear
     * once the person adds them.
     */
    suspend fun initializeSeedDataIfNeeded() = withContext(Dispatchers.IO) {
        if (userSettingsDao.getSettingsDirect() == null) {
            userSettingsDao.insertSettings(
                UserSettingsEntity(
                    googleSheetId = "1ITMN0Zz5vg0vTECz_Uty__-IKneTCtu1Fw-lYe2Ic_M",
                    googleSheetUrl = "https://docs.google.com/spreadsheets/d/1ITMN0Zz5vg0vTECz_Uty__-IKneTCtu1Fw-lYe2Ic_M/edit"
                )
            )
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
    }

    suspend fun resetDataWithPreset(preset: String, currentMonthKey: String) = withContext(Dispatchers.IO) {
        transactionDao.deleteAll()
        goalDao.deleteAll()

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        fun getDateOffset(daysAgo: Int): String {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -daysAgo)
            return sdf.format(c.time)
        }

        when (preset) {
            "CLEAN" -> {
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
            }
            "TECH" -> {
                val seedTxs = listOf(
                    TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        type = "INCOME",
                        categoryId = "salary",
                        amount = 85000.0,
                        date = getDateOffset(5),
                        time = "09:00",
                        description = "Senior Engineering Salary"
                    ),
                    TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        type = "EXPENSE",
                        categoryId = "rent",
                        amount = 22000.0,
                        date = getDateOffset(4),
                        time = "10:00",
                        description = "Apartment Rent"
                    ),
                    TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        type = "EXPENSE",
                        categoryId = "investment",
                        amount = 25000.0,
                        date = getDateOffset(3),
                        time = "11:30",
                        description = "Index Mutual Funds SIP"
                    ),
                    TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        type = "EXPENSE",
                        categoryId = "shopping",
                        amount = 4500.0,
                        date = getDateOffset(2),
                        time = "16:00",
                        description = "Mechanical Keyboard & Hub"
                    ),
                    TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        type = "EXPENSE",
                        categoryId = "food",
                        amount = 1200.0,
                        date = getDateOffset(1),
                        time = "20:00",
                        description = "Weekend Brunch"
                    )
                )
                transactionDao.insertAll(seedTxs)

                val seedGoals = listOf(
                    GoalEntity(
                        id = UUID.randomUUID().toString(),
                        name = "MacBook Pro M3 Max",
                        targetAmount = 250000.0,
                        currentAmount = 140000.0,
                        targetDate = "2026-11-30"
                    ),
                    GoalEntity(
                        id = UUID.randomUUID().toString(),
                        name = "Index Retirement Corpus",
                        targetAmount = 1000000.0,
                        currentAmount = 450000.0,
                        targetDate = "2028-12-31"
                    )
                )
                goalDao.insertAll(seedGoals)

                val alloc = JSONObject().apply {
                    put("rent", 22000.0)
                    put("investment", 25000.0)
                    put("savings", 15000.0)
                    put("food", 8000.0)
                    put("shopping", 6000.0)
                    put("transport", 3000.0)
                    put("entertainment", 3000.0)
                    put("emergency", 3000.0)
                }
                budgetDao.insertBudget(
                    BudgetEntity(
                        monthKey = currentMonthKey,
                        income = 85000.0,
                        allocationsJson = alloc.toString(),
                        customCategoriesJson = "[]"
                    )
                )
                val existingTech = userSettingsDao.getSettingsDirect() ?: UserSettingsEntity()
                userSettingsDao.insertSettings(existingTech.copy(salaryAmount = 85000.0))
            }
            "STUDENT" -> {
                val seedTxs = listOf(
                    TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        type = "INCOME",
                        categoryId = "freelance",
                        amount = 25000.0,
                        date = getDateOffset(4),
                        time = "10:00",
                        description = "Internship Stipend"
                    ),
                    TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        type = "EXPENSE",
                        categoryId = "rent",
                        amount = 8000.0,
                        date = getDateOffset(3),
                        time = "11:00",
                        description = "Hostel & Room Share"
                    ),
                    TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        type = "EXPENSE",
                        categoryId = "food",
                        amount = 3500.0,
                        date = getDateOffset(2),
                        time = "13:00",
                        description = "Mess & Canteen"
                    ),
                    TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        type = "EXPENSE",
                        categoryId = "transport",
                        amount = 1200.0,
                        date = getDateOffset(1),
                        time = "09:00",
                        description = "Metro Monthly Pass"
                    )
                )
                transactionDao.insertAll(seedTxs)

                val seedGoals = listOf(
                    GoalEntity(
                        id = UUID.randomUUID().toString(),
                        name = "Online Course Certifications",
                        targetAmount = 15000.0,
                        currentAmount = 8500.0,
                        targetDate = "2026-10-31"
                    )
                )
                goalDao.insertAll(seedGoals)

                val alloc = JSONObject().apply {
                    put("rent", 8000.0)
                    put("food", 4500.0)
                    put("transport", 1500.0)
                    put("savings", 5000.0)
                    put("shopping", 2000.0)
                    put("entertainment", 2000.0)
                    put("emergency", 2000.0)
                }
                budgetDao.insertBudget(
                    BudgetEntity(
                        monthKey = currentMonthKey,
                        income = 25000.0,
                        allocationsJson = alloc.toString(),
                        customCategoriesJson = "[]"
                    )
                )
                val existingStudent = userSettingsDao.getSettingsDirect() ?: UserSettingsEntity()
                userSettingsDao.insertSettings(existingStudent.copy(salaryAmount = 25000.0))
            }
            "FAMILY" -> {
                val seedTxs = listOf(
                    TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        type = "INCOME",
                        categoryId = "salary",
                        amount = 120000.0,
                        date = getDateOffset(6),
                        time = "09:00",
                        description = "Primary Monthly Earnings"
                    ),
                    TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        type = "EXPENSE",
                        categoryId = "emi",
                        amount = 32000.0,
                        date = getDateOffset(5),
                        time = "10:00",
                        description = "Home Loan EMI"
                    ),
                    TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        type = "EXPENSE",
                        categoryId = "food",
                        amount = 14000.0,
                        date = getDateOffset(4),
                        time = "14:00",
                        description = "Supermarket Groceries & Pantry"
                    ),
                    TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        type = "EXPENSE",
                        categoryId = "medicine",
                        amount = 3500.0,
                        date = getDateOffset(3),
                        time = "16:00",
                        description = "Family Health Insurance & Pharmacy"
                    )
                )
                transactionDao.insertAll(seedTxs)

                val seedGoals = listOf(
                    GoalEntity(
                        id = UUID.randomUUID().toString(),
                        name = "Higher Education Fund",
                        targetAmount = 500000.0,
                        currentAmount = 280000.0,
                        targetDate = "2027-06-30"
                    ),
                    GoalEntity(
                        id = UUID.randomUUID().toString(),
                        name = "Family Vacation",
                        targetAmount = 150000.0,
                        currentAmount = 75000.0,
                        targetDate = "2026-12-25"
                    )
                )
                goalDao.insertAll(seedGoals)

                val alloc = JSONObject().apply {
                    put("emi", 32000.0)
                    put("food", 16000.0)
                    put("savings", 25000.0)
                    put("investment", 18000.0)
                    put("medicine", 5000.0)
                    put("transport", 6000.0)
                    put("shopping", 8000.0)
                    put("emergency", 10000.0)
                }
                budgetDao.insertBudget(
                    BudgetEntity(
                        monthKey = currentMonthKey,
                        income = 120000.0,
                        allocationsJson = alloc.toString(),
                        customCategoriesJson = "[]"
                    )
                )
                val existingFamily = userSettingsDao.getSettingsDirect() ?: UserSettingsEntity()
                userSettingsDao.insertSettings(existingFamily.copy(salaryAmount = 120000.0))
            }
        }
    }

    suspend fun saveTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun saveGoal(goal: GoalEntity) = withContext(Dispatchers.IO) {
        goalDao.insertGoal(goal)
    }

    suspend fun deleteGoal(id: String) = withContext(Dispatchers.IO) {
        goalDao.deleteById(id)
    }

    suspend fun saveBudget(budget: BudgetEntity) = withContext(Dispatchers.IO) {
        budgetDao.insertBudget(budget)
    }

    suspend fun updateSettings(settings: UserSettingsEntity) = withContext(Dispatchers.IO) {
        userSettingsDao.insertSettings(settings)
    }
}
