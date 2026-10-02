package com.example.data.repository

import android.content.Context
import com.example.data.local.BudgetLimitDao
import com.example.data.local.TransactionDao
import com.example.data.model.AlertSeverity
import com.example.data.model.BudgetLimitEntity
import com.example.data.model.MonthlySummary
import com.example.data.model.ThresholdAlert
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionEntity
import com.example.data.sms.SampleMpesaData
import com.example.data.sms.SmsReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PesaPlanRepository(
    private val transactionDao: TransactionDao,
    private val budgetLimitDao: BudgetLimitDao
) {

    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allBudgetLimits: Flow<List<BudgetLimitEntity>> = budgetLimitDao.getAllLimits()

    suspend fun initializeDefaultsIfEmpty() = withContext(Dispatchers.IO) {
        val limitCount = budgetLimitDao.getCount()
        if (limitCount == 0) {
            val defaultLimits = TransactionCategory.DEFAULT_CATEGORIES.map { meta ->
                BudgetLimitEntity(
                    category = meta.id,
                    monthlyLimit = meta.defaultLimit,
                    warningThresholdPercent = 80,
                    colorHex = meta.colorHex,
                    iconKey = meta.iconKey,
                    isEnabled = true
                )
            }
            budgetLimitDao.insertLimits(defaultLimits)
        }

        // Check if transactions are empty; if so, populate sample M-Pesa data for initial experience
        val currentTx = transactionDao.getTransactionsBetweenSync(0, System.currentTimeMillis() + 86400000L)
        if (currentTx.isEmpty()) {
            val samples = SampleMpesaData.getSampleTransactions()
            transactionDao.insertTransactions(samples)
        }
    }

    suspend fun syncInboxSms(context: Context): Int = withContext(Dispatchers.IO) {
        val readMessages = SmsReader.readMpesaSms(context)
        var newCount = 0
        for (tx in readMessages) {
            val rowId = transactionDao.insertTransaction(tx)
            if (rowId > 0) {
                newCount++
            }
        }
        newCount
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long = withContext(Dispatchers.IO) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransactionCategory(id: Long, newCategory: String) = withContext(Dispatchers.IO) {
        transactionDao.updateCategory(id, newCategory)
    }

    suspend fun deleteTransaction(id: Long) = withContext(Dispatchers.IO) {
        transactionDao.deleteTransaction(id)
    }

    suspend fun saveBudgetLimit(limit: BudgetLimitEntity) = withContext(Dispatchers.IO) {
        budgetLimitDao.insertLimit(limit)
    }

    suspend fun deleteBudgetLimit(category: String) = withContext(Dispatchers.IO) {
        budgetLimitDao.deleteLimit(category)
    }

    suspend fun resetWithSampleData() = withContext(Dispatchers.IO) {
        transactionDao.deleteAllTransactions()
        transactionDao.insertTransactions(SampleMpesaData.getSampleTransactions())
    }

    fun getMonthlySummaryFlow(year: Int, month: Int): Flow<MonthlySummary> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startTime = cal.timeInMillis

        val totalDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, totalDaysInMonth)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endTime = cal.timeInMillis

        val now = Calendar.getInstance()
        val isCurrentMonth = (now.get(Calendar.YEAR) == year && now.get(Calendar.MONTH) == month)
        val currentDay = if (isCurrentMonth) now.get(Calendar.DAY_OF_MONTH) else totalDaysInMonth
        val daysRemaining = if (isCurrentMonth) (totalDaysInMonth - currentDay).coerceAtLeast(0) else 0

        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val yearMonthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val displayMonth = monthFormat.format(cal.time)
        val yearMonth = yearMonthFormat.format(cal.time)

        val txFlow = transactionDao.getTransactionsBetween(startTime, endTime)
        val limitsFlow = budgetLimitDao.getAllLimits()

        return combine(txFlow, limitsFlow) { txList, limits ->
            var income = 0.0
            var expenses = 0.0
            var fees = 0.0
            val spendMap = mutableMapOf<String, Double>()
            val merchantMap = mutableMapOf<String, Double>()
            var largestExpense: TransactionEntity? = null

            for (tx in txList) {
                fees += tx.cost
                if (tx.isExpense) {
                    expenses += tx.amount
                    spendMap[tx.category] = (spendMap[tx.category] ?: 0.0) + tx.amount
                    merchantMap[tx.recipientOrSender] = (merchantMap[tx.recipientOrSender] ?: 0.0) + tx.amount

                    if (largestExpense == null || tx.amount > largestExpense.amount) {
                        largestExpense = tx
                    }
                } else {
                    income += tx.amount
                }
            }

            // Also account for fees in spend map
            if (fees > 0) {
                spendMap[TransactionCategory.FEES] = (spendMap[TransactionCategory.FEES] ?: 0.0) + fees
            }

            val totalBudget = limits.filter { it.isEnabled }.sumOf { it.monthlyLimit }
            val totalSpentWithFees = expenses + fees
            val budgetRemaining = (totalBudget - totalSpentWithFees).coerceAtLeast(0.0)
            val budgetPercent = if (totalBudget > 0) (totalSpentWithFees / totalBudget) * 100.0 else 0.0

            val daysElapsed = currentDay.coerceAtLeast(1)
            val dailyAvg = totalSpentWithFees / daysElapsed
            val projectedMonthEnd = dailyAvg * totalDaysInMonth
            val safeDailyRemaining = if (daysRemaining > 0) budgetRemaining / daysRemaining else 0.0

            val topCategory = spendMap.maxByOrNull { it.value }?.key ?: "None"
            val topMerchant = merchantMap.maxByOrNull { it.value }?.key ?: "None"

            // Compute alerts
            val alerts = mutableListOf<ThresholdAlert>()
            val limitsMap = limits.associateBy { it.category }

            for ((cat, spent) in spendMap) {
                val budget = limitsMap[cat]
                if (budget != null && budget.isEnabled && budget.monthlyLimit > 0) {
                    val percent = (spent / budget.monthlyLimit) * 100.0
                    val isExceeded = spent >= budget.monthlyLimit
                    val isApproaching = percent >= budget.warningThresholdPercent && !isExceeded

                    if (isExceeded) {
                        val overAmount = spent - budget.monthlyLimit
                        alerts.add(
                            ThresholdAlert(
                                category = cat,
                                spent = spent,
                                limit = budget.monthlyLimit,
                                thresholdPercent = budget.warningThresholdPercent,
                                currentPercent = percent,
                                isExceeded = true,
                                isApproaching = false,
                                remainingAmount = -overAmount,
                                severity = AlertSeverity.CRITICAL,
                                title = "Budget Exceeded: $cat",
                                message = "You have exceeded your $cat budget by Ksh %,.2f!".format(overAmount),
                                colorHex = budget.colorHex
                            )
                        )
                    } else if (isApproaching) {
                        val remaining = budget.monthlyLimit - spent
                        alerts.add(
                            ThresholdAlert(
                                category = cat,
                                spent = spent,
                                limit = budget.monthlyLimit,
                                thresholdPercent = budget.warningThresholdPercent,
                                currentPercent = percent,
                                isExceeded = false,
                                isApproaching = true,
                                remainingAmount = remaining,
                                severity = AlertSeverity.WARNING,
                                title = "Approaching Limit: $cat",
                                message = "$cat is at %.0f%% of limit. Only Ksh %,.2f remaining.".format(percent, remaining),
                                colorHex = budget.colorHex
                            )
                        )
                    }
                }
            }

            // Overall budget warning if applicable
            if (totalBudget > 0 && budgetPercent >= 85.0 && !alerts.any { it.category == "Overall" }) {
                alerts.add(
                    0,
                    ThresholdAlert(
                        category = "Overall Budget",
                        spent = totalSpentWithFees,
                        limit = totalBudget,
                        thresholdPercent = 85,
                        currentPercent = budgetPercent,
                        isExceeded = budgetPercent >= 100.0,
                        isApproaching = budgetPercent < 100.0,
                        remainingAmount = totalBudget - totalSpentWithFees,
                        severity = if (budgetPercent >= 100.0) AlertSeverity.CRITICAL else AlertSeverity.WARNING,
                        title = if (budgetPercent >= 100.0) "Total Monthly Budget Exceeded" else "Overall Budget at %.0f%%".format(budgetPercent),
                        message = if (budgetPercent >= 100.0)
                            "Total spending has exceeded your planned monthly limit by Ksh %,.2f".format(totalSpentWithFees - totalBudget)
                        else
                            "You have used %.0f%% of your total monthly budget with %d days remaining.".format(budgetPercent, daysRemaining),
                        colorHex = "#EF4444"
                    )
                )
            }

            MonthlySummary(
                yearMonth = yearMonth,
                displayMonth = displayMonth,
                totalIncome = income,
                totalExpenses = expenses,
                totalFees = fees,
                netCashflow = income - totalSpentWithFees,
                totalBudget = totalBudget,
                budgetRemaining = budgetRemaining,
                budgetUsedPercent = budgetPercent,
                dailyAverageSpent = dailyAvg,
                projectedMonthEndSpent = projectedMonthEnd,
                safeDailyRemainingBudget = safeDailyRemaining,
                daysRemainingInMonth = daysRemaining,
                daysElapsedInMonth = daysElapsed,
                totalDaysInMonth = totalDaysInMonth,
                categorySpendMap = spendMap,
                topCategory = topCategory,
                topMerchant = topMerchant,
                largestExpense = largestExpense,
                transactionCount = txList.size,
                alerts = alerts
            )
        }
    }
}
