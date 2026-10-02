package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_limits")
data class BudgetLimitEntity(
    @PrimaryKey
    val category: String,
    val monthlyLimit: Double,
    val warningThresholdPercent: Int = 80,
    val colorHex: String = "#10B981",
    val iconKey: String = "category",
    val isEnabled: Boolean = true
)
