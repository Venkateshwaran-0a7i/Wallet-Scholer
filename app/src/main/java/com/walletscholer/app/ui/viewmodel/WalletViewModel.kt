package com.walletscholer.app.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.walletscholer.app.data.banking.BankingAppHelper
import com.walletscholer.app.data.banking.BankingAppInfo
import com.walletscholer.app.data.local.AppDatabase
import com.walletscholer.app.data.model.BudgetEntity
import com.walletscholer.app.data.model.CategoryItem
import com.walletscholer.app.data.model.DefaultCategories
import com.walletscholer.app.data.model.GoalEntity
import com.walletscholer.app.data.model.TransactionEntity
import com.walletscholer.app.data.model.UserSettingsEntity
import com.walletscholer.app.data.remote.GoogleAuthManager
import com.walletscholer.app.data.remote.GoogleSheetsSyncEngine
import com.walletscholer.app.data.repository.IWalletScholarRepository
import com.walletscholer.app.data.repository.WalletScholarRepository
import com.walletscholer.app.domain.result.AppResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class WalletViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: IWalletScholarRepository

    val currentMonthKey: String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

    // Calendar navigation state for Dashboard
    private val _selectedMonthKey = MutableStateFlow(currentMonthKey)
    val selectedMonthKey: StateFlow<String> = _selectedMonthKey.asStateFlow()

    // Session App Lock State
    private val _isAppUnlocked = MutableStateFlow(false)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    val transactions: StateFlow<List<TransactionEntity>>
    val goals: StateFlow<List<GoalEntity>>
    val settings: StateFlow<UserSettingsEntity?>
    val budget: StateFlow<BudgetEntity?>

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _bankingApps = MutableStateFlow<List<BankingAppInfo>>(emptyList())
    val bankingApps: StateFlow<List<BankingAppInfo>> = _bankingApps.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = WalletScholarRepository(db)

        transactions = repository.allTransactions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        goals = repository.allGoals.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        settings = repository.userSettings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
        budget = _selectedMonthKey.flatMapLatest { monthKey ->
            repository.getBudgetForMonth(monthKey)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        viewModelScope.launch {
            repository.initializeSeedDataIfNeeded()
            refreshBankingApps()
            checkAndCreditMonthlySalary()
        }
    }

    fun setAppUnlocked(unlocked: Boolean) {
        _isAppUnlocked.value = unlocked
    }

    fun selectMonth(monthKey: String) {
        _selectedMonthKey.value = monthKey
    }

    fun nextMonth() {
        try {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val cal = Calendar.getInstance()
            cal.time = sdf.parse(_selectedMonthKey.value) ?: Date()
            cal.add(Calendar.MONTH, 1)
            _selectedMonthKey.value = sdf.format(cal.time)
        } catch (_: Exception) {}
    }

    fun previousMonth() {
        try {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val cal = Calendar.getInstance()
            cal.time = sdf.parse(_selectedMonthKey.value) ?: Date()
            cal.add(Calendar.MONTH, -1)
            _selectedMonthKey.value = sdf.format(cal.time)
        } catch (_: Exception) {}
    }

    fun resetToCurrentMonth() {
        _selectedMonthKey.value = currentMonthKey
    }

    fun refreshBankingApps() {
        val ctx = getApplication<Application>()
        val installed = BankingAppHelper.getInstalledBankingApps(ctx)
        if (installed.isNotEmpty()) {
            _bankingApps.value = installed
        } else {
            // Provide all supported banking apps with status so user can see & open/install them
            _bankingApps.value = BankingAppHelper.getAllSupportedWithStatus(ctx)
        }
    }

    /**
     * Checks if auto-credit salary is enabled for the current month and adds the salary transaction if not already credited.
     */
    fun checkAndCreditMonthlySalary() {
        viewModelScope.launch {
            val s = repository.getSettingsDirect() ?: return@launch
            if (!s.autoCreditSalary || s.salaryAmount <= 0.0) return@launch
            if (s.lastSalaryCreditedMonth == currentMonthKey) return@launch

            // Check if salary for this month already exists in transactions
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val tx = TransactionEntity(
                id = UUID.randomUUID().toString(),
                type = "INCOME",
                categoryId = "salary",
                amount = s.salaryAmount,
                date = todayStr,
                time = "09:00",
                description = "Monthly Salary (Auto-credited)",
                status = "ACTIVE"
            )
            repository.saveTransaction(tx)
            repository.updateSettings(s.copy(lastSalaryCreditedMonth = currentMonthKey))
            triggerAutoSync()
        }
    }

    fun toggleAutoCreditSalary(enabled: Boolean) {
        viewModelScope.launch {
            val cur = settings.value ?: repository.getSettingsDirect() ?: UserSettingsEntity()
            repository.updateSettings(cur.copy(autoCreditSalary = enabled))
            if (enabled) {
                checkAndCreditMonthlySalary()
            }
        }
    }

    fun creditSalaryNow() {
        viewModelScope.launch {
            val s = settings.value ?: repository.getSettingsDirect() ?: UserSettingsEntity()
            val salaryAmt = if (s.salaryAmount > 0.0) s.salaryAmount else 50000.0
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val tx = TransactionEntity(
                id = UUID.randomUUID().toString(),
                type = "INCOME",
                categoryId = "salary",
                amount = salaryAmt,
                date = todayStr,
                time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                description = "Monthly Salary",
                status = "ACTIVE"
            )
            repository.saveTransaction(tx)
            repository.updateSettings(s.copy(salaryAmount = salaryAmt, lastSalaryCreditedMonth = currentMonthKey))
            triggerAutoSync()
        }
    }

    // ─── Auto-sync helper ────────────────────────────────────────────────────────
    private fun triggerAutoSync() {
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            try {
                val s = settings.value ?: return@launch
                if (!s.syncEnabled) return@launch
                val sheetId = s.googleSheetId.takeIf { it.isNotBlank() } ?: return@launch
                val account = GoogleAuthManager.getLastSignedInAccount(ctx) ?: return@launch

                val txList = transactions.value
                val goalList = goals.value
                val allocMap = parseAllocations(budget.value)

                val result = GoogleSheetsSyncEngine.performGoogleSheetsSync(
                    context = ctx,
                    account = account,
                    sheetId = sheetId,
                    transactions = txList,
                    goals = goalList,
                    allocations = allocMap
                )

                val cur = settings.value ?: UserSettingsEntity()
                val time = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                repository.updateSettings(
                    cur.copy(
                        lastSyncTime = if (result.success) time else cur.lastSyncTime,
                        syncStatus = if (result.success) "SYNCED" else "FAILED",
                        syncEnabled = result.success || cur.syncEnabled
                    )
                )
            } catch (e: Exception) {
                Log.w("AutoSync", "Auto-sync exception: ${e.message}")
            }
        }
    }

    // ─── Parsing helpers ─────────────────────────────────────────────────────────

    fun parseAllocations(budgetEntity: BudgetEntity?): Map<String, Double> {
        val map = mutableMapOf<String, Double>()
        if (budgetEntity == null) {
            DefaultCategories.EXPENSE_CATEGORIES.forEach {
                map[it.id] = when (it.id) {
                    "rent" -> 12000.0
                    "food" -> 6000.0
                    "transport" -> 2500.0
                    "shopping" -> 3000.0
                    "entertainment" -> 1500.0
                    "savings" -> 8000.0
                    "emergency" -> 2000.0
                    "medicine" -> 1000.0
                    "investment" -> 5000.0
                    "other" -> 1000.0
                    else -> 0.0
                }
            }
            return map
        }
        try {
            val json = JSONObject(budgetEntity.allocationsJson)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = json.optDouble(key, 0.0)
            }
        } catch (_: Exception) {}
        DefaultCategories.EXPENSE_CATEGORIES.forEach {
            if (!map.containsKey(it.id)) map[it.id] = 0.0
        }
        return map
    }

    fun parseCustomCategories(budgetEntity: BudgetEntity?): List<CategoryItem> {
        val list = mutableListOf<CategoryItem>()
        if (budgetEntity == null) return list
        try {
            val arr = JSONArray(budgetEntity.customCategoriesJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    CategoryItem(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        group = obj.optString("group", "OTHER"),
                        iconKey = obj.optString("iconKey", "more_horizontal"),
                        isExpense = true
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    // ─── Mutations ───────────────────────────────────────────────────────────────

    fun saveTransaction(
        id: String? = null,
        type: String,
        categoryId: String,
        amount: Double,
        date: String,
        time: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
        description: String
    ) {
        viewModelScope.launch {
            val tx = TransactionEntity(
                id = id ?: UUID.randomUUID().toString(),
                type = type,
                categoryId = categoryId,
                amount = amount,
                date = date,
                time = time,
                description = description.trim(),
                status = "ACTIVE"
            )
            val result = repository.saveTransaction(tx)
            if (result is AppResult.Success) {
                triggerAutoSync()
            }
        }
    }

    fun voidTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(tx.copy(status = "VOIDED"))
            triggerAutoSync()
        }
    }

    fun deleteTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
            triggerAutoSync()
        }
    }

    fun saveGoal(
        id: String? = null,
        name: String,
        targetAmount: Double,
        currentAmount: Double,
        targetDate: String
    ) {
        viewModelScope.launch {
            val goal = GoalEntity(
                id = id ?: UUID.randomUUID().toString(),
                name = name.trim(),
                targetAmount = targetAmount,
                currentAmount = currentAmount,
                targetDate = targetDate
            )
            repository.saveGoal(goal)
            triggerAutoSync()
        }
    }

    fun deleteGoal(id: String) {
        viewModelScope.launch {
            repository.deleteGoal(id)
            triggerAutoSync()
        }
    }

    fun updateBudgetAllocations(allocations: Map<String, Double>) {
        viewModelScope.launch {
            val cur = budget.value ?: BudgetEntity(monthKey = _selectedMonthKey.value)
            val json = JSONObject()
            allocations.forEach { (k, v) -> json.put(k, v) }
            repository.saveBudget(cur.copy(allocationsJson = json.toString()))
            triggerAutoSync()
        }
    }

    fun updateBudgetIncome(income: Double) {
        viewModelScope.launch {
            val cur = budget.value ?: BudgetEntity(monthKey = _selectedMonthKey.value)
            repository.saveBudget(cur.copy(income = income))
            triggerAutoSync()
        }
    }

    fun addCustomCategory(name: String, initialAmount: Double, iconKey: String = "target") {
        viewModelScope.launch {
            val cur = budget.value ?: BudgetEntity(monthKey = _selectedMonthKey.value)
            val customList = parseCustomCategories(cur).toMutableList()
            val newId = "custom_${UUID.randomUUID().toString().take(8)}"
            customList.add(CategoryItem(id = newId, name = name, group = "OTHER", iconKey = iconKey))

            val arr = JSONArray()
            customList.forEach {
                val obj = JSONObject()
                obj.put("id", it.id)
                obj.put("name", it.name)
                obj.put("group", it.group)
                obj.put("iconKey", it.iconKey)
                arr.put(obj)
            }

            val allocations = parseAllocations(cur).toMutableMap()
            allocations[newId] = initialAmount
            val allocJson = JSONObject()
            allocations.forEach { (k, v) -> allocJson.put(k, v) }

            repository.saveBudget(
                cur.copy(
                    customCategoriesJson = arr.toString(),
                    allocationsJson = allocJson.toString()
                )
            )
            triggerAutoSync()
        }
    }

    fun updateSalaryCycle(salaryAmount: Double, salaryDate: Int) {
        viewModelScope.launch {
            val cur = settings.value ?: repository.getSettingsDirect() ?: UserSettingsEntity()
            repository.updateSettings(cur.copy(salaryAmount = salaryAmount, salaryDate = salaryDate))
        }
    }

    fun toggleNotifMaster(enabled: Boolean) {
        viewModelScope.launch {
            val cur = settings.value ?: repository.getSettingsDirect() ?: UserSettingsEntity()
            repository.updateSettings(cur.copy(notifMaster = enabled))
        }
    }

    fun toggleNotifThreshold(thresholdKey: String, enabled: Boolean) {
        viewModelScope.launch {
            val cur = settings.value ?: repository.getSettingsDirect() ?: UserSettingsEntity()
            try {
                val json = JSONObject(cur.notifThresholdsJson)
                json.put(thresholdKey, enabled)
                repository.updateSettings(cur.copy(notifThresholdsJson = json.toString()))
            } catch (_: Exception) {}
        }
    }

    fun toggleDarkTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
        viewModelScope.launch {
            val cur = settings.value ?: repository.getSettingsDirect() ?: UserSettingsEntity()
            repository.updateSettings(cur.copy(isDarkTheme = isDark))
        }
    }

    fun toggleBiometricLock(enable: Boolean) {
        viewModelScope.launch {
            val cur = settings.value ?: repository.getSettingsDirect() ?: UserSettingsEntity()
            repository.updateSettings(cur.copy(biometricLockEnabled = enable))
            if (!enable) {
                _isAppUnlocked.value = true
            }
        }
    }

    fun toggleSync(enable: Boolean) {
        viewModelScope.launch {
            val cur = settings.value ?: repository.getSettingsDirect() ?: UserSettingsEntity()
            val time = if (enable) SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()) else ""
            repository.updateSettings(cur.copy(syncEnabled = enable, lastSyncTime = time, syncStatus = "SYNCED"))
            if (enable) triggerAutoSync()
        }
    }

    fun loginUser(name: String, email: String) {
        viewModelScope.launch {
            val cur = settings.value ?: repository.getSettingsDirect() ?: UserSettingsEntity()
            repository.updateSettings(
                cur.copy(
                    userName = name,
                    userEmail = email,
                    isLoggedIn = true,
                    loginProvider = "GOOGLE"
                )
            )
        }
    }

    fun logoutUser() {
        viewModelScope.launch {
            val cur = settings.value ?: repository.getSettingsDirect() ?: UserSettingsEntity()
            repository.updateSettings(
                cur.copy(
                    userName = "",
                    userEmail = "",
                    isLoggedIn = false,
                    loginProvider = "GUEST",
                    syncEnabled = false
                )
            )
        }
    }

    fun updateSheetId(sheetId: String) {
        viewModelScope.launch {
            val cur = settings.value ?: repository.getSettingsDirect() ?: UserSettingsEntity()
            repository.updateSettings(
                cur.copy(
                    googleSheetId = sheetId.trim(),
                    googleSheetUrl = if (sheetId.isNotBlank())
                        "https://docs.google.com/spreadsheets/d/${sheetId.trim()}/edit" else ""
                )
            )
        }
    }

    fun freshStart(preset: String) {
        viewModelScope.launch {
            repository.resetDataWithPreset(preset, currentMonthKey)
        }
    }

    fun setSyncResult(success: Boolean) {
        viewModelScope.launch {
            val cur = settings.value ?: repository.getSettingsDirect() ?: UserSettingsEntity()
            val time = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            repository.updateSettings(
                cur.copy(
                    lastSyncTime = if (success) time else cur.lastSyncTime,
                    syncStatus = if (success) "SYNCED" else "FAILED",
                    syncEnabled = success || cur.syncEnabled
                )
            )
        }
    }
}
