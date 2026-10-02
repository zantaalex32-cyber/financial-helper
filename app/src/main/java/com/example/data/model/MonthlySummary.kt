package com.example.data.model

data class MonthlySummary(
    val yearMonth: String, // e.g. "2026-10"
    val displayMonth: String, // e.g. "October 2026"
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val totalFees: Double = 0.0,
    val netCashflow: Double = 0.0, // income - expenses - fees
    val totalBudget: Double = 0.0,
    val budgetRemaining: Double = 0.0,
    val budgetUsedPercent: Double = 0.0,
    val dailyAverageSpent: Double = 0.0,
    val projectedMonthEndSpent: Double = 0.0,
    val safeDailyRemainingBudget: Double = 0.0,
    val daysRemainingInMonth: Int = 0,
    val daysElapsedInMonth: Int = 1,
    val totalDaysInMonth: Int = 30,
    val categorySpendMap: Map<String, Double> = emptyMap(),
    val topCategory: String = "",
    val topMerchant: String = "",
    val largestExpense: TransactionEntity? = null,
    val transactionCount: Int = 0,
    val alerts: List<ThresholdAlert> = emptyList()
)
