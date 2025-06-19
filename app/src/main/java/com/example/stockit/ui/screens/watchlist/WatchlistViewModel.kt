package com.example.stockit.ui.screens.watchlist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockit.data.repository.WatchlistRepository
import com.example.stockit.network.WatchlistStock
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
        viewModelScope.launch {
            // Initialize with current auth state
            val isLoggedIn = authManager.isLoggedIn()
            _uiState.update { it.copy(isAuthenticated = isLoggedIn) }
            
            if (isLoggedIn) {
                loadWatchlist()
            }
            
            // Listen for auth state changes
            authManager.isLoggedInState.collect { authState ->
                println("DEBUG: Auth state changed to: $authState")
                _uiState.update { it.copy(isAuthenticated = authState) }
                
                if (authState) {
                    // User just logged in, load watchlist
                    loadWatchlist()
                } else {
                    // User logged out, clear watchlist
                    _uiState.update { 
                        it.copy(
                            watchlistStocks = emptyList(),
                            isLoading = false,
                            isRefreshing = false,
                            error = null
                        )
                    }
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

    fun retryWatchlistInBackground() {
        if (!_uiState.value.isLoading && !_uiState.value.isRefreshing) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                try {
                    watchlistRepository.getUserWatchlist()
                        .onSuccess { stocks ->
                            _uiState.update { 
                                it.copy(
                                    watchlistStocks = stocks,
                                    isLoading = false,
                                    error = null
                                )
                            }
                            Log.i("WatchlistViewModel", "Background watchlist retry successful: ${stocks.size} stocks loaded")
                        }
                        .onFailure { exception ->
                            _uiState.update { it.copy(isLoading = false) }
                            Log.w("WatchlistViewModel", "Background watchlist retry failed", exception)
                        }
                } catch (e: Exception) {
                    Log.w("WatchlistViewModel", "Background watchlist retry failed", e)
                    _uiState.update { it.copy(isLoading = false) }
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