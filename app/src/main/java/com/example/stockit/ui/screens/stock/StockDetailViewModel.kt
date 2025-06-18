package com.example.stockit.ui.screens.stock

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockit.network.ApiConfig
import com.example.stockit.network.AffordabilityRequest
import com.example.stockit.network.TradeRequest
import com.example.stockit.utils.AuthManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    val affordabilityResult: AffordabilityResult? = null
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

class StockDetailViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(StockDetailUiState())
    val uiState: StateFlow<StockDetailUiState> = _uiState.asStateFlow()
    
    private val gson = Gson()
    private var authManager: AuthManager? = null
    
    fun initializeAuth(context: Context) {
        authManager = AuthManager.getInstance(context)
        _uiState.value = _uiState.value.copy(
            isAuthenticated = authManager?.hasValidToken() == true
        )
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
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load stock data: ${e.message}"
                )
            }
        }
    }
    
    fun loadHistoricalData(symbol: String, timeFrame: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingChart = true)
            
            try {
                val (period, interval) = when (timeFrame) {
                    "LIVE" -> "1d" to "1m"
                    "1W" -> "5d" to "5m"
                    "1M" -> "1mo" to "1d"
                    "3M" -> "3mo" to "1d"
                    "6M" -> "6mo" to "1d"
                    "Y" -> "1y" to "1wk"
                    "All" -> "5y" to "1mo"
                    else -> "1mo" to "1d"
                }
                
                val response = ApiConfig.stockApiService.getHistoricalData(
                    symbol = symbol,
                    period = period,
                    interval = interval
                )
                
                if (response.success) {
                    val chartData = parseChartData(response.data)
                    _uiState.value = _uiState.value.copy(
                        chartData = chartData,
                        isLoadingChart = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingChart = false,
                        error = "Failed to load chart data"
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingChart = false,
                    error = "Failed to load chart data: ${e.message}"
                )
            }
        }
    }
    
    // Enhanced trading functions with proper token management
    fun buyStock(symbol: String, quantity: Int, pricePerShare: Double?) {
        viewModelScope.launch {
            if (!validateAuthentication()) return@launch
            
            _uiState.value = _uiState.value.copy(isTradingLoading = true, error = null)
            
            try {
                val token = authManager?.getBearerToken() ?: throw Exception("No authentication token")
                
                val response = ApiConfig.stockApiService.buyStock(
                    token = token,
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
                        successMessage = "Successfully bought $quantity shares of $symbol"
                    )
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
                val token = authManager?.getBearerToken() ?: throw Exception("No authentication token")
                
                val response = ApiConfig.stockApiService.sellStock(
                    token = token,
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
                        successMessage = "Successfully sold $quantity shares of $symbol"
                    )
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
                val token = authManager?.getBearerToken() ?: throw Exception("No authentication token")
                
                val response = ApiConfig.stockApiService.checkAffordability(
                    token = token,
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
    
    private fun validateAuthentication(): Boolean {
        val isAuthenticated = authManager?.hasValidToken() == true
        if (!isAuthenticated) {
            _uiState.value = _uiState.value.copy(
                error = "Please sign in to trade stocks",
                isAuthenticated = false
            )
        }
        return isAuthenticated
    }
    
    private fun handleTradingError(e: Exception, action: String) {
        val errorMessage = when {
            e.message?.contains("401") == true || e.message?.contains("Unauthorized") == true -> {
                authManager?.logout()
                _uiState.value = _uiState.value.copy(isAuthenticated = false)
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
    
    private fun parseChartData(data: Any?): List<ChartPoint> {
        return try {
            val jsonObject = gson.toJsonTree(data).asJsonObject
            val result = jsonObject.getAsJsonObject("chart")
                ?.getAsJsonArray("result")?.get(0)?.asJsonObject
                
            val timestamps = result?.getAsJsonArray("timestamp")
            val quotes = result?.getAsJsonObject("indicators")
                ?.getAsJsonArray("quote")?.get(0)?.asJsonObject
                
            val closes = quotes?.getAsJsonArray("close")
            val volumes = quotes?.getAsJsonArray("volume")
            
            val chartPoints = mutableListOf<ChartPoint>()
            
            timestamps?.let { ts ->
                for (i in 0 until ts.size()) {
                    val timestamp = ts.get(i).asLong
                    val price = closes?.get(i)?.let { 
                        if (it.isJsonNull) null else it.asDouble 
                    } ?: continue
                    val volume = volumes?.get(i)?.let { 
                        if (it.isJsonNull) null else it.asLong 
                    }
                    
                    chartPoints.add(ChartPoint(timestamp, price, volume))
                }
            }
            
            chartPoints
        } catch (e: Exception) {
            // Fallback: generate dummy data for demonstration
            generateDummyChartData()
        }
    }
    
    private fun generateDummyChartData(): List<ChartPoint> {
        val basePrice = 100.0
        val points = mutableListOf<ChartPoint>()
        val currentTime = System.currentTimeMillis()
        
        for (i in 0 until 30) {
            val timestamp = currentTime - (29 - i) * 24 * 60 * 60 * 1000L
            val price = basePrice + (Math.random() - 0.5) * 20
            val volume = (Math.random() * 1000000).toLong()
            
            points.add(ChartPoint(timestamp, price, volume))
        }
        
        return points
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
}