package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "mpesa_transactions",
    indices = [
        Index(value = ["code"], unique = true),
        Index(value = ["timestamp"]),
        Index(value = ["category"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val code: String,
    val type: String,
    val recipientOrSender: String,
    val accountOrDetails: String = "",
    val amount: Double,
    val cost: Double = 0.0,
    val balance: Double? = null,
    val timestamp: Long,
    val category: String,
    val rawSms: String = "",
    val isExpense: Boolean = true,
    val notes: String = ""
)
