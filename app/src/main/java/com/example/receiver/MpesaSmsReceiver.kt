package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.data.local.AppDatabase
import com.example.data.parser.MpesaSmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class MpesaSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) return

            val pendingResult = goAsync()
            val database = AppDatabase.getInstance(context)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    for (sms in messages) {
                        val sender = sms.displayOriginatingAddress ?: ""
                        val body = sms.displayMessageBody ?: ""
                        val timestamp = sms.timestampMillis

                        // Process if sender is MPESA or text contains Confirmed. Ksh
                        if (sender.contains("MPESA", ignoreCase = true) || (body.contains("Confirmed", ignoreCase = true) && body.contains("Ksh", ignoreCase = true))) {
                            val parsed = MpesaSmsParser.parse(body, timestamp)
                            if (parsed != null) {
                                val rowId = database.transactionDao().insertTransaction(parsed)
                                if (rowId > 0 && parsed.isExpense) {
                                    // Check threshold alert for this category
                                    checkAndNotifyCategoryThreshold(context, database, parsed.category)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private suspend fun checkAndNotifyCategoryThreshold(
        context: Context,
        database: AppDatabase,
        category: String
    ) {
        val budget = database.budgetLimitDao().getLimitForCategory(category) ?: return
        if (!budget.isEnabled || budget.monthlyLimit <= 0) return

        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val startTime = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val endTime = cal.timeInMillis

        val monthTransactions = database.transactionDao().getTransactionsBetweenSync(startTime, endTime)
        val categorySpent = monthTransactions.filter { it.isExpense && it.category == category }.sumOf { it.amount }

        val percent = (categorySpent / budget.monthlyLimit) * 100.0
        val notifId = category.hashCode()

        if (categorySpent >= budget.monthlyLimit) {
            val over = categorySpent - budget.monthlyLimit
            NotificationHelper.showThresholdNotification(
                context = context,
                notificationId = notifId,
                title = "🚨 Budget Exceeded: $category",
                message = "You have exceeded your $category limit by Ksh %,.2f (Total: Ksh %,.2f / %,.2f)".format(over, categorySpent, budget.monthlyLimit)
            )
        } else if (percent >= budget.warningThresholdPercent) {
            val remaining = budget.monthlyLimit - categorySpent
            NotificationHelper.showThresholdNotification(
                context = context,
                notificationId = notifId,
                title = "⚠️ Spending Alert: $category",
                message = "$category has reached %.0f%% of your budget limit. Only Ksh %,.2f remaining this month.".format(percent, remaining)
            )
        }
    }
}
