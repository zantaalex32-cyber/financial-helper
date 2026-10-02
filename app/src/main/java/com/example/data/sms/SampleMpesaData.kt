package com.example.data.sms

import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import java.util.Calendar

object SampleMpesaData {

    fun getSampleTransactions(): List<TransactionEntity> {
        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        val currentMonth = now.get(Calendar.MONTH) // 0-indexed (e.g. 9 for October)

        fun createDate(day: Int, hour: Int, minute: Int, monthOffset: Int = 0): Long {
            val cal = Calendar.getInstance()
            cal.set(Calendar.YEAR, currentYear)
            cal.set(Calendar.MONTH, currentMonth + monthOffset)
            cal.set(Calendar.DAY_OF_MONTH, day)
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        return listOf(
            // --- October 2026 Transactions ---
            TransactionEntity(
                code = "QA92KF019X",
                type = TransactionType.BUY_GOODS.name,
                recipientOrSender = "NAIVAS SUPERMARKET WESTLANDS",
                accountOrDetails = "Till 542109",
                amount = 4850.00,
                cost = 0.0,
                balance = 28450.00,
                timestamp = createDate(2, 14, 25),
                category = TransactionCategory.GROCERIES,
                rawSms = "QA92KF019X Confirmed. Ksh4,850.00 paid to NAIVAS SUPERMARKET WESTLANDS. on 2/10/26 at 2:25 PM. New M-PESA balance is Ksh28,450.00. Transaction cost, Ksh0.00.",
                isExpense = true
            ),
            TransactionEntity(
                code = "QA88LK412B",
                type = TransactionType.PAYBILL.name,
                recipientOrSender = "KPLC PREPAID",
                accountOrDetails = "Acc: 1428571290",
                amount = 2500.00,
                cost = 0.0,
                balance = 33300.00,
                timestamp = createDate(2, 9, 15),
                category = TransactionCategory.UTILITIES,
                rawSms = "QA88LK412B Confirmed. Ksh2,500.00 sent to KPLC PREPAID for account 1428571290 on 2/10/26 at 9:15 AM. New M-PESA balance is Ksh33,300.00. Transaction cost, Ksh0.00.",
                isExpense = true
            ),
            TransactionEntity(
                code = "QA71MN834T",
                type = TransactionType.BUY_GOODS.name,
                recipientOrSender = "SHELL SERVICE STATION KILIMANI",
                accountOrDetails = "Till 882310",
                amount = 3200.00,
                cost = 0.0,
                balance = 35800.00,
                timestamp = createDate(1, 18, 40),
                category = TransactionCategory.TRANSPORT,
                rawSms = "QA71MN834T Confirmed. Ksh3,200.00 paid to SHELL SERVICE STATION KILIMANI. on 1/10/26 at 6:40 PM. New M-PESA balance is Ksh35,800.00. Transaction cost, Ksh0.00.",
                isExpense = true
            ),
            TransactionEntity(
                code = "QA65PQ992M",
                type = TransactionType.BUY_GOODS.name,
                recipientOrSender = "JAVA HOUSE VALLEY ARCADE",
                accountOrDetails = "Till 104523",
                amount = 1650.00,
                cost = 0.0,
                balance = 39000.00,
                timestamp = createDate(1, 13, 10),
                category = TransactionCategory.FOOD_DINING,
                rawSms = "QA65PQ992M Confirmed. Ksh1,650.00 paid to JAVA HOUSE VALLEY ARCADE. on 1/10/26 at 1:10 PM. New M-PESA balance is Ksh39,000.00. Transaction cost, Ksh0.00.",
                isExpense = true
            ),
            TransactionEntity(
                code = "QA52RS118K",
                type = TransactionType.AIRTIME.name,
                recipientOrSender = "Safaricom Airtime",
                accountOrDetails = "0712345678",
                amount = 500.00,
                cost = 0.0,
                balance = 40650.00,
                timestamp = createDate(1, 8, 30),
                category = TransactionCategory.AIRTIME,
                rawSms = "QA52RS118K Confirmed. You bought Ksh500.00 of airtime on 1/10/26 at 8:30 AM. New M-PESA balance is Ksh40,650.00. Transaction cost, Ksh0.00.",
                isExpense = true
            ),
            TransactionEntity(
                code = "QA44TT720Z",
                type = TransactionType.RECEIVE_MONEY.name,
                recipientOrSender = "ACME CONSULTING LTD",
                accountOrDetails = "0722000111",
                amount = 45000.00,
                cost = 0.0,
                balance = 41150.00,
                timestamp = createDate(1, 7, 0),
                category = TransactionCategory.INCOME,
                rawSms = "QA44TT720Z Confirmed. You have received Ksh45,000.00 from ACME CONSULTING LTD on 1/10/26 at 7:00 AM. New M-PESA balance is Ksh41,150.00.",
                isExpense = false
            ),

            // --- Late September 2026 Transactions (for trend comparisons & insights) ---
            TransactionEntity(
                code = "PZ99BB123A",
                type = TransactionType.SEND_MONEY.name,
                recipientOrSender = "GRACE WANJIKU (MUM)",
                accountOrDetails = "0723456789",
                amount = 6000.00,
                cost = 34.0,
                balance = 3850.00,
                timestamp = createDate(30, 19, 15, -1),
                category = TransactionCategory.FAMILY_P2P,
                rawSms = "PZ99BB123A Confirmed. Ksh6,000.00 sent to GRACE WANJIKU 0723456789 on 30/9/26 at 7:15 PM. New M-PESA balance is Ksh3,850.00. Transaction cost, Ksh34.00.",
                isExpense = true
            ),
            TransactionEntity(
                code = "PZ88CC456B",
                type = TransactionType.BUY_GOODS.name,
                recipientOrSender = "QUICKMART KILIMANI",
                accountOrDetails = "Till 773124",
                amount = 3450.00,
                cost = 0.0,
                balance = 9884.00,
                timestamp = createDate(29, 17, 30, -1),
                category = TransactionCategory.GROCERIES,
                rawSms = "PZ88CC456B Confirmed. Ksh3,450.00 paid to QUICKMART KILIMANI. on 29/9/26 at 5:30 PM. New M-PESA balance is Ksh9,884.00. Transaction cost, Ksh0.00.",
                isExpense = true
            ),
            TransactionEntity(
                code = "PZ77DD789C",
                type = TransactionType.PAYBILL.name,
                recipientOrSender = "ZUKU FIBER INTERNET",
                accountOrDetails = "Acc: 304912",
                amount = 4699.00,
                cost = 0.0,
                balance = 13334.00,
                timestamp = createDate(28, 10, 0, -1),
                category = TransactionCategory.UTILITIES,
                rawSms = "PZ77DD789C Confirmed. Ksh4,699.00 sent to ZUKU FIBER INTERNET for account 304912 on 28/9/26 at 10:00 AM. New M-PESA balance is Ksh13,334.00. Transaction cost, Ksh0.00.",
                isExpense = true
            ),
            TransactionEntity(
                code = "PZ66EE012D",
                type = TransactionType.PAYBILL.name,
                recipientOrSender = "NETFLIX KENYA",
                accountOrDetails = "Acc: Sub-9912",
                amount = 1100.00,
                cost = 0.0,
                balance = 18033.00,
                timestamp = createDate(27, 21, 45, -1),
                category = TransactionCategory.ENTERTAINMENT,
                rawSms = "PZ66EE012D Confirmed. Ksh1,100.00 sent to NETFLIX KENYA for account Sub-9912 on 27/9/26 at 9:45 PM. New M-PESA balance is Ksh18,033.00. Transaction cost, Ksh0.00.",
                isExpense = true
            ),
            TransactionEntity(
                code = "PZ55FF345E",
                type = TransactionType.BUY_GOODS.name,
                recipientOrSender = "UBER TRIP NAIROBI",
                accountOrDetails = "Till 621450",
                amount = 890.00,
                cost = 0.0,
                balance = 19133.00,
                timestamp = createDate(26, 22, 10, -1),
                category = TransactionCategory.TRANSPORT,
                rawSms = "PZ55FF345E Confirmed. Ksh890.00 paid to UBER TRIP NAIROBI. on 26/9/26 at 10:10 PM. New M-PESA balance is Ksh19,133.00. Transaction cost, Ksh0.00.",
                isExpense = true
            ),
            TransactionEntity(
                code = "PZ44GG678F",
                type = TransactionType.WITHDRAW.name,
                recipientOrSender = "128456 - AGENT KIOSK NGONG ROAD",
                accountOrDetails = "Agent 128456",
                amount = 5000.00,
                cost = 67.0,
                balance = 20023.00,
                timestamp = createDate(25, 14, 0, -1),
                category = TransactionCategory.OTHER,
                rawSms = "PZ44GG678F Confirmed. on 25/9/26 at 2:00 PM Withdraw Ksh5,000.00 from 128456 - AGENT KIOSK NGONG ROAD. New M-PESA balance is Ksh20,023.00. Transaction cost, Ksh67.00.",
                isExpense = true
            ),
            TransactionEntity(
                code = "PZ33HH901G",
                type = TransactionType.BUY_GOODS.name,
                recipientOrSender = "ARTCAFFE THE HUB KAREN",
                accountOrDetails = "Till 451299",
                amount = 2400.00,
                cost = 0.0,
                balance = 25090.00,
                timestamp = createDate(24, 15, 30, -1),
                category = TransactionCategory.FOOD_DINING,
                rawSms = "PZ33HH901G Confirmed. Ksh2,400.00 paid to ARTCAFFE THE HUB KAREN. on 24/9/26 at 3:30 PM. New M-PESA balance is Ksh25,090.00. Transaction cost, Ksh0.00.",
                isExpense = true
            ),
            TransactionEntity(
                code = "PZ22II234H",
                type = TransactionType.BUY_GOODS.name,
                recipientOrSender = "JUMIA ONLINE SHOP",
                accountOrDetails = "Till 894123",
                amount = 4200.00,
                cost = 0.0,
                balance = 27490.00,
                timestamp = createDate(23, 11, 20, -1),
                category = TransactionCategory.SHOPPING,
                rawSms = "PZ22II234H Confirmed. Ksh4,200.00 paid to JUMIA ONLINE SHOP. on 23/9/26 at 11:20 AM. New M-PESA balance is Ksh27,490.00. Transaction cost, Ksh0.00.",
                isExpense = true
            ),
            TransactionEntity(
                code = "PZ11JJ567I",
                type = TransactionType.SEND_MONEY.name,
                recipientOrSender = "BRIAN KIPKORIR",
                accountOrDetails = "0711998877",
                amount = 1500.00,
                cost = 23.0,
                balance = 31690.00,
                timestamp = createDate(22, 16, 50, -1),
                category = TransactionCategory.FAMILY_P2P,
                rawSms = "PZ11JJ567I Confirmed. Ksh1,500.00 sent to BRIAN KIPKORIR 0711998877 on 22/9/26 at 4:50 PM. New M-PESA balance is Ksh31,690.00. Transaction cost, Ksh23.00.",
                isExpense = true
            )
        )
    }
}
