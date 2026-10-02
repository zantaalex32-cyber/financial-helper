package com.example.data.parser

import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

object MpesaSmsParser {

    private val CODE_REGEX = Pattern.compile("^[A-Z0-9]{10}")
    private val AMOUNT_REGEX = Pattern.compile("Ksh\\s*([0-9,]+(?:\\.[0-9]{2})?)", Pattern.CASE_INSENSITIVE)
    private val BALANCE_REGEX = Pattern.compile("(?:New M-PESA balance is|balance is)\\s*Ksh\\s*([0-9,]+(?:\\.[0-9]{2})?)", Pattern.CASE_INSENSITIVE)
    private val COST_REGEX = Pattern.compile("Transaction cost,?\\s*Ksh\\s*([0-9,]+(?:\\.[0-9]{2})?)", Pattern.CASE_INSENSITIVE)
    private val DATE_TIME_REGEX = Pattern.compile("on\\s+(\\d{1,2}/\\d{1,2}/\\d{2,4})\\s+at\\s+(\\d{1,2}:\\d{2}\\s*(?:AM|PM|am|pm)?)", Pattern.CASE_INSENSITIVE)

    fun parse(smsBody: String, defaultTimestamp: Long = System.currentTimeMillis()): TransactionEntity? {
        val trimmed = smsBody.trim()
        if (trimmed.isEmpty()) return null

        // 1. Transaction Code (Usually 10 chars e.g. QA12BC34DE at start or within first words)
        var code = ""
        val firstWords = trimmed.split(" ")
        if (firstWords.isNotEmpty() && firstWords[0].length == 10 && firstWords[0].all { it.isLetterOrDigit() && (it.isUpperCase() || it.isDigit()) }) {
            code = firstWords[0]
        } else {
            val codeMatcher = Pattern.compile("\\b([A-Z0-9]{10})\\b").matcher(trimmed)
            if (codeMatcher.find()) {
                code = codeMatcher.group(1) ?: ""
            }
        }

        if (code.isEmpty()) {
            // Not a standard M-Pesa format
            return null
        }

        // 2. Amount
        var amount = 0.0
        val amountMatcher = AMOUNT_REGEX.matcher(trimmed)
        if (amountMatcher.find()) {
            amount = parseAmount(amountMatcher.group(1))
        }

        // 3. Balance
        var balance: Double? = null
        val balanceMatcher = BALANCE_REGEX.matcher(trimmed)
        if (balanceMatcher.find()) {
            balance = parseAmount(balanceMatcher.group(1))
        }

        // 4. Transaction Cost / Fee
        var cost = 0.0
        val costMatcher = COST_REGEX.matcher(trimmed)
        if (costMatcher.find()) {
            cost = parseAmount(costMatcher.group(1))
        }

        // 5. Date & Time
        var timestamp = defaultTimestamp
        val dateMatcher = DATE_TIME_REGEX.matcher(trimmed)
        if (dateMatcher.find()) {
            val dateStr = dateMatcher.group(1) ?: ""
            val timeStr = dateMatcher.group(2) ?: ""
            val parsedTime = parseMpesaDateTime(dateStr, timeStr)
            if (parsedTime != null) {
                timestamp = parsedTime
            }
        }

        // 6. Transaction Type & Recipient
        var type = TransactionType.OTHER
        var recipient = "M-Pesa Service"
        var accountOrDetails = ""
        var isExpense = true

        val lower = trimmed.lowercase(Locale.ROOT)

        when {
            // Received money
            lower.contains("you have received") || lower.contains("received ksh") -> {
                type = TransactionType.RECEIVE_MONEY
                isExpense = false
                val fromMatcher = Pattern.compile("from\\s+([^0-9]+?)(?:\\s+(\\d{10}))?\\s+on", Pattern.CASE_INSENSITIVE).matcher(trimmed)
                if (fromMatcher.find()) {
                    recipient = fromMatcher.group(1)?.trim() ?: "Sender"
                    accountOrDetails = fromMatcher.group(2)?.trim() ?: ""
                } else {
                    recipient = extractEntityAfterWord(trimmed, "from")
                }
            }
            // Buy goods / Till
            lower.contains("paid to") -> {
                type = TransactionType.BUY_GOODS
                val paidMatcher = Pattern.compile("paid to\\s+([^.]+?)\\.?\\s+on", Pattern.CASE_INSENSITIVE).matcher(trimmed)
                if (paidMatcher.find()) {
                    recipient = paidMatcher.group(1)?.trim() ?: "Merchant"
                } else {
                    recipient = extractEntityAfterWord(trimmed, "paid to")
                }
            }
            // Paybill
            lower.contains("for account") || (lower.contains("sent to") && lower.contains("account")) -> {
                type = TransactionType.PAYBILL
                val paybillMatcher = Pattern.compile("sent to\\s+(.+?)\\s+for account\\s+([^\\s]+)", Pattern.CASE_INSENSITIVE).matcher(trimmed)
                if (paybillMatcher.find()) {
                    recipient = paybillMatcher.group(1)?.trim() ?: "Biller"
                    accountOrDetails = "Acc: " + (paybillMatcher.group(2)?.trim() ?: "")
                } else {
                    recipient = extractEntityAfterWord(trimmed, "sent to")
                }
            }
            // Send Money (P2P)
            lower.contains("sent to") -> {
                type = TransactionType.SEND_MONEY
                val sentMatcher = Pattern.compile("sent to\\s+([^0-9]+?)(?:\\s+(\\d{10}))?\\s+on", Pattern.CASE_INSENSITIVE).matcher(trimmed)
                if (sentMatcher.find()) {
                    recipient = sentMatcher.group(1)?.trim() ?: "Recipient"
                    accountOrDetails = sentMatcher.group(2)?.trim() ?: ""
                } else {
                    recipient = extractEntityAfterWord(trimmed, "sent to")
                }
            }
            // Cash Withdrawal
            lower.contains("withdraw") || lower.contains("withdrawn") -> {
                type = TransactionType.WITHDRAW
                val agentMatcher = Pattern.compile("from\\s+(\\d+\\s*-\\s*[^.]+?)\\.?\\s+New", Pattern.CASE_INSENSITIVE).matcher(trimmed)
                if (agentMatcher.find()) {
                    recipient = agentMatcher.group(1)?.trim() ?: "M-Pesa Agent"
                } else {
                    recipient = "M-Pesa Agent"
                }
            }
            // Airtime
            lower.contains("bought") && lower.contains("airtime") -> {
                type = TransactionType.AIRTIME
                recipient = "Safaricom Airtime"
            }
            // Bank transfer
            lower.contains("transferred from") || lower.contains("bank") -> {
                type = TransactionType.BANK_TRANSFER
                isExpense = false
                recipient = extractEntityAfterWord(trimmed, "transferred from")
            }
        }

        // Clean recipient formatting
        recipient = cleanRecipient(recipient)

        // 7. Auto Category
        val category = determineCategory(type, recipient, isExpense)

        return TransactionEntity(
            code = code,
            type = type.name,
            recipientOrSender = recipient,
            accountOrDetails = accountOrDetails,
            amount = amount,
            cost = cost,
            balance = balance,
            timestamp = timestamp,
            category = category,
            rawSms = trimmed,
            isExpense = isExpense
        )
    }

