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
    val availableBalance: Double
)

data class StockData(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val changePercent: Double
)