package com.example.stockit.data.model

data class WatchlistStock(
    val symbol: String,
    val companyName: String?,
    val price: Double?,
    val change: Double?,
    val changePercent: Double?,
    val high: Double?,
    val low: Double?,
    val volume: Long?,
    val addedAt: String?,
    val inWatchlist: Boolean = true
)

data class WatchlistResponse(
    val success: Boolean,
    val totalStocks: Int?,
    val stocks: List<WatchlistStock>?,
    val error: String?,
    val timestamp: String?
)

data class WatchlistActionResponse(
    val success: Boolean,
    val added: Boolean?,
    val removed: Boolean?,
    val symbol: String?,
    val companyName: String?,
    val message: String?,
    val error: String?,
    val inWatchlist: Boolean?,
    val timestamp: String?
)

data class AddToWatchlistRequest(
    val symbol: String
)