package com.example.stockit.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import com.example.stockit.network.ApiConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.util.Log
import retrofit2.HttpException
import java.io.IOException
import kotlin.math.min
import kotlin.math.pow

class HomeViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private val apiService = ApiConfig.stockApiService
    private val gson = Gson()
    private var authToken: String? = null
    
    // Retry configuration
    private var portfolioRetryAttempt = 0
    private var stocksRetryAttempt = 0
    private val maxRetryAttempts = 5
    private val baseDelayMs = 2000L // 2 seconds
    private val maxDelayMs = 30000L // 30 seconds
    
    fun setAuthToken(token: String?) {
        authToken = token
    }
    
    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingPortfolio = true,
                isLoadingStocks = true,
                error = null
            )
            
            // Reset retry counters
            portfolioRetryAttempt = 0
            stocksRetryAttempt = 0
            
            try {
                val portfolioDeferred = async { loadPortfolioSummaryWithRetry() }
                val stocksDeferred = async { loadTrendingStocksWithRetry() }
                
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
    
    private suspend fun loadPortfolioSummaryWithRetry() {
        while (portfolioRetryAttempt < maxRetryAttempts) {
            try {
                loadPortfolioSummary()
                return // Success, exit retry loop
            } catch (e: Exception) {
                portfolioRetryAttempt++
                
                if (portfolioRetryAttempt >= maxRetryAttempts) {
                    Log.e("HomeViewModel", "Max retry attempts reached for portfolio", e)
                    _uiState.value = _uiState.value.copy(
                        isLoadingPortfolio = false,
                        error = "Failed to load portfolio after ${maxRetryAttempts} attempts"
                    )
                    return
                }
                
                val delayMs = calculateBackoffDelay(portfolioRetryAttempt)
                Log.w("HomeViewModel", "Portfolio retry attempt $portfolioRetryAttempt in ${delayMs}ms", e)
                
                delay(delayMs)
            }
        }
    }
    
    private suspend fun loadTrendingStocksWithRetry() {
        while (stocksRetryAttempt < maxRetryAttempts) {
            try {
                loadTrendingStocks()
                return // Success, exit retry loop
            } catch (e: HttpException) {
                when (e.code()) {
                    503, 502, 504 -> {
                        // Server errors - retry with backoff
                        stocksRetryAttempt++
                        
                        if (stocksRetryAttempt >= maxRetryAttempts) {
                            Log.w("HomeViewModel", "Max retry attempts reached for trending stocks, using fallback")
                            loadFallbackTrendingStocks()
                            return
                        }
                        
                        val delayMs = calculateBackoffDelay(stocksRetryAttempt)
                        Log.w("HomeViewModel", "Trending stocks HTTP ${e.code()} retry attempt $stocksRetryAttempt in ${delayMs}ms")
                        
                        delay(delayMs)
                    }
                    else -> {
                        // Other HTTP errors - use fallback immediately
                        Log.w("HomeViewModel", "HTTP ${e.code()} error, using fallback data immediately")
                        loadFallbackTrendingStocks()
                        return
                    }
                }
            } catch (e: IOException) {
                // Network errors - retry with backoff
                stocksRetryAttempt++
                
                if (stocksRetryAttempt >= maxRetryAttempts) {
                    Log.w("HomeViewModel", "Max retry attempts reached for trending stocks due to network error, using fallback")
                    loadFallbackTrendingStocks()
                    return
                }
                
                val delayMs = calculateBackoffDelay(stocksRetryAttempt)
                Log.w("HomeViewModel", "Network error retry attempt $stocksRetryAttempt in ${delayMs}ms", e)
                
                delay(delayMs)
            } catch (e: Exception) {
                // Other errors - use fallback immediately
                Log.e("HomeViewModel", "Unexpected error loading trending stocks, using fallback", e)
                loadFallbackTrendingStocks()
                return
            }
        }
    }
    
    private fun calculateBackoffDelay(attempt: Int): Long {
        // Exponential backoff with jitter: delay = base * 2^attempt + random(0, 1000)
        val exponentialDelay = (baseDelayMs * 2.0.pow(attempt - 1)).toLong()
        val jitter = (0..1000).random()
        return min(exponentialDelay + jitter, maxDelayMs)
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
            
            if (response.success) {
                val summary = PortfolioSummary(
                    totalValue = response.portfolioCurrentValue ?: 0.0,
                    totalInvestment = response.portfolioInvested ?: 0.0,
                    availableBalance = response.balance ?: 0.0,
                    totalShares = response.totalHoldings ?: 0,
                    totalProfitLoss = response.portfolioPnL ?: 0.0,
                    totalProfitLossPercentage = response.portfolioPnLPercent ?: 0.0
                )
                
                _uiState.value = _uiState.value.copy(
                    portfolioSummary = summary,
                    isLoadingPortfolio = false
                )
                
                Log.i("HomeViewModel", "Portfolio loaded successfully: $summary")
            } else {
                throw Exception("API returned success=false")
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error loading portfolio", e)
            throw e // Re-throw to be handled by retry logic
        }
    }
    
    private suspend fun loadTrendingStocks() {
        try {
            val response = apiService.getTrendingStocks()
            Log.d("HomeViewModel", "Trending stocks response: $response")
            
            if (response.success && !response.stocks.isNullOrEmpty()) {
                val stockList = response.stocks.map { stock ->
                    StockData(
                        symbol = stock.symbol,
                        name = stock.name,
                        price = stock.price ?: 0.0,
                        change = stock.change,
                        changePercent = stock.changePercent,
                        volume = stock.volume,
                        high = stock.high,
                        low = stock.low,
                        rank = stock.rank,
                        positive = stock.positive
                    )
                }
                
                _uiState.value = _uiState.value.copy(
                    trendingStocks = stockList,
                    isLoadingStocks = false
                )
                
                Log.i("HomeViewModel", "Trending stocks loaded: ${stockList.size} stocks")
            } else {
                Log.w("HomeViewModel", "Empty or unsuccessful trending stocks response")
                loadFallbackTrendingStocks()
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error loading trending stocks", e)
            throw e // Re-throw to be handled by retry logic
        }
    }
    
    private fun loadFallbackTrendingStocks() {
        val fallbackStocks = listOf(
            StockData(
                symbol = "RELIANCE",
                name = "Reliance Industries Limited",
                price = 2456.75,
                change = 23.45,
                changePercent = 0.96,
                positive = true,
                rank = 1
            ),
            StockData(
                symbol = "TCS",
                name = "Tata Consultancy Services Limited",
                price = 3567.80,
                change = -12.30,
                changePercent = -0.34,
                positive = false,
                rank = 2
            ),
            StockData(
                symbol = "HDFCBANK",
                name = "HDFC Bank Limited",
                price = 1632.45,
                change = 8.90,
                changePercent = 0.55,
                positive = true,
                rank = 3
            ),
            StockData(
                symbol = "INFY",
                name = "Infosys Limited",
                price = 1456.25,
                change = 15.67,
                changePercent = 1.09,
                positive = true,
                rank = 4
            ),
            StockData(
                symbol = "ICICIBANK",
                name = "ICICI Bank Limited",
                price = 987.60,
                change = -5.40,
                changePercent = -0.54,
                positive = false,
                rank = 5
            )
        )
        
        _uiState.value = _uiState.value.copy(
            trendingStocks = fallbackStocks,
            isLoadingStocks = false
        )
        
        Log.i("HomeViewModel", "Loaded fallback trending stocks")
    }
    
    fun retry() {
        loadData()
    }
    
    fun retryPortfolioInBackground() {
        if (!_uiState.value.isLoadingPortfolio) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoadingPortfolio = true)
                try {
                    loadPortfolioSummary()
                } catch (e: Exception) {
                    Log.w("HomeViewModel", "Background portfolio retry failed", e)
                    _uiState.value = _uiState.value.copy(isLoadingPortfolio = false)
                }
            }
        }
    }
    
    fun retryTrendingStocksInBackground() {
        if (!_uiState.value.isLoadingStocks) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoadingStocks = true)
                try {
                    loadTrendingStocks()
                } catch (e: Exception) {
                    Log.w("HomeViewModel", "Background trending stocks retry failed", e)
                    loadFallbackTrendingStocks()
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}