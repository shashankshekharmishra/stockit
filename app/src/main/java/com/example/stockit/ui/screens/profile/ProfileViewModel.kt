package com.example.stockit.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockit.data.model.PortfolioStock
import com.example.stockit.data.model.RecentTransaction
import com.example.stockit.data.model.UserProfile
import com.example.stockit.network.ApiConfig
import com.example.stockit.utils.AuthManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val userProfile: UserProfile? = null,
    val portfolioStocks: List<PortfolioStock> = emptyList(),
    val recentTransactions: List<RecentTransaction> = emptyList(),
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val gson = Gson()

    init {
        checkAuthStatus()
        if (authManager.isLoggedIn()) {
            loadProfileData()
        }
    }

    private fun checkAuthStatus() {
        _uiState.value = _uiState.value.copy(
            isAuthenticated = authManager.isLoggedIn()
        )
    }

    fun loadProfileData() {
        if (!authManager.isLoggedIn()) {
            _uiState.value = _uiState.value.copy(
                isAuthenticated = false,
                error = "Please log in to view your profile"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val bearerToken = authManager.getBearerToken()
                
                // Load user data with error handling for each API call
                var userProfile: UserProfile? = null
                var portfolioStocks: List<PortfolioStock> = emptyList()
                var recentTransactions: List<RecentTransaction> = emptyList()

                // Try to get wallet data
                try {
                    val walletResponse = ApiConfig.stockApiService.getUserWallet(bearerToken)
                    if (walletResponse.success) {
                        userProfile = createUserProfileFromWallet(walletResponse.data)
                    }
                } catch (e: Exception) {
                    // Continue without wallet data
                }

                // Try to get portfolio data
                try {
                    val portfolioResponse = ApiConfig.stockApiService.getUserPortfolio(bearerToken, updatePrices = "true")
                    if (portfolioResponse.success && portfolioResponse.data != null) {
                        portfolioStocks = parsePortfolioStocks(portfolioResponse.data)
                        // Update user profile with portfolio summary
                        userProfile = updateUserProfileWithPortfolio(userProfile, portfolioResponse.data)
                    }
                } catch (e: Exception) {
                    // Continue without portfolio data
                }

                // Try to get transaction history
                try {
                    val transactionsResponse = ApiConfig.stockApiService.getTransactionHistory(bearerToken, limit = 10)
                    if (transactionsResponse.success && transactionsResponse.data != null) {
                        recentTransactions = parseRecentTransactions(transactionsResponse.data)
                    }
                } catch (e: Exception) {
                    // Continue without transaction data
                }

                // If we don't have a user profile yet, create a basic one
                if (userProfile == null) {
                    userProfile = createBasicUserProfile()
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    userProfile = userProfile,
                    portfolioStocks = portfolioStocks,
                    recentTransactions = recentTransactions,
                    isAuthenticated = true,
                    error = null
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error loading profile: ${e.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    fun refreshProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            loadProfileData()
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    fun logout() {
        authManager.logout()
        _uiState.value = ProfileUiState(isAuthenticated = false)
    }

    private fun createUserProfileFromWallet(walletData: Any?): UserProfile? {
        return try {
            val walletJson = gson.toJsonTree(walletData).asJsonObject
            val balance = walletJson.get("balance")?.asDouble ?: 0.0

            UserProfile(
                id = authManager.getUserId(),
                fullName = authManager.getUserFullName(),
                email = authManager.getUserEmail(),
                balance = balance,
                totalInvested = 0.0,
                totalValue = 0.0,
                profitLoss = 0.0,
                profitLossPercent = 0.0,
                totalStocks = 0,
                joinedDate = null
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun updateUserProfileWithPortfolio(userProfile: UserProfile?, portfolioData: Any?): UserProfile {
        val baseProfile = userProfile ?: createBasicUserProfile()
        
        return try {
            val portfolioJson = gson.toJsonTree(portfolioData).asJsonObject
            val summary = portfolioJson.getAsJsonObject("summary")
            
            if (summary != null) {
                baseProfile.copy(
                    totalInvested = summary.get("totalInvested")?.asDouble ?: baseProfile.totalInvested,
                    totalValue = summary.get("totalValue")?.asDouble ?: baseProfile.totalValue,
                    profitLoss = summary.get("totalProfitLoss")?.asDouble ?: baseProfile.profitLoss,
                    profitLossPercent = summary.get("totalProfitLossPercent")?.asDouble ?: baseProfile.profitLossPercent,
                    totalStocks = summary.get("totalStocks")?.asInt ?: baseProfile.totalStocks
                )
            } else {
                baseProfile
            }
        } catch (e: Exception) {
            baseProfile
        }
    }

    private fun createBasicUserProfile(): UserProfile {
        return UserProfile(
            id = authManager.getUserId(),
            fullName = authManager.getUserFullName(),
            email = authManager.getUserEmail(),
            balance = 0.0,
            totalInvested = 0.0,
            totalValue = 0.0,
            profitLoss = 0.0,
            profitLossPercent = 0.0,
            totalStocks = 0,
            joinedDate = null
        )
    }

    private fun parsePortfolioStocks(data: Any?): List<PortfolioStock> {
        return try {
            val jsonObject = gson.toJsonTree(data).asJsonObject
            val stocksArray = jsonObject.getAsJsonArray("stocks")
            
            stocksArray?.mapNotNull { stockElement ->
                try {
                    val stock = stockElement.asJsonObject
                    val symbol = stock.get("symbol")?.asString
                    if (symbol.isNullOrBlank()) return@mapNotNull null
                    
                    PortfolioStock(
                        symbol = symbol,
                        companyName = stock.get("companyName")?.asString,
                        quantity = stock.get("quantity")?.asInt ?: 0,
                        averagePrice = stock.get("averagePrice")?.asDouble ?: 0.0,
                        currentPrice = stock.get("currentPrice")?.asDouble ?: 0.0,
                        investedAmount = stock.get("investedAmount")?.asDouble ?: 0.0,
                        currentValue = stock.get("currentValue")?.asDouble ?: 0.0,
                        profitLoss = stock.get("profitLoss")?.asDouble ?: 0.0,
                        profitLossPercent = stock.get("profitLossPercent")?.asDouble ?: 0.0,
                        firstBuyDate = stock.get("firstBuyDate")?.asString
                    )
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseRecentTransactions(data: Any?): List<RecentTransaction> {
        return try {
            val jsonObject = gson.toJsonTree(data).asJsonObject
            val transactionsArray = jsonObject.getAsJsonArray("transactions")
            
            transactionsArray?.mapNotNull { transactionElement ->
                try {
                    val transaction = transactionElement.asJsonObject
                    val id = transaction.get("id")?.asString 
                        ?: transaction.get("transactionId")?.asString 
                        ?: System.currentTimeMillis().toString()
                    val symbol = transaction.get("symbol")?.asString
                    val type = transaction.get("type")?.asString
                    
                    if (symbol.isNullOrBlank() || type.isNullOrBlank()) return@mapNotNull null
                    
                    RecentTransaction(
                        id = id,
                        symbol = symbol,
                        type = type,
                        quantity = transaction.get("quantity")?.asInt ?: 0,
                        price = transaction.get("pricePerShare")?.asDouble 
                            ?: transaction.get("price")?.asDouble ?: 0.0,
                        amount = transaction.get("totalAmount")?.asDouble 
                            ?: transaction.get("amount")?.asDouble ?: 0.0,
                        timestamp = transaction.get("timestamp")?.asString 
                            ?: transaction.get("createdAt")?.asString ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}