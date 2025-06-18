package com.example.stockit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockit.network.ApiConfig
import com.example.stockit.ui.screens.home.*
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class HomeViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private var authToken: String? = null
    private val gson = Gson()
    
    fun setAuthToken(token: String?) {
        authToken = token
    }
    
    fun loadData() {
        loadPortfolioSummary()
        loadTrendingStocks()
    }
    
    private fun loadPortfolioSummary() {
        if (authToken == null) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingPortfolio = true, error = null)
            
            try {
                val response = ApiConfig.stockApiService.getUserSummary("Bearer $authToken")
                if (response.success && response.data != null) {
                    val summaryData = gson.fromJson(gson.toJson(response.data), ApiUserSummary::class.java)
                    
                    val portfolioSummary = PortfolioSummary(
                        totalValue = summaryData.portfolio.totalValue,
                        totalInvestment = summaryData.portfolio.totalInvestment,
                        availableBalance = summaryData.wallet.balance,
                        totalShares = summaryData.portfolio.totalShares,
                        totalProfitLoss = summaryData.totalProfitLoss,
                        totalProfitLossPercentage = summaryData.totalProfitLossPercentage
                    )
                    
                    _uiState.value = _uiState.value.copy(
                        portfolioSummary = portfolioSummary,
                        isLoadingPortfolio = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingPortfolio = false,
                        error = "Failed to load portfolio"
                    )
                }
            } catch (e: HttpException) {
                _uiState.value = _uiState.value.copy(
                    isLoadingPortfolio = false,
                    error = "Network error: ${e.code()}"
                )
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(
                    isLoadingPortfolio = false,
                    error = "Connection error"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingPortfolio = false,
                    error = "Error loading portfolio: ${e.message}"
                )
            }
        }
    }
    
    private fun loadTrendingStocks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingStocks = true, error = null)
            
            try {
                val response = ApiConfig.stockApiService.getTrendingStocks(
                    limit = 30,
                    update = "false",
                    force = "false"
                )
                
                if (response.success && !response.stocks.isNullOrEmpty()) {
                    val stockList = response.stocks.map { trendingStock ->
                        StockData(
                            symbol = trendingStock.symbol,
                            name = trendingStock.name,
                            price = trendingStock.price ?: 0.0,
                            change = trendingStock.change,
                            changePercent = trendingStock.changePercent,
                            volume = trendingStock.volume,
                            high = trendingStock.high,
                            low = trendingStock.low,
                            rank = trendingStock.rank,
                            positive = trendingStock.positive
                        )
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        trendingStocks = stockList,
                        isLoadingStocks = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingStocks = false,
                        error = response.error ?: "No trending stocks available"
                    )
                }
            } catch (e: HttpException) {
                _uiState.value = _uiState.value.copy(
                    isLoadingStocks = false,
                    error = "Network error: ${e.code()}"
                )
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(
                    isLoadingStocks = false,
                    error = "Connection error"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingStocks = false,
                    error = "Error loading stocks: ${e.message}"
                )
            }
        }
    }
    
    fun retry() {
        loadData()
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}