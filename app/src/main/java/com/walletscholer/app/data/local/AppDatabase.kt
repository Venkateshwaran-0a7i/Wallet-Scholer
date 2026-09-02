package com.walletscholer.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.walletscholer.app.data.model.BudgetEntity
import com.walletscholer.app.data.model.GoalEntity
import com.walletscholer.app.data.model.TransactionEntity
import com.walletscholer.app.data.model.UserSettingsEntity

@Database(
    entities = [
        TransactionEntity::class,
        BudgetEntity::class,
        GoalEntity::class,
        UserSettingsEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wallet_scholar.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
