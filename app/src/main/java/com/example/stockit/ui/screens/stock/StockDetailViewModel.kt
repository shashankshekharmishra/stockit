package com.example.stockit.ui.screens.stock

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockit.data.repository.WatchlistRepository
import com.example.stockit.network.ApiConfig
import com.example.stockit.network.AffordabilityRequest
import com.example.stockit.network.TradeRequest
import com.example.stockit.network.ChartDataResponse
import com.example.stockit.network.PricePoint
import com.example.stockit.utils.AuthManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import android.util.Log

data class StockDetailUiState(
    val stockData: StockData? = null,
    val stockDetails: StockDetails? = null,
    val chartData: List<ChartPoint> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingChart: Boolean = false,
    val isTradingLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val isAuthenticated: Boolean = false,
    val affordabilityResult: AffordabilityResult? = null,
    val isInWatchlist: Boolean = false,
    val watchlistLoading: Boolean = false,
    val userHolding: UserHolding? = null,
    val isLoadingHolding: Boolean = false,
    val holdingsRequestCount: Int = 0, // Add this to track request count
    val shouldContinuouslyRetryHoldings: Boolean = false // Add this flag
)

data class StockData(
    val symbol: String,
    val name: String?,
    val price: Double,
    val change: Double?,
    val changePercent: Double?,
    val open: Double?,
    val high: Double?,
    val low: Double?,
    val previousClose: Double?,
    val volume: Long?
)

data class StockDetails(
    val marketCap: Double?,
    val peRatio: Double?,
    val eps: Double?,
    val dividend: Double?,
    val beta: Double?,
    val description: String?
)

data class ChartPoint(
    val timestamp: Long,
    val price: Double,
    val volume: Long?
)

data class AffordabilityResult(
    val canAfford: Boolean,
    val totalCost: Double,
    val availableBalance: Double,
    val message: String?
)

data class UserHolding(
    val symbol: String,
    val owns: Boolean,
    val quantity: Int,
    val averagePrice: Double,
    val investedAmount: Double,
    val currentPrice: Double,
    val currentValue: Double,
    val profitLoss: Double,
    val firstBuyDate: String?
)

