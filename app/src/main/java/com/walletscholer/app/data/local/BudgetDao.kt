package com.walletscholer.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.walletscholer.app.data.model.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE monthKey = :monthKey")
    fun getBudgetForMonth(monthKey: String): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE monthKey = :monthKey")
    suspend fun getBudgetForMonthDirect(monthKey: String): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    @Update
    suspend fun updateBudget(budget: BudgetEntity)
}
