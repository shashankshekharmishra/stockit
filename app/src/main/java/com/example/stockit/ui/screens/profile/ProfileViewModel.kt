package com.example.stockit.ui.screens.profile

import android.util.Log
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
                    Log.d("ProfileViewModel", "Wallet Response: $walletResponse")
                    // Check if the response indicates success AND has valid data
                    if (walletResponse.success && walletResponse.balance != null) {
                        userProfile = createUserProfileFromWallet(walletResponse)
                        Log.d("ProfileViewModel", "Wallet data parsed: balance=${userProfile?.balance}")
                    } else {
                        Log.w("ProfileViewModel", "Wallet API returned success=false or missing data")
                    }
                } catch (e: Exception) {
                    Log.e("ProfileViewModel", "Failed to get wallet data", e)
                }

                // Try to get portfolio data
                try {
                    val portfolioResponse = ApiConfig.stockApiService.getUserPortfolio(bearerToken, updatePrices = "true")
                    Log.d("ProfileViewModel", "Portfolio Response: $portfolioResponse")
                    // Check if the response indicates success AND has holdings data
                    if (portfolioResponse.success && portfolioResponse.holdings != null) {
                        portfolioStocks = parsePortfolioStocks(portfolioResponse)
                        // Update user profile with portfolio summary
                        userProfile = updateUserProfileWithPortfolio(userProfile, portfolioResponse)
                        Log.d("ProfileViewModel", "Portfolio data parsed: ${portfolioStocks.size} stocks")
                    } else {
                        Log.w("ProfileViewModel", "Portfolio API returned success=false or no holdings")
                    }
                } catch (e: Exception) {
                    Log.e("ProfileViewModel", "Failed to get portfolio data", e)
                }

                // Try to get transaction history
                try {
                    val transactionsResponse = ApiConfig.stockApiService.getTransactionHistory(bearerToken, limit = 10)
                    Log.d("ProfileViewModel", "Transactions Response: $transactionsResponse")
                    // Check if the response indicates success AND has transactions data
                    if (transactionsResponse.success && transactionsResponse.transactions != null) {
                        recentTransactions = parseRecentTransactions(transactionsResponse)
                        Log.d("ProfileViewModel", "Transaction data parsed: ${recentTransactions.size} transactions")
                    } else {
                        Log.w("ProfileViewModel", "Transactions API returned success=false or no transactions")
                    }
                } catch (e: Exception) {
                    Log.e("ProfileViewModel", "Failed to get transaction data", e)
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

                Log.d("ProfileViewModel", "Profile loaded successfully: user=${userProfile != null}, portfolio=${portfolioStocks.size}, transactions=${recentTransactions.size}")

            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading profile", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error loading profile: ${e.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    // Add the missing refreshProfile method
    fun refreshProfile() {
        if (!authManager.isLoggedIn()) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            
            try {
                val bearerToken = authManager.getBearerToken()
                
                // Load user data with error handling for each API call
                var userProfile: UserProfile? = null
                var portfolioStocks: List<PortfolioStock> = emptyList()
                var recentTransactions: List<RecentTransaction> = emptyList()

                // Try to get wallet data
                try {
                    val walletResponse = ApiConfig.stockApiService.getUserWallet(bearerToken)
                    Log.d("ProfileViewModel", "Refresh Wallet Response: $walletResponse")
                    if (walletResponse.success && walletResponse.balance != null) {
                        userProfile = createUserProfileFromWallet(walletResponse)
                        Log.d("ProfileViewModel", "Refresh wallet data parsed: balance=${userProfile?.balance}")
                    }
                } catch (e: Exception) {
                    Log.e("ProfileViewModel", "Failed to refresh wallet data", e)
                }

                // Try to get portfolio data
                try {
                    val portfolioResponse = ApiConfig.stockApiService.getUserPortfolio(bearerToken, updatePrices = "true")
                    Log.d("ProfileViewModel", "Refresh Portfolio Response: $portfolioResponse")
                    if (portfolioResponse.success && portfolioResponse.holdings != null) {
                        portfolioStocks = parsePortfolioStocks(portfolioResponse)
                        userProfile = updateUserProfileWithPortfolio(userProfile, portfolioResponse)
                        Log.d("ProfileViewModel", "Refresh portfolio data parsed: ${portfolioStocks.size} stocks")
                    }
                } catch (e: Exception) {
                    Log.e("ProfileViewModel", "Failed to refresh portfolio data", e)
                }

                // Try to get transaction history
                try {
                    val transactionsResponse = ApiConfig.stockApiService.getTransactionHistory(bearerToken, limit = 10)
                    Log.d("ProfileViewModel", "Refresh Transactions Response: $transactionsResponse")
                    if (transactionsResponse.success && transactionsResponse.transactions != null) {
                        recentTransactions = parseRecentTransactions(transactionsResponse)
                        Log.d("ProfileViewModel", "Refresh transaction data parsed: ${recentTransactions.size} transactions")
                    }
                } catch (e: Exception) {
                    Log.e("ProfileViewModel", "Failed to refresh transaction data", e)
                }

                // If we don't have a user profile yet, create a basic one
                if (userProfile == null) {
                    userProfile = createBasicUserProfile()
                }

                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    userProfile = userProfile,
                    portfolioStocks = portfolioStocks,
                    recentTransactions = recentTransactions,
                    error = null
                )

                Log.d("ProfileViewModel", "Profile refresh completed: user=${userProfile != null}, portfolio=${portfolioStocks.size}, transactions=${recentTransactions.size}")

            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error refreshing profile", e)
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = "Error refreshing profile: ${e.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    // Background retry method - fix the condition warnings
    fun retryProfileInBackground() {
        if (!_uiState.value.isLoading && !_uiState.value.isRefreshing) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true)
                try {
                    val bearerToken = authManager.getBearerToken()
                    
                    var userProfile: UserProfile? = null
                    var portfolioStocks: List<PortfolioStock> = emptyList()
                    var recentTransactions: List<RecentTransaction> = emptyList()
                    var hasData = false

                    // Try to get wallet data
                    try {
                        val walletResponse = ApiConfig.stockApiService.getUserWallet(bearerToken)
                        if (walletResponse.success && walletResponse.balance != null) {
                            userProfile = createUserProfileFromWallet(walletResponse)
                            hasData = true
                        }
                    } catch (e: Exception) {
                        // Continue without wallet data
                    }

                    // Try to get portfolio data
                    try {
                        val portfolioResponse = ApiConfig.stockApiService.getUserPortfolio(bearerToken, updatePrices = "true")
                        if (portfolioResponse.success && portfolioResponse.holdings != null) {
                            portfolioStocks = parsePortfolioStocks(portfolioResponse)
                            userProfile = updateUserProfileWithPortfolio(userProfile, portfolioResponse)
                            hasData = true
                        }
                    } catch (e: Exception) {
                        // Continue without portfolio data
                    }

                    // Try to get transaction history
                    try {
                        val transactionsResponse = ApiConfig.stockApiService.getTransactionHistory(bearerToken, limit = 10)
                        if (transactionsResponse.success && transactionsResponse.transactions != null) {
                            recentTransactions = parseRecentTransactions(transactionsResponse)
                            hasData = true
                        }
                    } catch (e: Exception) {
                        // Continue without transaction data
                    }

                    // If we don't have a user profile yet, create a basic one
                    if (userProfile == null) {
                        userProfile = createBasicUserProfile()
                        hasData = true // Basic profile counts as having data
                    }

                    if (hasData) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            userProfile = userProfile,
                            portfolioStocks = portfolioStocks,
                            recentTransactions = recentTransactions,
                            error = null
                        )
                        Log.i("ProfileViewModel", "Background profile retry successful: wallet=${userProfile != null}, portfolio=${portfolioStocks.size} stocks, transactions=${recentTransactions.size}")
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        Log.w("ProfileViewModel", "Background profile retry failed: no data received")
                    }

                } catch (e: Exception) {
                    Log.w("ProfileViewModel", "Background profile retry failed", e)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }

    fun logout() {
        authManager.logout()
        _uiState.value = ProfileUiState(isAuthenticated = false)
    }

    // Fixed: Parse wallet response correctly - handle the WalletResponse structure
    private fun createUserProfileFromWallet(walletResponse: Any?): UserProfile? {
        return try {
            val responseJson = gson.toJsonTree(walletResponse).asJsonObject
            Log.d("ProfileViewModel", "Full Wallet Response: $responseJson")
            
            // For WalletResponse, the data is at the root level, not nested under "data"
            val balance = responseJson.get("balance")?.asDouble ?: 0.0
            val totalInvested = responseJson.get("totalInvested")?.asDouble ?: 0.0
            val totalCurrentValue = responseJson.get("totalCurrentValue")?.asDouble ?: 0.0
            val totalProfitLoss = responseJson.get("totalProfitLoss")?.asDouble ?: 0.0
            val totalProfitLossPercent = responseJson.get("totalProfitLossPercent")?.asDouble ?: 0.0

            val profile = UserProfile(
                id = authManager.getUserId(),
                fullName = authManager.getUserFullName(),
                email = authManager.getUserEmail(),
                balance = balance,
                totalInvested = totalInvested,
                totalValue = totalCurrentValue,
                profitLoss = totalProfitLoss,
                profitLossPercent = totalProfitLossPercent,
                totalStocks = 0,
                joinedDate = null
            )
            
            Log.d("ProfileViewModel", "Created profile from wallet: balance=$balance, invested=$totalInvested")
            profile
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Failed to parse wallet response", e)
            null
        }
    }

    // Simplified portfolio parsing since we know the exact structure
    private fun parsePortfolioStocks(data: Any?): List<PortfolioStock> {
        return try {
            val portfolioResponse = data as? com.example.stockit.network.PortfolioResponse
            if (portfolioResponse?.holdings != null) {
                Log.d("ProfileViewModel", "Portfolio holdings found: ${portfolioResponse.holdings.size}")
                portfolioResponse.holdings.map { holding ->
                    PortfolioStock(
                        symbol = holding.symbol,
                        companyName = holding.companyName,
                        quantity = holding.quantity,
                        averagePrice = holding.avgPrice,
                        currentPrice = holding.currentPrice,
                        investedAmount = holding.investedAmount,
                        currentValue = holding.currentValue,
                        profitLoss = holding.pnl,
                        profitLossPercent = holding.pnlPercent,
                        firstBuyDate = holding.firstBuyDate
                    )
                }.also {
                    Log.d("ProfileViewModel", "Successfully parsed ${it.size} portfolio stocks")
                }
            } else {
                Log.w("ProfileViewModel", "No holdings found in portfolio response")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Failed to parse portfolio stocks", e)
            emptyList()
        }
    }

    // Simplified transaction parsing since we know the exact structure
    private fun parseRecentTransactions(data: Any?): List<RecentTransaction> {
        return try {
            val transactionResponse = data as? com.example.stockit.network.TransactionHistoryResponse
            if (transactionResponse?.transactions != null) {
                Log.d("ProfileViewModel", "Transactions found: ${transactionResponse.transactions.size}")
                transactionResponse.transactions.map { transaction ->
                    RecentTransaction(
                        id = transaction.id.toString(),
                        symbol = transaction.symbol,
                        type = transaction.transactionType,
                        quantity = transaction.quantity,
                        price = transaction.price,
                        amount = transaction.totalAmount,
                        timestamp = transaction.transactionDate
                    )
                }.also {
                    Log.d("ProfileViewModel", "Successfully parsed ${it.size} transactions")
                }
            } else {
                Log.w("ProfileViewModel", "No transactions found in response")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Failed to parse transactions", e)
            emptyList()
        }
    }

    // Update the portfolio profile update method to use direct response structure
    private fun updateUserProfileWithPortfolio(userProfile: UserProfile?, portfolioData: Any?): UserProfile {
        val baseProfile = userProfile ?: createBasicUserProfile()
        
        return try {
            val portfolioResponse = portfolioData as? com.example.stockit.network.PortfolioResponse
            if (portfolioResponse != null) {
                Log.d("ProfileViewModel", "Updating profile with portfolio data")
                
                val updatedProfile = baseProfile.copy(
                    totalInvested = portfolioResponse.totalInvested ?: baseProfile.totalInvested,
                    totalValue = portfolioResponse.totalCurrentValue ?: baseProfile.totalValue,
                    profitLoss = portfolioResponse.totalPnL ?: baseProfile.profitLoss,
                    profitLossPercent = portfolioResponse.totalPnLPercent ?: baseProfile.profitLossPercent,
                    totalStocks = portfolioResponse.totalHoldings ?: baseProfile.totalStocks
                )
                
                Log.d("ProfileViewModel", "Updated profile: holdings=${updatedProfile.totalStocks}, invested=${updatedProfile.totalInvested}")
                updatedProfile
            } else {
                Log.w("ProfileViewModel", "Portfolio data is not in expected format")
                baseProfile
            }
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Failed to update profile with portfolio", e)
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

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}