    private fun parseAmount(str: String?): Double {
        if (str == null) return 0.0
        return try {
            str.replace(",", "").trim().toDouble()
        } catch (e: Exception) {
            0.0
        }
    }

    private fun cleanRecipient(name: String): String {
        var clean = name.replace(".", "").trim()
        clean = clean.replace(Regex("\\s+on$"), "")
        clean = clean.replace(Regex("\\s+at$"), "")
        return if (clean.isEmpty()) "M-Pesa Merchant" else clean
    }

    private fun extractEntityAfterWord(text: String, keyword: String): String {
        val idx = text.indexOf(keyword, ignoreCase = true)
        if (idx == -1) return "M-Pesa Merchant"
        val after = text.substring(idx + keyword.length).trim()
        val endIdx = after.indexOfAny(listOf(" on ", " for ", " at ", ".", ","))
        return if (endIdx != -1) after.substring(0, endIdx).trim() else after.take(30).trim()
    }

    private fun parseMpesaDateTime(dateStr: String, timeStr: String): Long? {
        val formats = listOf(
            "d/M/yy h:mm a",
            "d/M/yyyy h:mm a",
            "dd/MM/yy h:mm a",
            "dd/MM/yyyy h:mm a",
            "d/M/yy HH:mm",
            "d/M/yyyy HH:mm"
        )
        val combined = "$dateStr $timeStr".trim().uppercase(Locale.ROOT)
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.ENGLISH)
                sdf.isLenient = true
                val date = sdf.parse(combined)
                if (date != null) {
                    val cal = Calendar.getInstance()
                    cal.time = date
                    // Adjust 2-digit year if parsed as 19xx instead of 20xx
                    if (cal.get(Calendar.YEAR) < 2000) {
                        cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 100)
                    }
                    return cal.timeInMillis
                }
            } catch (_: Exception) {
            }
        }
        return null
    }

    fun determineCategory(type: TransactionType, recipient: String, isExpense: Boolean): String {
        if (!isExpense) {
            return TransactionCategory.INCOME
        }

        if (type == TransactionType.AIRTIME) {
            return TransactionCategory.AIRTIME
        }

        val name = recipient.uppercase(Locale.ROOT)

        return when {
            // Groceries & Supermarkets
            name.contains("NAIVAS") || name.contains("QUICKMART") || name.contains("CARREFOUR") ||
            name.contains("CHANDARANA") || name.contains("CLEANSHELF") || name.contains("SUPERMARKET") ||
            name.contains("GROCERY") || name.contains("FOODPLUS") || name.contains("TUSKYS") ->
                TransactionCategory.GROCERIES

            // Utilities & Power Bills
            name.contains("KPLC") || name.contains("KENYA POWER") || name.contains("WATER") ||
            name.contains("ZUKU") || name.contains("DSTV") || name.contains("GOTV") ||
            name.contains("STARTIMES") || name.contains("TOKEN") || name.contains("SAFARICOM HOME") ->
                TransactionCategory.UTILITIES

            // Food & Dining
            name.contains("JAVA") || name.contains("ARTCAFFE") || name.contains("KFC") ||
            name.contains("PIZZA") || name.contains("BURGER") || name.contains("RESTAURANT") ||
            name.contains("CAFE") || name.contains("BAKERY") || name.contains("GALITOS") ||
            name.contains("CHICKEN") || name.contains("COFFEE") || name.contains("BAR & GRILL") ->
                TransactionCategory.FOOD_DINING

            // Transport & Fuel
            name.contains("UBER") || name.contains("BOLT") || name.contains("LITTLE") ||
            name.contains("SHELL") || name.contains("TOTAL") || name.contains("RUBIS") ||
            name.contains("OLA") || name.contains("PETROL") || name.contains("FUEL") ||
            name.contains("MATATU") || name.contains("SACCO") || name.contains("2NK") ||
            name.contains("EASY COACH") || name.contains("GUARDIAN") ->
                TransactionCategory.TRANSPORT

            // Entertainment & Subscriptions
            name.contains("NETFLIX") || name.contains("SPOTIFY") || name.contains("SHOWMAX") ||
            name.contains("CINEMA") || name.contains("CLUB") || name.contains("LOUNGE") ||
            name.contains("BAR") || name.contains("BET") || name.contains("SPORTPESA") ->
                TransactionCategory.ENTERTAINMENT

            // Shopping & Retail
            name.contains("JUMIA") || name.contains("KILIMALL") || name.contains("ZARA") ||
            name.contains("WOOLWORTHS") || name.contains("MALL") || name.contains("STORE") ||
            name.contains("PHARMACY") || name.contains("CHEMIST") || name.contains("ELECTRONICS") ->
                TransactionCategory.SHOPPING

            // Airtime
            name.contains("AIRTIME") || name.contains("DATA") || name.contains("FAIBA") ||
            name.contains("AIRTEL") ->
                TransactionCategory.AIRTIME

            // P2P / Send Money default to Family & Friends
            type == TransactionType.SEND_MONEY ->
                TransactionCategory.FAMILY_P2P

            // Cash withdrawal
            type == TransactionType.WITHDRAW ->
                TransactionCategory.OTHER

            else -> TransactionCategory.OTHER
        }
    }
}
