package com.example.stockit.data.model

data class UserProfile(
    val id: String,
    val fullName: String,
    val email: String,
    val balance: Double,
    val totalInvested: Double,
    val totalValue: Double,
    val profitLoss: Double,
    val profitLossPercent: Double,
    val totalStocks: Int,
    val joinedDate: String?
)

data class PortfolioStock(
    val symbol: String,
    val companyName: String?,
    val quantity: Int,
    val averagePrice: Double,
    val currentPrice: Double,
    val investedAmount: Double,
    val currentValue: Double,
    val profitLoss: Double,
    val profitLossPercent: Double,
    val firstBuyDate: String?
)

data class RecentTransaction(
    val id: String,
    val symbol: String,
    val type: String, // "buy" or "sell"
    val quantity: Int,
    val price: Double,
    val amount: Double,
    val timestamp: String
)