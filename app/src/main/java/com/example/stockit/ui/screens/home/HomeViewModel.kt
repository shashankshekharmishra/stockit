package com.example.stockit.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import com.example.stockit.network.ApiConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.util.Log

class HomeViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private val apiService = ApiConfig.stockApiService
    private val gson = Gson()
    private var authToken: String? = null
    
    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingPortfolio = true,
                isLoadingStocks = true,
                error = null
            )
            
            try {
                val portfolioDeferred = async { loadPortfolioSummary() }
                val stocksDeferred = async { loadTrendingStocks() }
                
                awaitAll(portfolioDeferred, stocksDeferred)
                
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading data", e)
                _uiState.value = _uiState.value.copy(
                    isLoadingPortfolio = false,
                    isLoadingStocks = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }
    
    private suspend fun loadPortfolioSummary() {
        try {
            if (authToken == null) {
                _uiState.value = _uiState.value.copy(
                    portfolioSummary = null,
                    isLoadingPortfolio = false
                )
                return
            }
            
            val response = apiService.getUserSummary("Bearer $authToken")
            Log.d("HomeViewModel", "Portfolio response: $response")
            
            if (response.success && response.data != null) {
                // Parse the response data
                val dataJson = gson.toJson(response.data)
                val summaryData = gson.fromJson(dataJson, ApiUserSummary::class.java)
                
                val summary = PortfolioSummary(
                    totalValue = summaryData.portfolio.totalValue,
                    totalInvestment = summaryData.portfolio.totalInvestment,
                    availableBalance = summaryData.wallet.balance,
                    totalShares = summaryData.portfolio.totalShares,
                    totalProfitLoss = summaryData.totalProfitLoss,
                    totalProfitLossPercentage = summaryData.totalProfitLossPercentage
                )
                
                _uiState.value = _uiState.value.copy(
                    portfolioSummary = summary,
                    isLoadingPortfolio = false
                )
            } else {
                throw Exception("Failed to load portfolio summary")
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error loading portfolio", e)
            _uiState.value = _uiState.value.copy(
                isLoadingPortfolio = false,
                error = "Failed to load portfolio: ${e.message}"
            )
        }
    }
    
    private suspend fun loadTrendingStocks() {
        try {
            val response = apiService.getTrendingStocks(limit = 20, update = "false") // Increased from 10 to 20
            Log.d("HomeViewModel", "Trending stocks response: $response")
            
            if (response.success && response.stocks != null) {
                // Convert TrendingStock to StockData format - removed .take(5) to show all 20
                val stocks = response.stocks.map { stock ->
                    StockData(
                        symbol = stock.symbol,
                        name = stock.name ?: stock.symbol,
                        price = stock.price ?: 0.0,
                        change = stock.change,
                        changePercent = stock.changePercent,
                        marketCap = null, // Not provided in trending API
                        volume = stock.volume
                    )
                }
                
                _uiState.value = _uiState.value.copy(
                    trendingStocks = stocks,
                    isLoadingStocks = false
                )
            } else {
                throw Exception("Failed to load trending stocks")
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error loading trending stocks", e)
            // Load fallback data for trending stocks if API fails
            loadFallbackTrendingStocks()
        }
    }
    
    fun setAuthToken(token: String?) {
        authToken = token
        Log.d("HomeViewModel", "Auth token set: ${token?.take(10)}...")
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun retry() {
        loadData()
    }
    
    fun refreshTrendingStocks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingStocks = true)
            
            try {
                // Request fresh data from server - increased from 10 to 20
                val response = apiService.getTrendingStocks(limit = 20, update = "true")
                Log.d("HomeViewModel", "Refreshed trending stocks response: $response")
                
                if (response.success && response.stocks != null) {
                    // Removed .take(5) to show all 20 stocks
                    val stocks = response.stocks.map { stock ->
                        StockData(
                            symbol = stock.symbol,
                            name = stock.name ?: stock.symbol,
                            price = stock.price ?: 0.0,
                            change = stock.change,
                            changePercent = stock.changePercent,
                            marketCap = null,
                            volume = stock.volume
                        )
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        trendingStocks = stocks,
                        isLoadingStocks = false
                    )
                } else {
                    throw Exception("Failed to refresh trending stocks")
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error refreshing trending stocks", e)
                _uiState.value = _uiState.value.copy(
                    isLoadingStocks = false,
                    error = "Failed to refresh trending stocks: ${e.message}"
                )
            }
        }
    }
    
    private fun loadFallbackTrendingStocks() {
        // Expanded fallback data to show more stocks
        val fallbackStocks = listOf(
            StockData("RELIANCE", "Reliance Industries Ltd.", 2850.0, 25.0, 0.88),
            StockData("TCS", "Tata Consultancy Services Ltd.", 3720.0, -15.0, -0.40),
            StockData("INFY", "Infosys Ltd.", 1650.0, 12.0, 0.73),
            StockData("HDFCBANK", "HDFC Bank Ltd.", 1580.0, 8.0, 0.51),
            StockData("ICICIBANK", "ICICI Bank Ltd.", 1020.0, -5.0, -0.49),
            StockData("HINDUNILVR", "Hindustan Unilever Ltd.", 2480.0, 18.0, 0.73),
            StockData("BHARTIARTL", "Bharti Airtel Ltd.", 860.0, -8.0, -0.92),
            StockData("ITC", "ITC Ltd.", 420.0, 3.5, 0.84),
            StockData("KOTAKBANK", "Kotak Mahindra Bank Ltd.", 1750.0, 12.0, 0.69),
            StockData("LT", "Larsen & Toubro Ltd.", 3200.0, -20.0, -0.62),
            StockData("SBIN", "State Bank of India", 580.0, 5.0, 0.87),
            StockData("ASIANPAINT", "Asian Paints Ltd.", 3150.0, -25.0, -0.79),
            StockData("MARUTI", "Maruti Suzuki India Ltd.", 10500.0, 150.0, 1.45),
            StockData("HCLTECH", "HCL Technologies Ltd.", 1180.0, 8.0, 0.68),
            StockData("AXISBANK", "Axis Bank Ltd.", 980.0, -12.0, -1.21),
            StockData("BAJFINANCE", "Bajaj Finance Ltd.", 6800.0, 85.0, 1.27),
            StockData("WIPRO", "Wipro Ltd.", 420.0, -3.0, -0.71),
            StockData("TECHM", "Tech Mahindra Ltd.", 1250.0, 15.0, 1.22),
            StockData("ULTRACEMCO", "UltraTech Cement Ltd.", 8200.0, -100.0, -1.20),
            StockData("NESTLEIND", "Nestle India Ltd.", 22000.0, 200.0, 0.92)
        )
        
        _uiState.value = _uiState.value.copy(
            trendingStocks = fallbackStocks,
            isLoadingStocks = false,
            error = "Using cached trending stocks data"
        )
    }
}