package com.walletscholer.app.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.walletscholer.app.data.local.AppDatabase
import com.walletscholer.app.data.model.BudgetEntity
import com.walletscholer.app.data.model.CategoryItem
import com.walletscholer.app.data.model.DefaultCategories
import com.walletscholer.app.data.model.GoalEntity
import com.walletscholer.app.data.model.TransactionEntity
import com.walletscholer.app.data.model.UserSettingsEntity
import com.walletscholer.app.data.remote.GoogleAuthManager
import com.walletscholer.app.data.remote.GoogleSheetsSyncEngine
import com.walletscholer.app.data.repository.WalletScholarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class WalletViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WalletScholarRepository

    val currentMonthKey: String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

    val transactions: StateFlow<List<TransactionEntity>>
    val goals: StateFlow<List<GoalEntity>>
    val settings: StateFlow<UserSettingsEntity?>
    val budget: StateFlow<BudgetEntity?>

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

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
            initialValue = UserSettingsEntity()
        )

        budget = repository.getBudgetForMonth(currentMonthKey).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        viewModelScope.launch {
            repository.initializeSeedDataIfNeeded()
        }
    }

    // ─── Auto-sync helper ────────────────────────────────────────────────────────
    /**
     * Triggers a background Google Sheets sync after any data mutation, but ONLY
     * when the user has sync enabled and is signed in to Google.  Failures are
     * logged silently — we never let sync errors surface to the user as crashes.
     */
    private fun triggerAutoSync() {
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            try {
                val s = settings.value ?: return@launch
                if (!s.syncEnabled) return@launch
                val sheetId = s.googleSheetId.takeIf { it.isNotBlank() } ?: return@launch
                val account = GoogleAuthManager.getLastSignedInAccount(ctx) ?: return@launch

                // Collect the latest data snapshots for sync
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

                // Persist sync result without surfacing to UI (silent background sync)
                val cur = settings.value ?: UserSettingsEntity()
                val time = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                repository.updateSettings(
                    cur.copy(
                        lastSyncTime = if (result.success) time else cur.lastSyncTime,
                        syncStatus = if (result.success) "SYNCED" else "FAILED",
                        syncEnabled = result.success || cur.syncEnabled
                    )
                )
                Log.d("AutoSync", if (result.success) "✅ Auto-synced ${result.exportedRows} rows" else "❌ Auto-sync failed: ${result.message}")
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
        // ensure default categories are present
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

    // ─── Mutations (each calls triggerAutoSync) ───────────────────────────────────

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
            repository.saveTransaction(tx)
            triggerAutoSync()
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
            val cur = budget.value ?: BudgetEntity(monthKey = currentMonthKey)
            val json = JSONObject()
            allocations.forEach { (k, v) -> json.put(k, v) }
            repository.saveBudget(cur.copy(allocationsJson = json.toString()))
            triggerAutoSync()
        }
    }

    fun updateBudgetIncome(income: Double) {
        viewModelScope.launch {
            val cur = budget.value ?: BudgetEntity(monthKey = currentMonthKey)
            repository.saveBudget(cur.copy(income = income))
            triggerAutoSync()
        }
    }

    fun addCustomCategory(name: String, initialAmount: Double, iconKey: String = "target") {
        viewModelScope.launch {
            val cur = budget.value ?: BudgetEntity(monthKey = currentMonthKey)
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
            val cur = settings.value ?: UserSettingsEntity()
            repository.updateSettings(cur.copy(salaryAmount = salaryAmount, salaryDate = salaryDate))
        }
    }

    fun toggleNotifMaster(enabled: Boolean) {
        viewModelScope.launch {
            val cur = settings.value ?: UserSettingsEntity()
            repository.updateSettings(cur.copy(notifMaster = enabled))
        }
    }

    fun toggleNotifThreshold(thresholdKey: String, enabled: Boolean) {
        viewModelScope.launch {
            val cur = settings.value ?: UserSettingsEntity()
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
            val cur = settings.value ?: UserSettingsEntity()
            repository.updateSettings(cur.copy(isDarkTheme = isDark))
        }
    }

    fun toggleBiometricLock(enable: Boolean) {
        viewModelScope.launch {
            val cur = settings.value ?: UserSettingsEntity()
            repository.updateSettings(cur.copy(biometricLockEnabled = enable))
        }
    }

    fun toggleSync(enable: Boolean) {
        viewModelScope.launch {
            val cur = settings.value ?: UserSettingsEntity()
            val time = if (enable) SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()) else ""
            repository.updateSettings(cur.copy(syncEnabled = enable, lastSyncTime = time, syncStatus = "SYNCED"))
            // Immediately sync when user enables it
            if (enable) triggerAutoSync()
        }
    }

    /** Called after a REAL Google Sign-In result (see MainActivity) with the account's actual name/email. */
    fun loginUser(name: String, email: String) {
        viewModelScope.launch {
            val cur = settings.value ?: UserSettingsEntity()
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
            val cur = settings.value ?: UserSettingsEntity()
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

    /** Saves the ID of the user's own Google Sheet (must exist and be editable by their account). */
    fun updateSheetId(sheetId: String) {
        viewModelScope.launch {
            val cur = settings.value ?: UserSettingsEntity()
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

    /** Called after a REAL sync (success or failure) so the stored status reflects reality. */
    fun setSyncResult(success: Boolean) {
        viewModelScope.launch {
            val cur = settings.value ?: UserSettingsEntity()
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
