package com.example.data.model

enum class AlertSeverity {
    INFO,
    WARNING,
    CRITICAL
}

data class ThresholdAlert(
    val category: String,
    val spent: Double,
    val limit: Double,
    val thresholdPercent: Int,
    val currentPercent: Double,
    val isExceeded: Boolean,
    val isApproaching: Boolean,
    val remainingAmount: Double,
    val severity: AlertSeverity,
    val title: String,
    val message: String,
    val colorHex: String
)
