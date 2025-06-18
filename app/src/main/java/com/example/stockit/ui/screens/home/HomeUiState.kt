package com.example.stockit.ui.screens.home

data class HomeUiState(
    val portfolioSummary: PortfolioSummary? = null,
    val trendingStocks: List<StockData> = emptyList(),
    val isLoadingPortfolio: Boolean = false,
    val isLoadingStocks: Boolean = false,
    val error: String? = null
)

data class PortfolioSummary(
    val totalValue: Double,
    val totalInvestment: Double,
    val availableBalance: Double,
    val totalShares: Int = 0,
    val totalProfitLoss: Double = 0.0,
    val totalProfitLossPercentage: Double = 0.0
)

data class StockData(
    val symbol: String,
    val name: String?,
    val price: Double,
    val change: Double?,
    val changePercent: Double?,
    val marketCap: Double? = null,
    val volume: Long? = null,
    val high: Double? = null,
    val low: Double? = null,
    val rank: Int? = null,
    val positive: Boolean? = null
)

// API Response Models - Updated to match ApiConfig
data class ApiUserSummary(
    val wallet: WalletData,
    val portfolio: PortfolioData,
    val totalProfitLoss: Double,
    val totalProfitLossPercentage: Double
)

data class WalletData(
    val balance: Double,
    val totalDeposited: Double,
    val totalWithdrawn: Double
)

data class PortfolioData(
    val totalValue: Double,
    val totalInvestment: Double,
    val totalShares: Int,
    val holdings: List<HoldingData>
)

data class HoldingData(
    val symbol: String,
    val quantity: Int,
    val averagePrice: Double,
    val currentPrice: Double,
    val totalValue: Double,
    val profitLoss: Double,
    val profitLossPercentage: Double
)

// Updated TrendingStocks response models to match ApiConfig
data class TrendingStocksApiResponse(
    val success: Boolean,
    val count: Int?,
    val source: String?,
    val lastUpdated: String?,
    val cached: Boolean?,
    val stocks: List<TrendingStockApi>?,
    val error: String?,
    val timestamp: String?
)

data class TrendingStockApi(
    val symbol: String,
    val name: String?,
    val price: Double?,
    val change: Double?,
    val changePercent: Double?,
    val volume: Long?,
    val high: Double?,
    val low: Double?,
    val rank: Int?,
    val positive: Boolean?
)