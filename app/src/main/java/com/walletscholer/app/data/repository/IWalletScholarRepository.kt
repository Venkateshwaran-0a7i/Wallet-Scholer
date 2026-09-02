package com.walletscholer.app.data.repository

import com.walletscholer.app.data.model.BudgetEntity
import com.walletscholer.app.data.model.GoalEntity
import com.walletscholer.app.data.model.TransactionEntity
import com.walletscholer.app.data.model.UserSettingsEntity
import com.walletscholer.app.domain.result.AppResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface mediating between Room database and ViewModels,
 * enforcing validation rules and returning typed AppResult outcomes.
 */
interface IWalletScholarRepository {
    val allTransactions: Flow<List<TransactionEntity>>
    val allGoals: Flow<List<GoalEntity>>
    val userSettings: Flow<UserSettingsEntity?>

    fun getBudgetForMonth(monthKey: String): Flow<BudgetEntity?>
    suspend fun initializeSeedDataIfNeeded(): AppResult<Unit>
    suspend fun resetDataWithPreset(preset: String, currentMonthKey: String): AppResult<Unit>

    suspend fun saveTransaction(transaction: TransactionEntity): AppResult<Unit>
    suspend fun deleteTransaction(transaction: TransactionEntity): AppResult<Unit>
    suspend fun updateTransaction(transaction: TransactionEntity): AppResult<Unit>

    suspend fun saveGoal(goal: GoalEntity): AppResult<Unit>
    suspend fun deleteGoal(id: String): AppResult<Unit>

    suspend fun saveBudget(budget: BudgetEntity): AppResult<Unit>
    suspend fun updateSettings(settings: UserSettingsEntity): AppResult<Unit>
    suspend fun getSettingsDirect(): UserSettingsEntity?
    suspend fun getBudgetForMonthDirect(monthKey: String): BudgetEntity?
}