@HiltViewModel
class StockDetailViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val watchlistRepository: WatchlistRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(StockDetailUiState())
    val uiState: StateFlow<StockDetailUiState> = _uiState.asStateFlow()
    
    private val gson = Gson()
    
    init {
        viewModelScope.launch {
            try {
                // Use the StateFlow from AuthManager instead of the function
                authManager.isLoggedInState.collectLatest { isLoggedIn ->
                    _uiState.value = _uiState.value.copy(isAuthenticated = isLoggedIn)
                }
            } catch (e: Exception) {
                println("⚠️ Error collecting auth state: ${e.message}")
                _uiState.value = _uiState.value.copy(isAuthenticated = false)
            }
        }
    }
    
    fun loadStockData(symbol: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Load basic stock data
                val stockResponse = ApiConfig.stockApiService.getStockQuote(symbol)
                
                if (stockResponse.success) {
                    val stockData = parseStockData(stockResponse.data, symbol)
                    _uiState.value = _uiState.value.copy(stockData = stockData)
                }
                
                // Load detailed stock information
                val detailsResponse = ApiConfig.stockApiService.getStockDetails(symbol)
                
                if (detailsResponse.success) {
                    val stockDetails = parseStockDetails(detailsResponse.data)
                    _uiState.value = _uiState.value.copy(stockDetails = stockDetails)
                }
                
                _uiState.value = _uiState.value.copy(isLoading = false)
                
                // Load user holding if authenticated - Initial load attempt 1
                if (_uiState.value.isAuthenticated) {
                    loadUserHolding(symbol)
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load stock data: ${e.message}"
                )
            }
        }
    }
    
    // Background retry mechanisms - Updated to respect holdings retry strategy
    fun retryStockDataInBackground(symbol: String) {
        if (!_uiState.value.isLoading) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                try {
                    // Load basic stock data
                    val stockResponse = ApiConfig.stockApiService.getStockQuote(symbol)
                    
                    if (stockResponse.success) {
                        val stockData = parseStockData(stockResponse.data, symbol)
                        _uiState.value = _uiState.value.copy(stockData = stockData)
                        
                        // Load detailed stock information
                        val detailsResponse = ApiConfig.stockApiService.getStockDetails(symbol)
                        
                        if (detailsResponse.success) {
                            val stockDetails = parseStockDetails(detailsResponse.data)
                            _uiState.value = _uiState.value.copy(stockDetails = stockDetails)
                        }
                        
                        Log.i("StockDetailViewModel", "Background stock data retry successful for $symbol")
                    }
                    
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    
                } catch (e: Exception) {
                    Log.w("StockDetailViewModel", "Background stock data retry failed for $symbol", e)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }
    
    fun retryChartDataInBackground(symbol: String, timeFrame: String) {
        if (!_uiState.value.isLoadingChart) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoadingChart = true)
                
                try {
                    val response = when (timeFrame) {
                        "LIVE" -> ApiConfig.stockApiService.getIntradayData(symbol)
                        "1W" -> ApiConfig.stockApiService.getWeeklyData(symbol)
                        "1M" -> ApiConfig.stockApiService.getMonthlyData(symbol)
                        "3M" -> ApiConfig.stockApiService.getQuarterlyData(symbol)
                        "6M" -> ApiConfig.stockApiService.getHistoricalData(symbol)
                        "Y" -> ApiConfig.stockApiService.getYearlyData(symbol)
                        "All" -> ApiConfig.stockApiService.getHistoricalData(symbol)
                        else -> ApiConfig.stockApiService.getMonthlyData(symbol)
                    }
                    
                    if (response.success && response.prices != null) {
                        val chartData = parseChartDataFromResponse(response)
                        
                        _uiState.value = _uiState.value.copy(
                            chartData = chartData,
                            isLoadingChart = false
                        )
                        
                        Log.i("StockDetailViewModel", "Background chart data retry successful: ${chartData.size} points for $symbol ($timeFrame)")
                    } else {
                        _uiState.value = _uiState.value.copy(isLoadingChart = false)
                        Log.w("StockDetailViewModel", "Background chart data retry failed for $symbol: ${response.error}")
                    }
                    
                } catch (e: Exception) {
                    Log.w("StockDetailViewModel", "Background chart data retry failed for $symbol", e)
                    _uiState.value = _uiState.value.copy(isLoadingChart = false)
                }
            }
        }
    }
    
    // Updated: Only retry holdings based on new strategy
    fun retryUserHoldingInBackground(symbol: String) {
        val currentState = _uiState.value
        
        // Only retry if:
        // 1. Not currently loading holdings
        // 2. User is authenticated
        // 3. Either:
        //    - Haven't made 2 initial requests yet (holdingsRequestCount < 2)
        //    - OR continuously retry flag is enabled (after successful trade)
        if (!currentState.isLoadingHolding && 
            currentState.isAuthenticated && 
            (currentState.holdingsRequestCount < 2 || currentState.shouldContinuouslyRetryHoldings)) {
            
            loadUserHoldingInternal(symbol, isBackgroundRetry = true)
        }
    }
    
    private fun parseStockData(data: Any?, symbol: String): StockData? {
        return try {
            val jsonObject = gson.toJsonTree(data).asJsonObject
            
            StockData(
                symbol = symbol,
                name = jsonObject.get("longName")?.asString 
                    ?: jsonObject.get("shortName")?.asString 
                    ?: jsonObject.get("displayName")?.asString,
                price = jsonObject.get("regularMarketPrice")?.asDouble 
                    ?: jsonObject.get("currentPrice")?.asDouble 
                    ?: jsonObject.get("price")?.asDouble ?: 0.0,
                change = jsonObject.get("regularMarketChange")?.asDouble
                    ?: jsonObject.get("change")?.asDouble,
                changePercent = jsonObject.get("regularMarketChangePercent")?.asDouble
                    ?: jsonObject.get("changePercent")?.asDouble,
                open = jsonObject.get("regularMarketOpen")?.asDouble
                    ?: jsonObject.get("open")?.asDouble,
                high = jsonObject.get("regularMarketDayHigh")?.asDouble
                    ?: jsonObject.get("dayHigh")?.asDouble
                    ?: jsonObject.get("high")?.asDouble,
                low = jsonObject.get("regularMarketDayLow")?.asDouble
                    ?: jsonObject.get("dayLow")?.asDouble
                    ?: jsonObject.get("low")?.asDouble,
                previousClose = jsonObject.get("regularMarketPreviousClose")?.asDouble
                    ?: jsonObject.get("previousClose")?.asDouble,
                volume = jsonObject.get("regularMarketVolume")?.asLong
                    ?: jsonObject.get("volume")?.asLong
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseStockDetails(data: Any?): StockDetails? {
        return try {
            val jsonObject = gson.toJsonTree(data).asJsonObject
            
            StockDetails(
                marketCap = jsonObject.get("marketCap")?.asDouble
                    ?: jsonObject.get("enterpriseValue")?.asDouble,
                peRatio = jsonObject.get("trailingPE")?.asDouble
                    ?: jsonObject.get("forwardPE")?.asDouble,
                eps = jsonObject.get("trailingEps")?.asDouble
                    ?: jsonObject.get("forwardEps")?.asDouble,
                dividend = jsonObject.get("dividendYield")?.asDouble,
                beta = jsonObject.get("beta")?.asDouble,
                description = jsonObject.get("longBusinessSummary")?.asString
                    ?: jsonObject.get("description")?.asString
            )
        } catch (e: Exception) {
            null
        }
    }
    
    // Main function to load historical data
    fun loadHistoricalData(symbol: String, timeFrame: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingChart = true)
            
            try {
                println("📊 Loading chart data for $symbol with timeFrame: $timeFrame")
                
                val response = when (timeFrame) {
                    "LIVE" -> ApiConfig.stockApiService.getIntradayData(symbol)
                    "1W" -> ApiConfig.stockApiService.getWeeklyData(symbol)
                    "1M" -> ApiConfig.stockApiService.getMonthlyData(symbol)
                    "3M" -> ApiConfig.stockApiService.getQuarterlyData(symbol)
                    "6M" -> ApiConfig.stockApiService.getHistoricalData(symbol)
                    "Y" -> ApiConfig.stockApiService.getYearlyData(symbol)
                    "All" -> ApiConfig.stockApiService.getHistoricalData(symbol)
                    else -> ApiConfig.stockApiService.getMonthlyData(symbol)
                }
                
                println("📊 API Response success: ${response.success}")
                println("📊 Prices count: ${response.prices?.size ?: 0}")
                
                if (response.success && response.prices != null) {
                    val chartData = parseChartDataFromResponse(response)
                    
                    _uiState.value = _uiState.value.copy(
                        chartData = chartData,
                        isLoadingChart = false
                    )
                    
                    println("📊 Chart data loaded: ${chartData.size} points for $timeFrame")
                    if (chartData.isNotEmpty()) {
                        println("📊 First point: price=${chartData.first().price}, timestamp=${chartData.first().timestamp}")
                        println("📊 Last point: price=${chartData.last().price}, timestamp=${chartData.last().timestamp}")
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingChart = false,
                        error = response.error ?: "Failed to load chart data"
                    )
                }
                
            } catch (e: Exception) {
                println("❌ Chart data error: ${e.message}")
                e.printStackTrace()
                
                _uiState.value = _uiState.value.copy(
                    isLoadingChart = false,
                    error = "Failed to load chart data: ${e.message}"
                )
            }
        }
    }
    
    // Parse from ChartDataResponse (new API structure)
    private fun parseChartDataFromResponse(response: ChartDataResponse): List<ChartPoint> {
        return try {
            val chartPoints = mutableListOf<ChartPoint>()
            
            response.prices?.forEach { pricePoint ->
                try {
                    val timestamp = parseISOToTimestamp(pricePoint.time)
                    val price = pricePoint.close // Use closing price for the chart
                    val volume = pricePoint.volume
                    
                    chartPoints.add(ChartPoint(timestamp, price, volume))
                    
                } catch (e: Exception) {
                    println("⚠️ Error parsing price point: ${e.message}")
                }
            }
            
            println("✅ Successfully parsed ${chartPoints.size} chart points from response")
            chartPoints.sortedBy { it.timestamp }
            
        } catch (e: Exception) {
            println("❌ Error parsing chart data from response: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
    
    // Enhanced trading functions with proper token management
    fun buyStock(symbol: String, quantity: Int, pricePerShare: Double?) {
        viewModelScope.launch {
            if (!validateAuthentication()) return@launch
            
            _uiState.value = _uiState.value.copy(isTradingLoading = true, error = null)
            
            try {
                val token = authManager.getAccessToken()
                if (token.isEmpty()) {
                    throw Exception("No authentication token")
                }
                
                val response = ApiConfig.stockApiService.buyStock(
                    token = "Bearer $token",
                    request = TradeRequest(
                        symbol = symbol,
                        quantity = quantity,
                        pricePerShare = pricePerShare
                    )
                )
                
                if (response.success) {
                    _uiState.value = _uiState.value.copy(
                        isTradingLoading = false,
                        error = null,
                        successMessage = "Successfully bought $quantity shares of $symbol",
                        shouldContinuouslyRetryHoldings = true // Enable continuous retry after successful buy
                    )
                    
                    // Immediately load holdings after successful trade
                    loadUserHoldingInternal(symbol, isPostTrade = true)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isTradingLoading = false,
                        error = response.error ?: response.message ?: "Failed to buy stock"
                    )
                }
                
            } catch (e: Exception) {
                handleTradingError(e, "buy")
            }
        }
    }
    
    fun sellStock(symbol: String, quantity: Int, pricePerShare: Double?) {
        viewModelScope.launch {
            if (!validateAuthentication()) return@launch
            
            _uiState.value = _uiState.value.copy(isTradingLoading = true, error = null)
            
            try {
                val token = authManager.getAccessToken()
                if (token.isEmpty()) {
                    throw Exception("No authentication token")
                }
                
                val response = ApiConfig.stockApiService.sellStock(
                    token = "Bearer $token",
                    request = TradeRequest(
                        symbol = symbol,
                        quantity = quantity,
                        pricePerShare = pricePerShare
                    )
                )
                
                if (response.success) {
                    _uiState.value = _uiState.value.copy(
                        isTradingLoading = false,
                        error = null,
                        successMessage = "Successfully sold $quantity shares of $symbol",
                        shouldContinuouslyRetryHoldings = true // Enable continuous retry after successful sell
                    )
                    
                    // Immediately load holdings after successful trade
                    loadUserHoldingInternal(symbol, isPostTrade = true)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isTradingLoading = false,
                        error = response.error ?: response.message ?: "Failed to sell stock"
                    )
                }
                
            } catch (e: Exception) {
                handleTradingError(e, "sell")
            }
        }
    }
    
    fun checkAffordability(symbol: String, quantity: Int, pricePerShare: Double?) {
        viewModelScope.launch {
            if (!validateAuthentication()) return@launch
            
            try {
                val token = authManager.getAccessToken()
                if (token.isEmpty()) {
                    throw Exception("No authentication token")
                }
                
                val response = ApiConfig.stockApiService.checkAffordability(
                    token = "Bearer $token",
                    request = AffordabilityRequest(
                        symbol = symbol,
                        quantity = quantity,
                        pricePerShare = pricePerShare
                    )
                )
                
                if (response.success && response.data != null) {
                    val dataJson = gson.toJson(response.data)
                    val affordabilityData = gson.fromJson(dataJson, com.google.gson.JsonObject::class.java)
                    
                    val result = AffordabilityResult(
                        canAfford = affordabilityData.get("canAfford")?.asBoolean ?: false,
                        totalCost = affordabilityData.get("totalCost")?.asDouble ?: 0.0,
                        availableBalance = affordabilityData.get("availableBalance")?.asDouble ?: 0.0,
                        message = affordabilityData.get("message")?.asString
                    )
                    
                    _uiState.value = _uiState.value.copy(affordabilityResult = result)
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to check affordability: ${e.message}"
                )
            }
        }
    }
    
    private suspend fun validateAuthentication(): Boolean {
        return try {
            val isAuthenticated = authManager.isLoggedIn()
            if (!isAuthenticated) {
                _uiState.value = _uiState.value.copy(
                    error = "Please sign in to trade stocks",
                    isAuthenticated = false
                )
                false
            } else {
                true
            }
        } catch (e: Exception) {
            println("⚠️ Error validating authentication: ${e.message}")
            _uiState.value = _uiState.value.copy(
                error = "Authentication error. Please sign in again",
                isAuthenticated = false
            )
            false
        }
    }
    
    private fun handleTradingError(e: Exception, action: String) {
        val errorMessage = when {
            e.message?.contains("401") == true || e.message?.contains("Unauthorized") == true -> {
                "Session expired. Please sign in again"
            }
            e.message?.contains("403") == true -> "You don't have permission to $action stocks"
            e.message?.contains("400") == true -> "Invalid request. Please check your input"
            e.message?.contains("network") == true -> "Network error. Please check your connection"
            e.message?.contains("timeout") == true -> "Request timed out. Please try again"
            else -> "Failed to $action stock: ${e.message}"
        }
        
        _uiState.value = _uiState.value.copy(
            isTradingLoading = false,
            error = errorMessage
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
    
    fun clearAffordabilityResult() {
        _uiState.value = _uiState.value.copy(affordabilityResult = null)
    }

    // Debug function for testing specific endpoint
    fun debugSpecificEndpoint(symbol: String, timeFrame: String = "1M") {
        viewModelScope.launch {
            try {
                println("🧪 Testing specific endpoint for $symbol with timeFrame: $timeFrame")
                
                val response = ApiConfig.stockApiService.getMonthlyData(symbol)
                
                println("✅ Response success: ${response.success}")
                println("📄 Response symbol: ${response.symbol}")
                println("📄 Response period: ${response.period}")
                println("📄 Response prices count: ${response.prices?.size}")
                
                if (response.success && response.prices != null) {
                    println("📊 First price point: ${response.prices.firstOrNull()}")
                    println("📊 Last price point: ${response.prices.lastOrNull()}")
                    
                    // Parse the chart data using the new method
                    val chartData = parseChartDataFromResponse(response)
                    println("📊 Parsed ${chartData.size} chart points")
                    
                    // Update UI with the data for testing
                    _uiState.value = _uiState.value.copy(
                        chartData = chartData,
                        isLoadingChart = false
                    )
                } else {
                    println("❌ No prices data in response")
                }
                
            } catch (e: Exception) {
                println("❌ Debug test failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    // Helper functions for date/time parsing
    private fun parseDateToTimestamp(dateString: String): Long {
        return try {
            // Try multiple date formats
            val formats = listOf(
                SimpleDateFormat("yyyy-MM-dd", Locale.US),
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
                SimpleDateFormat("MM/dd/yyyy", Locale.US),
                SimpleDateFormat("dd/MM/yyyy", Locale.US),
                SimpleDateFormat("yyyy/MM/dd", Locale.US)
            )
            
            for (format in formats) {
                try {
                    return format.parse(dateString)?.time ?: System.currentTimeMillis()
                } catch (e: Exception) {
                    // Continue to next format
                }
            }
            
            // If all formats fail, return current time
            System.currentTimeMillis()
        } catch (e: Exception) {
            println("⚠️ Failed to parse date: $dateString, error: ${e.message}")
            System.currentTimeMillis()
        }
    }
    
    private fun parseISOToTimestamp(isoString: String): Long {
        return try {
            // Handle ISO 8601 format from your API: "2025-06-12T09:55:00.000Z"
            val formats = listOf(
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US),
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
            )
            
            for (format in formats) {
                try {
                    format.timeZone = TimeZone.getTimeZone("UTC")
                    val parsed = format.parse(isoString)?.time
                    if (parsed != null) {
                        println("📅 Parsed timestamp: $isoString -> $parsed")
                        return parsed
                    }
                } catch (e: Exception) {
                    // Continue to next format
                }
            }
            
            // Fallback: try parsing as regular date
            println("⚠️ Could not parse ISO date: $isoString, using fallback")
            parseDateToTimestamp(isoString)
        } catch (e: Exception) {
            println("⚠️ Failed to parse ISO date: $isoString, error: ${e.message}")
            System.currentTimeMillis()
        }
    }

    fun checkWatchlistStatus(symbol: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(watchlistLoading = true)
            
            watchlistRepository.checkWatchlistStatus(symbol)
                .onSuccess { isInWatchlist ->
                    _uiState.value = _uiState.value.copy(
                        isInWatchlist = isInWatchlist,
                        watchlistLoading = false
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isInWatchlist = false,
                        watchlistLoading = false
                    )
                }
        }
    }

    fun addToWatchlist(symbol: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(watchlistLoading = true)
            
            watchlistRepository.addToWatchlist(symbol)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        isInWatchlist = true,
                        watchlistLoading = false,
                        successMessage = response.message ?: "Added to watchlist"
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        watchlistLoading = false,
                        error = exception.message ?: "Failed to add to watchlist"
                    )
                }
        }
    }

    fun removeFromWatchlist(symbol: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(watchlistLoading = true)
            
            watchlistRepository.removeFromWatchlist(symbol)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        isInWatchlist = false,
                        watchlistLoading = false,
                        successMessage = response.message ?: "Removed from watchlist"
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        watchlistLoading = false,
                        error = exception.message ?: "Failed to remove from watchlist"
                    )
                }
        }
    }

    // Updated loadUserHolding to use internal method
    fun loadUserHolding(symbol: String) {
        loadUserHoldingInternal(symbol, isInitialLoad = true)
    }
    
    // Internal method that handles all holdings loading logic
    private fun loadUserHoldingInternal(
        symbol: String, 
        isInitialLoad: Boolean = false,
        isBackgroundRetry: Boolean = false,
        isPostTrade: Boolean = false
    ) {
        viewModelScope.launch {
            if (!validateAuthentication()) return@launch
            
            _uiState.value = _uiState.value.copy(isLoadingHolding = true)
            
            try {
                val token = authManager.getAccessToken()
                if (token.isEmpty()) {
                    throw Exception("No authentication token")
                }
                
                val response = ApiConfig.stockApiService.getUserStockHolding(
                    token = "Bearer $token",
                    symbol = symbol
                )
                
                if (response.success && response.owns == true) {
                    val holding = UserHolding(
                        symbol = response.symbol ?: symbol,
                        owns = response.owns ?: false,
                        quantity = response.quantity ?: 0,
                        averagePrice = response.averagePrice ?: 0.0,
                        investedAmount = response.investedAmount ?: 0.0,
                        currentPrice = response.currentPrice ?: 0.0,
                        currentValue = response.currentValue ?: 0.0,
                        profitLoss = response.profitLoss ?: 0.0,
                        firstBuyDate = response.firstBuyDate
                    )
                    
                    val newRequestCount = if (isInitialLoad || isBackgroundRetry) {
                        _uiState.value.holdingsRequestCount + 1
                    } else {
                        _uiState.value.holdingsRequestCount
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        userHolding = holding,
                        isLoadingHolding = false,
                        holdingsRequestCount = newRequestCount
                    )
                    
                    val logType = when {
                        isInitialLoad -> "Initial"
                        isBackgroundRetry -> "Background retry"
                        isPostTrade -> "Post-trade"
                        else -> "Manual"
                    }
                    Log.i("StockDetailViewModel", "$logType holdings load successful for $symbol (Request #$newRequestCount)")
                    
                } else {
                    val newRequestCount = if (isInitialLoad || isBackgroundRetry) {
                        _uiState.value.holdingsRequestCount + 1
                    } else {
                        _uiState.value.holdingsRequestCount
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        userHolding = null,
                        isLoadingHolding = false,
                        holdingsRequestCount = newRequestCount
                    )
                }
                
            } catch (e: Exception) {
                val newRequestCount = if (isInitialLoad || isBackgroundRetry) {
                    _uiState.value.holdingsRequestCount + 1
                } else {
                    _uiState.value.holdingsRequestCount
                }
                
                println("⚠️ Error loading user holding: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    userHolding = null,
                    isLoadingHolding = false,
                    holdingsRequestCount = newRequestCount
                )
            }
        }
    }
    
    // Function to stop continuous holdings retry (call when user navigates away)
    fun stopContinuousHoldingsRetry() {
        _uiState.value = _uiState.value.copy(shouldContinuouslyRetryHoldings = false)
    }
}
