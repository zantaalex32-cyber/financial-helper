package com.example.data.model

data class CategoryMeta(
    val id: String,
    val displayName: String,
    val defaultLimit: Double,
    val colorHex: String,
    val iconKey: String
)

object TransactionCategory {
    const val GROCERIES = "Groceries"
    const val UTILITIES = "Utilities & Bills"
    const val FOOD_DINING = "Food & Dining"
    const val TRANSPORT = "Transport & Fuel"
    const val ENTERTAINMENT = "Entertainment"
    const val SHOPPING = "Shopping"
    const val AIRTIME = "Airtime & Internet"
    const val FAMILY_P2P = "Family & Friends"
    const val FEES = "Transaction Fees"
    const val INCOME = "Income / Received"
    const val OTHER = "Other Expenses"

    val DEFAULT_CATEGORIES = listOf(
        CategoryMeta(GROCERIES, "Groceries & Supermarket", 15000.0, "#10B981", "shopping_cart"),
        CategoryMeta(UTILITIES, "Utilities & Power Bills", 8000.0, "#3B82F6", "bolt"),
        CategoryMeta(FOOD_DINING, "Food & Restaurants", 10000.0, "#F59E0B", "restaurant"),
        CategoryMeta(TRANSPORT, "Transport & Fuel", 7500.0, "#8B5CF6", "directions_car"),
        CategoryMeta(SHOPPING, "Shopping & Retail", 6000.0, "#06B6D4", "storefront"),
        CategoryMeta(ENTERTAINMENT, "Entertainment & Subs", 4000.0, "#EC4899", "movie"),
        CategoryMeta(AIRTIME, "Airtime & Data Bundles", 3000.0, "#14B8A6", "phone_iphone"),
        CategoryMeta(FAMILY_P2P, "Family & P2P Transfers", 12000.0, "#6366F1", "people"),
        CategoryMeta(FEES, "M-Pesa Fees & Charges", 1500.0, "#EF4444", "receipt_long"),
        CategoryMeta(OTHER, "Other Expenses", 5000.0, "#64748B", "category")
    )
}
