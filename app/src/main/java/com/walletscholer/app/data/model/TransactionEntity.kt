package com.walletscholer.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val type: String, // "INCOME" or "EXPENSE"
    val categoryId: String,
    val amount: Double,
    val date: String, // "YYYY-MM-DD"
    val time: String = "12:00", // "HH:mm"
    val description: String = "",
    val status: String = "ACTIVE", // "ACTIVE" or "VOIDED"
    val createdAt: Long = System.currentTimeMillis()
)
