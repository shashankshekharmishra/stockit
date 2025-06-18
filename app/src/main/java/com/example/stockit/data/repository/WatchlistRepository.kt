package com.example.stockit.data.repository

import com.example.stockit.data.model.*
import com.example.stockit.network.ApiConfig
import com.example.stockit.utils.AuthManager
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject

@ActivityRetainedScoped
class WatchlistRepository @Inject constructor(
    private val authManager: AuthManager
) {
    private val apiService = ApiConfig.stockApiService

    suspend fun getUserWatchlist(): Result<List<WatchlistStock>> {
        return try {
            val token = authManager.getAccessToken()
            if (token.isBlank()) {
                return Result.failure(Exception("Not authenticated"))
            }

            val response = apiService.getUserWatchlist("Bearer $token")
            if (response.success && response.stocks != null) {
                // Convert network model to data model - network response already has the correct structure
                val watchlistStocks = response.stocks.map { networkStock ->
                    WatchlistStock(
                        symbol = networkStock.symbol,
                        companyName = networkStock.companyName,
                        price = networkStock.price,
                        change = networkStock.change,
                        changePercent = networkStock.changePercent,
                        high = networkStock.high,
                        low = networkStock.low,
                        volume = networkStock.volume,
                        addedAt = networkStock.addedAt,
                        inWatchlist = networkStock.inWatchlist
                    )
                }
                Result.success(watchlistStocks)
            } else {
                Result.failure(Exception(response.error ?: "Failed to fetch watchlist"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addToWatchlist(symbol: String): Result<WatchlistActionResponse> {
        return try {
            val token = authManager.getAccessToken()
            if (token.isBlank()) {
                return Result.failure(Exception("Not authenticated"))
            }

            val response = apiService.addToWatchlist(
                "Bearer $token", 
                com.example.stockit.network.AddToWatchlistRequest(symbol.uppercase())
            )
            if (response.success) {
                // Convert network model to data model
                val actionResponse = WatchlistActionResponse(
                    success = response.success,
                    added = response.added,
                    removed = response.removed,
                    symbol = response.symbol,
                    companyName = response.companyName,
                    message = response.message,
                    error = response.error,
                    inWatchlist = response.inWatchlist,
                    timestamp = response.timestamp
                )
                Result.success(actionResponse)
            } else {
                Result.failure(Exception(response.error ?: "Failed to add to watchlist"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFromWatchlist(symbol: String): Result<WatchlistActionResponse> {
        return try {
            val token = authManager.getAccessToken()
            if (token.isBlank()) {
                return Result.failure(Exception("Not authenticated"))
            }

            val response = apiService.removeFromWatchlist("Bearer $token", symbol.uppercase())
            if (response.success) {
                // Convert network model to data model
                val actionResponse = WatchlistActionResponse(
                    success = response.success,
                    added = response.added,
                    removed = response.removed,
                    symbol = response.symbol,
                    companyName = response.companyName,
                    message = response.message,
                    error = response.error,
                    inWatchlist = response.inWatchlist,
                    timestamp = response.timestamp
                )
                Result.success(actionResponse)
            } else {
                Result.failure(Exception(response.error ?: "Failed to remove from watchlist"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkWatchlistStatus(symbol: String): Result<Boolean> {
        return try {
            val token = authManager.getAccessToken()
            if (token.isBlank()) {
                return Result.failure(Exception("Not authenticated"))
            }

            val response = apiService.checkWatchlistStatus("Bearer $token", symbol.uppercase())
            if (response.success) {
                Result.success(response.inWatchlist ?: false)
            } else {
                Result.success(false) // If error, assume not in watchlist
            }
        } catch (e: Exception) {
            Result.success(false) // If error, assume not in watchlist
        }
    }
}