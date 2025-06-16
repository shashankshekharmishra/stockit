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

class HomeViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private val apiService = ApiConfig.stockApiService
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
            
            if (response.success) {
                val summary = PortfolioSummary(
                    totalValue = 0.0, // Parse from response.data
                    totalInvestment = 0.0, // Parse from response.data
                    availableBalance = 0.0 // Parse from response.data
                )
                
                _uiState.value = _uiState.value.copy(
                    portfolioSummary = summary,
                    isLoadingPortfolio = false
                )
            } else {
                throw Exception("Failed to load portfolio summary")
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoadingPortfolio = false,
                error = "Failed to load portfolio: ${e.message}"
            )
        }
    }
    
    private suspend fun loadTrendingStocks() {
        try {
            val response = apiService.getPopularStocks("nifty50", 10)
            
            if (response.success) {
                val stocks = listOf(
                    StockData("RELIANCE", "Reliance Industries", 2500.0, 25.0, 1.0),
                    StockData("TCS", "Tata Consultancy Services", 3500.0, -15.0, -0.4),
                    StockData("INFY", "Infosys", 1800.0, 12.0, 0.7),
                    StockData("HDFCBANK", "HDFC Bank", 1600.0, 8.0, 0.5),
                    StockData("ICICIBANK", "ICICI Bank", 900.0, -5.0, -0.6)
                )
                
                _uiState.value = _uiState.value.copy(
                    trendingStocks = stocks,
                    isLoadingStocks = false
                )
            } else {
                throw Exception("Failed to load trending stocks")
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoadingStocks = false,
                error = "Failed to load trending stocks: ${e.message}"
            )
        }
    }
    
    fun setAuthToken(token: String?) {
        authToken = token
    }
}