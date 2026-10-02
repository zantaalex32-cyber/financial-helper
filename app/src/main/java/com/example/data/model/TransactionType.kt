package com.example.data.model

enum class TransactionType(val label: String) {
    SEND_MONEY("Send Money"),
    PAYBILL("Paybill"),
    BUY_GOODS("Buy Goods / Till"),
    WITHDRAW("Withdraw Cash"),
    RECEIVE_MONEY("Received Money"),
    AIRTIME("Airtime Purchase"),
    BANK_TRANSFER("Bank Transfer"),
    OTHER("M-Pesa Transaction")
}
