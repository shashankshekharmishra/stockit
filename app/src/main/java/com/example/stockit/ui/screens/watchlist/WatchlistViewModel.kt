package com.example.stockit.ui.screens.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockit.network.WatchlistStock
import com.example.stockit.data.repository.WatchlistRepository
import com.example.stockit.utils.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WatchlistUiState(
    val watchlistStocks: List<WatchlistStock> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val isAuthenticated: Boolean = false
)

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val watchlistRepository: WatchlistRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(WatchlistUiState())
    val uiState: StateFlow<WatchlistUiState> = _uiState.asStateFlow()

    init {
        // Add debug logging
        viewModelScope.launch {
            val hasToken = authManager.getAccessToken().isNotEmpty()
            val isLoggedIn = authManager.isLoggedIn()
            println("DEBUG: AuthManager - hasToken: $hasToken, isLoggedIn: $isLoggedIn")
            println("DEBUG: Access token: ${authManager.getAccessToken().take(10)}...")
            
            _uiState.update { it.copy(isAuthenticated = isLoggedIn) }
            
            if (isLoggedIn) {
                loadWatchlist()
            }
            
            authManager.isLoggedInState.collect { authState ->
                println("DEBUG: Auth state changed to: $authState")
                _uiState.update { it.copy(isAuthenticated = authState) }
                if (authState) {
                    loadWatchlist()
                } else {
                    _uiState.update { it.copy(watchlistStocks = emptyList()) }
                }
            }
        }
    }

    fun loadWatchlist() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            watchlistRepository.getUserWatchlist()
                .onSuccess { stocks ->
                    _uiState.update { 
                        it.copy(
                            watchlistStocks = stocks,
                            isLoading = false,
                            error = null
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load watchlist"
                        )
                    }
                }
        }
    }

    fun refreshWatchlist() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            
            watchlistRepository.getUserWatchlist()
                .onSuccess { stocks ->
                    _uiState.update { 
                        it.copy(
                            watchlistStocks = stocks,
                            isRefreshing = false,
                            error = null,
                            successMessage = "Watchlist updated"
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update { 
                        it.copy(
                            isRefreshing = false,
                            error = exception.message ?: "Failed to refresh watchlist"
                        )
                    }
                }
        }
    }

    fun removeFromWatchlist(symbol: String) {
        viewModelScope.launch {
            watchlistRepository.removeFromWatchlist(symbol)
                .onSuccess { response ->
                    _uiState.update { 
                        it.copy(
                            successMessage = response.message ?: "Removed from watchlist",
                            watchlistStocks = it.watchlistStocks.filter { stock -> 
                                stock.symbol != symbol.uppercase() 
                            }
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update { 
                        it.copy(error = exception.message ?: "Failed to remove from watchlist")
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}