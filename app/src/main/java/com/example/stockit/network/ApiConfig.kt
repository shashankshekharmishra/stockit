package com.example.stockit.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

object ApiConfig {
    private const val BASE_URL = "https://test.vardhin.tech/"
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val stockApiService: StockApiService = retrofit.create(StockApiService::class.java)
    val authApiService: AuthApiService = retrofit.create(AuthApiService::class.java)
}

// Stock API Service Interface
interface StockApiService {
    // Basic stock data
    @GET("api/stock/{symbol}")
    suspend fun getStock(@Path("symbol") symbol: String, @Query("cache") cache: String = "true"): StockResponse
    
    @GET("api/stock/{symbol}/quote")
    suspend fun getStockQuote(@Path("symbol") symbol: String, @Query("cache") cache: String = "true"): StockResponse
    
    @GET("api/stock/{symbol}/details")
    suspend fun getStockDetails(@Path("symbol") symbol: String, @Query("cache") cache: String = "true"): StockResponse
    
    // Historical data endpoints
    @GET("api/stock/{symbol}/intraday")
    suspend fun getIntradayData(@Path("symbol") symbol: String, @Query("cache") cache: String = "true"): StockResponse
    
    @GET("api/stock/{symbol}/weekly")
    suspend fun getWeeklyData(@Path("symbol") symbol: String, @Query("cache") cache: String = "true"): StockResponse
    
    @GET("api/stock/{symbol}/monthly")
    suspend fun getMonthlyData(@Path("symbol") symbol: String, @Query("cache") cache: String = "true"): StockResponse
    
    @GET("api/stock/{symbol}/quarterly")
    suspend fun getQuarterlyData(@Path("symbol") symbol: String, @Query("cache") cache: String = "true"): StockResponse
    
    @GET("api/stock/{symbol}/yearly")
    suspend fun getYearlyData(@Path("symbol") symbol: String, @Query("cache") cache: String = "true"): StockResponse
    
    @GET("api/stock/{symbol}/historical")
    suspend fun getHistoricalData(
        @Path("symbol") symbol: String,
        @Query("period") period: String = "1mo",
        @Query("interval") interval: String = "1d",
        @Query("cache") cache: String = "true"
    ): StockResponse
    
    // Search endpoints
    @GET("api/search")
    suspend fun searchStocks(
        @Query("q") query: String,
        @Query("limit") limit: Int = 10,
        @Query("online") online: String = "true"
    ): StockSearchResponse
    
    @GET("api/search/online")
    suspend fun searchOnlineStocks(
        @Query("q") query: String,
        @Query("limit") limit: Int = 10
    ): StockSearchResponse
    
    @GET("api/search-quote")
    suspend fun searchAndQuote(
        @Query("q") query: String,
        @Query("cache") cache: String = "true"
    ): StockResponse
    
    // Popular stocks
    @GET("api/popular")
    suspend fun getPopularStocks(
        @Query("category") category: String = "nifty50",
        @Query("limit") limit: Int = 10
    ): PopularStocksResponse
    
    // Compare stocks
    @POST("api/stocks/compare")
    suspend fun compareStocks(@Body request: CompareStocksRequest): StockCompareResponse
    
    // User trading endpoints (require authentication)
    @GET("api/user/wallet")
    suspend fun getUserWallet(@Header("Authorization") token: String): WalletResponse
    
    @POST("api/user/wallet/deposit")
    suspend fun depositToWallet(
        @Header("Authorization") token: String,
        @Body request: WalletTransactionRequest
    ): WalletResponse
    
    @POST("api/user/wallet/withdraw")
    suspend fun withdrawFromWallet(
        @Header("Authorization") token: String,
        @Body request: WalletTransactionRequest
    ): WalletResponse
    
    @GET("api/user/wallet/transactions")
    suspend fun getWalletTransactions(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): WalletTransactionsResponse
    
    @POST("api/user/stocks/buy")
    suspend fun buyStock(
        @Header("Authorization") token: String,
        @Body request: TradeRequest
    ): TradeResponse
    
    @POST("api/user/stocks/sell")
    suspend fun sellStock(
        @Header("Authorization") token: String,
        @Body request: TradeRequest
    ): TradeResponse
    
    @GET("api/user/portfolio")
    suspend fun getUserPortfolio(
        @Header("Authorization") token: String,
        @Query("updatePrices") updatePrices: String = "true"
    ): PortfolioResponse
    
    @GET("api/user/transactions")
    suspend fun getTransactionHistory(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): TransactionHistoryResponse
    
    @GET("api/user/summary")
    suspend fun getUserSummary(@Header("Authorization") token: String): UserSummaryResponse
    
    @POST("api/user/stocks/check-affordability")
    suspend fun checkAffordability(
        @Header("Authorization") token: String,
        @Body request: AffordabilityRequest
    ): AffordabilityResponse
}

// Auth API Service Interface
interface AuthApiService {
    @POST("api/auth/signin-simple")
    suspend fun signIn(@Body request: SignInRequest): AuthResponse
    
    @POST("api/auth/signup-simple")
    suspend fun signUp(@Body request: SignUpRequest): AuthResponse
}

// Request Data Classes
data class SignInRequest(
    val email: String,
    val password: String
)

data class SignUpRequest(
    val fullName: String,
    val email: String,
    val password: String,
    val confirmPassword: String
)

data class CompareStocksRequest(
    val symbols: List<String>,
    val cache: Boolean = true
)

data class WalletTransactionRequest(
    val amount: Double,
    val description: String? = null
)

data class TradeRequest(
    val symbol: String,
    val quantity: Int,
    val pricePerShare: Double? = null
)

data class AffordabilityRequest(
    val symbol: String,
    val quantity: Int,
    val pricePerShare: Double? = null
)

// Response Data Classes
data class AuthResponse(
    val success: Boolean,
    val message: String? = null,
    val access_token: String? = null,
    val refresh_token: String? = null,
    val user_id: String? = null,
    val user_email: String? = null,
    val user_fullName: String? = null,
    val starting_balance: Double? = null,
    val timestamp: String? = null
)

data class StockResponse(
    val success: Boolean,
    val data: Any? = null,
    val error: String? = null,
    val timestamp: String? = null
)

data class StockSearchResponse(
    val success: Boolean,
    val data: Any? = null,
    val timestamp: String? = null
)

data class PopularStocksResponse(
    val success: Boolean,
    val data: Any? = null,
    val timestamp: String? = null
)

data class StockCompareResponse(
    val success: Boolean,
    val data: Any? = null,
    val symbolsCompared: Int? = null,
    val timestamp: String? = null
)

data class WalletResponse(
    val success: Boolean,
    val data: Any? = null,
    val message: String? = null,
    val error: String? = null,
    val timestamp: String? = null
)

data class WalletTransactionsResponse(
    val success: Boolean,
    val data: Any? = null,
    val pagination: Any? = null,
    val timestamp: String? = null
)

data class TradeResponse(
    val success: Boolean,
    val data: Any? = null,
    val message: String? = null,
    val error: String? = null,
    val timestamp: String? = null
)

data class PortfolioResponse(
    val success: Boolean,
    val data: Any? = null,
    val timestamp: String? = null
)

data class TransactionHistoryResponse(
    val success: Boolean,
    val data: Any? = null,
    val pagination: Any? = null,
    val timestamp: String? = null
)

data class UserSummaryResponse(
    val success: Boolean,
    val data: Any? = null,
    val timestamp: String? = null
)

data class AffordabilityResponse(
    val success: Boolean,
    val data: Any? = null,
    val error: String? = null,
    val timestamp: String? = null
)